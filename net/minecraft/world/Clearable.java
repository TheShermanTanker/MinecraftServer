package net.minecraft.world;

import javax.annotation.Nullable;

public interface Clearable {
    void clear();

    static void tryClear(@Nullable Object o) {
        if (o instanceof Clearable) {
            ((Clearable)o).clear();
        }

    }
}
