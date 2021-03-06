package net.minecraft.server.level;

import java.util.Comparator;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.Unit;
import net.minecraft.world.level.ChunkCoordIntPair;

public class TicketType<T> {
    private final String name;
    private final Comparator<T> comparator;
    public long timeout;
    public static final TicketType<Unit> START = create("start", (unit, unit2) -> {
        return 0;
    });
    public static final TicketType<Unit> DRAGON = create("dragon", (unit, unit2) -> {
        return 0;
    });
    public static final TicketType<ChunkCoordIntPair> PLAYER = create("player", Comparator.comparingLong(ChunkCoordIntPair::pair));
    public static final TicketType<ChunkCoordIntPair> FORCED = create("forced", Comparator.comparingLong(ChunkCoordIntPair::pair));
    public static final TicketType<ChunkCoordIntPair> LIGHT = create("light", Comparator.comparingLong(ChunkCoordIntPair::pair));
    public static final TicketType<BlockPosition> PORTAL = create("portal", BaseBlockPosition::compareTo, 300);
    public static final TicketType<Integer> POST_TELEPORT = create("post_teleport", Integer::compareTo, 5);
    public static final TicketType<ChunkCoordIntPair> UNKNOWN = create("unknown", Comparator.comparingLong(ChunkCoordIntPair::pair), 1);

    public static <T> TicketType<T> create(String name, Comparator<T> argumentComparator) {
        return new TicketType<>(name, argumentComparator, 0L);
    }

    public static <T> TicketType<T> create(String name, Comparator<T> argumentComparator, int expiryTicks) {
        return new TicketType<>(name, argumentComparator, (long)expiryTicks);
    }

    protected TicketType(String name, Comparator<T> argumentComparator, long expiryTicks) {
        this.name = name;
        this.comparator = argumentComparator;
        this.timeout = expiryTicks;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public Comparator<T> getComparator() {
        return this.comparator;
    }

    public long timeout() {
        return this.timeout;
    }
}
