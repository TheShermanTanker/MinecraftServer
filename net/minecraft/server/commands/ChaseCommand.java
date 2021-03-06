package net.minecraft.server.commands;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.IOException;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.chase.ChaseClient;
import net.minecraft.server.chase.ChaseServer;
import net.minecraft.world.level.World;

public class ChaseCommand {
    private static final String DEFAULT_CONNECT_HOST = "localhost";
    private static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";
    private static final int DEFAULT_PORT = 10000;
    private static final int BROADCAST_INTERVAL_MS = 100;
    public static BiMap<String, ResourceKey<World>> DIMENSION_NAMES = ImmutableBiMap.of("o", World.OVERWORLD, "n", World.NETHER, "e", World.END);
    @Nullable
    private static ChaseServer chaseServer;
    @Nullable
    private static ChaseClient chaseClient;

    public static void register(CommandDispatcher<CommandListenerWrapper> dispatcher) {
        dispatcher.register(net.minecraft.commands.CommandDispatcher.literal("chase").then(net.minecraft.commands.CommandDispatcher.literal("follow").then(net.minecraft.commands.CommandDispatcher.argument("host", StringArgumentType.string()).executes((commandContext) -> {
            return follow(commandContext.getSource(), StringArgumentType.getString(commandContext, "host"), 10000);
        }).then(net.minecraft.commands.CommandDispatcher.argument("port", IntegerArgumentType.integer(1, 65535)).executes((commandContext) -> {
            return follow(commandContext.getSource(), StringArgumentType.getString(commandContext, "host"), IntegerArgumentType.getInteger(commandContext, "port"));
        }))).executes((commandContext) -> {
            return follow(commandContext.getSource(), "localhost", 10000);
        })).then(net.minecraft.commands.CommandDispatcher.literal("lead").then(net.minecraft.commands.CommandDispatcher.argument("bind_address", StringArgumentType.string()).executes((commandContext) -> {
            return lead(commandContext.getSource(), StringArgumentType.getString(commandContext, "bind_address"), 10000);
        }).then(net.minecraft.commands.CommandDispatcher.argument("port", IntegerArgumentType.integer(1024, 65535)).executes((commandContext) -> {
            return lead(commandContext.getSource(), StringArgumentType.getString(commandContext, "bind_address"), IntegerArgumentType.getInteger(commandContext, "port"));
        }))).executes((commandContext) -> {
            return lead(commandContext.getSource(), "0.0.0.0", 10000);
        })).then(net.minecraft.commands.CommandDispatcher.literal("stop").executes((commandContext) -> {
            return stop(commandContext.getSource());
        })));
    }

    private static int stop(CommandListenerWrapper source) {
        if (chaseClient != null) {
            chaseClient.stop();
            source.sendMessage(new ChatComponentText("You have now stopped chasing"), false);
            chaseClient = null;
        }

        if (chaseServer != null) {
            chaseServer.stop();
            source.sendMessage(new ChatComponentText("You are no longer being chased"), false);
            chaseServer = null;
        }

        return 0;
    }

    private static boolean alreadyRunning(CommandListenerWrapper source) {
        if (chaseServer != null) {
            source.sendFailureMessage(new ChatComponentText("Chase server is already running. Stop it using /chase stop"));
            return true;
        } else if (chaseClient != null) {
            source.sendFailureMessage(new ChatComponentText("You are already chasing someone. Stop it using /chase stop"));
            return true;
        } else {
            return false;
        }
    }

    private static int lead(CommandListenerWrapper source, String ip, int port) {
        if (alreadyRunning(source)) {
            return 0;
        } else {
            chaseServer = new ChaseServer(ip, port, source.getServer().getPlayerList(), 100);

            try {
                chaseServer.start();
                source.sendMessage(new ChatComponentText("Chase server is now running on port " + port + ". Clients can follow you using /chase follow <ip> <port>"), false);
            } catch (IOException var4) {
                var4.printStackTrace();
                source.sendFailureMessage(new ChatComponentText("Failed to start chase server on port " + port));
                chaseServer = null;
            }

            return 0;
        }
    }

    private static int follow(CommandListenerWrapper source, String ip, int port) {
        if (alreadyRunning(source)) {
            return 0;
        } else {
            chaseClient = new ChaseClient(ip, port, source.getServer());
            chaseClient.start();
            source.sendMessage(new ChatComponentText("You are now chasing " + ip + ":" + port + ". If that server does '/chase lead' then you will automatically go to the same position. Use '/chase stop' to stop chasing."), false);
            return 0;
        }
    }
}
