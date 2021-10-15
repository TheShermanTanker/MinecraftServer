package net.minecraft.util;

import java.util.concurrent.TimeUnit;
import net.minecraft.util.valueproviders.UniformInt;

public class TimeRange {
    public static final long NANOSECONDS_PER_SECOND = TimeUnit.SECONDS.toNanos(1L);
    public static final long NANOSECONDS_PER_MILLISECOND = TimeUnit.MILLISECONDS.toNanos(1L);

    public static UniformInt rangeOfSeconds(int min, int max) {
        return UniformInt.of(min * 20, max * 20);
    }
}
