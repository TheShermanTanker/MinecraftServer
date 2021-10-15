package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.util.MathHelper;
import net.minecraft.world.scores.ScoreboardScore;

public class ArgumentMathOperation implements ArgumentType<ArgumentMathOperation.Operation> {
    private static final Collection<String> EXAMPLES = Arrays.asList("=", ">", "<");
    private static final SimpleCommandExceptionType ERROR_INVALID_OPERATION = new SimpleCommandExceptionType(new ChatMessage("arguments.operation.invalid"));
    private static final SimpleCommandExceptionType ERROR_DIVIDE_BY_ZERO = new SimpleCommandExceptionType(new ChatMessage("arguments.operation.div0"));

    public static ArgumentMathOperation operation() {
        return new ArgumentMathOperation();
    }

    public static ArgumentMathOperation.Operation getOperation(CommandContext<CommandListenerWrapper> commandContext, String string) {
        return commandContext.getArgument(string, ArgumentMathOperation.Operation.class);
    }

    @Override
    public ArgumentMathOperation.Operation parse(StringReader stringReader) throws CommandSyntaxException {
        if (!stringReader.canRead()) {
            throw ERROR_INVALID_OPERATION.create();
        } else {
            int i = stringReader.getCursor();

            while(stringReader.canRead() && stringReader.peek() != ' ') {
                stringReader.skip();
            }

            return getOperation(stringReader.getString().substring(i, stringReader.getCursor()));
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return ICompletionProvider.suggest(new String[]{"=", "+=", "-=", "*=", "/=", "%=", "<", ">", "><"}, suggestionsBuilder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    private static ArgumentMathOperation.Operation getOperation(String string) throws CommandSyntaxException {
        return (ArgumentMathOperation.Operation)(string.equals("><") ? (score, score2) -> {
            int i = score.getScore();
            score.setScore(score2.getScore());
            score2.setScore(i);
        } : getSimpleOperation(string));
    }

    private static ArgumentMathOperation.SimpleOperation getSimpleOperation(String string) throws CommandSyntaxException {
        switch(string) {
        case "=":
            return (i, j) -> {
                return j;
            };
        case "+=":
            return (i, j) -> {
                return i + j;
            };
        case "-=":
            return (i, j) -> {
                return i - j;
            };
        case "*=":
            return (i, j) -> {
                return i * j;
            };
        case "/=":
            return (i, j) -> {
                if (j == 0) {
                    throw ERROR_DIVIDE_BY_ZERO.create();
                } else {
                    return MathHelper.intFloorDiv(i, j);
                }
            };
        case "%=":
            return (i, j) -> {
                if (j == 0) {
                    throw ERROR_DIVIDE_BY_ZERO.create();
                } else {
                    return MathHelper.positiveModulo(i, j);
                }
            };
        case "<":
            return Math::min;
        case ">":
            return Math::max;
        default:
            throw ERROR_INVALID_OPERATION.create();
        }
    }

    @FunctionalInterface
    public interface Operation {
        void apply(ScoreboardScore score, ScoreboardScore score2) throws CommandSyntaxException;
    }

    @FunctionalInterface
    interface SimpleOperation extends ArgumentMathOperation.Operation {
        int apply(int i, int j) throws CommandSyntaxException;

        @Override
        default void apply(ScoreboardScore score, ScoreboardScore score2) throws CommandSyntaxException {
            score.setScore(this.apply(score.getScore(), score2.getScore()));
        }
    }
}