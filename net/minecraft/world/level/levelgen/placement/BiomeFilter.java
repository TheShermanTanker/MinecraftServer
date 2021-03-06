package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.biome.BiomeBase;

public class BiomeFilter extends PlacementFilter {
    private static final BiomeFilter INSTANCE = new BiomeFilter();
    public static Codec<BiomeFilter> CODEC = Codec.unit(() -> {
        return INSTANCE;
    });

    private BiomeFilter() {
    }

    public static BiomeFilter biome() {
        return INSTANCE;
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, Random random, BlockPosition pos) {
        PlacedFeature placedFeature = context.topFeature().orElseThrow(() -> {
            return new IllegalStateException("Tried to biome check an unregistered feature");
        });
        BiomeBase biome = context.getLevel().getBiome(pos);
        return biome.getGenerationSettings().hasFeature(placedFeature);
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.BIOME_FILTER;
    }
}
