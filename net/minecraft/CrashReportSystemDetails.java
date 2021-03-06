package net.minecraft;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.block.state.IBlockData;

public class CrashReportSystemDetails {
    private final String title;
    private final List<CrashReportSystemDetails.CrashReportDetail> entries = Lists.newArrayList();
    private StackTraceElement[] stackTrace = new StackTraceElement[0];

    public CrashReportSystemDetails(String title) {
        this.title = title;
    }

    public static String formatLocation(IWorldHeightAccess world, double x, double y, double z) {
        return String.format(Locale.ROOT, "%.2f,%.2f,%.2f - %s", x, y, z, formatLocation(world, new BlockPosition(x, y, z)));
    }

    public static String formatLocation(IWorldHeightAccess world, BlockPosition pos) {
        return formatLocation(world, pos.getX(), pos.getY(), pos.getZ());
    }

    public static String formatLocation(IWorldHeightAccess world, int x, int y, int z) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            stringBuilder.append(String.format("World: (%d,%d,%d)", x, y, z));
        } catch (Throwable var19) {
            stringBuilder.append("(Error finding world loc)");
        }

        stringBuilder.append(", ");

        try {
            int i = SectionPosition.blockToSectionCoord(x);
            int j = SectionPosition.blockToSectionCoord(y);
            int k = SectionPosition.blockToSectionCoord(z);
            int l = x & 15;
            int m = y & 15;
            int n = z & 15;
            int o = SectionPosition.sectionToBlockCoord(i);
            int p = world.getMinBuildHeight();
            int q = SectionPosition.sectionToBlockCoord(k);
            int r = SectionPosition.sectionToBlockCoord(i + 1) - 1;
            int s = world.getMaxBuildHeight() - 1;
            int t = SectionPosition.sectionToBlockCoord(k + 1) - 1;
            stringBuilder.append(String.format("Section: (at %d,%d,%d in %d,%d,%d; chunk contains blocks %d,%d,%d to %d,%d,%d)", l, m, n, i, j, k, o, p, q, r, s, t));
        } catch (Throwable var18) {
            stringBuilder.append("(Error finding chunk loc)");
        }

        stringBuilder.append(", ");

        try {
            int u = x >> 9;
            int v = z >> 9;
            int w = u << 5;
            int aa = v << 5;
            int ab = (u + 1 << 5) - 1;
            int ac = (v + 1 << 5) - 1;
            int ad = u << 9;
            int ae = world.getMinBuildHeight();
            int af = v << 9;
            int ag = (u + 1 << 9) - 1;
            int ah = world.getMaxBuildHeight() - 1;
            int ai = (v + 1 << 9) - 1;
            stringBuilder.append(String.format("Region: (%d,%d; contains chunks %d,%d to %d,%d, blocks %d,%d,%d to %d,%d,%d)", u, v, w, aa, ab, ac, ad, ae, af, ag, ah, ai));
        } catch (Throwable var17) {
            stringBuilder.append("(Error finding world loc)");
        }

        return stringBuilder.toString();
    }

    public CrashReportSystemDetails setDetail(String name, CrashReportCallable<String> crashReportDetail) {
        try {
            this.setDetail(name, crashReportDetail.call());
        } catch (Throwable var4) {
            this.setDetailError(name, var4);
        }

        return this;
    }

    public CrashReportSystemDetails setDetail(String name, Object detail) {
        this.entries.add(new CrashReportSystemDetails.CrashReportDetail(name, detail));
        return this;
    }

    public void setDetailError(String name, Throwable throwable) {
        this.setDetail(name, throwable);
    }

    public int fillInStackTrace(int ignoredCallCount) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        if (stackTraceElements.length <= 0) {
            return 0;
        } else {
            this.stackTrace = new StackTraceElement[stackTraceElements.length - 3 - ignoredCallCount];
            System.arraycopy(stackTraceElements, 3 + ignoredCallCount, this.stackTrace, 0, this.stackTrace.length);
            return this.stackTrace.length;
        }
    }

    public boolean validateStackTrace(StackTraceElement prev, StackTraceElement next) {
        if (this.stackTrace.length != 0 && prev != null) {
            StackTraceElement stackTraceElement = this.stackTrace[0];
            if (stackTraceElement.isNativeMethod() == prev.isNativeMethod() && stackTraceElement.getClassName().equals(prev.getClassName()) && stackTraceElement.getFileName().equals(prev.getFileName()) && stackTraceElement.getMethodName().equals(prev.getMethodName())) {
                if (next != null != this.stackTrace.length > 1) {
                    return false;
                } else if (next != null && !this.stackTrace[1].equals(next)) {
                    return false;
                } else {
                    this.stackTrace[0] = prev;
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void trimStacktrace(int callCount) {
        StackTraceElement[] stackTraceElements = new StackTraceElement[this.stackTrace.length - callCount];
        System.arraycopy(this.stackTrace, 0, stackTraceElements, 0, stackTraceElements.length);
        this.stackTrace = stackTraceElements;
    }

    public void getDetails(StringBuilder crashReportBuilder) {
        crashReportBuilder.append("-- ").append(this.title).append(" --\n");
        crashReportBuilder.append("Details:");

        for(CrashReportSystemDetails.CrashReportDetail entry : this.entries) {
            crashReportBuilder.append("\n\t");
            crashReportBuilder.append(entry.getKey());
            crashReportBuilder.append(": ");
            crashReportBuilder.append(entry.getValue());
        }

        if (this.stackTrace != null && this.stackTrace.length > 0) {
            crashReportBuilder.append("\nStacktrace:");

            for(StackTraceElement stackTraceElement : this.stackTrace) {
                crashReportBuilder.append("\n\tat ");
                crashReportBuilder.append((Object)stackTraceElement);
            }
        }

    }

    public StackTraceElement[] getStacktrace() {
        return this.stackTrace;
    }

    public static void populateBlockDetails(CrashReportSystemDetails element, IWorldHeightAccess world, BlockPosition pos, @Nullable IBlockData state) {
        if (state != null) {
            element.setDetail("Block", state::toString);
        }

        element.setDetail("Block location", () -> {
            return formatLocation(world, pos);
        });
    }

    static class CrashReportDetail {
        private final String key;
        private final String value;

        public CrashReportDetail(String name, @Nullable Object detail) {
            this.key = name;
            if (detail == null) {
                this.value = "~~NULL~~";
            } else if (detail instanceof Throwable) {
                Throwable throwable = (Throwable)detail;
                this.value = "~~ERROR~~ " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
            } else {
                this.value = detail.toString();
            }

        }

        public String getKey() {
            return this.key;
        }

        public String getValue() {
            return this.value;
        }
    }
}
