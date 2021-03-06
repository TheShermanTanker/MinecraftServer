package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;

public class DataConverterCustomNameItem extends DataFix {
    public DataConverterCustomNameItem(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    private Dynamic<?> fixTag(Dynamic<?> dynamic) {
        Optional<? extends Dynamic<?>> optional = dynamic.get("display").result();
        if (optional.isPresent()) {
            Dynamic<?> dynamic2 = optional.get();
            Optional<String> optional2 = dynamic2.get("Name").asString().result();
            if (optional2.isPresent()) {
                dynamic2 = dynamic2.set("Name", dynamic2.createString(IChatBaseComponent.ChatSerializer.toJson(new ChatComponentText(optional2.get()))));
            } else {
                Optional<String> optional3 = dynamic2.get("LocName").asString().result();
                if (optional3.isPresent()) {
                    dynamic2 = dynamic2.set("Name", dynamic2.createString(IChatBaseComponent.ChatSerializer.toJson(new ChatMessage(optional3.get()))));
                    dynamic2 = dynamic2.remove("LocName");
                }
            }

            return dynamic.set("display", dynamic2);
        } else {
            return dynamic;
        }
    }

    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(DataConverterTypes.ITEM_STACK);
        OpticFinder<?> opticFinder = type.findField("tag");
        return this.fixTypeEverywhereTyped("ItemCustomNameToComponentFix", type, (typed) -> {
            return typed.updateTyped(opticFinder, (typedx) -> {
                return typedx.update(DSL.remainderFinder(), this::fixTag);
            });
        });
    }
}
