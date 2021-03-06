package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.io.IOException;
import net.minecraft.network.protocol.EnumProtocolDirection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class PacketEncoder extends MessageToByteEncoder<Packet<?>> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker MARKER = MarkerManager.getMarker("PACKET_SENT", NetworkManager.PACKET_MARKER);
    private final EnumProtocolDirection flow;

    public PacketEncoder(EnumProtocolDirection side) {
        this.flow = side;
    }

    protected void encode(ChannelHandlerContext channelHandlerContext, Packet<?> packet, ByteBuf byteBuf) throws Exception {
        EnumProtocol connectionProtocol = channelHandlerContext.channel().attr(NetworkManager.ATTRIBUTE_PROTOCOL).get();
        if (connectionProtocol == null) {
            throw new RuntimeException("ConnectionProtocol unknown: " + packet);
        } else {
            Integer integer = connectionProtocol.getPacketId(this.flow, packet);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(MARKER, "OUT: [{}:{}] {}", channelHandlerContext.channel().attr(NetworkManager.ATTRIBUTE_PROTOCOL).get(), integer, packet.getClass().getName());
            }

            if (integer == null) {
                throw new IOException("Can't serialize unregistered packet");
            } else {
                PacketDataSerializer friendlyByteBuf = new PacketDataSerializer(byteBuf);
                friendlyByteBuf.writeVarInt(integer);

                try {
                    int i = friendlyByteBuf.writerIndex();
                    packet.write(friendlyByteBuf);
                    int j = friendlyByteBuf.writerIndex() - i;
                    if (j > 8388608) {
                        throw new IllegalArgumentException("Packet too big (is " + j + ", should be less than 8388608): " + packet);
                    } else {
                        int k = channelHandlerContext.channel().attr(NetworkManager.ATTRIBUTE_PROTOCOL).get().getId();
                        JvmProfiler.INSTANCE.onPacketSent(k, integer, channelHandlerContext.channel().remoteAddress(), j);
                    }
                } catch (Throwable var10) {
                    LOGGER.error(var10);
                    if (packet.isSkippable()) {
                        throw new SkipEncodeException(var10);
                    } else {
                        throw var10;
                    }
                }
            }
        }
    }
}
