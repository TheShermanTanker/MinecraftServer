package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType.StringType;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.PacketDataSerializer;

public class ArgumentSerializerString implements ArgumentSerializer<StringArgumentType> {
    @Override
    public void serializeToNetwork(StringArgumentType stringArgumentType, PacketDataSerializer friendlyByteBuf) {
        friendlyByteBuf.writeEnum(stringArgumentType.getType());
    }

    @Override
    public StringArgumentType deserializeFromNetwork(PacketDataSerializer friendlyByteBuf) {
        StringType stringType = friendlyByteBuf.readEnum(StringType.class);
        switch(stringType) {
        case SINGLE_WORD:
            return StringArgumentType.word();
        case QUOTABLE_PHRASE:
            return StringArgumentType.string();
        case GREEDY_PHRASE:
        default:
            return StringArgumentType.greedyString();
        }
    }

    @Override
    public void serializeToJson(StringArgumentType stringArgumentType, JsonObject jsonObject) {
        switch(stringArgumentType.getType()) {
        case SINGLE_WORD:
            jsonObject.addProperty("type", "word");
            break;
        case QUOTABLE_PHRASE:
            jsonObject.addProperty("type", "phrase");
            break;
        case GREEDY_PHRASE:
        default:
            jsonObject.addProperty("type", "greedy");
        }

    }
}
