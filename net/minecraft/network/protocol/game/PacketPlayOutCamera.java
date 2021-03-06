package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.World;

public class PacketPlayOutCamera implements Packet<PacketListenerPlayOut> {
    private final int cameraId;

    public PacketPlayOutCamera(Entity entity) {
        this.cameraId = entity.getId();
    }

    public PacketPlayOutCamera(PacketDataSerializer buf) {
        this.cameraId = buf.readVarInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.cameraId);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetCamera(this);
    }

    @Nullable
    public Entity getEntity(World world) {
        return world.getEntity(this.cameraId);
    }
}
