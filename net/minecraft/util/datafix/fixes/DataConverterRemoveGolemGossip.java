package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class DataConverterRemoveGolemGossip extends DataConverterNamedEntity {
    public DataConverterRemoveGolemGossip(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "Remove Golem Gossip Fix", DataConverterTypes.ENTITY, "minecraft:villager");
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), DataConverterRemoveGolemGossip::fixValue);
    }

    private static Dynamic<?> fixValue(Dynamic<?> villagerData) {
        return villagerData.update("Gossips", (dynamic2) -> {
            return villagerData.createList(dynamic2.asStream().filter((dynamic) -> {
                return !dynamic.get("Type").asString("").equals("golem");
            }));
        });
    }
}
