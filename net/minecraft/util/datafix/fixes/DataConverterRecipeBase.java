package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.util.datafix.schemas.DataConverterSchemaNamed;

public class DataConverterRecipeBase extends DataFix {
    private final String name;
    private final Function<String, String> renamer;

    public DataConverterRecipeBase(Schema outputSchema, boolean changesType, String name, Function<String, String> renamer) {
        super(outputSchema, changesType);
        this.name = name;
        this.renamer = renamer;
    }

    protected TypeRewriteRule makeRule() {
        Type<Pair<String, String>> type = DSL.named(DataConverterTypes.RECIPE.typeName(), DataConverterSchemaNamed.namespacedString());
        if (!Objects.equals(type, this.getInputSchema().getType(DataConverterTypes.RECIPE))) {
            throw new IllegalStateException("Recipe type is not what was expected.");
        } else {
            return this.fixTypeEverywhere(this.name, type, (dynamicOps) -> {
                return (pair) -> {
                    return pair.mapSecond(this.renamer);
                };
            });
        }
    }
}
