package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.saveddata.maps.MapIcon;
import net.minecraft.world.level.saveddata.maps.WorldMap;

public class PacketPlayOutMap implements Packet<PacketListenerPlayOut> {
    private final int mapId;
    private final byte scale;
    private final boolean locked;
    @Nullable
    private final List<MapIcon> decorations;
    @Nullable
    private final WorldMap.MapPatch colorPatch;

    public PacketPlayOutMap(int id, byte scale, boolean showIcons, @Nullable Collection<MapIcon> icons, @Nullable WorldMap.MapPatch updateData) {
        this.mapId = id;
        this.scale = scale;
        this.locked = showIcons;
        this.decorations = icons != null ? Lists.newArrayList(icons) : null;
        this.colorPatch = updateData;
    }

    public PacketPlayOutMap(PacketDataSerializer buf) {
        this.mapId = buf.readVarInt();
        this.scale = buf.readByte();
        this.locked = buf.readBoolean();
        if (buf.readBoolean()) {
            this.decorations = buf.readList((b) -> {
                MapIcon.Type type = b.readEnum(MapIcon.Type.class);
                return new MapIcon(type, b.readByte(), b.readByte(), (byte)(b.readByte() & 15), b.readBoolean() ? b.readComponent() : null);
            });
        } else {
            this.decorations = null;
        }

        int i = buf.readUnsignedByte();
        if (i > 0) {
            int j = buf.readUnsignedByte();
            int k = buf.readUnsignedByte();
            int l = buf.readUnsignedByte();
            byte[] bs = buf.readByteArray();
            this.colorPatch = new WorldMap.MapPatch(k, l, i, j, bs);
        } else {
            this.colorPatch = null;
        }

    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.mapId);
        buf.writeByte(this.scale);
        buf.writeBoolean(this.locked);
        if (this.decorations != null) {
            buf.writeBoolean(true);
            buf.writeCollection(this.decorations, (b, icon) -> {
                b.writeEnum(icon.getType());
                b.writeByte(icon.getX());
                b.writeByte(icon.getY());
                b.writeByte(icon.getRotation() & 15);
                if (icon.getName() != null) {
                    b.writeBoolean(true);
                    b.writeComponent(icon.getName());
                } else {
                    b.writeBoolean(false);
                }

            });
        } else {
            buf.writeBoolean(false);
        }

        if (this.colorPatch != null) {
            buf.writeByte(this.colorPatch.width);
            buf.writeByte(this.colorPatch.height);
            buf.writeByte(this.colorPatch.startX);
            buf.writeByte(this.colorPatch.startY);
            buf.writeByteArray(this.colorPatch.mapColors);
        } else {
            buf.writeByte(0);
        }

    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleMapItemData(this);
    }

    public int getMapId() {
        return this.mapId;
    }

    public void applyToMap(WorldMap mapState) {
        if (this.decorations != null) {
            mapState.addClientSideDecorations(this.decorations);
        }

        if (this.colorPatch != null) {
            this.colorPatch.applyToMap(mapState);
        }

    }

    public byte getScale() {
        return this.scale;
    }

    public boolean isLocked() {
        return this.locked;
    }
}
