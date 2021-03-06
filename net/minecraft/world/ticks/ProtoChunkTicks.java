package net.minecraft.world.ticks;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.level.ChunkCoordIntPair;

public class ProtoChunkTicks<T> implements SerializableTickContainer<T>, TickContainerAccess<T> {
    private final List<SavedTick<T>> ticks = Lists.newArrayList();
    private final Set<SavedTick<?>> ticksPerPosition = new ObjectOpenCustomHashSet<>(SavedTick.UNIQUE_TICK_HASH);

    @Override
    public void schedule(ScheduledTick<T> orderedTick) {
        SavedTick<T> savedTick = new SavedTick<>(orderedTick.type(), orderedTick.pos(), 0, orderedTick.priority());
        this.schedule(savedTick);
    }

    private void schedule(SavedTick<T> tick) {
        if (this.ticksPerPosition.add(tick)) {
            this.ticks.add(tick);
        }

    }

    @Override
    public boolean hasScheduledTick(BlockPosition pos, T type) {
        return this.ticksPerPosition.contains(SavedTick.probe(type, pos));
    }

    @Override
    public int count() {
        return this.ticks.size();
    }

    @Override
    public NBTBase save(long time, Function<T, String> typeToNameFunction) {
        NBTTagList listTag = new NBTTagList();

        for(SavedTick<T> savedTick : this.ticks) {
            listTag.add(savedTick.save(typeToNameFunction));
        }

        return listTag;
    }

    public List<SavedTick<T>> scheduledTicks() {
        return List.copyOf(this.ticks);
    }

    public static <T> ProtoChunkTicks<T> load(NBTTagList tickList, Function<String, Optional<T>> typeToNameFunction, ChunkCoordIntPair pos) {
        ProtoChunkTicks<T> protoChunkTicks = new ProtoChunkTicks<>();
        SavedTick.loadTickList(tickList, typeToNameFunction, pos, protoChunkTicks::schedule);
        return protoChunkTicks;
    }
}
