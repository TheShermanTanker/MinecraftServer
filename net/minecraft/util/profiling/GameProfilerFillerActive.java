package net.minecraft.util.profiling;

import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.util.profiling.metrics.EnumMetricCategory;
import org.apache.commons.lang3.tuple.Pair;

public interface GameProfilerFillerActive extends GameProfilerFiller {
    MethodProfilerResults getResults();

    @Nullable
    MethodProfiler.PathEntry getEntry(String name);

    Set<Pair<String, EnumMetricCategory>> getChartedPaths();
}
