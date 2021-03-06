package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.network.chat.ChatMessage;

public class ArgumentTime implements ArgumentType<Integer> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0d", "0s", "0t", "0");
    private static final SimpleCommandExceptionType ERROR_INVALID_UNIT = new SimpleCommandExceptionType(new ChatMessage("argument.time.invalid_unit"));
    private static final DynamicCommandExceptionType ERROR_INVALID_TICK_COUNT = new DynamicCommandExceptionType((time) -> {
        return new ChatMessage("argument.time.invalid_tick_count", time);
    });
    private static final Object2IntMap<String> UNITS = new Object2IntOpenHashMap<>();

    public static ArgumentTime time() {
        return new ArgumentTime();
    }

    public Integer parse(StringReader stringReader) throws CommandSyntaxException {
        float f = stringReader.readFloat();
        String string = stringReader.readUnquotedString();
        int i = UNITS.getOrDefault(string, 0);
        if (i == 0) {
            throw ERROR_INVALID_UNIT.create();
        } else {
            int j = Math.round(f * (float)i);
            if (j < 0) {
                throw ERROR_INVALID_TICK_COUNT.create(j);
            } else {
                return j;
            }
        }
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        StringReader stringReader = new StringReader(suggestionsBuilder.getRemaining());

        try {
            stringReader.readFloat();
        } catch (CommandSyntaxException var5) {
            return suggestionsBuilder.buildFuture();
        }

        return ICompletionProvider.suggest(UNITS.keySet(), suggestionsBuilder.createOffset(suggestionsBuilder.getStart() + stringReader.getCursor()));
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    static {
        UNITS.put("d", 24000);
        UNITS.put("s", 20);
        UNITS.put("t", 1);
        UNITS.put("", 1);
    }
}
