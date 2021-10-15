package net.minecraft.commands.arguments;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3D;

public class ArgumentAnchor implements ArgumentType<ArgumentAnchor.Anchor> {
    private static final Collection<String> EXAMPLES = Arrays.asList("eyes", "feet");
    private static final DynamicCommandExceptionType ERROR_INVALID = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("argument.anchor.invalid", object);
    });

    public static ArgumentAnchor.Anchor getAnchor(CommandContext<CommandListenerWrapper> commandContext, String string) {
        return commandContext.getArgument(string, ArgumentAnchor.Anchor.class);
    }

    public static ArgumentAnchor anchor() {
        return new ArgumentAnchor();
    }

    @Override
    public ArgumentAnchor.Anchor parse(StringReader stringReader) throws CommandSyntaxException {
        int i = stringReader.getCursor();
        String string = stringReader.readUnquotedString();
        ArgumentAnchor.Anchor anchor = ArgumentAnchor.Anchor.getByName(string);
        if (anchor == null) {
            stringReader.setCursor(i);
            throw ERROR_INVALID.createWithContext(stringReader, string);
        } else {
            return anchor;
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return ICompletionProvider.suggest(ArgumentAnchor.Anchor.BY_NAME.keySet(), suggestionsBuilder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static enum Anchor {
        FEET("feet", (vec3, entity) -> {
            return vec3;
        }),
        EYES("eyes", (vec3, entity) -> {
            return new Vec3D(vec3.x, vec3.y + (double)entity.getHeadHeight(), vec3.z);
        });

        static final Map<String, ArgumentAnchor.Anchor> BY_NAME = SystemUtils.make(Maps.newHashMap(), (hashMap) -> {
            for(ArgumentAnchor.Anchor anchor : values()) {
                hashMap.put(anchor.name, anchor);
            }

        });
        private final String name;
        private final BiFunction<Vec3D, Entity, Vec3D> transform;

        private Anchor(String id, BiFunction<Vec3D, Entity, Vec3D> offset) {
            this.name = id;
            this.transform = offset;
        }

        @Nullable
        public static ArgumentAnchor.Anchor getByName(String id) {
            return BY_NAME.get(id);
        }

        public Vec3D apply(Entity entity) {
            return this.transform.apply(entity.getPositionVector(), entity);
        }

        public Vec3D apply(CommandListenerWrapper commandSourceStack) {
            Entity entity = commandSourceStack.getEntity();
            return entity == null ? commandSourceStack.getPosition() : this.transform.apply(commandSourceStack.getPosition(), entity);
        }
    }
}
