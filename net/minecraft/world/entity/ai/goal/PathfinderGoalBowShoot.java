package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.monster.EntityMonster;
import net.minecraft.world.entity.monster.IRangedEntity;
import net.minecraft.world.entity.projectile.ProjectileHelper;
import net.minecraft.world.item.ItemBow;
import net.minecraft.world.item.Items;

public class PathfinderGoalBowShoot<T extends EntityMonster & IRangedEntity> extends PathfinderGoal {
    private final T mob;
    private final double speedModifier;
    private int attackIntervalMin;
    private final float attackRadiusSqr;
    private int attackTime = -1;
    private int seeTime;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public PathfinderGoalBowShoot(T actor, double speed, int attackInterval, float range) {
        this.mob = actor;
        this.speedModifier = speed;
        this.attackIntervalMin = attackInterval;
        this.attackRadiusSqr = range * range;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
    }

    public void setMinAttackInterval(int attackInterval) {
        this.attackIntervalMin = attackInterval;
    }

    @Override
    public boolean canUse() {
        return this.mob.getGoalTarget() == null ? false : this.isHoldingBow();
    }

    protected boolean isHoldingBow() {
        return this.mob.isHolding(Items.BOW);
    }

    @Override
    public boolean canContinueToUse() {
        return (this.canUse() || !this.mob.getNavigation().isDone()) && this.isHoldingBow();
    }

    @Override
    public void start() {
        super.start();
        this.mob.setAggressive(true);
    }

    @Override
    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.seeTime = 0;
        this.attackTime = -1;
        this.mob.clearActiveItem();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        EntityLiving livingEntity = this.mob.getGoalTarget();
        if (livingEntity != null) {
            double d = this.mob.distanceToSqr(livingEntity.locX(), livingEntity.locY(), livingEntity.locZ());
            boolean bl = this.mob.getEntitySenses().hasLineOfSight(livingEntity);
            boolean bl2 = this.seeTime > 0;
            if (bl != bl2) {
                this.seeTime = 0;
            }

            if (bl) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }

            if (!(d > (double)this.attackRadiusSqr) && this.seeTime >= 20) {
                this.mob.getNavigation().stop();
                ++this.strafingTime;
            } else {
                this.mob.getNavigation().moveTo(livingEntity, this.speedModifier);
                this.strafingTime = -1;
            }

            if (this.strafingTime >= 20) {
                if ((double)this.mob.getRandom().nextFloat() < 0.3D) {
                    this.strafingClockwise = !this.strafingClockwise;
                }

                if ((double)this.mob.getRandom().nextFloat() < 0.3D) {
                    this.strafingBackwards = !this.strafingBackwards;
                }

                this.strafingTime = 0;
            }

            if (this.strafingTime > -1) {
                if (d > (double)(this.attackRadiusSqr * 0.75F)) {
                    this.strafingBackwards = false;
                } else if (d < (double)(this.attackRadiusSqr * 0.25F)) {
                    this.strafingBackwards = true;
                }

                this.mob.getControllerMove().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
                this.mob.lookAt(livingEntity, 30.0F, 30.0F);
            } else {
                this.mob.getControllerLook().setLookAt(livingEntity, 30.0F, 30.0F);
            }

            if (this.mob.isHandRaised()) {
                if (!bl && this.seeTime < -60) {
                    this.mob.clearActiveItem();
                } else if (bl) {
                    int i = this.mob.getTicksUsingItem();
                    if (i >= 20) {
                        this.mob.clearActiveItem();
                        this.mob.performRangedAttack(livingEntity, ItemBow.getPowerForTime(i));
                        this.attackTime = this.attackIntervalMin;
                    }
                }
            } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
                this.mob.startUsingItem(ProjectileHelper.getWeaponHoldingHand(this.mob, Items.BOW));
            }

        }
    }
}
