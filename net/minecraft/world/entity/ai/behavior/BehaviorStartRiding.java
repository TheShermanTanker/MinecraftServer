package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorStartRiding<E extends EntityLiving> extends Behavior<E> {
    private static final int CLOSE_ENOUGH_TO_START_RIDING_DIST = 1;
    private final float speedModifier;

    public BehaviorStartRiding(float speed) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.RIDE_TARGET, MemoryStatus.VALUE_PRESENT));
        this.speedModifier = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, E entity) {
        return !entity.isPassenger();
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        if (this.isCloseEnoughToStartRiding(entity)) {
            entity.startRiding(this.getRidableEntity(entity));
        } else {
            BehaviorUtil.setWalkAndLookTargetMemories(entity, this.getRidableEntity(entity), this.speedModifier, 1);
        }

    }

    private boolean isCloseEnoughToStartRiding(E entity) {
        return this.getRidableEntity(entity).closerThan(entity, 1.0D);
    }

    private Entity getRidableEntity(E entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.RIDE_TARGET).get();
    }
}
