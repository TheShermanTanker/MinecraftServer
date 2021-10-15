package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;

public class PacketPlayOutAnimation implements Packet<PacketListenerPlayOut> {
    public static final int SWING_MAIN_HAND = 0;
    public static final int HURT = 1;
    public static final int WAKE_UP = 2;
    public static final int SWING_OFF_HAND = 3;
    public static final int CRITICAL_HIT = 4;
    public static final int MAGIC_CRITICAL_HIT = 5;
    private final int id;
    private final int action;

    public PacketPlayOutAnimation(Entity entity, int animationId) {
        this.id = entity.getId();
        this.action = animationId;
    }

    public PacketPlayOutAnimation(PacketDataSerializer buf) {
        this.id = buf.readVarInt();
        this.action = buf.readUnsignedByte();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.id);
        buf.writeByte(this.action);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleAnimate(this);
    }

    public int getId() {
        return this.id;
    }

    public int getAction() {
        return this.action;
    }
}
