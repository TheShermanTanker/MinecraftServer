package net.minecraft.util.datafix.schemas;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.DataConverterTypes;

public class V2832 extends DataConverterSchemaNamed {
    public V2832(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> map, Map<String, Supplier<TypeTemplate>> map2) {
        super.registerTypes(schema, map, map2);
        schema.registerType(false, DataConverterTypes.CHUNK, () -> {
            return DSL.fields("Level", DSL.optionalFields("Entities", DSL.list(DataConverterTypes.ENTITY_TREE.in(schema)), "TileEntities", DSL.list(DataConverterTypes.BLOCK_ENTITY.in(schema)), "TileTicks", DSL.list(DSL.fields("i", DataConverterTypes.BLOCK_NAME.in(schema))), "Sections", DSL.list(DSL.optionalFields("biomes", DSL.optionalFields("palette", DSL.list(DataConverterTypes.BIOME.in(schema))), "block_states", DSL.optionalFields("palette", DSL.list(DataConverterTypes.BLOCK_STATE.in(schema))))), "Structures", DSL.optionalFields("Starts", DSL.compoundList(DataConverterTypes.STRUCTURE_FEATURE.in(schema)))));
        });
        schema.registerType(false, DataConverterTypes.WORLD_GEN_SETTINGS, () -> {
            return DSL.fields("dimensions", DSL.compoundList(DSL.constType(namespacedString()), DSL.fields("generator", DSL.taggedChoiceLazy("type", DSL.string(), ImmutableMap.of("minecraft:debug", DSL::remainder, "minecraft:flat", () -> {
                return DSL.optionalFields("settings", DSL.optionalFields("biome", DataConverterTypes.BIOME.in(schema), "layers", DSL.list(DSL.optionalFields("block", DataConverterTypes.BLOCK_NAME.in(schema)))));
            }, "minecraft:noise", () -> {
                return DSL.optionalFields("biome_source", DSL.taggedChoiceLazy("type", DSL.string(), ImmutableMap.of("minecraft:fixed", () -> {
                    return DSL.fields("biome", DataConverterTypes.BIOME.in(schema));
                }, "minecraft:multi_noise", () -> {
                    return DSL.or(DSL.fields("preset", namespacedString().template()), DSL.list(DSL.fields("biome", DataConverterTypes.BIOME.in(schema))));
                }, "minecraft:checkerboard", () -> {
                    return DSL.fields("biomes", DSL.list(DataConverterTypes.BIOME.in(schema)));
                }, "minecraft:the_end", DSL::remainder)), "settings", DSL.or(DSL.constType(DSL.string()), DSL.optionalFields("default_block", DataConverterTypes.BLOCK_NAME.in(schema), "default_fluid", DataConverterTypes.BLOCK_NAME.in(schema))));
            })))));
        });
    }
}
