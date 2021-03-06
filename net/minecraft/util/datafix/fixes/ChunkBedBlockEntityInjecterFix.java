package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class ChunkBedBlockEntityInjecterFix extends DataFix {
    public ChunkBedBlockEntityInjecterFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        Type<?> type = this.getOutputSchema().getType(DataConverterTypes.CHUNK);
        Type<?> type2 = type.findFieldType("Level");
        Type<?> type3 = type2.findFieldType("TileEntities");
        if (!(type3 instanceof ListType)) {
            throw new IllegalStateException("Tile entity type is not a list type.");
        } else {
            ListType<?> listType = (ListType)type3;
            return this.cap(type2, listType);
        }
    }

    private <TE> TypeRewriteRule cap(Type<?> level, ListType<TE> blockEntities) {
        Type<TE> type = blockEntities.getElement();
        OpticFinder<?> opticFinder = DSL.fieldFinder("Level", level);
        OpticFinder<List<TE>> opticFinder2 = DSL.fieldFinder("TileEntities", blockEntities);
        int i = 416;
        return TypeRewriteRule.seq(this.fixTypeEverywhere("InjectBedBlockEntityType", this.getInputSchema().findChoiceType(DataConverterTypes.BLOCK_ENTITY), this.getOutputSchema().findChoiceType(DataConverterTypes.BLOCK_ENTITY), (dynamicOps) -> {
            return (pair) -> {
                return pair;
            };
        }), this.fixTypeEverywhereTyped("BedBlockEntityInjecter", this.getOutputSchema().getType(DataConverterTypes.CHUNK), (typed) -> {
            Typed<?> typed2 = typed.getTyped(opticFinder);
            Dynamic<?> dynamic = typed2.get(DSL.remainderFinder());
            int i = dynamic.get("xPos").asInt(0);
            int j = dynamic.get("zPos").asInt(0);
            List<TE> list = Lists.newArrayList(typed2.getOrCreate(opticFinder2));
            List<? extends Dynamic<?>> list2 = dynamic.get("Sections").asList(Function.identity());

            for(int k = 0; k < list2.size(); ++k) {
                Dynamic<?> dynamic2 = list2.get(k);
                int l = dynamic2.get("Y").asInt(0);
                Stream<Integer> stream = dynamic2.get("Blocks").asStream().map((dynamicx) -> {
                    return dynamicx.asInt(0);
                });
                int m = 0;

                for(int n : stream::iterator) {
                    if (416 == (n & 255) << 4) {
                        int o = m & 15;
                        int p = m >> 8 & 15;
                        int q = m >> 4 & 15;
                        Map<Dynamic<?>, Dynamic<?>> map = Maps.newHashMap();
                        map.put(dynamic2.createString("id"), dynamic2.createString("minecraft:bed"));
                        map.put(dynamic2.createString("x"), dynamic2.createInt(o + (i << 4)));
                        map.put(dynamic2.createString("y"), dynamic2.createInt(p + (l << 4)));
                        map.put(dynamic2.createString("z"), dynamic2.createInt(q + (j << 4)));
                        map.put(dynamic2.createString("color"), dynamic2.createShort((short)14));
                        list.add(type.read(dynamic2.createMap(map)).result().orElseThrow(() -> {
                            return new IllegalStateException("Could not parse newly created bed block entity.");
                        }).getFirst());
                    }

                    ++m;
                }
            }

            return !list.isEmpty() ? typed.set(opticFinder, typed2.set(opticFinder2, list)) : typed;
        }));
    }
}
