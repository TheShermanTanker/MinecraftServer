package net.minecraft.world.level.entity;

import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.world.phys.AxisAlignedBB;

public class WorldEntityAccess<T extends EntityAccess> implements IWorldEntityAccess<T> {
    private final EntityLookup<T> visibleEntities;
    private final EntitySectionStorage<T> sectionStorage;

    public WorldEntityAccess(EntityLookup<T> index, EntitySectionStorage<T> cache) {
        this.visibleEntities = index;
        this.sectionStorage = cache;
    }

    @Nullable
    @Override
    public T get(int id) {
        return this.visibleEntities.getEntity(id);
    }

    @Nullable
    @Override
    public T get(UUID uuid) {
        return this.visibleEntities.getEntity(uuid);
    }

    @Override
    public Iterable<T> getAll() {
        return this.visibleEntities.getAllEntities();
    }

    @Override
    public <U extends T> void get(EntityTypeTest<T, U> filter, Consumer<U> action) {
        this.visibleEntities.getEntities(filter, action);
    }

    @Override
    public void get(AxisAlignedBB box, Consumer<T> action) {
        this.sectionStorage.getEntities(box, action);
    }

    @Override
    public <U extends T> void get(EntityTypeTest<T, U> filter, AxisAlignedBB box, Consumer<U> action) {
        this.sectionStorage.getEntities(filter, box, action);
    }
}
