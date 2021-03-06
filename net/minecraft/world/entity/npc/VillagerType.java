package net.minecraft.world.entity.npc;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.Biomes;

public final class VillagerType {
    public static final VillagerType DESERT = register("desert");
    public static final VillagerType JUNGLE = register("jungle");
    public static final VillagerType PLAINS = register("plains");
    public static final VillagerType SAVANNA = register("savanna");
    public static final VillagerType SNOW = register("snow");
    public static final VillagerType SWAMP = register("swamp");
    public static final VillagerType TAIGA = register("taiga");
    private final String name;
    private static final Map<ResourceKey<BiomeBase>, VillagerType> BY_BIOME = SystemUtils.make(Maps.newHashMap(), (map) -> {
        map.put(Biomes.BADLANDS, DESERT);
        map.put(Biomes.DESERT, DESERT);
        map.put(Biomes.ERODED_BADLANDS, DESERT);
        map.put(Biomes.WOODED_BADLANDS, DESERT);
        map.put(Biomes.BAMBOO_JUNGLE, JUNGLE);
        map.put(Biomes.JUNGLE, JUNGLE);
        map.put(Biomes.SPARSE_JUNGLE, JUNGLE);
        map.put(Biomes.SAVANNA_PLATEAU, SAVANNA);
        map.put(Biomes.SAVANNA, SAVANNA);
        map.put(Biomes.WINDSWEPT_SAVANNA, SAVANNA);
        map.put(Biomes.DEEP_FROZEN_OCEAN, SNOW);
        map.put(Biomes.FROZEN_OCEAN, SNOW);
        map.put(Biomes.FROZEN_RIVER, SNOW);
        map.put(Biomes.ICE_SPIKES, SNOW);
        map.put(Biomes.SNOWY_BEACH, SNOW);
        map.put(Biomes.SNOWY_TAIGA, SNOW);
        map.put(Biomes.SNOWY_PLAINS, SNOW);
        map.put(Biomes.GROVE, SNOW);
        map.put(Biomes.SNOWY_SLOPES, SNOW);
        map.put(Biomes.FROZEN_PEAKS, SNOW);
        map.put(Biomes.JAGGED_PEAKS, SNOW);
        map.put(Biomes.SWAMP, SWAMP);
        map.put(Biomes.OLD_GROWTH_SPRUCE_TAIGA, TAIGA);
        map.put(Biomes.OLD_GROWTH_PINE_TAIGA, TAIGA);
        map.put(Biomes.WINDSWEPT_GRAVELLY_HILLS, TAIGA);
        map.put(Biomes.WINDSWEPT_HILLS, TAIGA);
        map.put(Biomes.TAIGA, TAIGA);
        map.put(Biomes.WINDSWEPT_FOREST, TAIGA);
    });

    private VillagerType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    private static VillagerType register(String id) {
        return IRegistry.register(IRegistry.VILLAGER_TYPE, new MinecraftKey(id), new VillagerType(id));
    }

    public static VillagerType byBiome(Optional<ResourceKey<BiomeBase>> biomeKey) {
        return biomeKey.flatMap((biomeKeyx) -> {
            return Optional.ofNullable(BY_BIOME.get(biomeKeyx));
        }).orElse(PLAINS);
    }
}
