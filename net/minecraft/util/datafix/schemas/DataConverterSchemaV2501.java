package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.DataConverterTypes;

public class DataConverterSchemaV2501 extends DataConverterSchemaNamed {
    public DataConverterSchemaV2501(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    private static void registerFurnace(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
        schema.register(map, name, () -> {
            return DSL.optionalFields("Items", DSL.list(DataConverterTypes.ITEM_STACK.in(schema)), "RecipesUsed", DSL.compoundList(DataConverterTypes.RECIPE.in(schema), DSL.constType(DSL.intType())));
        });
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
        registerFurnace(schema, map, "minecraft:furnace");
        registerFurnace(schema, map, "minecraft:smoker");
        registerFurnace(schema, map, "minecraft:blast_furnace");
        return map;
    }
}
