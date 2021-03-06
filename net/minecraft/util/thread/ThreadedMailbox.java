package net.minecraft.util.thread;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2BooleanFunction;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.SystemUtils;
import net.minecraft.util.profiling.metrics.EnumMetricCategory;
import net.minecraft.util.profiling.metrics.IProfilerMeasured;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.MetricsRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ThreadedMailbox<T> implements IProfilerMeasured, Mailbox<T>, AutoCloseable, Runnable {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int CLOSED_BIT = 1;
    private static final int SCHEDULED_BIT = 2;
    private final AtomicInteger status = new AtomicInteger(0);
    private final PairedQueue<? super T, ? extends Runnable> queue;
    private final Executor dispatcher;
    private final String name;

    public static ThreadedMailbox<Runnable> create(Executor executor, String name) {
        return new ThreadedMailbox<>(new PairedQueue.QueueStrictQueue<>(new ConcurrentLinkedQueue<>()), executor, name);
    }

    public ThreadedMailbox(PairedQueue<? super T, ? extends Runnable> queue, Executor executor, String name) {
        this.dispatcher = executor;
        this.queue = queue;
        this.name = name;
        MetricsRegistry.INSTANCE.add(this);
    }

    private boolean setAsScheduled() {
        int i;
        do {
            i = this.status.get();
            if ((i & 3) != 0) {
                return false;
            }
        } while(!this.status.compareAndSet(i, i | 2));

        return true;
    }

    private void setAsIdle() {
        int i;
        do {
            i = this.status.get();
        } while(!this.status.compareAndSet(i, i & -3));

    }

    private boolean canBeScheduled() {
        if ((this.status.get() & 1) != 0) {
            return false;
        } else {
            return !this.queue.isEmpty();
        }
    }

    @Override
    public void close() {
        int i;
        do {
            i = this.status.get();
        } while(!this.status.compareAndSet(i, i | 1));

    }

    private boolean shouldProcess() {
        return (this.status.get() & 2) != 0;
    }

    private boolean pollTask() {
        if (!this.shouldProcess()) {
            return false;
        } else {
            Runnable runnable = this.queue.pop();
            if (runnable == null) {
                return false;
            } else {
                SystemUtils.wrapThreadWithTaskName(this.name, runnable).run();
                return true;
            }
        }
    }

    @Override
    public void run() {
        try {
            this.pollUntil((runCount) -> {
                return runCount == 0;
            });
        } finally {
            this.setAsIdle();
            this.registerForExecution();
        }

    }

    public void runAll() {
        try {
            this.pollUntil((runCount) -> {
                return true;
            });
        } finally {
            this.setAsIdle();
            this.registerForExecution();
        }

    }

    @Override
    public void tell(T message) {
        this.queue.push(message);
        this.registerForExecution();
    }

    private void registerForExecution() {
        if (this.canBeScheduled() && this.setAsScheduled()) {
            try {
                this.dispatcher.execute(this);
            } catch (RejectedExecutionException var4) {
                try {
                    this.dispatcher.execute(this);
                } catch (RejectedExecutionException var3) {
                    LOGGER.error("Cound not schedule mailbox", (Throwable)var3);
                }
            }
        }

    }

    private int pollUntil(Int2BooleanFunction condition) {
        int i;
        for(i = 0; condition.get(i) && this.pollTask(); ++i) {
        }

        return i;
    }

    public int size() {
        return this.queue.size();
    }

    @Override
    public String toString() {
        return this.name + " " + this.status.get() + " " + this.queue.isEmpty();
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public List<MetricSampler> profiledMetrics() {
        return ImmutableList.of(MetricSampler.create(this.name + "-queue-size", EnumMetricCategory.MAIL_BOXES, this::size));
    }
}
