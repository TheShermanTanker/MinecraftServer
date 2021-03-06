package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.data.worldgen.WorldGenStructureFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.StructureSettingsFeature;
import net.minecraft.world.level.levelgen.feature.configurations.StructureSettingsStronghold;

public class StructureSettings {
    public static final Codec<StructureSettings> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(StructureSettingsStronghold.CODEC.optionalFieldOf("stronghold").forGetter((config) -> {
            return Optional.ofNullable(config.stronghold);
        }), Codec.simpleMap(IRegistry.STRUCTURE_FEATURE.byNameCodec(), StructureSettingsFeature.CODEC, IRegistry.STRUCTURE_FEATURE).fieldOf("structures").forGetter((config) -> {
            return config.structureConfig;
        })).apply(instance, StructureSettings::new);
    });
    public static final ImmutableMap<StructureGenerator<?>, StructureSettingsFeature> DEFAULTS = ImmutableMap.<StructureGenerator<?>, StructureSettingsFeature>builder().put(StructureGenerator.VILLAGE, new StructureSettingsFeature(34, 8, 10387312)).put(StructureGenerator.DESERT_PYRAMID, new StructureSettingsFeature(32, 8, 14357617)).put(StructureGenerator.IGLOO, new StructureSettingsFeature(32, 8, 14357618)).put(StructureGenerator.JUNGLE_TEMPLE, new StructureSettingsFeature(32, 8, 14357619)).put(StructureGenerator.SWAMP_HUT, new StructureSettingsFeature(32, 8, 14357620)).put(StructureGenerator.PILLAGER_OUTPOST, new StructureSettingsFeature(32, 8, 165745296)).put(StructureGenerator.STRONGHOLD, new StructureSettingsFeature(1, 0, 0)).put(StructureGenerator.OCEAN_MONUMENT, new StructureSettingsFeature(32, 5, 10387313)).put(StructureGenerator.END_CITY, new StructureSettingsFeature(20, 11, 10387313)).put(StructureGenerator.WOODLAND_MANSION, new StructureSettingsFeature(80, 20, 10387319)).put(StructureGenerator.BURIED_TREASURE, new StructureSettingsFeature(1, 0, 0)).put(StructureGenerator.MINESHAFT, new StructureSettingsFeature(1, 0, 0)).put(StructureGenerator.RUINED_PORTAL, new StructureSettingsFeature(40, 15, 34222645)).put(StructureGenerator.SHIPWRECK, new StructureSettingsFeature(24, 4, 165745295)).put(StructureGenerator.OCEAN_RUIN, new StructureSettingsFeature(20, 8, 14357621)).put(StructureGenerator.BASTION_REMNANT, new StructureSettingsFeature(27, 4, 30084232)).put(StructureGenerator.NETHER_BRIDGE, new StructureSettingsFeature(27, 4, 30084232)).put(StructureGenerator.NETHER_FOSSIL, new StructureSettingsFeature(2, 1, 14357921)).build();
    public static final StructureSettingsStronghold DEFAULT_STRONGHOLD;
    private final Map<StructureGenerator<?>, StructureSettingsFeature> structureConfig;
    private final ImmutableMap<StructureGenerator<?>, ImmutableMultimap<StructureFeature<?, ?>, ResourceKey<BiomeBase>>> configuredStructures;
    @Nullable
    private final StructureSettingsStronghold stronghold;

    private StructureSettings(Map<StructureGenerator<?>, StructureSettingsFeature> structures, @Nullable StructureSettingsStronghold stronghold) {
        this.stronghold = stronghold;
        this.structureConfig = structures;
        HashMap<StructureGenerator<?>, Builder<StructureFeature<?, ?>, ResourceKey<BiomeBase>>> hashMap = new HashMap<>();
        WorldGenStructureFeatures.registerStructures((feature, biome) -> {
            hashMap.computeIfAbsent(feature.feature, (featurex) -> {
                return ImmutableMultimap.builder();
            }).put(feature, biome);
        });
        this.configuredStructures = hashMap.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (entry) -> {
            return entry.getValue().build();
        }));
    }

    public StructureSettings(Optional<StructureSettingsStronghold> stronghold, Map<StructureGenerator<?>, StructureSettingsFeature> structures) {
        this(structures, stronghold.orElse((StructureSettingsStronghold)null));
    }

    public StructureSettings(boolean withStronghold) {
        this(Maps.newHashMap(DEFAULTS), withStronghold ? DEFAULT_STRONGHOLD : null);
    }

    @VisibleForTesting
    public Map<StructureGenerator<?>, StructureSettingsFeature> structureConfig() {
        return this.structureConfig;
    }

    @Nullable
    public StructureSettingsFeature getConfig(StructureGenerator<?> structureType) {
        return this.structureConfig.get(structureType);
    }

    @Nullable
    public StructureSettingsStronghold stronghold() {
        return this.stronghold;
    }

    public ImmutableMultimap<StructureFeature<?, ?>, ResourceKey<BiomeBase>> structures(StructureGenerator<?> feature) {
        return this.configuredStructures.getOrDefault(feature, ImmutableMultimap.of());
    }

    static {
        for(StructureGenerator<?> structureFeature : IRegistry.STRUCTURE_FEATURE) {
            if (!DEFAULTS.containsKey(structureFeature)) {
                throw new IllegalStateException("Structure feature without default settings: " + IRegistry.STRUCTURE_FEATURE.getKey(structureFeature));
            }
        }

        DEFAULT_STRONGHOLD = new StructureSettingsStronghold(32, 3, 128);
    }
}
