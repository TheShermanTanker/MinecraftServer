package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.stream.Stream;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;

public class DataConverterItemLoreComponentize extends DataFix {
    public DataConverterItemLoreComponentize(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(DataConverterTypes.ITEM_STACK);
        OpticFinder<?> opticFinder = type.findField("tag");
        return this.fixTypeEverywhereTyped("Item Lore componentize", type, (typed) -> {
            return typed.updateTyped(opticFinder, (typedx) -> {
                return typedx.update(DSL.remainderFinder(), (dynamic) -> {
                    return dynamic.update("display", (dynamicx) -> {
                        return dynamicx.update("Lore", (dynamic) -> {
                            return DataFixUtils.orElse(dynamic.asStreamOpt().map(DataConverterItemLoreComponentize::fixLoreList).map(dynamic::createList).result(), dynamic);
                        });
                    });
                });
            });
        });
    }

    private static <T> Stream<Dynamic<T>> fixLoreList(Stream<Dynamic<T>> nbt) {
        return nbt.map((dynamic) -> {
            return DataFixUtils.orElse(dynamic.asString().map(DataConverterItemLoreComponentize::fixLoreEntry).map(dynamic::createString).result(), dynamic);
        });
    }

    private static String fixLoreEntry(String string) {
        return IChatBaseComponent.ChatSerializer.toJson(new ChatComponentText(string));
    }
}
