package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.FieldFinder;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.CompoundList.CompoundListType;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import java.util.List;
import net.minecraft.util.datafix.schemas.DataConverterSchemaNamed;

public class DataConverterMissingDimension extends DataFix {
    public DataConverterMissingDimension(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    protected static <A> Type<Pair<A, Dynamic<?>>> fields(String string, Type<A> type) {
        return DSL.and(DSL.field(string, type), DSL.remainderType());
    }

    protected static <A> Type<Pair<Either<A, Unit>, Dynamic<?>>> optionalFields(String string, Type<A> type) {
        return DSL.and(DSL.optional(DSL.field(string, type)), DSL.remainderType());
    }

    protected static <A1, A2> Type<Pair<Either<A1, Unit>, Pair<Either<A2, Unit>, Dynamic<?>>>> optionalFields(String string, Type<A1> type, String string2, Type<A2> type2) {
        return DSL.and(DSL.optional(DSL.field(string, type)), DSL.optional(DSL.field(string2, type2)), DSL.remainderType());
    }

    protected TypeRewriteRule makeRule() {
        Schema schema = this.getInputSchema();
        TaggedChoiceType<String> taggedChoiceType = new TaggedChoiceType<>("type", DSL.string(), ImmutableMap.of("minecraft:debug", DSL.remainderType(), "minecraft:flat", flatType(schema), "minecraft:noise", optionalFields("biome_source", DSL.taggedChoiceType("type", DSL.string(), ImmutableMap.of("minecraft:fixed", fields("biome", schema.getType(DataConverterTypes.BIOME)), "minecraft:multi_noise", DSL.list(fields("biome", schema.getType(DataConverterTypes.BIOME))), "minecraft:checkerboard", fields("biomes", DSL.list(schema.getType(DataConverterTypes.BIOME))), "minecraft:vanilla_layered", DSL.remainderType(), "minecraft:the_end", DSL.remainderType())), "settings", DSL.or(DSL.string(), optionalFields("default_block", schema.getType(DataConverterTypes.BLOCK_NAME), "default_fluid", schema.getType(DataConverterTypes.BLOCK_NAME))))));
        CompoundListType<String, ?> compoundListType = DSL.compoundList(DataConverterSchemaNamed.namespacedString(), fields("generator", taggedChoiceType));
        Type<?> type = DSL.and(compoundListType, DSL.remainderType());
        Type<?> type2 = schema.getType(DataConverterTypes.WORLD_GEN_SETTINGS);
        FieldFinder<?> fieldFinder = new FieldFinder<>("dimensions", type);
        if (!type2.findFieldType("dimensions").equals(type)) {
            throw new IllegalStateException();
        } else {
            OpticFinder<? extends List<? extends Pair<String, ?>>> opticFinder = compoundListType.finder();
            return this.fixTypeEverywhereTyped("MissingDimensionFix", type2, (typed) -> {
                return typed.updateTyped(fieldFinder, (typed2) -> {
                    return typed2.updateTyped(opticFinder, (typed2x) -> {
                        if (!(typed2x.getValue() instanceof List)) {
                            throw new IllegalStateException("List exptected");
                        } else if (((List)typed2x.getValue()).isEmpty()) {
                            Dynamic<?> dynamic = typed.get(DSL.remainderFinder());
                            Dynamic<?> dynamic2 = this.recreateSettings(dynamic);
                            return DataFixUtils.orElse(compoundListType.readTyped(dynamic2).result().map(Pair::getFirst), typed2x);
                        } else {
                            return typed2x;
                        }
                    });
                });
            });
        }
    }

    protected static Type<? extends Pair<? extends Either<? extends Pair<? extends Either<?, Unit>, ? extends Pair<? extends Either<? extends List<? extends Pair<? extends Either<?, Unit>, Dynamic<?>>>, Unit>, Dynamic<?>>>, Unit>, Dynamic<?>>> flatType(Schema schema) {
        return optionalFields("settings", optionalFields("biome", schema.getType(DataConverterTypes.BIOME), "layers", DSL.list(optionalFields("block", schema.getType(DataConverterTypes.BLOCK_NAME)))));
    }

    private <T> Dynamic<T> recreateSettings(Dynamic<T> dynamic) {
        long l = dynamic.get("seed").asLong(0L);
        return new Dynamic<>(dynamic.getOps(), DataConverterWorldGenSettingsBuilding.vanillaLevels(dynamic, l, DataConverterWorldGenSettingsBuilding.defaultOverworld(dynamic, l), false));
    }
}
