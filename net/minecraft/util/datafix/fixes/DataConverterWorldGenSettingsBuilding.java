package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicLike;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

public class DataConverterWorldGenSettingsBuilding extends DataFix {
    private static final String VILLAGE = "minecraft:village";
    private static final String DESERT_PYRAMID = "minecraft:desert_pyramid";
    private static final String IGLOO = "minecraft:igloo";
    private static final String JUNGLE_TEMPLE = "minecraft:jungle_pyramid";
    private static final String SWAMP_HUT = "minecraft:swamp_hut";
    private static final String PILLAGER_OUTPOST = "minecraft:pillager_outpost";
    private static final String END_CITY = "minecraft:endcity";
    private static final String WOODLAND_MANSION = "minecraft:mansion";
    private static final String OCEAN_MONUMENT = "minecraft:monument";
    private static final ImmutableMap<String, DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration> DEFAULTS = ImmutableMap.<String, DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration>builder().put("minecraft:village", new DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration(32, 8, 10387312)).put("minecraft:desert_pyramid", new DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration(32, 8, 14357617)).put("minecraft:igloo", new DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration(32, 8, 14357618)).put("minecraft:jungle_pyramid", new DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration(32, 8, 14357619)).put("minecraft:swamp_hut", new DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration(32, 8, 14357620)).put("minecraft:pillager_outpost", new DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration(32, 8, 165745296)).put("minecraft:monument", new DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration(32, 5, 10387313)).put("minecraft:endcity", new DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration(20, 11, 10387313)).put("minecraft:mansion", new DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration(80, 20, 10387319)).build();

    public DataConverterWorldGenSettingsBuilding(Schema outputSchema) {
        super(outputSchema, true);
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("WorldGenSettings building", this.getInputSchema().getType(DataConverterTypes.WORLD_GEN_SETTINGS), (typed) -> {
            return typed.update(DSL.remainderFinder(), DataConverterWorldGenSettingsBuilding::fix);
        });
    }

    private static <T> Dynamic<T> noise(long l, DynamicLike<T> dynamicLike, Dynamic<T> dynamic, Dynamic<T> dynamic2) {
        return dynamicLike.createMap(ImmutableMap.of(dynamicLike.createString("type"), dynamicLike.createString("minecraft:noise"), dynamicLike.createString("biome_source"), dynamic2, dynamicLike.createString("seed"), dynamicLike.createLong(l), dynamicLike.createString("settings"), dynamic));
    }

    private static <T> Dynamic<T> vanillaBiomeSource(Dynamic<T> dynamic, long l, boolean bl, boolean bl2) {
        Builder<Dynamic<T>, Dynamic<T>> builder = ImmutableMap.<Dynamic<T>, Dynamic<T>>builder().put(dynamic.createString("type"), dynamic.createString("minecraft:vanilla_layered")).put(dynamic.createString("seed"), dynamic.createLong(l)).put(dynamic.createString("large_biomes"), dynamic.createBoolean(bl2));
        if (bl) {
            builder.put(dynamic.createString("legacy_biome_init_layer"), dynamic.createBoolean(bl));
        }

        return dynamic.createMap(builder.build());
    }

    private static <T> Dynamic<T> fix(Dynamic<T> dynamic) {
        DynamicOps<T> dynamicOps = dynamic.getOps();
        long l = dynamic.get("RandomSeed").asLong(0L);
        Optional<String> optional = dynamic.get("generatorName").asString().map((string) -> {
            return string.toLowerCase(Locale.ROOT);
        }).result();
        Optional<String> optional2 = dynamic.get("legacy_custom_options").asString().result().map(Optional::of).orElseGet(() -> {
            return optional.equals(Optional.of("customized")) ? dynamic.get("generatorOptions").asString().result() : Optional.empty();
        });
        boolean bl = false;
        Dynamic<T> dynamic2;
        if (optional.equals(Optional.of("customized"))) {
            dynamic2 = defaultOverworld(dynamic, l);
        } else if (!optional.isPresent()) {
            dynamic2 = defaultOverworld(dynamic, l);
        } else {
            String bl6 = optional.get();
            switch(bl6) {
            case "flat":
                OptionalDynamic<T> optionalDynamic = dynamic.get("generatorOptions");
                Map<Dynamic<T>, Dynamic<T>> map = fixFlatStructures(dynamicOps, optionalDynamic);
                dynamic2 = dynamic.createMap(ImmutableMap.of(dynamic.createString("type"), dynamic.createString("minecraft:flat"), dynamic.createString("settings"), dynamic.createMap(ImmutableMap.of(dynamic.createString("structures"), dynamic.createMap(map), dynamic.createString("layers"), optionalDynamic.get("layers").result().orElseGet(() -> {
                    return dynamic.createList(Stream.of(dynamic.createMap(ImmutableMap.of(dynamic.createString("height"), dynamic.createInt(1), dynamic.createString("block"), dynamic.createString("minecraft:bedrock"))), dynamic.createMap(ImmutableMap.of(dynamic.createString("height"), dynamic.createInt(2), dynamic.createString("block"), dynamic.createString("minecraft:dirt"))), dynamic.createMap(ImmutableMap.of(dynamic.createString("height"), dynamic.createInt(1), dynamic.createString("block"), dynamic.createString("minecraft:grass_block")))));
                }), dynamic.createString("biome"), dynamic.createString(optionalDynamic.get("biome").asString("minecraft:plains"))))));
                break;
            case "debug_all_block_states":
                dynamic2 = dynamic.createMap(ImmutableMap.of(dynamic.createString("type"), dynamic.createString("minecraft:debug")));
                break;
            case "buffet":
                OptionalDynamic<T> optionalDynamic2 = dynamic.get("generatorOptions");
                OptionalDynamic<?> optionalDynamic3 = optionalDynamic2.get("chunk_generator");
                Optional<String> optional3 = optionalDynamic3.get("type").asString().result();
                Dynamic<T> dynamic6;
                if (Objects.equals(optional3, Optional.of("minecraft:caves"))) {
                    dynamic6 = dynamic.createString("minecraft:caves");
                    bl = true;
                } else if (Objects.equals(optional3, Optional.of("minecraft:floating_islands"))) {
                    dynamic6 = dynamic.createString("minecraft:floating_islands");
                } else {
                    dynamic6 = dynamic.createString("minecraft:overworld");
                }

                Dynamic<T> dynamic9 = optionalDynamic2.get("biome_source").result().orElseGet(() -> {
                    return dynamic.createMap(ImmutableMap.of(dynamic.createString("type"), dynamic.createString("minecraft:fixed")));
                });
                Dynamic<T> dynamic10;
                if (dynamic9.get("type").asString().result().equals(Optional.of("minecraft:fixed"))) {
                    String string = dynamic9.get("options").get("biomes").asStream().findFirst().flatMap((dynamicx) -> {
                        return dynamicx.asString().result();
                    }).orElse("minecraft:ocean");
                    dynamic10 = dynamic9.remove("options").set("biome", dynamic.createString(string));
                } else {
                    dynamic10 = dynamic9;
                }

                dynamic2 = noise(l, dynamic, dynamic6, dynamic10);
                break;
            default:
                boolean bl2 = optional.get().equals("default");
                boolean bl3 = optional.get().equals("default_1_1") || bl2 && dynamic.get("generatorVersion").asInt(0) == 0;
                boolean bl4 = optional.get().equals("amplified");
                boolean bl5 = optional.get().equals("largebiomes");
                dynamic2 = noise(l, dynamic, dynamic.createString(bl4 ? "minecraft:amplified" : "minecraft:overworld"), vanillaBiomeSource(dynamic, l, bl3, bl5));
            }
        }

        boolean bl6 = dynamic.get("MapFeatures").asBoolean(true);
        boolean bl7 = dynamic.get("BonusChest").asBoolean(false);
        Builder<T, T> builder = ImmutableMap.builder();
        builder.put(dynamicOps.createString("seed"), dynamicOps.createLong(l));
        builder.put(dynamicOps.createString("generate_features"), dynamicOps.createBoolean(bl6));
        builder.put(dynamicOps.createString("bonus_chest"), dynamicOps.createBoolean(bl7));
        builder.put(dynamicOps.createString("dimensions"), vanillaLevels(dynamic, l, dynamic2, bl));
        optional2.ifPresent((string) -> {
            builder.put(dynamicOps.createString("legacy_custom_options"), dynamicOps.createString(string));
        });
        return new Dynamic<>(dynamicOps, dynamicOps.createMap(builder.build()));
    }

    protected static <T> Dynamic<T> defaultOverworld(Dynamic<T> dynamic, long l) {
        return noise(l, dynamic, dynamic.createString("minecraft:overworld"), vanillaBiomeSource(dynamic, l, false, false));
    }

    protected static <T> T vanillaLevels(Dynamic<T> dynamic, long l, Dynamic<T> dynamic2, boolean bl) {
        DynamicOps<T> dynamicOps = dynamic.getOps();
        return dynamicOps.createMap(ImmutableMap.of(dynamicOps.createString("minecraft:overworld"), dynamicOps.createMap(ImmutableMap.of(dynamicOps.createString("type"), dynamicOps.createString("minecraft:overworld" + (bl ? "_caves" : "")), dynamicOps.createString("generator"), dynamic2.getValue())), dynamicOps.createString("minecraft:the_nether"), dynamicOps.createMap(ImmutableMap.of(dynamicOps.createString("type"), dynamicOps.createString("minecraft:the_nether"), dynamicOps.createString("generator"), noise(l, dynamic, dynamic.createString("minecraft:nether"), dynamic.createMap(ImmutableMap.of(dynamic.createString("type"), dynamic.createString("minecraft:multi_noise"), dynamic.createString("seed"), dynamic.createLong(l), dynamic.createString("preset"), dynamic.createString("minecraft:nether")))).getValue())), dynamicOps.createString("minecraft:the_end"), dynamicOps.createMap(ImmutableMap.of(dynamicOps.createString("type"), dynamicOps.createString("minecraft:the_end"), dynamicOps.createString("generator"), noise(l, dynamic, dynamic.createString("minecraft:end"), dynamic.createMap(ImmutableMap.of(dynamic.createString("type"), dynamic.createString("minecraft:the_end"), dynamic.createString("seed"), dynamic.createLong(l)))).getValue()))));
    }

    private static <T> Map<Dynamic<T>, Dynamic<T>> fixFlatStructures(DynamicOps<T> dynamicOps, OptionalDynamic<T> optionalDynamic) {
        MutableInt mutableInt = new MutableInt(32);
        MutableInt mutableInt2 = new MutableInt(3);
        MutableInt mutableInt3 = new MutableInt(128);
        MutableBoolean mutableBoolean = new MutableBoolean(false);
        Map<String, DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration> map = Maps.newHashMap();
        if (!optionalDynamic.result().isPresent()) {
            mutableBoolean.setTrue();
            map.put("minecraft:village", DEFAULTS.get("minecraft:village"));
        }

        optionalDynamic.get("structures").flatMap(Dynamic::getMapValues).result().ifPresent((map2) -> {
            map2.forEach((dynamic, dynamic2) -> {
                dynamic2.getMapValues().result().ifPresent((map2) -> {
                    map2.forEach((dynamic2, dynamic3) -> {
                        String string = dynamic.asString("");
                        String string2 = dynamic2.asString("");
                        String string3 = dynamic3.asString("");
                        if ("stronghold".equals(string)) {
                            mutableBoolean.setTrue();
                            switch(string2) {
                            case "distance":
                                mutableInt.setValue(getInt(string3, mutableInt.getValue(), 1));
                                return;
                            case "spread":
                                mutableInt2.setValue(getInt(string3, mutableInt2.getValue(), 1));
                                return;
                            case "count":
                                mutableInt3.setValue(getInt(string3, mutableInt3.getValue(), 1));
                                return;
                            default:
                            }
                        } else {
                            switch(string2) {
                            case "distance":
                                switch(string) {
                                case "village":
                                    setSpacing(map, "minecraft:village", string3, 9);
                                    return;
                                case "biome_1":
                                    setSpacing(map, "minecraft:desert_pyramid", string3, 9);
                                    setSpacing(map, "minecraft:igloo", string3, 9);
                                    setSpacing(map, "minecraft:jungle_pyramid", string3, 9);
                                    setSpacing(map, "minecraft:swamp_hut", string3, 9);
                                    setSpacing(map, "minecraft:pillager_outpost", string3, 9);
                                    return;
                                case "endcity":
                                    setSpacing(map, "minecraft:endcity", string3, 1);
                                    return;
                                case "mansion":
                                    setSpacing(map, "minecraft:mansion", string3, 1);
                                    return;
                                default:
                                    return;
                                }
                            case "separation":
                                if ("oceanmonument".equals(string)) {
                                    DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration structureFeatureConfiguration = map.getOrDefault("minecraft:monument", DEFAULTS.get("minecraft:monument"));
                                    int i = getInt(string3, structureFeatureConfiguration.separation, 1);
                                    map.put("minecraft:monument", new DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration(i, structureFeatureConfiguration.separation, structureFeatureConfiguration.salt));
                                }

                                return;
                            case "spacing":
                                if ("oceanmonument".equals(string)) {
                                    setSpacing(map, "minecraft:monument", string3, 1);
                                }

                                return;
                            default:
                            }
                        }
                    });
                });
            });
        });
        Builder<Dynamic<T>, Dynamic<T>> builder = ImmutableMap.builder();
        builder.put(optionalDynamic.createString("structures"), optionalDynamic.createMap(map.entrySet().stream().collect(Collectors.toMap((entry) -> {
            return optionalDynamic.createString(entry.getKey());
        }, (entry) -> {
            return entry.getValue().serialize(dynamicOps);
        }))));
        if (mutableBoolean.isTrue()) {
            builder.put(optionalDynamic.createString("stronghold"), optionalDynamic.createMap(ImmutableMap.of(optionalDynamic.createString("distance"), optionalDynamic.createInt(mutableInt.getValue()), optionalDynamic.createString("spread"), optionalDynamic.createInt(mutableInt2.getValue()), optionalDynamic.createString("count"), optionalDynamic.createInt(mutableInt3.getValue()))));
        }

        return builder.build();
    }

    private static int getInt(String string, int i) {
        return NumberUtils.toInt(string, i);
    }

    private static int getInt(String string, int i, int j) {
        return Math.max(j, getInt(string, i));
    }

    private static void setSpacing(Map<String, DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration> map, String string, String string2, int i) {
        DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration structureFeatureConfiguration = map.getOrDefault(string, DEFAULTS.get(string));
        int j = getInt(string2, structureFeatureConfiguration.spacing, i);
        map.put(string, new DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration(j, structureFeatureConfiguration.separation, structureFeatureConfiguration.salt));
    }

    static final class StructureFeatureConfiguration {
        public static final Codec<DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(Codec.INT.fieldOf("spacing").forGetter((structureFeatureConfiguration) -> {
                return structureFeatureConfiguration.spacing;
            }), Codec.INT.fieldOf("separation").forGetter((structureFeatureConfiguration) -> {
                return structureFeatureConfiguration.separation;
            }), Codec.INT.fieldOf("salt").forGetter((structureFeatureConfiguration) -> {
                return structureFeatureConfiguration.salt;
            })).apply(instance, DataConverterWorldGenSettingsBuilding.StructureFeatureConfiguration::new);
        });
        final int spacing;
        final int separation;
        final int salt;

        public StructureFeatureConfiguration(int spacing, int separation, int salt) {
            this.spacing = spacing;
            this.separation = separation;
            this.salt = salt;
        }

        public <T> Dynamic<T> serialize(DynamicOps<T> dynamicOps) {
            return new Dynamic<>(dynamicOps, CODEC.encodeStart(dynamicOps, this).result().orElse(dynamicOps.emptyMap()));
        }
    }
}
