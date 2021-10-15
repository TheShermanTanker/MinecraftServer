package net.minecraft.util;

import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;

public class ThreadingDetector {
    public static void checkAndLock(Semaphore semaphore, @Nullable DebugBuffer<Pair<Thread, StackTraceElement[]>> lockStack, String message) {
        boolean bl = semaphore.tryAcquire();
        if (!bl) {
            throw makeThreadingException(message, lockStack);
        }
    }

    public static ReportedException makeThreadingException(String message, @Nullable DebugBuffer<Pair<Thread, StackTraceElement[]>> lockStack) {
        String string = Thread.getAllStackTraces().keySet().stream().filter(Objects::nonNull).map((thread) -> {
            return thread.getName() + ": \n\tat " + (String)Arrays.stream(thread.getStackTrace()).map(Object::toString).collect(Collectors.joining("\n\tat "));
        }).collect(Collectors.joining("\n"));
        CrashReport crashReport = new CrashReport("Accessing " + message + " from multiple threads", new IllegalStateException());
        CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Thread dumps");
        crashReportCategory.setDetail("Thread dumps", string);
        if (lockStack != null) {
            StringBuilder stringBuilder = new StringBuilder();

            for(Pair<Thread, StackTraceElement[]> pair : lockStack.dump()) {
                stringBuilder.append("Thread ").append(pair.getFirst().getName()).append(": \n\tat ").append(Arrays.stream(pair.getSecond()).map(Object::toString).collect(Collectors.joining("\n\tat "))).append("\n");
            }

            crashReportCategory.setDetail("Last threads", stringBuilder.toString());
        }

        return new ReportedException(crashReport);
    }
}
