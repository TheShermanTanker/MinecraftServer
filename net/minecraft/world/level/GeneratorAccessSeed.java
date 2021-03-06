package net.minecraft.world.level;

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public interface GeneratorAccessSeed extends WorldAccess {
    long getSeed();

    List<? extends StructureStart<?>> startsForFeature(SectionPosition pos, StructureGenerator<?> feature);

    default boolean ensureCanWrite(BlockPosition pos) {
        return true;
    }

    default void setCurrentlyGenerating(@Nullable Supplier<String> structureName) {
    }
}
