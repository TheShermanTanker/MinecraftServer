package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class BehaviorRaid extends Behavior<EntityLiving> {
    public BehaviorRaid() {
        super(ImmutableMap.of());
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityLiving entity) {
        return world.random.nextInt(20) == 0;
    }

    @Override
    protected void start(WorldServer world, EntityLiving entity, long time) {
        BehaviorController<?> brain = entity.getBehaviorController();
        Raid raid = world.getRaidAt(entity.getChunkCoordinates());
        if (raid != null) {
            if (raid.hasFirstWaveSpawned() && !raid.isBetweenWaves()) {
                brain.setDefaultActivity(Activity.RAID);
                brain.setActiveActivityIfPossible(Activity.RAID);
            } else {
                brain.setDefaultActivity(Activity.PRE_RAID);
                brain.setActiveActivityIfPossible(Activity.PRE_RAID);
            }
        }

    }
}
