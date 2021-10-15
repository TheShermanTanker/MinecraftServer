package net.minecraft.world.level.levelgen.surfacebuilders;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class WorldGenSurfaceBasaltDeltas extends WorldGenSurfaceNetherAbstract {
    private static final IBlockData BASALT = Blocks.BASALT.getBlockData();
    private static final IBlockData BLACKSTONE = Blocks.BLACKSTONE.getBlockData();
    private static final IBlockData GRAVEL = Blocks.GRAVEL.getBlockData();
    private static final ImmutableList<IBlockData> FLOOR_BLOCK_STATES = ImmutableList.of(BASALT, BLACKSTONE);
    private static final ImmutableList<IBlockData> CEILING_BLOCK_STATES = ImmutableList.of(BASALT);

    public WorldGenSurfaceBasaltDeltas(Codec<WorldGenSurfaceConfigurationBase> codec) {
        super(codec);
    }

    @Override
    protected ImmutableList<IBlockData> getFloorBlockStates() {
        return FLOOR_BLOCK_STATES;
    }

    @Override
    protected ImmutableList<IBlockData> getCeilingBlockStates() {
        return CEILING_BLOCK_STATES;
    }

    @Override
    protected IBlockData getPatchBlockState() {
        return GRAVEL;
    }
}
