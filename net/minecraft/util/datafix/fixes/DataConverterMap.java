package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.DataConverterSchemaNamed;

public class DataConverterMap extends DataFix {
    public DataConverterMap(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    @Override
    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(DataConverterTypes.ITEM_STACK);
        OpticFinder<Pair<String, String>> opticFinder = DSL.fieldFinder("id", DSL.named(DataConverterTypes.ITEM_NAME.typeName(), DataConverterSchemaNamed.namespacedString()));
        OpticFinder<?> opticFinder2 = type.findField("tag");
        return this.fixTypeEverywhereTyped("ItemInstanceMapIdFix", type, (typed) -> {
            Optional<Pair<String, String>> optional = typed.getOptional(opticFinder);
            if (optional.isPresent() && Objects.equals(optional.get().getSecond(), "minecraft:filled_map")) {
                Dynamic<?> dynamic = typed.get(DSL.remainderFinder());
                Typed<?> typed2 = typed.getOrCreateTyped(opticFinder2);
                Dynamic<?> dynamic2 = typed2.get(DSL.remainderFinder());
                dynamic2 = dynamic2.set("map", dynamic2.createInt(dynamic.get("Damage").asInt(0)));
                return typed.set(opticFinder2, typed2.set(DSL.remainderFinder(), dynamic2));
            } else {
                return typed;
            }
        });
    }
}
