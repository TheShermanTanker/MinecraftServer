package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.RootSystemConfiguration;

public class WorldGenFeatureRootSystem extends WorldGenerator<RootSystemConfiguration> {
    public WorldGenFeatureRootSystem(Codec<RootSystemConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<RootSystemConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        if (!worldGenLevel.getType(blockPos).isAir()) {
            return false;
        } else {
            Random random = context.random();
            BlockPosition blockPos2 = context.origin();
            RootSystemConfiguration rootSystemConfiguration = context.config();
            BlockPosition.MutableBlockPosition mutableBlockPos = blockPos2.mutable();
            if (placeDirtAndTree(worldGenLevel, context.chunkGenerator(), rootSystemConfiguration, random, mutableBlockPos, blockPos2)) {
                placeRoots(worldGenLevel, rootSystemConfiguration, random, blockPos2, mutableBlockPos);
            }

            return true;
        }
    }

    private static boolean spaceForTree(GeneratorAccessSeed world, RootSystemConfiguration config, BlockPosition pos) {
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();

        for(int i = 1; i <= config.requiredVerticalSpaceForTree; ++i) {
            mutableBlockPos.move(EnumDirection.UP);
            IBlockData blockState = world.getType(mutableBlockPos);
            if (!isAllowedTreeSpace(blockState, i, config.allowedVerticalWaterForTree)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isAllowedTreeSpace(IBlockData state, int height, int allowedVerticalWaterForTree) {
        if (state.isAir()) {
            return true;
        } else {
            int i = height + 1;
            return i <= allowedVerticalWaterForTree && state.getFluid().is(TagsFluid.WATER);
        }
    }

    private static boolean placeDirtAndTree(GeneratorAccessSeed world, ChunkGenerator generator, RootSystemConfiguration config, Random random, BlockPosition.MutableBlockPosition mutablePos, BlockPosition pos) {
        for(int i = 0; i < config.rootColumnMaxHeight; ++i) {
            mutablePos.move(EnumDirection.UP);
            if (config.allowedTreePosition.test(world, mutablePos) && spaceForTree(world, config, mutablePos)) {
                BlockPosition blockPos = mutablePos.below();
                if (world.getFluid(blockPos).is(TagsFluid.LAVA) || !world.getType(blockPos).getMaterial().isBuildable()) {
                    return false;
                }

                if (config.treeFeature.get().place(world, generator, random, mutablePos)) {
                    placeDirt(pos, pos.getY() + i, world, config, random);
                    return true;
                }
            }
        }

        return false;
    }

    private static void placeDirt(BlockPosition pos, int maxY, GeneratorAccessSeed world, RootSystemConfiguration config, Random random) {
        int i = pos.getX();
        int j = pos.getZ();
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();

        for(int k = pos.getY(); k < maxY; ++k) {
            placeRootedDirt(world, config, random, i, j, mutableBlockPos.set(i, k, j));
        }

    }

    private static void placeRootedDirt(GeneratorAccessSeed world, RootSystemConfiguration config, Random random, int x, int z, BlockPosition.MutableBlockPosition mutablePos) {
        int i = config.rootRadius;
        Tag<Block> tag = TagsBlock.getAllTags().getTag(config.rootReplaceable);
        Predicate<IBlockData> predicate = tag == null ? (state) -> {
            return true;
        } : (state) -> {
            return state.is(tag);
        };

        for(int j = 0; j < config.rootPlacementAttempts; ++j) {
            mutablePos.setWithOffset(mutablePos, random.nextInt(i) - random.nextInt(i), 0, random.nextInt(i) - random.nextInt(i));
            if (predicate.test(world.getType(mutablePos))) {
                world.setTypeAndData(mutablePos, config.rootStateProvider.getState(random, mutablePos), 2);
            }

            mutablePos.setX(x);
            mutablePos.setZ(z);
        }

    }

    private static void placeRoots(GeneratorAccessSeed world, RootSystemConfiguration config, Random random, BlockPosition pos, BlockPosition.MutableBlockPosition mutablePos) {
        int i = config.hangingRootRadius;
        int j = config.hangingRootsVerticalSpan;

        for(int k = 0; k < config.hangingRootPlacementAttempts; ++k) {
            mutablePos.setWithOffset(pos, random.nextInt(i) - random.nextInt(i), random.nextInt(j) - random.nextInt(j), random.nextInt(i) - random.nextInt(i));
            if (world.isEmpty(mutablePos)) {
                IBlockData blockState = config.hangingRootStateProvider.getState(random, mutablePos);
                if (blockState.canPlace(world, mutablePos) && world.getType(mutablePos.above()).isFaceSturdy(world, mutablePos, EnumDirection.DOWN)) {
                    world.setTypeAndData(mutablePos, blockState, 2);
                }
            }
        }

    }
}
