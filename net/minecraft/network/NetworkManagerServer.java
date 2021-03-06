package net.minecraft.network;

import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.EnumProtocolDirection;
import net.minecraft.network.protocol.game.PacketPlayOutKickDisconnect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkManagerServer extends NetworkManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final IChatBaseComponent EXCEED_REASON = new ChatMessage("disconnect.exceeded_packet_rate");
    private final int rateLimitPacketsPerSecond;

    public NetworkManagerServer(int rateLimit) {
        super(EnumProtocolDirection.SERVERBOUND);
        this.rateLimitPacketsPerSecond = rateLimit;
    }

    @Override
    protected void tickSecond() {
        super.tickSecond();
        float f = this.getAverageReceivedPackets();
        if (f > (float)this.rateLimitPacketsPerSecond) {
            LOGGER.warn("Player exceeded rate-limit (sent {} packets per second)", (float)f);
            this.send(new PacketPlayOutKickDisconnect(EXCEED_REASON), (future) -> {
                this.close(EXCEED_REASON);
            });
            this.stopReading();
        }

    }
}
