package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.core.IRegistry;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.entity.EntityTypes;

public class ArgumentEntitySummon implements ArgumentType<MinecraftKey> {
    private static final Collection<String> EXAMPLES = Arrays.asList("minecraft:pig", "cow");
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_ENTITY = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("entity.notFound", object);
    });

    public static ArgumentEntitySummon id() {
        return new ArgumentEntitySummon();
    }

    public static MinecraftKey getSummonableEntity(CommandContext<CommandListenerWrapper> context, String name) throws CommandSyntaxException {
        return verifyCanSummon(context.getArgument(name, MinecraftKey.class));
    }

    private static MinecraftKey verifyCanSummon(MinecraftKey resourceLocation) throws CommandSyntaxException {
        IRegistry.ENTITY_TYPE.getOptional(resourceLocation).filter(EntityTypes::canSummon).orElseThrow(() -> {
            return ERROR_UNKNOWN_ENTITY.create(resourceLocation);
        });
        return resourceLocation;
    }

    @Override
    public MinecraftKey parse(StringReader stringReader) throws CommandSyntaxException {
        return verifyCanSummon(MinecraftKey.read(stringReader));
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
