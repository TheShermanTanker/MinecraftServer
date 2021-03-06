package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;

public class DataConverterGossip extends DataConverterNamedEntity {
    public DataConverterGossip(Schema outputSchema, String choiceType) {
        super(outputSchema, false, "Gossip for for " + choiceType, DataConverterTypes.ENTITY, choiceType);
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), (dynamic) -> {
            return dynamic.update("Gossips", (dynamicx) -> {
                return DataFixUtils.orElse(dynamicx.asStreamOpt().result().map((stream) -> {
                    return stream.map((dynamic) -> {
                        return DataConverterUUIDBase.replaceUUIDLeastMost(dynamic, "Target", "Target").orElse(dynamic);
                    });
                }).map(dynamicx::createList), dynamicx);
            });
        });
    }
}
