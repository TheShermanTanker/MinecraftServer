package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

public class DataConverterIglooMetadataRemoval extends DataFix {
    public DataConverterIglooMetadataRemoval(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(DataConverterTypes.STRUCTURE_FEATURE);
        Type<?> type2 = this.getOutputSchema().getType(DataConverterTypes.STRUCTURE_FEATURE);
        return this.writeFixAndRead("IglooMetadataRemovalFix", type, type2, DataConverterIglooMetadataRemoval::fixTag);
    }

    private static <T> Dynamic<T> fixTag(Dynamic<T> dynamic) {
        boolean bl = dynamic.get("Children").asStreamOpt().map((stream) -> {
            return stream.allMatch(DataConverterIglooMetadataRemoval::isIglooPiece);
        }).result().orElse(false);
        return bl ? dynamic.set("id", dynamic.createString("Igloo")).remove("Children") : dynamic.update("Children", DataConverterIglooMetadataRemoval::removeIglooPieces);
    }

    private static <T> Dynamic<T> removeIglooPieces(Dynamic<T> dynamic) {
        return dynamic.asStreamOpt().map((stream) -> {
            return stream.filter((dynamic) -> {
                return !isIglooPiece(dynamic);
            });
        }).map(dynamic::createList).result().orElse(dynamic);
    }

    private static boolean isIglooPiece(Dynamic<?> dynamic) {
        return dynamic.get("id").asString("").equals("Iglu");
    }
}
