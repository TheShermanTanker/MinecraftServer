package net.minecraft.world.level.levelgen.structure;

import java.util.Random;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorBlockIgnore;

public class WorldGenNetherFossil {
    private static final MinecraftKey[] FOSSILS = new MinecraftKey[]{new MinecraftKey("nether_fossils/fossil_1"), new MinecraftKey("nether_fossils/fossil_2"), new MinecraftKey("nether_fossils/fossil_3"), new MinecraftKey("nether_fossils/fossil_4"), new MinecraftKey("nether_fossils/fossil_5"), new MinecraftKey("nether_fossils/fossil_6"), new MinecraftKey("nether_fossils/fossil_7"), new MinecraftKey("nether_fossils/fossil_8"), new MinecraftKey("nether_fossils/fossil_9"), new MinecraftKey("nether_fossils/fossil_10"), new MinecraftKey("nether_fossils/fossil_11"), new MinecraftKey("nether_fossils/fossil_12"), new MinecraftKey("nether_fossils/fossil_13"), new MinecraftKey("nether_fossils/fossil_14")};

    public static void addPieces(DefinedStructureManager manager, StructurePieceAccessor holder, Random random, BlockPosition pos) {
        EnumBlockRotation rotation = EnumBlockRotation.getRandom(random);
        holder.addPiece(new WorldGenNetherFossil.NetherFossilPiece(manager, SystemUtils.getRandom(FOSSILS, random), pos, rotation));
    }

    public static class NetherFossilPiece extends DefinedStructurePiece {
        public NetherFossilPiece(DefinedStructureManager manager, MinecraftKey template, BlockPosition pos, EnumBlockRotation rotation) {
            super(WorldGenFeatureStructurePieceType.NETHER_FOSSIL, 0, manager, template, template.toString(), makeSettings(rotation), pos);
        }

        public NetherFossilPiece(DefinedStructureManager manager, NBTTagCompound nbt) {
            super(WorldGenFeatureStructurePieceType.NETHER_FOSSIL, nbt, manager, (id) -> {
                return makeSettings(EnumBlockRotation.valueOf(nbt.getString("Rot")));
            });
        }

        private static DefinedStructureInfo makeSettings(EnumBlockRotation rotation) {
            return (new DefinedStructureInfo()).setRotation(rotation).setMirror(EnumBlockMirror.NONE).addProcessor(DefinedStructureProcessorBlockIgnore.STRUCTURE_AND_AIR);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, NBTTagCompound nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.setString("Rot", this.placeSettings.getRotation().name());
        }

        @Override
        protected void handleDataMarker(String metadata, BlockPosition pos, WorldAccess world, Random random, StructureBoundingBox boundingBox) {
        }

        @Override
        public void postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox chunkBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
            chunkBox.encapsulate(this.template.getBoundingBox(this.placeSettings, this.templatePosition));
            super.postProcess(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, pos);
        }
    }
}
