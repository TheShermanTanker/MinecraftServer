package net.minecraft.gametest.framework;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.entity.TileEntityStructure;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.phys.AxisAlignedBB;

public class GameTestHarnessInfo {
    private final GameTestHarnessTestFunction testFunction;
    @Nullable
    private BlockPosition structureBlockPos;
    private final WorldServer level;
    private final Collection<GameTestHarnessListener> listeners = Lists.newArrayList();
    private final int timeoutTicks;
    private final Collection<GameTestHarnessSequence> sequences = Lists.newCopyOnWriteArrayList();
    private final Object2LongMap<Runnable> runAtTickTimeMap = new Object2LongOpenHashMap<>();
    private long startTick;
    private long tickCount;
    private boolean started;
    private final Stopwatch timer = Stopwatch.createUnstarted();
    private boolean done;
    private final EnumBlockRotation rotation;
    @Nullable
    private Throwable error;
    @Nullable
    private TileEntityStructure structureBlockEntity;

    public GameTestHarnessInfo(GameTestHarnessTestFunction testFunction, EnumBlockRotation rotation, WorldServer world) {
        this.testFunction = testFunction;
        this.level = world;
        this.timeoutTicks = testFunction.getMaxTicks();
        this.rotation = testFunction.getRotation().getRotated(rotation);
    }

    void setStructureBlockPos(BlockPosition pos) {
        this.structureBlockPos = pos;
    }

    void startExecution() {
        this.startTick = this.level.getTime() + 1L + this.testFunction.getSetupTicks();
        this.timer.start();
    }

    public void tick() {
        if (!this.isDone()) {
            this.tickInternal();
            if (this.isDone()) {
                if (this.error != null) {
                    this.listeners.forEach((listener) -> {
                        listener.testFailed(this);
                    });
                } else {
                    this.listeners.forEach((listener) -> {
                        listener.testPassed(this);
                    });
                }
            }

        }
    }

    private void tickInternal() {
        this.tickCount = this.level.getTime() - this.startTick;
        if (this.tickCount >= 0L) {
            if (this.tickCount == 0L) {
                this.startTest();
            }

            ObjectIterator<Entry<Runnable>> objectIterator = this.runAtTickTimeMap.object2LongEntrySet().iterator();

            while(objectIterator.hasNext()) {
                Entry<Runnable> entry = objectIterator.next();
                if (entry.getLongValue() <= this.tickCount) {
                    try {
                        entry.getKey().run();
                    } catch (Exception var4) {
                        this.fail(var4);
                    }

                    objectIterator.remove();
                }
            }

            if (this.tickCount > (long)this.timeoutTicks) {
                if (this.sequences.isEmpty()) {
                    this.fail(new GameTestHarnessTimeout("Didn't succeed or fail within " + this.testFunction.getMaxTicks() + " ticks"));
                } else {
                    this.sequences.forEach((runner) -> {
                        runner.tickAndFailIfNotComplete(this.tickCount);
                    });
                    if (this.error == null) {
                        this.fail(new GameTestHarnessTimeout("No sequences finished"));
                    }
                }
            } else {
                this.sequences.forEach((runner) -> {
                    runner.tickAndContinue(this.tickCount);
                });
            }

        }
    }

    private void startTest() {
        if (this.started) {
            throw new IllegalStateException("Test already started");
        } else {
            this.started = true;

            try {
                this.testFunction.run(new GameTestHarnessHelper(this));
            } catch (Exception var2) {
                this.fail(var2);
            }

        }
    }

    public void setRunAtTickTime(long tick, Runnable runnable) {
        this.runAtTickTimeMap.put(runnable, tick);
    }

    public String getTestName() {
        return this.testFunction.getTestName();
    }

    public BlockPosition getStructureBlockPos() {
        return this.structureBlockPos;
    }

    @Nullable
    public BaseBlockPosition getStructureSize() {
        TileEntityStructure structureBlockEntity = this.getStructureBlockEntity();
        return structureBlockEntity == null ? null : structureBlockEntity.getStructureSize();
    }

    @Nullable
    public AxisAlignedBB getStructureBounds() {
        TileEntityStructure structureBlockEntity = this.getStructureBlockEntity();
        return structureBlockEntity == null ? null : GameTestHarnessStructures.getStructureBounds(structureBlockEntity);
    }

    @Nullable
    private TileEntityStructure getStructureBlockEntity() {
        return (TileEntityStructure)this.level.getTileEntity(this.structureBlockPos);
    }

    public WorldServer getLevel() {
        return this.level;
    }

    public boolean hasSucceeded() {
        return this.done && this.error == null;
    }

    public boolean hasFailed() {
        return this.error != null;
    }

    public boolean hasStarted() {
        return this.started;
    }

    public boolean isDone() {
        return this.done;
    }

    public long getRunTime() {
        return this.timer.elapsed(TimeUnit.MILLISECONDS);
    }

    private void finish() {
        if (!this.done) {
            this.done = true;
            this.timer.stop();
        }

    }

    public void succeed() {
        if (this.error == null) {
            this.finish();
        }

    }

    public void fail(Throwable throwable) {
        this.error = throwable;
        this.finish();
    }

    @Nullable
    public Throwable getError() {
        return this.error;
    }

    @Override
    public String toString() {
        return this.getTestName();
    }

    public void addListener(GameTestHarnessListener listener) {
        this.listeners.add(listener);
    }

    public void spawnStructure(BlockPosition pos, int i) {
        this.structureBlockEntity = GameTestHarnessStructures.spawnStructure(this.getStructureName(), pos, this.getRotation(), i, this.level, false);
        this.structureBlockPos = this.structureBlockEntity.getPosition();
        this.structureBlockEntity.setStructureName(this.getTestName());
        GameTestHarnessStructures.addCommandBlockAndButtonToStartTest(this.structureBlockPos, new BlockPosition(1, 0, -1), this.getRotation(), this.level);
        this.listeners.forEach((listener) -> {
            listener.testStructureLoaded(this);
        });
    }

    public void clearStructure() {
        if (this.structureBlockEntity == null) {
            throw new IllegalStateException("Expected structure to be initialized, but it was null");
        } else {
            StructureBoundingBox boundingBox = GameTestHarnessStructures.getStructureBoundingBox(this.structureBlockEntity);
            GameTestHarnessStructures.clearSpaceForStructure(boundingBox, this.structureBlockPos.getY(), this.level);
        }
    }

    long getTick() {
        return this.tickCount;
    }

    GameTestHarnessSequence createSequence() {
        GameTestHarnessSequence gameTestSequence = new GameTestHarnessSequence(this);
        this.sequences.add(gameTestSequence);
        return gameTestSequence;
    }

    public boolean isRequired() {
        return this.testFunction.isRequired();
    }

    public boolean isOptional() {
        return !this.testFunction.isRequired();
    }

    public String getStructureName() {
        return this.testFunction.getStructureName();
    }

    public EnumBlockRotation getRotation() {
        return this.rotation;
    }

    public GameTestHarnessTestFunction getTestFunction() {
        return this.testFunction;
    }

    public int getTimeoutTicks() {
        return this.timeoutTicks;
    }

    public boolean isFlaky() {
        return this.testFunction.isFlaky();
    }

    public int maxAttempts() {
        return this.testFunction.getMaxAttempts();
    }

    public int requiredSuccesses() {
        return this.testFunction.getRequiredSuccesses();
    }
}
