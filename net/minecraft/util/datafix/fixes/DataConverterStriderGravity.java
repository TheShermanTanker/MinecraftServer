package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class DataConverterStriderGravity extends DataConverterNamedEntity {
    public DataConverterStriderGravity(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "StriderGravityFix", DataConverterTypes.ENTITY, "minecraft:strider");
    }

    public Dynamic<?> fixTag(Dynamic<?> dynamic) {
        return dynamic.get("NoGravity").asBoolean(false) ? dynamic.set("NoGravity", dynamic.createBoolean(false)) : dynamic;
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), this::fixTag);
    }
}
