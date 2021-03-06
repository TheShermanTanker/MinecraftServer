package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import org.apache.commons.lang3.mutable.MutableInt;

public class WorldGenFossils extends WorldGenerator<FossilFeatureConfiguration> {
    public WorldGenFossils(Codec<FossilFeatureConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<FossilFeatureConfiguration> context) {
        Random random = context.random();
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        EnumBlockRotation rotation = EnumBlockRotation.getRandom(random);
        FossilFeatureConfiguration fossilFeatureConfiguration = context.config();
        int i = random.nextInt(fossilFeatureConfiguration.fossilStructures.size());
        DefinedStructureManager structureManager = worldGenLevel.getLevel().getMinecraftServer().getDefinedStructureManager();
        DefinedStructure structureTemplate = structureManager.getOrCreate(fossilFeatureConfiguration.fossilStructures.get(i));
        DefinedStructure structureTemplate2 = structureManager.getOrCreate(fossilFeatureConfiguration.overlayStructures.get(i));
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(blockPos);
        StructureBoundingBox boundingBox = new StructureBoundingBox(chunkPos.getMinBlockX() - 16, worldGenLevel.getMinBuildHeight(), chunkPos.getMinBlockZ() - 16, chunkPos.getMaxBlockX() + 16, worldGenLevel.getMaxBuildHeight(), chunkPos.getMaxBlockZ() + 16);
        DefinedStructureInfo structurePlaceSettings = (new DefinedStructureInfo()).setRotation(rotation).setBoundingBox(boundingBox).setRandom(random);
        BaseBlockPosition vec3i = structureTemplate.getSize(rotation);
        BlockPosition blockPos2 = blockPos.offset(-vec3i.getX() / 2, 0, -vec3i.getZ() / 2);
        int j = blockPos.getY();

        for(int k = 0; k < vec3i.getX(); ++k) {
            for(int l = 0; l < vec3i.getZ(); ++l) {
                j = Math.min(j, worldGenLevel.getHeight(HeightMap.Type.OCEAN_FLOOR_WG, blockPos2.getX() + k, blockPos2.getZ() + l));
            }
        }

        int m = Math.max(j - 15 - random.nextInt(10), worldGenLevel.getMinBuildHeight() + 10);
        BlockPosition blockPos3 = structureTemplate.getZeroPositionWithTransform(blockPos2.atY(m), EnumBlockMirror.NONE, rotation);
        if (countEmptyCorners(worldGenLevel, structureTemplate.getBoundingBox(structurePlaceSettings, blockPos3)) > fossilFeatureConfiguration.maxEmptyCornersAllowed) {
            return false;
        } else {
            structurePlaceSettings.clearProcessors();
            fossilFeatureConfiguration.fossilProcessors.get().list().forEach((processor) -> {
                structurePlaceSettings.addProcessor(processor);
            });
            structureTemplate.placeInWorld(worldGenLevel, blockPos3, blockPos3, structurePlaceSettings, random, 4);
            structurePlaceSettings.clearProcessors();
            fossilFeatureConfiguration.overlayProcessors.get().list().forEach((processor) -> {
                structurePlaceSettings.addProcessor(processor);
            });
            structureTemplate2.placeInWorld(worldGenLevel, blockPos3, blockPos3, structurePlaceSettings, random, 4);
            return true;
        }
    }

    private static int countEmptyCorners(GeneratorAccessSeed world, StructureBoundingBox box) {
        MutableInt mutableInt = new MutableInt(0);
        box.forAllCorners((pos) -> {
            IBlockData blockState = world.getType(pos);
            if (blockState.isAir() || blockState.is(Blocks.LAVA) || blockState.is(Blocks.WATER)) {
                mutableInt.add(1);
            }

        });
        return mutableInt.getValue();
    }
}
