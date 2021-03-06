package net.minecraft.world.entity.ai.goal;

import java.util.function.Predicate;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;

public class PathfinderGoalBreakDoor extends PathfinderGoalDoorInteract {
    private static final int DEFAULT_DOOR_BREAK_TIME = 240;
    private final Predicate<EnumDifficulty> validDifficulties;
    protected int breakTime;
    protected int lastBreakProgress = -1;
    protected int doorBreakTime = -1;

    public PathfinderGoalBreakDoor(EntityInsentient mob, Predicate<EnumDifficulty> difficultySufficientPredicate) {
        super(mob);
        this.validDifficulties = difficultySufficientPredicate;
    }

    public PathfinderGoalBreakDoor(EntityInsentient mob, int maxProgress, Predicate<EnumDifficulty> difficultySufficientPredicate) {
        this(mob, difficultySufficientPredicate);
        this.doorBreakTime = maxProgress;
    }

    protected int getDoorBreakTime() {
        return Math.max(240, this.doorBreakTime);
    }

    @Override
    public boolean canUse() {
        if (!super.canUse()) {
            return false;
        } else if (!this.mob.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        } else {
            return this.isValidDifficulty(this.mob.level.getDifficulty()) && !this.isOpen();
        }
    }

    @Override
    public void start() {
        super.start();
        this.breakTime = 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.breakTime <= this.getDoorBreakTime() && !this.isOpen() && this.doorPos.closerThan(this.mob.getPositionVector(), 2.0D) && this.isValidDifficulty(this.mob.level.getDifficulty());
    }

    @Override
    public void stop() {
        super.stop();
        this.mob.level.destroyBlockProgress(this.mob.getId(), this.doorPos, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.mob.getRandom().nextInt(20) == 0) {
            this.mob.level.triggerEffect(1019, this.doorPos, 0);
            if (!this.mob.swinging) {
                this.mob.swingHand(this.mob.getRaisedHand());
            }
        }

        ++this.breakTime;
        int i = (int)((float)this.breakTime / (float)this.getDoorBreakTime() * 10.0F);
        if (i != this.lastBreakProgress) {
            this.mob.level.destroyBlockProgress(this.mob.getId(), this.doorPos, i);
            this.lastBreakProgress = i;
        }

        if (this.breakTime == this.getDoorBreakTime() && this.isValidDifficulty(this.mob.level.getDifficulty())) {
            this.mob.level.removeBlock(this.doorPos, false);
            this.mob.level.triggerEffect(1021, this.doorPos, 0);
            this.mob.level.triggerEffect(2001, this.doorPos, Block.getCombinedId(this.mob.level.getType(this.doorPos)));
        }

    }

    private boolean isValidDifficulty(EnumDifficulty difficulty) {
        return this.validDifficulties.test(difficulty);
    }
}
