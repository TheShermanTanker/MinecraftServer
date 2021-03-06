package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;

public abstract class PathfinderGoalWaterJumpAbstract extends PathfinderGoal {
    public PathfinderGoalWaterJumpAbstract() {
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.JUMP));
    }
}
