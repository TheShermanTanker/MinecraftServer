package net.minecraft.world.level.block.piston;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.EnumPistonReaction;

public class PistonExtendsChecker {
    public static final int MAX_PUSH_DEPTH = 12;
    private final World level;
    private final BlockPosition pistonPos;
    private final boolean extending;
    private final BlockPosition startPos;
    private final EnumDirection pushDirection;
    private final List<BlockPosition> toPush = Lists.newArrayList();
    private final List<BlockPosition> toDestroy = Lists.newArrayList();
    private final EnumDirection pistonDirection;

    public PistonExtendsChecker(World world, BlockPosition pos, EnumDirection dir, boolean retracted) {
        this.level = world;
        this.pistonPos = pos;
        this.pistonDirection = dir;
        this.extending = retracted;
        if (retracted) {
            this.pushDirection = dir;
            this.startPos = pos.relative(dir);
        } else {
            this.pushDirection = dir.opposite();
            this.startPos = pos.relative(dir, 2);
        }

    }

    public boolean resolve() {
        this.toPush.clear();
        this.toDestroy.clear();
        IBlockData blockState = this.level.getType(this.startPos);
        if (!BlockPiston.isPushable(blockState, this.level, this.startPos, this.pushDirection, false, this.pistonDirection)) {
            if (this.extending && blockState.getPushReaction() == EnumPistonReaction.DESTROY) {
                this.toDestroy.add(this.startPos);
                return true;
            } else {
                return false;
            }
        } else if (!this.addBlockLine(this.startPos, this.pushDirection)) {
            return false;
        } else {
            for(int i = 0; i < this.toPush.size(); ++i) {
                BlockPosition blockPos = this.toPush.get(i);
                if (isSticky(this.level.getType(blockPos)) && !this.addBranchingBlocks(blockPos)) {
                    return false;
                }
            }

            return true;
        }
    }

    private static boolean isSticky(IBlockData state) {
        return state.is(Blocks.SLIME_BLOCK) || state.is(Blocks.HONEY_BLOCK);
    }

    private static boolean canStickToEachOther(IBlockData state, IBlockData adjacentState) {
        if (state.is(Blocks.HONEY_BLOCK) && adjacentState.is(Blocks.SLIME_BLOCK)) {
            return false;
        } else if (state.is(Blocks.SLIME_BLOCK) && adjacentState.is(Blocks.HONEY_BLOCK)) {
            return false;
        } else {
            return isSticky(state) || isSticky(adjacentState);
        }
    }

    private boolean addBlockLine(BlockPosition pos, EnumDirection dir) {
        IBlockData blockState = this.level.getType(pos);
        if (blockState.isAir()) {
            return true;
        } else if (!BlockPiston.isPushable(blockState, this.level, pos, this.pushDirection, false, dir)) {
            return true;
        } else if (pos.equals(this.pistonPos)) {
            return true;
        } else if (this.toPush.contains(pos)) {
            return true;
        } else {
            int i = 1;
            if (i + this.toPush.size() > 12) {
                return false;
            } else {
                while(isSticky(blockState)) {
                    BlockPosition blockPos = pos.relative(this.pushDirection.opposite(), i);
                    IBlockData blockState2 = blockState;
                    blockState = this.level.getType(blockPos);
                    if (blockState.isAir() || !canStickToEachOther(blockState2, blockState) || !BlockPiston.isPushable(blockState, this.level, blockPos, this.pushDirection, false, this.pushDirection.opposite()) || blockPos.equals(this.pistonPos)) {
                        break;
                    }

                    ++i;
                    if (i + this.toPush.size() > 12) {
                        return false;
                    }
                }

                int j = 0;

                for(int k = i - 1; k >= 0; --k) {
                    this.toPush.add(pos.relative(this.pushDirection.opposite(), k));
                    ++j;
                }

                int l = 1;

                while(true) {
                    BlockPosition blockPos2 = pos.relative(this.pushDirection, l);
                    int m = this.toPush.indexOf(blockPos2);
                    if (m > -1) {
                        this.reorderListAtCollision(j, m);

                        for(int n = 0; n <= m + j; ++n) {
                            BlockPosition blockPos3 = this.toPush.get(n);
                            if (isSticky(this.level.getType(blockPos3)) && !this.addBranchingBlocks(blockPos3)) {
                                return false;
                            }
                        }

                        return true;
                    }

                    blockState = this.level.getType(blockPos2);
                    if (blockState.isAir()) {
                        return true;
                    }

                    if (!BlockPiston.isPushable(blockState, this.level, blockPos2, this.pushDirection, true, this.pushDirection) || blockPos2.equals(this.pistonPos)) {
                        return false;
                    }

                    if (blockState.getPushReaction() == EnumPistonReaction.DESTROY) {
                        this.toDestroy.add(blockPos2);
                        return true;
                    }

                    if (this.toPush.size() >= 12) {
                        return false;
                    }

                    this.toPush.add(blockPos2);
                    ++j;
                    ++l;
                }
            }
        }
    }

    private void reorderListAtCollision(int from, int to) {
        List<BlockPosition> list = Lists.newArrayList();
        List<BlockPosition> list2 = Lists.newArrayList();
        List<BlockPosition> list3 = Lists.newArrayList();
        list.addAll(this.toPush.subList(0, to));
        list2.addAll(this.toPush.subList(this.toPush.size() - from, this.toPush.size()));
        list3.addAll(this.toPush.subList(to, this.toPush.size() - from));
        this.toPush.clear();
        this.toPush.addAll(list);
        this.toPush.addAll(list2);
        this.toPush.addAll(list3);
    }

    private boolean addBranchingBlocks(BlockPosition pos) {
        IBlockData blockState = this.level.getType(pos);

        for(EnumDirection direction : EnumDirection.values()) {
            if (direction.getAxis() != this.pushDirection.getAxis()) {
                BlockPosition blockPos = pos.relative(direction);
                IBlockData blockState2 = this.level.getType(blockPos);
                if (canStickToEachOther(blockState2, blockState) && !this.addBlockLine(blockPos, direction)) {
                    return false;
                }
            }
        }

        return true;
    }

    public EnumDirection getPushDirection() {
        return this.pushDirection;
    }

    public List<BlockPosition> getMovedBlocks() {
        return this.toPush;
    }

    public List<BlockPosition> getBrokenBlocks() {
        return this.toDestroy;
    }
}
