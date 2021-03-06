package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;

public class PacketPlayOutEntityTeleport implements Packet<PacketListenerPlayOut> {
    private final int id;
    private final double x;
    private final double y;
    private final double z;
    private final byte yRot;
    private final byte xRot;
    private final boolean onGround;

    public PacketPlayOutEntityTeleport(Entity entity) {
        this.id = entity.getId();
        this.x = entity.locX();
        this.y = entity.locY();
        this.z = entity.locZ();
        this.yRot = (byte)((int)(entity.getYRot() * 256.0F / 360.0F));
        this.xRot = (byte)((int)(entity.getXRot() * 256.0F / 360.0F));
        this.onGround = entity.isOnGround();
    }

    public PacketPlayOutEntityTeleport(PacketDataSerializer buf) {
        this.id = buf.readVarInt();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.yRot = buf.readByte();
        this.xRot = buf.readByte();
        this.onGround = buf.readBoolean();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.id);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeByte(this.yRot);
        buf.writeByte(this.xRot);
        buf.writeBoolean(this.onGround);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleTeleportEntity(this);
    }

    public int getId() {
        return this.id;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public byte getyRot() {
        return this.yRot;
    }

    public byte getxRot() {
        return this.xRot;
    }

    public boolean isOnGround() {
        return this.onGround;
    }
}
