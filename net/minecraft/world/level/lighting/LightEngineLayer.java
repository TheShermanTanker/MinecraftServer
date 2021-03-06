package net.minecraft.world.level.lighting;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.SectionPosition;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ILightAccess;
import net.minecraft.world.level.chunk.NibbleArray;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.apache.commons.lang3.mutable.MutableInt;

public abstract class LightEngineLayer<M extends LightEngineStorageArray<M>, S extends LightEngineStorage<M>> extends LightEngineGraph implements LightEngineLayerEventListener {
    public static final long SELF_SOURCE = Long.MAX_VALUE;
    private static final EnumDirection[] DIRECTIONS = EnumDirection.values();
    protected final ILightAccess chunkSource;
    protected final EnumSkyBlock layer;
    protected final S storage;
    private boolean runningLightUpdates;
    protected final BlockPosition.MutableBlockPosition pos = new BlockPosition.MutableBlockPosition();
    private static final int CACHE_SIZE = 2;
    private final long[] lastChunkPos = new long[2];
    private final IBlockAccess[] lastChunk = new IBlockAccess[2];

    public LightEngineLayer(ILightAccess chunkProvider, EnumSkyBlock type, S lightStorage) {
        super(16, 256, 8192);
        this.chunkSource = chunkProvider;
        this.layer = type;
        this.storage = lightStorage;
        this.clearCache();
    }

    @Override
    protected void checkNode(long id) {
        this.storage.runAllUpdates();
        if (this.storage.storingLightForSection(SectionPosition.blockToSection(id))) {
            super.checkNode(id);
        }

    }

    @Nullable
    private IBlockAccess getChunk(int chunkX, int chunkZ) {
        long l = ChunkCoordIntPair.pair(chunkX, chunkZ);

        for(int i = 0; i < 2; ++i) {
            if (l == this.lastChunkPos[i]) {
                return this.lastChunk[i];
            }
        }

        IBlockAccess blockGetter = this.chunkSource.getChunkForLighting(chunkX, chunkZ);

        for(int j = 1; j > 0; --j) {
            this.lastChunkPos[j] = this.lastChunkPos[j - 1];
            this.lastChunk[j] = this.lastChunk[j - 1];
        }

        this.lastChunkPos[0] = l;
        this.lastChunk[0] = blockGetter;
        return blockGetter;
    }

    private void clearCache() {
        Arrays.fill(this.lastChunkPos, ChunkCoordIntPair.INVALID_CHUNK_POS);
        Arrays.fill(this.lastChunk, (Object)null);
    }

    protected IBlockData getStateAndOpacity(long pos, @Nullable MutableInt mutableInt) {
        if (pos == Long.MAX_VALUE) {
            if (mutableInt != null) {
                mutableInt.setValue(0);
            }

            return Blocks.AIR.getBlockData();
        } else {
            int i = SectionPosition.blockToSectionCoord(BlockPosition.getX(pos));
            int j = SectionPosition.blockToSectionCoord(BlockPosition.getZ(pos));
            IBlockAccess blockGetter = this.getChunk(i, j);
            if (blockGetter == null) {
                if (mutableInt != null) {
                    mutableInt.setValue(16);
                }

                return Blocks.BEDROCK.getBlockData();
            } else {
                this.pos.set(pos);
                IBlockData blockState = blockGetter.getType(this.pos);
                boolean bl = blockState.canOcclude() && blockState.useShapeForLightOcclusion();
                if (mutableInt != null) {
                    mutableInt.setValue(blockState.getLightBlock(this.chunkSource.getWorld(), this.pos));
                }

                return bl ? blockState : Blocks.AIR.getBlockData();
            }
        }
    }

    protected VoxelShape getShape(IBlockData world, long pos, EnumDirection facing) {
        return world.canOcclude() ? world.getFaceOcclusionShape(this.chunkSource.getWorld(), this.pos.set(pos), facing) : VoxelShapes.empty();
    }

    public static int getLightBlockInto(IBlockAccess world, IBlockData state1, BlockPosition pos1, IBlockData state2, BlockPosition pos2, EnumDirection direction, int opacity2) {
        boolean bl = state1.canOcclude() && state1.useShapeForLightOcclusion();
        boolean bl2 = state2.canOcclude() && state2.useShapeForLightOcclusion();
        if (!bl && !bl2) {
            return opacity2;
        } else {
            VoxelShape voxelShape = bl ? state1.getOcclusionShape(world, pos1) : VoxelShapes.empty();
            VoxelShape voxelShape2 = bl2 ? state2.getOcclusionShape(world, pos2) : VoxelShapes.empty();
            return VoxelShapes.mergedFaceOccludes(voxelShape, voxelShape2, direction) ? 16 : opacity2;
        }
    }

    @Override
    protected boolean isSource(long id) {
        return id == Long.MAX_VALUE;
    }

    @Override
    protected int getComputedLevel(long id, long excludedId, int maxLevel) {
        return 0;
    }

    @Override
    protected int getLevel(long id) {
        return id == Long.MAX_VALUE ? 0 : 15 - this.storage.getStoredLevel(id);
    }

    protected int getLevel(NibbleArray section, long blockPos) {
        return 15 - section.get(SectionPosition.sectionRelative(BlockPosition.getX(blockPos)), SectionPosition.sectionRelative(BlockPosition.getY(blockPos)), SectionPosition.sectionRelative(BlockPosition.getZ(blockPos)));
    }

    @Override
    protected void setLevel(long id, int level) {
        this.storage.setStoredLevel(id, Math.min(15, 15 - level));
    }

    @Override
    protected int computeLevelFromNeighbor(long sourceId, long targetId, int level) {
        return 0;
    }

    @Override
    public boolean hasLightWork() {
        return this.hasWork() || this.storage.hasWork() || this.storage.hasInconsistencies();
    }

    @Override
    public int runUpdates(int i, boolean doSkylight, boolean skipEdgeLightPropagation) {
        if (!this.runningLightUpdates) {
            if (this.storage.hasWork()) {
                i = this.storage.runUpdates(i);
                if (i == 0) {
                    return i;
                }
            }

            this.storage.markNewInconsistencies(this, doSkylight, skipEdgeLightPropagation);
        }

        this.runningLightUpdates = true;
        if (this.hasWork()) {
            i = this.runUpdates(i);
            this.clearCache();
            if (i == 0) {
                return i;
            }
        }

        this.runningLightUpdates = false;
        this.storage.swapSectionMap();
        return i;
    }

    protected void queueSectionData(long sectionPos, @Nullable NibbleArray lightArray, boolean nonEdge) {
        this.storage.queueSectionData(sectionPos, lightArray, nonEdge);
    }

    @Nullable
    @Override
    public NibbleArray getDataLayerData(SectionPosition pos) {
        return this.storage.getDataLayerData(pos.asLong());
    }

    @Override
    public int getLightValue(BlockPosition pos) {
        return this.storage.getLightValue(pos.asLong());
    }

    public String getDebugData(long sectionPos) {
        return "" + this.storage.getLevel(sectionPos);
    }

    @Override
    public void checkBlock(BlockPosition pos) {
        long l = pos.asLong();
        this.checkNode(l);

        for(EnumDirection direction : DIRECTIONS) {
            this.checkNode(BlockPosition.offset(l, direction));
        }

    }

    @Override
    public void onBlockEmissionIncrease(BlockPosition pos, int level) {
    }

    @Override
    public void updateSectionStatus(SectionPosition pos, boolean notReady) {
        this.storage.updateSectionStatus(pos.asLong(), notReady);
    }

    @Override
    public void enableLightSources(ChunkCoordIntPair pos, boolean retainData) {
        long l = SectionPosition.getZeroNode(SectionPosition.asLong(pos.x, 0, pos.z));
        this.storage.enableLightSources(l, retainData);
    }

    public void retainData(ChunkCoordIntPair pos, boolean retainData) {
        long l = SectionPosition.getZeroNode(SectionPosition.asLong(pos.x, 0, pos.z));
        this.storage.retainData(l, retainData);
    }
}
