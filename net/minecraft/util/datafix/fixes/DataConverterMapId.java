package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import java.util.Optional;

public class DataConverterMapId extends DataFix {
    public DataConverterMapId(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(DataConverterTypes.SAVED_DATA);
        OpticFinder<?> opticFinder = type.findField("data");
        return this.fixTypeEverywhereTyped("Map id fix", type, (typed) -> {
            Optional<? extends Typed<?>> optional = typed.getOptionalTyped(opticFinder);
            return optional.isPresent() ? typed : typed.update(DSL.remainderFinder(), (dynamic) -> {
                return dynamic.createMap(ImmutableMap.of(dynamic.createString("data"), dynamic));
            });
        });
    }
}
