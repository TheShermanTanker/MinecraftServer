package net.minecraft.commands.arguments.blocks;

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
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.ITagRegistry;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import net.minecraft.world.level.block.state.properties.IBlockState;

public class ArgumentBlockPredicate implements ArgumentType<ArgumentBlockPredicate.Result> {
    private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "#stone", "#stone[foo=bar]{baz=nbt}");
    static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType((id) -> {
        return new ChatMessage("arguments.block.tag.unknown", id);
    });

    public static ArgumentBlockPredicate blockPredicate() {
        return new ArgumentBlockPredicate();
    }

    public ArgumentBlockPredicate.Result parse(StringReader stringReader) throws CommandSyntaxException {
        final ArgumentBlock blockStateParser = (new ArgumentBlock(stringReader, true)).parse(true);
        if (blockStateParser.getBlockData() != null) {
            final ArgumentBlockPredicate.BlockPredicate blockPredicate = new ArgumentBlockPredicate.BlockPredicate(blockStateParser.getBlockData(), blockStateParser.getStateMap().keySet(), blockStateParser.getNbt());
            return new ArgumentBlockPredicate.Result() {
                @Override
                public Predicate<ShapeDetectorBlock> create(ITagRegistry manager) {
                    return blockPredicate;
                }

                @Override
                public boolean requiresNbt() {
                    return blockPredicate.requiresNbt();
                }
            };
        } else {
            final MinecraftKey resourceLocation = blockStateParser.getTag();
            return new ArgumentBlockPredicate.Result() {
                @Override
                public Predicate<ShapeDetectorBlock> create(ITagRegistry manager) throws CommandSyntaxException {
                    Tag<Block> tag = manager.getTagOrThrow(IRegistry.BLOCK_REGISTRY, resourceLocation, (id) -> {
                        return ArgumentBlockPredicate.ERROR_UNKNOWN_TAG.create(id.toString());
                    });
                    return new ArgumentBlockPredicate.TagPredicate(tag, blockStateParser.getVagueProperties(), blockStateParser.getNbt());
                }

                @Override
                public boolean requiresNbt() {
                    return blockStateParser.getNbt() != null;
                }
            };
        }
    }

    public static Predicate<ShapeDetectorBlock> getBlockPredicate(CommandContext<CommandListenerWrapper> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, ArgumentBlockPredicate.Result.class).create(context.getSource().getServer().getTagRegistry());
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        StringReader stringReader = new StringReader(suggestionsBuilder.getInput());
        stringReader.setCursor(suggestionsBuilder.getStart());
        ArgumentBlock blockStateParser = new ArgumentBlock(stringReader, true);

        try {
            blockStateParser.parse(true);
        } catch (CommandSyntaxException var6) {
        }

        return blockStateParser.fillSuggestions(suggestionsBuilder, TagsBlock.getAllTags());
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    static class BlockPredicate implements Predicate<ShapeDetectorBlock> {
        private final IBlockData state;
        private final Set<IBlockState<?>> properties;
        @Nullable
        private final NBTTagCompound nbt;

        public BlockPredicate(IBlockData state, Set<IBlockState<?>> properties, @Nullable NBTTagCompound nbt) {
            this.state = state;
            this.properties = properties;
            this.nbt = nbt;
        }

        @Override
        public boolean test(ShapeDetectorBlock blockInWorld) {
            IBlockData blockState = blockInWorld.getState();
            if (!blockState.is(this.state.getBlock())) {
                return false;
            } else {
                for(IBlockState<?> property : this.properties) {
                    if (blockState.get(property) != this.state.get(property)) {
                        return false;
                    }
                }

                if (this.nbt == null) {
                    return true;
                } else {
                    TileEntity blockEntity = blockInWorld.getEntity();
                    return blockEntity != null && GameProfileSerializer.compareNbt(this.nbt, blockEntity.saveWithFullMetadata(), true);
                }
            }
        }

        public boolean requiresNbt() {
            return this.nbt != null;
        }
    }

    public interface Result {
        Predicate<ShapeDetectorBlock> create(ITagRegistry manager) throws CommandSyntaxException;

        boolean requiresNbt();
    }

    static class TagPredicate implements Predicate<ShapeDetectorBlock> {
        private final Tag<Block> tag;
        @Nullable
        private final NBTTagCompound nbt;
        private final Map<String, String> vagueProperties;

        TagPredicate(Tag<Block> tag, Map<String, String> properties, @Nullable NBTTagCompound nbt) {
            this.tag = tag;
            this.vagueProperties = properties;
            this.nbt = nbt;
        }

        @Override
        public boolean test(ShapeDetectorBlock blockInWorld) {
            IBlockData blockState = blockInWorld.getState();
            if (!blockState.is(this.tag)) {
                return false;
            } else {
                for(Entry<String, String> entry : this.vagueProperties.entrySet()) {
                    IBlockState<?> property = blockState.getBlock().getStates().getProperty(entry.getKey());
                    if (property == null) {
                        return false;
                    }

                    Comparable<?> comparable = (Comparable)property.getValue(entry.getValue()).orElse((Object)null);
                    if (comparable == null) {
                        return false;
                    }

                    if (blockState.get(property) != comparable) {
                        return false;
                    }
                }

                if (this.nbt == null) {
                    return true;
                } else {
                    TileEntity blockEntity = blockInWorld.getEntity();
                    return blockEntity != null && GameProfileSerializer.compareNbt(this.nbt, blockEntity.saveWithFullMetadata(), true);
                }
            }
        }
    }
}
