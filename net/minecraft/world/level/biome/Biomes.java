package net.minecraft.world.level.biome;

import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;

public abstract class Biomes {
    public static final ResourceKey<BiomeBase> OCEAN = register("ocean");
    public static final ResourceKey<BiomeBase> PLAINS = register("plains");
    public static final ResourceKey<BiomeBase> DESERT = register("desert");
    public static final ResourceKey<BiomeBase> MOUNTAINS = register("mountains");
    public static final ResourceKey<BiomeBase> FOREST = register("forest");
    public static final ResourceKey<BiomeBase> TAIGA = register("taiga");
    public static final ResourceKey<BiomeBase> SWAMP = register("swamp");
    public static final ResourceKey<BiomeBase> RIVER = register("river");
    public static final ResourceKey<BiomeBase> NETHER_WASTES = register("nether_wastes");
    public static final ResourceKey<BiomeBase> THE_END = register("the_end");
    public static final ResourceKey<BiomeBase> FROZEN_OCEAN = register("frozen_ocean");
    public static final ResourceKey<BiomeBase> FROZEN_RIVER = register("frozen_river");
    public static final ResourceKey<BiomeBase> SNOWY_TUNDRA = register("snowy_tundra");
    public static final ResourceKey<BiomeBase> SNOWY_MOUNTAINS = register("snowy_mountains");
    public static final ResourceKey<BiomeBase> MUSHROOM_FIELDS = register("mushroom_fields");
    public static final ResourceKey<BiomeBase> MUSHROOM_FIELD_SHORE = register("mushroom_field_shore");
    public static final ResourceKey<BiomeBase> BEACH = register("beach");
    public static final ResourceKey<BiomeBase> DESERT_HILLS = register("desert_hills");
    public static final ResourceKey<BiomeBase> WOODED_HILLS = register("wooded_hills");
    public static final ResourceKey<BiomeBase> TAIGA_HILLS = register("taiga_hills");
    public static final ResourceKey<BiomeBase> MOUNTAIN_EDGE = register("mountain_edge");
    public static final ResourceKey<BiomeBase> JUNGLE = register("jungle");
    public static final ResourceKey<BiomeBase> JUNGLE_HILLS = register("jungle_hills");
    public static final ResourceKey<BiomeBase> JUNGLE_EDGE = register("jungle_edge");
    public static final ResourceKey<BiomeBase> DEEP_OCEAN = register("deep_ocean");
    public static final ResourceKey<BiomeBase> STONE_SHORE = register("stone_shore");
    public static final ResourceKey<BiomeBase> SNOWY_BEACH = register("snowy_beach");
    public static final ResourceKey<BiomeBase> BIRCH_FOREST = register("birch_forest");
    public static final ResourceKey<BiomeBase> BIRCH_FOREST_HILLS = register("birch_forest_hills");
    public static final ResourceKey<BiomeBase> DARK_FOREST = register("dark_forest");
    public static final ResourceKey<BiomeBase> SNOWY_TAIGA = register("snowy_taiga");
    public static final ResourceKey<BiomeBase> SNOWY_TAIGA_HILLS = register("snowy_taiga_hills");
    public static final ResourceKey<BiomeBase> GIANT_TREE_TAIGA = register("giant_tree_taiga");
    public static final ResourceKey<BiomeBase> GIANT_TREE_TAIGA_HILLS = register("giant_tree_taiga_hills");
    public static final ResourceKey<BiomeBase> WOODED_MOUNTAINS = register("wooded_mountains");
    public static final ResourceKey<BiomeBase> SAVANNA = register("savanna");
    public static final ResourceKey<BiomeBase> SAVANNA_PLATEAU = register("savanna_plateau");
    public static final ResourceKey<BiomeBase> BADLANDS = register("badlands");
    public static final ResourceKey<BiomeBase> WOODED_BADLANDS_PLATEAU = register("wooded_badlands_plateau");
    public static final ResourceKey<BiomeBase> BADLANDS_PLATEAU = register("badlands_plateau");
    public static final ResourceKey<BiomeBase> SMALL_END_ISLANDS = register("small_end_islands");
    public static final ResourceKey<BiomeBase> END_MIDLANDS = register("end_midlands");
    public static final ResourceKey<BiomeBase> END_HIGHLANDS = register("end_highlands");
    public static final ResourceKey<BiomeBase> END_BARRENS = register("end_barrens");
    public static final ResourceKey<BiomeBase> WARM_OCEAN = register("warm_ocean");
    public static final ResourceKey<BiomeBase> LUKEWARM_OCEAN = register("lukewarm_ocean");
    public static final ResourceKey<BiomeBase> COLD_OCEAN = register("cold_ocean");
    public static final ResourceKey<BiomeBase> DEEP_WARM_OCEAN = register("deep_warm_ocean");
    public static final ResourceKey<BiomeBase> DEEP_LUKEWARM_OCEAN = register("deep_lukewarm_ocean");
    public static final ResourceKey<BiomeBase> DEEP_COLD_OCEAN = register("deep_cold_ocean");
    public static final ResourceKey<BiomeBase> DEEP_FROZEN_OCEAN = register("deep_frozen_ocean");
    public static final ResourceKey<BiomeBase> THE_VOID = register("the_void");
    public static final ResourceKey<BiomeBase> SUNFLOWER_PLAINS = register("sunflower_plains");
    public static final ResourceKey<BiomeBase> DESERT_LAKES = register("desert_lakes");
    public static final ResourceKey<BiomeBase> GRAVELLY_MOUNTAINS = register("gravelly_mountains");
    public static final ResourceKey<BiomeBase> FLOWER_FOREST = register("flower_forest");
    public static final ResourceKey<BiomeBase> TAIGA_MOUNTAINS = register("taiga_mountains");
    public static final ResourceKey<BiomeBase> SWAMP_HILLS = register("swamp_hills");
    public static final ResourceKey<BiomeBase> ICE_SPIKES = register("ice_spikes");
    public static final ResourceKey<BiomeBase> MODIFIED_JUNGLE = register("modified_jungle");
    public static final ResourceKey<BiomeBase> MODIFIED_JUNGLE_EDGE = register("modified_jungle_edge");
    public static final ResourceKey<BiomeBase> TALL_BIRCH_FOREST = register("tall_birch_forest");
    public static final ResourceKey<BiomeBase> TALL_BIRCH_HILLS = register("tall_birch_hills");
    public static final ResourceKey<BiomeBase> DARK_FOREST_HILLS = register("dark_forest_hills");
    public static final ResourceKey<BiomeBase> SNOWY_TAIGA_MOUNTAINS = register("snowy_taiga_mountains");
    public static final ResourceKey<BiomeBase> GIANT_SPRUCE_TAIGA = register("giant_spruce_taiga");
    public static final ResourceKey<BiomeBase> GIANT_SPRUCE_TAIGA_HILLS = register("giant_spruce_taiga_hills");
    public static final ResourceKey<BiomeBase> MODIFIED_GRAVELLY_MOUNTAINS = register("modified_gravelly_mountains");
    public static final ResourceKey<BiomeBase> SHATTERED_SAVANNA = register("shattered_savanna");
    public static final ResourceKey<BiomeBase> SHATTERED_SAVANNA_PLATEAU = register("shattered_savanna_plateau");
    public static final ResourceKey<BiomeBase> ERODED_BADLANDS = register("eroded_badlands");
    public static final ResourceKey<BiomeBase> MODIFIED_WOODED_BADLANDS_PLATEAU = register("modified_wooded_badlands_plateau");
    public static final ResourceKey<BiomeBase> MODIFIED_BADLANDS_PLATEAU = register("modified_badlands_plateau");
    public static final ResourceKey<BiomeBase> BAMBOO_JUNGLE = register("bamboo_jungle");
    public static final ResourceKey<BiomeBase> BAMBOO_JUNGLE_HILLS = register("bamboo_jungle_hills");
    public static final ResourceKey<BiomeBase> SOUL_SAND_VALLEY = register("soul_sand_valley");
    public static final ResourceKey<BiomeBase> CRIMSON_FOREST = register("crimson_forest");
    public static final ResourceKey<BiomeBase> WARPED_FOREST = register("warped_forest");
    public static final ResourceKey<BiomeBase> BASALT_DELTAS = register("basalt_deltas");
    public static final ResourceKey<BiomeBase> DRIPSTONE_CAVES = register("dripstone_caves");
    public static final ResourceKey<BiomeBase> LUSH_CAVES = register("lush_caves");

    private static ResourceKey<BiomeBase> register(String name) {
        return ResourceKey.create(IRegistry.BIOME_REGISTRY, new MinecraftKey(name));
    }
}