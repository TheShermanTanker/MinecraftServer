package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.phys.Vec3D;

public class BehavorMove extends Behavior<EntityInsentient> {
    private static final int MAX_COOLDOWN_BEFORE_RETRYING = 40;
    private int remainingCooldown;
    @Nullable
    private PathEntity path;
    @Nullable
    private BlockPosition lastTargetPos;
    private float speedModifier;

    public BehavorMove() {
        this(150, 250);
    }

    public BehavorMove(int minRunTime, int maxRunTime) {
        super(ImmutableMap.of(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED, MemoryModuleType.PATH, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_PRESENT), minRunTime, maxRunTime);
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityInsentient entity) {
        if (this.remainingCooldown > 0) {
            --this.remainingCooldown;
            return false;
        } else {
            BehaviorController<?> brain = entity.getBehaviorController();
            MemoryTarget walkTarget = brain.getMemory(MemoryModuleType.WALK_TARGET).get();
            boolean bl = this.reachedTarget(entity, walkTarget);
            if (!bl && this.tryComputePath(entity, walkTarget, world.getTime())) {
                this.lastTargetPos = walkTarget.getTarget().currentBlockPosition();
                return true;
            } else {
                brain.removeMemory(MemoryModuleType.WALK_TARGET);
                if (bl) {
                    brain.removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                }

                return false;
            }
        }
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityInsentient mob, long l) {
        if (this.path != null && this.lastTargetPos != null) {
            Optional<MemoryTarget> optional = mob.getBehaviorController().getMemory(MemoryModuleType.WALK_TARGET);
            NavigationAbstract pathNavigation = mob.getNavigation();
            return !pathNavigation.isDone() && optional.isPresent() && !this.reachedTarget(mob, optional.get());
        } else {
            return false;
        }
    }

    @Override
    protected void stop(WorldServer serverLevel, EntityInsentient mob, long l) {
        if (mob.getBehaviorController().hasMemory(MemoryModuleType.WALK_TARGET) && !this.reachedTarget(mob, mob.getBehaviorController().getMemory(MemoryModuleType.WALK_TARGET).get()) && mob.getNavigation().isStuck()) {
            this.remainingCooldown = serverLevel.getRandom().nextInt(40);
        }

        mob.getNavigation().stop();
        mob.getBehaviorController().removeMemory(MemoryModuleType.WALK_TARGET);
        mob.getBehaviorController().removeMemory(MemoryModuleType.PATH);
        this.path = null;
    }

    @Override
    protected void start(WorldServer serverLevel, EntityInsentient mob, long l) {
        mob.getBehaviorController().setMemory(MemoryModuleType.PATH, this.path);
        mob.getNavigation().moveTo(this.path, (double)this.speedModifier);
    }

    @Override
    protected void tick(WorldServer world, EntityInsentient entity, long time) {
        PathEntity path = entity.getNavigation().getPath();
        BehaviorController<?> brain = entity.getBehaviorController();
        if (this.path != path) {
            this.path = path;
            brain.setMemory(MemoryModuleType.PATH, path);
        }

        if (path != null && this.lastTargetPos != null) {
            MemoryTarget walkTarget = brain.getMemory(MemoryModuleType.WALK_TARGET).get();
            if (walkTarget.getTarget().currentBlockPosition().distSqr(this.lastTargetPos) > 4.0D && this.tryComputePath(entity, walkTarget, world.getTime())) {
                this.lastTargetPos = walkTarget.getTarget().currentBlockPosition();
                this.start(world, entity, time);
            }

        }
    }

    private boolean tryComputePath(EntityInsentient entity, MemoryTarget walkTarget, long time) {
        BlockPosition blockPos = walkTarget.getTarget().currentBlockPosition();
        this.path = entity.getNavigation().createPath(blockPos, 0);
        this.speedModifier = walkTarget.getSpeedModifier();
        BehaviorController<?> brain = entity.getBehaviorController();
        if (this.reachedTarget(entity, walkTarget)) {
            brain.removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        } else {
            boolean bl = this.path != null && this.path.canReach();
            if (bl) {
                brain.removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            } else if (!brain.hasMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)) {
                brain.setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, time);
            }

            if (this.path != null) {
                return true;
            }

            Vec3D vec3 = DefaultRandomPos.getPosTowards((EntityCreature)entity, 10, 7, Vec3D.atBottomCenterOf(blockPos), (double)((float)Math.PI / 2F));
            if (vec3 != null) {
                this.path = entity.getNavigation().createPath(vec3.x, vec3.y, vec3.z, 0);
                return this.path != null;
            }
        }

        return false;
    }

    private boolean reachedTarget(EntityInsentient entity, MemoryTarget walkTarget) {
        return walkTarget.getTarget().currentBlockPosition().distManhattan(entity.getChunkCoordinates()) <= walkTarget.getCloseEnoughDist();
    }
}
