package net.minecraft.server.rcon.thread;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.server.IMinecraftServer;
import net.minecraft.server.rcon.RemoteStatusReply;
import net.minecraft.server.rcon.StatusChallengeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RemoteStatusListener extends RemoteConnectionThread {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String GAME_TYPE = "SMP";
    private static final String GAME_ID = "MINECRAFT";
    private static final long CHALLENGE_CHECK_INTERVAL = 30000L;
    private static final long RESPONSE_CACHE_TIME = 5000L;
    private long lastChallengeCheck;
    private final int port;
    private final int serverPort;
    private final int maxPlayers;
    private final String serverName;
    private final String worldName;
    private DatagramSocket socket;
    private final byte[] buffer = new byte[1460];
    private String hostIp;
    private String serverIp;
    private final Map<SocketAddress, RemoteStatusListener.RemoteStatusChallenge> validChallenges;
    private final RemoteStatusReply rulesResponse;
    private long lastRulesResponse;
    private final IMinecraftServer serverInterface;

    private RemoteStatusListener(IMinecraftServer server, int queryPort) {
        super("Query Listener");
        this.serverInterface = server;
        this.port = queryPort;
        this.serverIp = server.getServerIp();
        this.serverPort = server.getServerPort();
        this.serverName = server.getServerName();
        this.maxPlayers = server.getMaxPlayers();
        this.worldName = server.getWorld();
        this.lastRulesResponse = 0L;
        this.hostIp = "0.0.0.0";
        if (!this.serverIp.isEmpty() && !this.hostIp.equals(this.serverIp)) {
            this.hostIp = this.serverIp;
        } else {
            this.serverIp = "0.0.0.0";

            try {
                InetAddress inetAddress = InetAddress.getLocalHost();
                this.hostIp = inetAddress.getHostAddress();
            } catch (UnknownHostException var4) {
                LOGGER.warn("Unable to determine local host IP, please set server-ip in server.properties", (Throwable)var4);
            }
        }

        this.rulesResponse = new RemoteStatusReply(1460);
        this.validChallenges = Maps.newHashMap();
    }

    @Nullable
    public static RemoteStatusListener create(IMinecraftServer server) {
        int i = server.getDedicatedServerProperties().queryPort;
        if (0 < i && 65535 >= i) {
            RemoteStatusListener queryThreadGs4 = new RemoteStatusListener(server, i);
            return !queryThreadGs4.start() ? null : queryThreadGs4;
        } else {
            LOGGER.warn("Invalid query port {} found in server.properties (queries disabled)", (int)i);
            return null;
        }
    }

    private void sendTo(byte[] buf, DatagramPacket packet) throws IOException {
        this.socket.send(new DatagramPacket(buf, buf.length, packet.getSocketAddress()));
    }

    private boolean processPacket(DatagramPacket packet) throws IOException {
        byte[] bs = packet.getData();
        int i = packet.getLength();
        SocketAddress socketAddress = packet.getSocketAddress();
        LOGGER.debug("Packet len {} [{}]", i, socketAddress);
        if (3 <= i && -2 == bs[0] && -3 == bs[1]) {
            LOGGER.debug("Packet '{}' [{}]", StatusChallengeUtils.toHexString(bs[2]), socketAddress);
            switch(bs[2]) {
            case 0:
                if (!this.validChallenge(packet)) {
                    LOGGER.debug("Invalid challenge [{}]", (Object)socketAddress);
                    return false;
                } else if (15 == i) {
                    this.sendTo(this.buildRuleResponse(packet), packet);
                    LOGGER.debug("Rules [{}]", (Object)socketAddress);
                } else {
                    RemoteStatusReply networkDataOutputStream = new RemoteStatusReply(1460);
                    networkDataOutputStream.write(0);
                    networkDataOutputStream.writeBytes(this.getIdentBytes(packet.getSocketAddress()));
                    networkDataOutputStream.writeString(this.serverName);
                    networkDataOutputStream.writeString("SMP");
                    networkDataOutputStream.writeString(this.worldName);
                    networkDataOutputStream.writeString(Integer.toString(this.serverInterface.getPlayerCount()));
                    networkDataOutputStream.writeString(Integer.toString(this.maxPlayers));
                    networkDataOutputStream.writeShort((short)this.serverPort);
                    networkDataOutputStream.writeString(this.hostIp);
                    this.sendTo(networkDataOutputStream.toByteArray(), packet);
                    LOGGER.debug("Status [{}]", (Object)socketAddress);
                }
            default:
                return true;
            case 9:
                this.sendChallenge(packet);
                LOGGER.debug("Challenge [{}]", (Object)socketAddress);
                return true;
            }
        } else {
            LOGGER.debug("Invalid packet [{}]", (Object)socketAddress);
            return false;
        }
    }

    private byte[] buildRuleResponse(DatagramPacket packet) throws IOException {
        long l = SystemUtils.getMonotonicMillis();
        if (l < this.lastRulesResponse + 5000L) {
            byte[] bs = this.rulesResponse.toByteArray();
            byte[] cs = this.getIdentBytes(packet.getSocketAddress());
            bs[1] = cs[0];
            bs[2] = cs[1];
            bs[3] = cs[2];
            bs[4] = cs[3];
            return bs;
        } else {
            this.lastRulesResponse = l;
            this.rulesResponse.reset();
            this.rulesResponse.write(0);
            this.rulesResponse.writeBytes(this.getIdentBytes(packet.getSocketAddress()));
            this.rulesResponse.writeString("splitnum");
            this.rulesResponse.write(128);
            this.rulesResponse.write(0);
            this.rulesResponse.writeString("hostname");
            this.rulesResponse.writeString(this.serverName);
            this.rulesResponse.writeString("gametype");
            this.rulesResponse.writeString("SMP");
            this.rulesResponse.writeString("game_id");
            this.rulesResponse.writeString("MINECRAFT");
            this.rulesResponse.writeString("version");
            this.rulesResponse.writeString(this.serverInterface.getVersion());
            this.rulesResponse.writeString("plugins");
            this.rulesResponse.writeString(this.serverInterface.getPlugins());
            this.rulesResponse.writeString("map");
            this.rulesResponse.writeString(this.worldName);
            this.rulesResponse.writeString("numplayers");
            this.rulesResponse.writeString("" + this.serverInterface.getPlayerCount());
            this.rulesResponse.writeString("maxplayers");
            this.rulesResponse.writeString("" + this.maxPlayers);
            this.rulesResponse.writeString("hostport");
            this.rulesResponse.writeString("" + this.serverPort);
            this.rulesResponse.writeString("hostip");
            this.rulesResponse.writeString(this.hostIp);
            this.rulesResponse.write(0);
            this.rulesResponse.write(1);
            this.rulesResponse.writeString("player_");
            this.rulesResponse.write(0);
            String[] strings = this.serverInterface.getPlayers();

            for(String string : strings) {
                this.rulesResponse.writeString(string);
            }

            this.rulesResponse.write(0);
            return this.rulesResponse.toByteArray();
        }
    }

    private byte[] getIdentBytes(SocketAddress address) {
        return this.validChallenges.get(address).getIdentBytes();
    }

    private Boolean validChallenge(DatagramPacket packet) {
        SocketAddress socketAddress = packet.getSocketAddress();
        if (!this.validChallenges.containsKey(socketAddress)) {
            return false;
        } else {
            byte[] bs = packet.getData();
            return this.validChallenges.get(socketAddress).getChallenge() == StatusChallengeUtils.intFromNetworkByteArray(bs, 7, packet.getLength());
        }
    }

    private void sendChallenge(DatagramPacket packet) throws IOException {
        RemoteStatusListener.RemoteStatusChallenge requestChallenge = new RemoteStatusListener.RemoteStatusChallenge(packet);
        this.validChallenges.put(packet.getSocketAddress(), requestChallenge);
        this.sendTo(requestChallenge.getChallengeBytes(), packet);
    }

    private void pruneChallenges() {
        if (this.running) {
            long l = SystemUtils.getMonotonicMillis();
            if (l >= this.lastChallengeCheck + 30000L) {
                this.lastChallengeCheck = l;
                this.validChallenges.values().removeIf((query) -> {
                    return query.before(l);
                });
            }
        }
    }

    @Override
    public void run() {
        LOGGER.info("Query running on {}:{}", this.serverIp, this.port);
        this.lastChallengeCheck = SystemUtils.getMonotonicMillis();
        DatagramPacket datagramPacket = new DatagramPacket(this.buffer, this.buffer.length);

        try {
            while(this.running) {
                try {
                    this.socket.receive(datagramPacket);
                    this.pruneChallenges();
                    this.processPacket(datagramPacket);
                } catch (SocketTimeoutException var8) {
                    this.pruneChallenges();
                } catch (PortUnreachableException var9) {
                } catch (IOException var10) {
                    this.recoverSocketError(var10);
                }
            }
        } finally {
            LOGGER.debug("closeSocket: {}:{}", this.serverIp, this.port);
            this.socket.close();
        }

    }

    @Override
    public boolean start() {
        if (this.running) {
            return true;
        } else {
            return !this.initSocket() ? false : super.start();
        }
    }

    private void recoverSocketError(Exception e) {
        if (this.running) {
            LOGGER.warn("Unexpected exception", (Throwable)e);
            if (!this.initSocket()) {
                LOGGER.error("Failed to recover from exception, shutting down!");
                this.running = false;
            }

        }
    }

    private boolean initSocket() {
        try {
            this.socket = new DatagramSocket(this.port, InetAddress.getByName(this.serverIp));
            this.socket.setSoTimeout(500);
            return true;
        } catch (Exception var2) {
            LOGGER.warn("Unable to initialise query system on {}:{}", this.serverIp, this.port, var2);
            return false;
        }
    }

    static class RemoteStatusChallenge {
        private final long time = (new Date()).getTime();
        private final int challenge;
        private final byte[] identBytes;
        private final byte[] challengeBytes;
        private final String ident;

        public RemoteStatusChallenge(DatagramPacket packet) {
            byte[] bs = packet.getData();
            this.identBytes = new byte[4];
            this.identBytes[0] = bs[3];
            this.identBytes[1] = bs[4];
            this.identBytes[2] = bs[5];
            this.identBytes[3] = bs[6];
            this.ident = new String(this.identBytes, StandardCharsets.UTF_8);
            this.challenge = (new Random()).nextInt(16777216);
            this.challengeBytes = String.format("\t%s%d\u0000", this.ident, this.challenge).getBytes(StandardCharsets.UTF_8);
        }

        public Boolean before(long lastQueryTime) {
            return this.time < lastQueryTime;
        }

        public int getChallenge() {
            return this.challenge;
        }

        public byte[] getChallengeBytes() {
            return this.challengeBytes;
        }

        public byte[] getIdentBytes() {
            return this.identBytes;
        }

        public String getIdent() {
            return this.ident;
        }
    }
}
