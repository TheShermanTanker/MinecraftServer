package net.minecraft.util.profiling.metrics.profiling;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;
import net.minecraft.util.profiling.GameProfilerFillerActive;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.MetricsRegistry;
import net.minecraft.util.profiling.metrics.MetricsSamplerProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

public class ServerMetricsSamplersProvider implements MetricsSamplerProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Set<MetricSampler> samplers = new ObjectOpenHashSet<>();
    private final ProfilerSamplerAdapter samplerFactory = new ProfilerSamplerAdapter();

    public ServerMetricsSamplersProvider(LongSupplier nanoTimeSupplier, boolean includeSystem) {
        this.samplers.add(tickTimeSampler(nanoTimeSupplier));
        if (includeSystem) {
            this.samplers.addAll(runtimeIndependentSamplers());
        }

    }

    public static Set<MetricSampler> runtimeIndependentSamplers() {
        Builder<MetricSampler> builder = ImmutableSet.builder();

        try {
            ServerMetricsSamplersProvider.CpuStats cpuStats = new ServerMetricsSamplersProvider.CpuStats();
            IntStream.range(0, cpuStats.nrOfCpus).mapToObj((index) -> {
                return MetricSampler.create("cpu#" + index, MetricCategory.CPU, () -> {
                    return cpuStats.loadForCpu(index);
                });
            }).forEach(builder::add);
        } catch (Throwable var2) {
            LOGGER.warn("Failed to query cpu, no cpu stats will be recorded", var2);
        }

        builder.add(MetricSampler.create("heap MiB", MetricCategory.JVM, () -> {
            return (double)((float)(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576.0F);
        }));
        builder.addAll(MetricsRegistry.INSTANCE.getRegisteredSamplers());
        return builder.build();
    }

    @Override
    public Set<MetricSampler> samplers(Supplier<GameProfilerFillerActive> profilerSupplier) {
        this.samplers.addAll(this.samplerFactory.newSamplersFoundInProfiler(profilerSupplier));
        return this.samplers;
    }

    public static MetricSampler tickTimeSampler(LongSupplier nanoTimeSupplier) {
        Stopwatch stopwatch = Stopwatch.createUnstarted(new Ticker() {
            @Override
            public long read() {
                return nanoTimeSupplier.getAsLong();
            }
        });
        ToDoubleFunction<Stopwatch> toDoubleFunction = (watch) -> {
            if (watch.isRunning()) {
                watch.stop();
            }

            long l = watch.elapsed(TimeUnit.NANOSECONDS);
            watch.reset();
            return (double)l;
        };
        MetricSampler.ValueIncreasedByPercentage valueIncreasedByPercentage = new MetricSampler.ValueIncreasedByPercentage(2.0F);
        return MetricSampler.builder("ticktime", MetricCategory.TICK_LOOP, toDoubleFunction, stopwatch).withBeforeTick(Stopwatch::start).withThresholdAlert(valueIncreasedByPercentage).build();
    }

    static class CpuStats {
        private final SystemInfo systemInfo = new SystemInfo();
        private final CentralProcessor processor = this.systemInfo.getHardware().getProcessor();
        public final int nrOfCpus = this.processor.getLogicalProcessorCount();
        private long[][] previousCpuLoadTick = this.processor.getProcessorCpuLoadTicks();
        private double[] currentLoad = this.processor.getProcessorCpuLoadBetweenTicks(this.previousCpuLoadTick);
        private long lastPollMs;

        public double loadForCpu(int index) {
            long l = System.currentTimeMillis();
            if (this.lastPollMs == 0L || this.lastPollMs + 501L < l) {
                this.currentLoad = this.processor.getProcessorCpuLoadBetweenTicks(this.previousCpuLoadTick);
                this.previousCpuLoadTick = this.processor.getProcessorCpuLoadTicks();
                this.lastPollMs = l;
            }

            return this.currentLoad[index] * 100.0D;
        }
    }
}