package net.minecraft.world.level.chunk.storage;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.BitSet;

public class RegionFileBitSet {
    private final BitSet used = new BitSet();

    public void force(int start, int size) {
        this.used.set(start, start + size);
    }

    public void free(int start, int size) {
        this.used.clear(start, start + size);
    }

    public int allocate(int size) {
        int i = 0;

        while(true) {
            int j = this.used.nextClearBit(i);
            int k = this.used.nextSetBit(j);
            if (k == -1 || k - j >= size) {
                this.force(j, size);
                return j;
            }

            i = k;
        }
    }

    @VisibleForTesting
    public IntSet getUsed() {
        return this.used.stream().collect(IntArraySet::new, IntCollection::add, IntCollection::addAll);
    }
}
