package net.minecraft.world.level.levelgen;

import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;

public class WorldGenerationContext {
    private final int minY;
    private final int height;

    public WorldGenerationContext(ChunkGenerator generator, IWorldHeightAccess world) {
        this.minY = Math.max(world.getMinBuildHeight(), generator.getMinY());
        this.height = Math.min(world.getHeight(), generator.getGenerationDepth());
    }

    public int getMinGenY() {
        return this.minY;
    }

    public int getGenDepth() {
        return this.height;
    }
}
