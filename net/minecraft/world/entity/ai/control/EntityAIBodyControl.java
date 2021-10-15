package net.minecraft.world.entity.ai.control;

import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityInsentient;

public class EntityAIBodyControl implements Control {
    private final EntityInsentient mob;
    private static final int HEAD_STABLE_ANGLE = 15;
    private static final int DELAY_UNTIL_STARTING_TO_FACE_FORWARD = 10;
    private static final int HOW_LONG_IT_TAKES_TO_FACE_FORWARD = 10;
    private int headStableTime;
    private float lastStableYHeadRot;

    public EntityAIBodyControl(EntityInsentient entity) {
        this.mob = entity;
    }

    public void clientTick() {
        if (this.isMoving()) {
            this.mob.yBodyRot = this.mob.getYRot();
            this.rotateHeadIfNecessary();
            this.lastStableYHeadRot = this.mob.yHeadRot;
            this.headStableTime = 0;
        } else {
            if (this.notCarryingMobPassengers()) {
                if (Math.abs(this.mob.yHeadRot - this.lastStableYHeadRot) > 15.0F) {
                    this.headStableTime = 0;
                    this.lastStableYHeadRot = this.mob.yHeadRot;
                    this.rotateBodyIfNecessary();
                } else {
                    ++this.headStableTime;
                    if (this.headStableTime > 10) {
                        this.rotateHeadTowardsFront();
                    }
                }
            }

        }
    }

    private void rotateBodyIfNecessary() {
        this.mob.yBodyRot = MathHelper.rotateIfNecessary(this.mob.yBodyRot, this.mob.yHeadRot, (float)this.mob.getMaxHeadYRot());
    }

    private void rotateHeadIfNecessary() {
        this.mob.yHeadRot = MathHelper.rotateIfNecessary(this.mob.yHeadRot, this.mob.yBodyRot, (float)this.mob.getMaxHeadYRot());
    }

    private void rotateHeadTowardsFront() {
        int i = this.headStableTime - 10;
        float f = MathHelper.clamp((float)i / 10.0F, 0.0F, 1.0F);
        float g = (float)this.mob.getMaxHeadYRot() * (1.0F - f);
        this.mob.yBodyRot = MathHelper.rotateIfNecessary(this.mob.yBodyRot, this.mob.yHeadRot, g);
    }

    private boolean notCarryingMobPassengers() {
        return !(this.mob.getFirstPassenger() instanceof EntityInsentient);
    }

    private boolean isMoving() {
        double d = this.mob.locX() - this.mob.xo;
        double e = this.mob.locZ() - this.mob.zo;
        return d * d + e * e > (double)2.5000003E-7F;
    }
}
