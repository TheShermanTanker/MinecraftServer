package net.minecraft.commands.arguments.blocks;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.MojangsonParser;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.Tag;
import net.minecraft.tags.Tags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;

public class ArgumentBlock {
    public static final SimpleCommandExceptionType ERROR_NO_TAGS_ALLOWED = new SimpleCommandExceptionType(new ChatMessage("argument.block.tag.disallowed"));
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_BLOCK = new DynamicCommandExceptionType((block) -> {
        return new ChatMessage("argument.block.id.invalid", block);
    });
    public static final Dynamic2CommandExceptionType ERROR_UNKNOWN_PROPERTY = new Dynamic2CommandExceptionType((block, property) -> {
        return new ChatMessage("argument.block.property.unknown", block, property);
    });
    public static final Dynamic2CommandExceptionType ERROR_DUPLICATE_PROPERTY = new Dynamic2CommandExceptionType((block, property) -> {
        return new ChatMessage("argument.block.property.duplicate", property, block);
    });
    public static final Dynamic3CommandExceptionType ERROR_INVALID_VALUE = new Dynamic3CommandExceptionType((block, property, value) -> {
        return new ChatMessage("argument.block.property.invalid", block, value, property);
    });
    public static final Dynamic2CommandExceptionType ERROR_EXPECTED_VALUE = new Dynamic2CommandExceptionType((block, property) -> {
        return new ChatMessage("argument.block.property.novalue", block, property);
    });
    public static final SimpleCommandExceptionType ERROR_EXPECTED_END_OF_PROPERTIES = new SimpleCommandExceptionType(new ChatMessage("argument.block.property.unclosed"));
    private static final char SYNTAX_START_PROPERTIES = '[';
    private static final char SYNTAX_START_NBT = '{';
    private static final char SYNTAX_END_PROPERTIES = ']';
    private static final char SYNTAX_EQUALS = '=';
    private static final char SYNTAX_PROPERTY_SEPARATOR = ',';
    private static final char SYNTAX_TAG = '#';
    private static final BiFunction<SuggestionsBuilder, Tags<Block>, CompletableFuture<Suggestions>> SUGGEST_NOTHING = (builder, tagGroup) -> {
        return builder.buildFuture();
    };
    private final StringReader reader;
    private final boolean forTesting;
    private final Map<IBlockState<?>, Comparable<?>> properties = Maps.newHashMap();
    private final Map<String, String> vagueProperties = Maps.newHashMap();
    public MinecraftKey id = new MinecraftKey("");
    private BlockStateList<Block, IBlockData> definition;
    private IBlockData state;
    @Nullable
    private NBTTagCompound nbt;
    private MinecraftKey tag = new MinecraftKey("");
    private int tagCursor;
    private BiFunction<SuggestionsBuilder, Tags<Block>, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;

    public ArgumentBlock(StringReader reader, boolean allowTag) {
        this.reader = reader;
        this.forTesting = allowTag;
    }

    public Map<IBlockState<?>, Comparable<?>> getStateMap() {
        return this.properties;
    }

    @Nullable
    public IBlockData getBlockData() {
        return this.state;
    }

    @Nullable
    public NBTTagCompound getNbt() {
        return this.nbt;
    }

    @Nullable
    public MinecraftKey getTag() {
        return this.tag;
    }

    public ArgumentBlock parse(boolean allowNbt) throws CommandSyntaxException {
        this.suggestions = this::suggestBlockIdOrTag;
        if (this.reader.canRead() && this.reader.peek() == '#') {
            this.readTag();
            this.suggestions = this::suggestOpenVaguePropertiesOrNbt;
            if (this.reader.canRead() && this.reader.peek() == '[') {
                this.readVagueProperties();
                this.suggestions = this::suggestOpenNbt;
            }
        } else {
            this.readBlock();
            this.suggestions = this::suggestOpenPropertiesOrNbt;
            if (this.reader.canRead() && this.reader.peek() == '[') {
                this.readProperties();
                this.suggestions = this::suggestOpenNbt;
            }
        }

        if (allowNbt && this.reader.canRead() && this.reader.peek() == '{') {
            this.suggestions = SUGGEST_NOTHING;
            this.readNbt();
        }

        return this;
    }

    private CompletableFuture<Suggestions> suggestPropertyNameOrEnd(SuggestionsBuilder builder, Tags<Block> tagGroup) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf(']'));
        }

        return this.suggestPropertyName(builder, tagGroup);
    }

    private CompletableFuture<Suggestions> suggestVaguePropertyNameOrEnd(SuggestionsBuilder builder, Tags<Block> tagGroup) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf(']'));
        }

        return this.suggestVaguePropertyName(builder, tagGroup);
    }

    private CompletableFuture<Suggestions> suggestPropertyName(SuggestionsBuilder builder, Tags<Block> tagGroup) {
        String string = builder.getRemaining().toLowerCase(Locale.ROOT);

        for(IBlockState<?> property : this.state.getProperties()) {
            if (!this.properties.containsKey(property) && property.getName().startsWith(string)) {
                builder.suggest(property.getName() + "=");
            }
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestVaguePropertyName(SuggestionsBuilder builder, Tags<Block> tagGroup) {
        String string = builder.getRemaining().toLowerCase(Locale.ROOT);
        if (this.tag != null && !this.tag.getKey().isEmpty()) {
            Tag<Block> tag = tagGroup.getTag(this.tag);
            if (tag != null) {
                for(Block block : tag.getTagged()) {
                    for(IBlockState<?> property : block.getStates().getProperties()) {
                        if (!this.vagueProperties.containsKey(property.getName()) && property.getName().startsWith(string)) {
                            builder.suggest(property.getName() + "=");
                        }
                    }
                }
            }
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestOpenNbt(SuggestionsBuilder builder, Tags<Block> tagGroup) {
        if (builder.getRemaining().isEmpty() && this.hasBlockEntity(tagGroup)) {
            builder.suggest(String.valueOf('{'));
        }

        return builder.buildFuture();
    }

    private boolean hasBlockEntity(Tags<Block> tagGroup) {
        if (this.state != null) {
            return this.state.isTileEntity();
        } else {
            if (this.tag != null) {
                Tag<Block> tag = tagGroup.getTag(this.tag);
                if (tag != null) {
                    for(Block block : tag.getTagged()) {
                        if (block.getBlockData().isTileEntity()) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }
    }

    private CompletableFuture<Suggestions> suggestEquals(SuggestionsBuilder builder, Tags<Block> tagGroup) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf('='));
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestNextPropertyOrEnd(SuggestionsBuilder builder, Tags<Block> tagGroup) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf(']'));
        }

        if (builder.getRemaining().isEmpty() && this.properties.size() < this.state.getProperties().size()) {
            builder.suggest(String.valueOf(','));
        }

        return builder.buildFuture();
    }

    private static <T extends Comparable<T>> SuggestionsBuilder addSuggestions(SuggestionsBuilder builder, IBlockState<T> property) {
        for(T comparable : property.getValues()) {
            if (comparable instanceof Integer) {
                builder.suggest(comparable);
            } else {
                builder.suggest(property.getName(comparable));
            }
        }

        return builder;
    }

    private CompletableFuture<Suggestions> suggestVaguePropertyValue(SuggestionsBuilder builder, Tags<Block> tagGroup, String propertyName) {
        boolean bl = false;
        if (this.tag != null && !this.tag.getKey().isEmpty()) {
            Tag<Block> tag = tagGroup.getTag(this.tag);
            if (tag != null) {
                for(Block block : tag.getTagged()) {
                    IBlockState<?> property = block.getStates().getProperty(propertyName);
                    if (property != null) {
                        addSuggestions(builder, property);
                    }

                    if (!bl) {
                        for(IBlockState<?> property2 : block.getStates().getProperties()) {
                            if (!this.vagueProperties.containsKey(property2.getName())) {
                                bl = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (bl) {
            builder.suggest(String.valueOf(','));
        }

        builder.suggest(String.valueOf(']'));
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestOpenVaguePropertiesOrNbt(SuggestionsBuilder builder, Tags<Block> tagGroup) {
        if (builder.getRemaining().isEmpty()) {
            Tag<Block> tag = tagGroup.getTag(this.tag);
            if (tag != null) {
                boolean bl = false;
                boolean bl2 = false;

                for(Block block : tag.getTagged()) {
                    bl |= !block.getStates().getProperties().isEmpty();
                    bl2 |= block.getBlockData().isTileEntity();
                    if (bl && bl2) {
                        break;
                    }
                }

                if (bl) {
                    builder.suggest(String.valueOf('['));
                }

                if (bl2) {
                    builder.suggest(String.valueOf('{'));
                }
            }
        }

        return this.suggestTag(builder, tagGroup);
    }

    private CompletableFuture<Suggestions> suggestOpenPropertiesOrNbt(SuggestionsBuilder builder, Tags<Block> tagGroup) {
        if (builder.getRemaining().isEmpty()) {
            if (!this.state.getBlock().getStates().getProperties().isEmpty()) {
                builder.suggest(String.valueOf('['));
            }

            if (this.state.isTileEntity()) {
                builder.suggest(String.valueOf('{'));
            }
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestTag(SuggestionsBuilder builder, Tags<Block> tagGroup) {
        return ICompletionProvider.suggestResource(tagGroup.getAvailableTags(), builder.createOffset(this.tagCursor).add(builder));
    }

    private CompletableFuture<Suggestions> suggestBlockIdOrTag(SuggestionsBuilder builder, Tags<Block> tagGroup) {
        if (this.forTesting) {
            ICompletionProvider.suggestResource(tagGroup.getAvailableTags(), builder, String.valueOf('#'));
        }

        ICompletionProvider.suggestResource(IRegistry.BLOCK.keySet(), builder);
        return builder.buildFuture();
    }

    public void readBlock() throws CommandSyntaxException {
        int i = this.reader.getCursor();
        this.id = MinecraftKey.read(this.reader);
        Block block = IRegistry.BLOCK.getOptional(this.id).orElseThrow(() -> {
            this.reader.setCursor(i);
            return ERROR_UNKNOWN_BLOCK.createWithContext(this.reader, this.id.toString());
        });
        this.definition = block.getStates();
        this.state = block.getBlockData();
    }

    public void readTag() throws CommandSyntaxException {
        if (!this.forTesting) {
            throw ERROR_NO_TAGS_ALLOWED.create();
        } else {
            this.suggestions = this::suggestTag;
            this.reader.expect('#');
            this.tagCursor = this.reader.getCursor();
            this.tag = MinecraftKey.read(this.reader);
        }
    }

    public void readProperties() throws CommandSyntaxException {
        this.reader.skip();
        this.suggestions = this::suggestPropertyNameOrEnd;
        this.reader.skipWhitespace();

        while(true) {
            if (this.reader.canRead() && this.reader.peek() != ']') {
                this.reader.skipWhitespace();
                int i = this.reader.getCursor();
                String string = this.reader.readString();
                IBlockState<?> property = this.definition.getProperty(string);
                if (property == null) {
                    this.reader.setCursor(i);
                    throw ERROR_UNKNOWN_PROPERTY.createWithContext(this.reader, this.id.toString(), string);
                }

                if (this.properties.containsKey(property)) {
                    this.reader.setCursor(i);
                    throw ERROR_DUPLICATE_PROPERTY.createWithContext(this.reader, this.id.toString(), string);
                }

                this.reader.skipWhitespace();
                this.suggestions = this::suggestEquals;
                if (!this.reader.canRead() || this.reader.peek() != '=') {
                    throw ERROR_EXPECTED_VALUE.createWithContext(this.reader, this.id.toString(), string);
                }

                this.reader.skip();
                this.reader.skipWhitespace();
                this.suggestions = (builder, tagGroup) -> {
                    return addSuggestions(builder, property).buildFuture();
                };
                int j = this.reader.getCursor();
                this.setValue(property, this.reader.readString(), j);
                this.suggestions = this::suggestNextPropertyOrEnd;
                this.reader.skipWhitespace();
                if (!this.reader.canRead()) {
                    continue;
                }

                if (this.reader.peek() == ',') {
                    this.reader.skip();
                    this.suggestions = this::suggestPropertyName;
                    continue;
                }

                if (this.reader.peek() != ']') {
                    throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
                }
            }

            if (this.reader.canRead()) {
                this.reader.skip();
                return;
            }

            throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
        }
    }

    public void readVagueProperties() throws CommandSyntaxException {
        this.reader.skip();
        this.suggestions = this::suggestVaguePropertyNameOrEnd;
        int i = -1;
        this.reader.skipWhitespace();

        while(true) {
            if (this.reader.canRead() && this.reader.peek() != ']') {
                this.reader.skipWhitespace();
                int j = this.reader.getCursor();
                String string = this.reader.readString();
                if (this.vagueProperties.containsKey(string)) {
                    this.reader.setCursor(j);
                    throw ERROR_DUPLICATE_PROPERTY.createWithContext(this.reader, this.id.toString(), string);
                }

                this.reader.skipWhitespace();
                if (!this.reader.canRead() || this.reader.peek() != '=') {
                    this.reader.setCursor(j);
                    throw ERROR_EXPECTED_VALUE.createWithContext(this.reader, this.id.toString(), string);
                }

                this.reader.skip();
                this.reader.skipWhitespace();
                this.suggestions = (builder, tagGroup) -> {
                    return this.suggestVaguePropertyValue(builder, tagGroup, string);
                };
                i = this.reader.getCursor();
                String string2 = this.reader.readString();
                this.vagueProperties.put(string, string2);
                this.reader.skipWhitespace();
                if (!this.reader.canRead()) {
                    continue;
                }

                i = -1;
                if (this.reader.peek() == ',') {
                    this.reader.skip();
                    this.suggestions = this::suggestVaguePropertyName;
                    continue;
                }

                if (this.reader.peek() != ']') {
                    throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
                }
            }

            if (this.reader.canRead()) {
                this.reader.skip();
                return;
            }

            if (i >= 0) {
                this.reader.setCursor(i);
            }

            throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
        }
    }

    public void readNbt() throws CommandSyntaxException {
        this.nbt = (new MojangsonParser(this.reader)).readStruct();
    }

    private <T extends Comparable<T>> void setValue(IBlockState<T> property, String value, int cursor) throws CommandSyntaxException {
        Optional<T> optional = property.getValue(value);
        if (optional.isPresent()) {
            this.state = this.state.set(property, optional.get());
            this.properties.put(property, optional.get());
        } else {
            this.reader.setCursor(cursor);
            throw ERROR_INVALID_VALUE.createWithContext(this.reader, this.id.toString(), property.getName(), value);
        }
    }

    public static String serialize(IBlockData state) {
        StringBuilder stringBuilder = new StringBuilder(IRegistry.BLOCK.getKey(state.getBlock()).toString());
        if (!state.getProperties().isEmpty()) {
            stringBuilder.append('[');
            boolean bl = false;

            for(Entry<IBlockState<?>, Comparable<?>> entry : state.getStateMap().entrySet()) {
                if (bl) {
                    stringBuilder.append(',');
                }

                appendProperty(stringBuilder, entry.getKey(), entry.getValue());
                bl = true;
            }

            stringBuilder.append(']');
        }

        return stringBuilder.toString();
    }

    private static <T extends Comparable<T>> void appendProperty(StringBuilder builder, IBlockState<T> property, Comparable<?> value) {
        builder.append(property.getName());
        builder.append('=');
        builder.append(property.getName((T)value));
    }

    public CompletableFuture<Suggestions> fillSuggestions(SuggestionsBuilder builder, Tags<Block> tagGroup) {
        return this.suggestions.apply(builder.createOffset(this.reader.getCursor()), tagGroup);
    }

    public Map<String, String> getVagueProperties() {
        return this.vagueProperties;
    }
}
