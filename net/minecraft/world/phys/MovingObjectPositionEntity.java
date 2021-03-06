package net.minecraft.world.phys;

import net.minecraft.world.entity.Entity;

public class MovingObjectPositionEntity extends MovingObjectPosition {
    private final Entity entity;

    public MovingObjectPositionEntity(Entity entity) {
        this(entity, entity.getPositionVector());
    }

    public MovingObjectPositionEntity(Entity entity, Vec3D pos) {
        super(pos);
        this.entity = entity;
    }

    public Entity getEntity() {
        return this.entity;
    }

    @Override
    public MovingObjectPosition.EnumMovingObjectType getType() {
        return MovingObjectPosition.EnumMovingObjectType.ENTITY;
    }
}
