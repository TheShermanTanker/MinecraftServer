package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;

public enum EnumBlockSupport {
    FULL {
        @Override
        public boolean isSupporting(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
            return Block.isFaceFull(state.getBlockSupportShape(world, pos), direction);
        }
    },
    CENTER {
        private final int CENTER_SUPPORT_WIDTH = 1;
        private final VoxelShape CENTER_SUPPORT_SHAPE = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 10.0D, 9.0D);

        @Override
        public boolean isSupporting(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
            return !VoxelShapes.joinIsNotEmpty(state.getBlockSupportShape(world, pos).getFaceShape(direction), this.CENTER_SUPPORT_SHAPE, OperatorBoolean.ONLY_SECOND);
        }
    },
    RIGID {
        private final int RIGID_SUPPORT_WIDTH = 2;
        private final VoxelShape RIGID_SUPPORT_SHAPE = VoxelShapes.join(VoxelShapes.block(), Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D), OperatorBoolean.ONLY_FIRST);

        @Override
        public boolean isSupporting(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction) {
            return !VoxelShapes.joinIsNotEmpty(state.getBlockSupportShape(world, pos).getFaceShape(direction), this.RIGID_SUPPORT_SHAPE, OperatorBoolean.ONLY_SECOND);
        }
    };

    public abstract boolean isSupporting(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection direction);
}
