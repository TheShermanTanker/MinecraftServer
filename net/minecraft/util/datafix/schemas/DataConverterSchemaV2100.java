package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.DataConverterTypes;

public class DataConverterSchemaV2100 extends DataConverterSchemaNamed {
    public DataConverterSchemaV2100(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    protected static void registerMob(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, String name) {
        schema.register(entityTypes, name, () -> {
            return DataConverterSchemaV100.equipment(schema);
        });
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
        registerMob(schema, map, "minecraft:bee");
        registerMob(schema, map, "minecraft:bee_stinger");
        return map;
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
        schema.register(map, "minecraft:beehive", () -> {
            return DSL.optionalFields("Bees", DSL.list(DSL.optionalFields("EntityData", DataConverterTypes.ENTITY_TREE.in(schema))));
        });
        return map;
    }
}
