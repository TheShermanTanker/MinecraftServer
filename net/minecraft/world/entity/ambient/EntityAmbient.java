package net.minecraft.world.entity.ambient;

import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;

public abstract class EntityAmbient extends EntityInsentient {
    protected EntityAmbient(EntityTypes<? extends EntityAmbient> type, World world) {
        super(type, world);
    }

    @Override
    public boolean canBeLeashed(EntityHuman player) {
        return false;
    }
}
