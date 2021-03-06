package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutEntityDestroy implements Packet<PacketListenerPlayOut> {
    private final IntList entityIds;

    public PacketPlayOutEntityDestroy(IntList entityIds) {
        this.entityIds = new IntArrayList(entityIds);
    }

    public PacketPlayOutEntityDestroy(int... entityIds) {
        this.entityIds = new IntArrayList(entityIds);
    }

    public PacketPlayOutEntityDestroy(PacketDataSerializer buf) {
        this.entityIds = buf.readIntIdList();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeIntIdList(this.entityIds);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleRemoveEntities(this);
    }

    public IntList getEntityIds() {
        return this.entityIds;
    }
}
