package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class DataConverterJigsawProperties extends DataConverterNamedEntity {
    public DataConverterJigsawProperties(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "JigsawPropertiesFix", DataConverterTypes.BLOCK_ENTITY, "minecraft:jigsaw");
    }

    private static Dynamic<?> fixTag(Dynamic<?> dynamic) {
        String string = dynamic.get("attachement_type").asString("minecraft:empty");
        String string2 = dynamic.get("target_pool").asString("minecraft:empty");
        return dynamic.set("name", dynamic.createString(string)).set("target", dynamic.createString(string)).remove("attachement_type").set("pool", dynamic.createString(string2)).remove("target_pool");
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), DataConverterJigsawProperties::fixTag);
    }
}
