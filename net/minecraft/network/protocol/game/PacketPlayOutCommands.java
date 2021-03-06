package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import javax.annotation.Nullable;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.synchronization.ArgumentRegistry;
import net.minecraft.commands.synchronization.CompletionProviders;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutCommands implements Packet<PacketListenerPlayOut> {
    private static final byte MASK_TYPE = 3;
    private static final byte FLAG_EXECUTABLE = 4;
    private static final byte FLAG_REDIRECT = 8;
    private static final byte FLAG_CUSTOM_SUGGESTIONS = 16;
    private static final byte TYPE_ROOT = 0;
    private static final byte TYPE_LITERAL = 1;
    private static final byte TYPE_ARGUMENT = 2;
    private final RootCommandNode<ICompletionProvider> root;

    public PacketPlayOutCommands(RootCommandNode<ICompletionProvider> commandTree) {
        this.root = commandTree;
    }

    public PacketPlayOutCommands(PacketDataSerializer buf) {
        List<PacketPlayOutCommands.Entry> list = buf.readList(PacketPlayOutCommands::readNode);
        resolveEntries(list);
        int i = buf.readVarInt();
        this.root = (RootCommandNode)(list.get(i)).node;
    }

    @Override
    public void write(PacketDataSerializer buf) {
        Object2IntMap<CommandNode<ICompletionProvider>> object2IntMap = enumerateNodes(this.root);
        List<CommandNode<ICompletionProvider>> list = getNodesInIdOrder(object2IntMap);
        buf.writeCollection(list, (friendlyByteBuf, node) -> {
            writeNode(friendlyByteBuf, node, object2IntMap);
        });
        buf.writeVarInt(object2IntMap.get(this.root));
    }

    private static void resolveEntries(List<PacketPlayOutCommands.Entry> nodeDatas) {
        List<PacketPlayOutCommands.Entry> list = Lists.newArrayList(nodeDatas);

        while(!list.isEmpty()) {
            boolean bl = list.removeIf((nodeData) -> {
                return nodeData.build(nodeDatas);
            });
            if (!bl) {
                throw new IllegalStateException("Server sent an impossible command tree");
            }
        }

    }

    private static Object2IntMap<CommandNode<ICompletionProvider>> enumerateNodes(RootCommandNode<ICompletionProvider> commandTree) {
        Object2IntMap<CommandNode<ICompletionProvider>> object2IntMap = new Object2IntOpenHashMap<>();
        Queue<CommandNode<ICompletionProvider>> queue = Queues.newArrayDeque();
        queue.add(commandTree);

        CommandNode<ICompletionProvider> commandNode;
        while((commandNode = queue.poll()) != null) {
            if (!object2IntMap.containsKey(commandNode)) {
                int i = object2IntMap.size();
                object2IntMap.put(commandNode, i);
                queue.addAll(commandNode.getChildren());
                if (commandNode.getRedirect() != null) {
                    queue.add(commandNode.getRedirect());
                }
            }
        }

        return object2IntMap;
    }

    private static List<CommandNode<ICompletionProvider>> getNodesInIdOrder(Object2IntMap<CommandNode<ICompletionProvider>> nodes) {
        ObjectArrayList<CommandNode<ICompletionProvider>> objectArrayList = new ObjectArrayList<>(nodes.size());
        objectArrayList.size(nodes.size());

        for(Object2IntMap.Entry<CommandNode<ICompletionProvider>> entry : Object2IntMaps.fastIterable(nodes)) {
            objectArrayList.set(entry.getIntValue(), entry.getKey());
        }

        return objectArrayList;
    }

    private static PacketPlayOutCommands.Entry readNode(PacketDataSerializer buf) {
        byte b = buf.readByte();
        int[] is = buf.readVarIntArray();
        int i = (b & 8) != 0 ? buf.readVarInt() : 0;
        ArgumentBuilder<ICompletionProvider, ?> argumentBuilder = createBuilder(buf, b);
        return new PacketPlayOutCommands.Entry(argumentBuilder, b, i, is);
    }

    @Nullable
    private static ArgumentBuilder<ICompletionProvider, ?> createBuilder(PacketDataSerializer buf, byte b) {
        int i = b & 3;
        if (i == 2) {
            String string = buf.readUtf();
            ArgumentType<?> argumentType = ArgumentRegistry.deserialize(buf);
            if (argumentType == null) {
                return null;
            } else {
                RequiredArgumentBuilder<ICompletionProvider, ?> requiredArgumentBuilder = RequiredArgumentBuilder.argument(string, argumentType);
                if ((b & 16) != 0) {
                    requiredArgumentBuilder.suggests(CompletionProviders.getProvider(buf.readResourceLocation()));
                }

                return requiredArgumentBuilder;
            }
        } else {
            return i == 1 ? LiteralArgumentBuilder.literal(buf.readUtf()) : null;
        }
    }

    private static void writeNode(PacketDataSerializer buf, CommandNode<ICompletionProvider> node, Map<CommandNode<ICompletionProvider>, Integer> nodeToIndex) {
        byte b = 0;
        if (node.getRedirect() != null) {
            b = (byte)(b | 8);
        }

        if (node.getCommand() != null) {
            b = (byte)(b | 4);
        }

        if (node instanceof RootCommandNode) {
            b = (byte)(b | 0);
        } else if (node instanceof ArgumentCommandNode) {
            b = (byte)(b | 2);
            if (((ArgumentCommandNode)node).getCustomSuggestions() != null) {
                b = (byte)(b | 16);
            }
        } else {
            if (!(node instanceof LiteralCommandNode)) {
                throw new UnsupportedOperationException("Unknown node type " + node);
            }

            b = (byte)(b | 1);
        }

        buf.writeByte(b);
        buf.writeVarInt(node.getChildren().size());

        for(CommandNode<ICompletionProvider> commandNode : node.getChildren()) {
            buf.writeVarInt(nodeToIndex.get(commandNode));
        }

        if (node.getRedirect() != null) {
            buf.writeVarInt(nodeToIndex.get(node.getRedirect()));
        }

        if (node instanceof ArgumentCommandNode) {
            ArgumentCommandNode<ICompletionProvider, ?> argumentCommandNode = (ArgumentCommandNode)node;
            buf.writeUtf(argumentCommandNode.getName());
            ArgumentRegistry.serialize(buf, argumentCommandNode.getType());
            if (argumentCommandNode.getCustomSuggestions() != null) {
                buf.writeResourceLocation(CompletionProviders.getName(argumentCommandNode.getCustomSuggestions()));
            }
        } else if (node instanceof LiteralCommandNode) {
            buf.writeUtf(((LiteralCommandNode)node).getLiteral());
        }

    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleCommands(this);
    }

    public RootCommandNode<ICompletionProvider> getRoot() {
        return this.root;
    }

    static class Entry {
        @Nullable
        private final ArgumentBuilder<ICompletionProvider, ?> builder;
        private final byte flags;
        private final int redirect;
        private final int[] children;
        @Nullable
        CommandNode<ICompletionProvider> node;

        Entry(@Nullable ArgumentBuilder<ICompletionProvider, ?> argumentBuilder, byte flags, int redirectNodeIndex, int[] childNodeIndices) {
            this.builder = argumentBuilder;
            this.flags = flags;
            this.redirect = redirectNodeIndex;
            this.children = childNodeIndices;
        }

        public boolean build(List<PacketPlayOutCommands.Entry> list) {
            if (this.node == null) {
                if (this.builder == null) {
                    this.node = new RootCommandNode<>();
                } else {
                    if ((this.flags & 8) != 0) {
                        if ((list.get(this.redirect)).node == null) {
                            return false;
                        }

                        this.builder.redirect((list.get(this.redirect)).node);
                    }

                    if ((this.flags & 4) != 0) {
                        this.builder.executes((context) -> {
                            return 0;
                        });
                    }

                    this.node = this.builder.build();
                }
            }

            for(int i : this.children) {
                if ((list.get(i)).node == null) {
                    return false;
                }
            }

            for(int j : this.children) {
                CommandNode<ICompletionProvider> commandNode = (list.get(j)).node;
                if (!(commandNode instanceof RootCommandNode)) {
                    this.node.addChild(commandNode);
                }
            }

            return true;
        }
    }
}
