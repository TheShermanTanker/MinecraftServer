package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.function.Function;

public class ChunkRenamesFix extends DataFix {
    public ChunkRenamesFix(Schema schema) {
        super(schema, true);
    }

    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(DataConverterTypes.CHUNK);
        OpticFinder<?> opticFinder = type.findField("Level");
        OpticFinder<?> opticFinder2 = opticFinder.type().findField("Structures");
        Type<?> type2 = this.getOutputSchema().getType(DataConverterTypes.CHUNK);
        Type<?> type3 = type2.findFieldType("structures");
        return this.fixTypeEverywhereTyped("Chunk Renames; purge Level-tag", type, type2, (typed) -> {
            Typed<?> typed2 = typed.getTyped(opticFinder);
            Typed<?> typed3 = appendChunkName(typed2);
            typed3 = typed3.set(DSL.remainderFinder(), mergeRemainders(typed, typed2.get(DSL.remainderFinder())));
            typed3 = renameField(typed3, "TileEntities", "block_entities");
            typed3 = renameField(typed3, "TileTicks", "block_ticks");
            typed3 = renameField(typed3, "Entities", "entities");
            typed3 = renameField(typed3, "Sections", "sections");
            typed3 = typed3.updateTyped(opticFinder2, type3, (typedx) -> {
                return renameField(typedx, "Starts", "starts");
            });
            typed3 = renameField(typed3, "Structures", "structures");
            return typed3.update(DSL.remainderFinder(), (dynamic) -> {
                return dynamic.remove("Level");
            });
        });
    }

    private static Typed<?> renameField(Typed<?> typed, String oldKey, String newKey) {
        return renameFieldHelper(typed, oldKey, newKey, typed.getType().findFieldType(oldKey)).update(DSL.remainderFinder(), (dynamic) -> {
            return dynamic.remove(oldKey);
        });
    }

    private static <A> Typed<?> renameFieldHelper(Typed<?> typed, String oldKey, String newKey, Type<A> type) {
        Type<Either<A, Unit>> type2 = DSL.optional(DSL.field(oldKey, type));
        Type<Either<A, Unit>> type3 = DSL.optional(DSL.field(newKey, type));
        return typed.update(type2.finder(), type3, Function.identity());
    }

    private static <A> Typed<Pair<String, A>> appendChunkName(Typed<A> typed) {
        return new Typed<>(DSL.named("chunk", typed.getType()), typed.getOps(), Pair.of("chunk", typed.getValue()));
    }

    private static <T> Dynamic<T> mergeRemainders(Typed<?> typed, Dynamic<T> dynamic) {
        DynamicOps<T> dynamicOps = dynamic.getOps();
        Dynamic<T> dynamic2 = typed.get(DSL.remainderFinder()).convert(dynamicOps);
        DataResult<T> dataResult = dynamicOps.getMap(dynamic.getValue()).flatMap((mapLike) -> {
            return dynamicOps.mergeToMap(dynamic2.getValue(), mapLike);
        });
        return dataResult.result().map((object) -> {
            return new Dynamic<>(dynamicOps, object);
        }).orElse(dynamic);
    }
}
