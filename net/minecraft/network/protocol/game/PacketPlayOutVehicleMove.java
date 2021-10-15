package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;

public class PacketPlayOutVehicleMove implements Packet<PacketListenerPlayOut> {
    private final double x;
    private final double y;
    private final double z;
    private final float yRot;
    private final float xRot;

    public PacketPlayOutVehicleMove(Entity entity) {
        this.x = entity.locX();
        this.y = entity.locY();
        this.z = entity.locZ();
        this.yRot = entity.getYRot();
        this.xRot = entity.getXRot();
    }

    public PacketPlayOutVehicleMove(PacketDataSerializer buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.yRot = buf.readFloat();
        this.xRot = buf.readFloat();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeFloat(this.yRot);
        buf.writeFloat(this.xRot);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleMoveVehicle(this);
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

    public float getYRot() {
        return this.yRot;
    }

    public float getXRot() {
        return this.xRot;
    }
}
