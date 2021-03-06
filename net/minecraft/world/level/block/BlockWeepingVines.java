package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockWeepingVines extends BlockGrowingTop {
    protected static final VoxelShape SHAPE = Block.box(4.0D, 9.0D, 4.0D, 12.0D, 16.0D, 12.0D);

    public BlockWeepingVines(BlockBase.Info settings) {
        super(settings, EnumDirection.DOWN, SHAPE, false, 0.1D);
    }

    @Override
    protected int getBlocksToGrowWhenBonemealed(Random random) {
        return BlockNetherVinesUtil.getBlocksToGrowWhenBonemealed(random);
    }

    @Override
    protected Block getBodyBlock() {
        return Blocks.WEEPING_VINES_PLANT;
    }

    @Override
    protected boolean canGrowInto(IBlockData state) {
        return BlockNetherVinesUtil.isValidGrowthState(state);
    }
}
