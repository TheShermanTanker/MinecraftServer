package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;

public abstract class DataConverterEntityName extends DataFix {
    protected final String name;

    public DataConverterEntityName(String name, Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
        this.name = name;
    }

    public TypeRewriteRule makeRule() {
        TaggedChoiceType<String> taggedChoiceType = this.getInputSchema().findChoiceType(DataConverterTypes.ENTITY);
        TaggedChoiceType<String> taggedChoiceType2 = this.getOutputSchema().findChoiceType(DataConverterTypes.ENTITY);
        return this.fixTypeEverywhere(this.name, taggedChoiceType, taggedChoiceType2, (dynamicOps) -> {
            return (pair) -> {
                String string = pair.getFirst();
                Type<?> type = taggedChoiceType.types().get(string);
                Pair<String, Typed<?>> pair2 = this.fix(string, this.getEntity(pair.getSecond(), dynamicOps, type));
                Type<?> type2 = taggedChoiceType2.types().get(pair2.getFirst());
                if (!type2.equals(pair2.getSecond().getType(), true, true)) {
                    throw new IllegalStateException(String.format("Dynamic type check failed: %s not equal to %s", type2, pair2.getSecond().getType()));
                } else {
                    return Pair.of(pair2.getFirst(), pair2.getSecond().getValue());
                }
            };
        });
    }

    private <A> Typed<A> getEntity(Object object, DynamicOps<?> dynamicOps, Type<A> type) {
        return new Typed<>(type, dynamicOps, (A)object);
    }

    protected abstract Pair<String, Typed<?>> fix(String choice, Typed<?> typed);
}
