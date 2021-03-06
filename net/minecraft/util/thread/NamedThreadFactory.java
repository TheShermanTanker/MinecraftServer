package net.minecraft.util.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NamedThreadFactory implements ThreadFactory {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    public NamedThreadFactory(String name) {
        SecurityManager securityManager = System.getSecurityManager();
        this.group = securityManager != null ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.namePrefix = name + "-";
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(this.group, runnable, this.namePrefix + this.threadNumber.getAndIncrement(), 0L);
        thread.setUncaughtExceptionHandler((threadx, throwable) -> {
            LOGGER.error("Caught exception in thread {} from {}", threadx, runnable);
            LOGGER.error("", throwable);
        });
        if (thread.getPriority() != 5) {
            thread.setPriority(5);
        }

        return thread;
    }
}
