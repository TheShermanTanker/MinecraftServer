package net.minecraft.util.profiling;

import java.nio.file.Path;
import java.util.List;

public interface MethodProfilerResults {
    char PATH_SEPARATOR = '\u001e';

    List<MethodProfilerResultsField> getTimes(String parentPath);

    boolean saveResults(Path path);

    long getStartTimeNano();

    int getStartTimeTicks();

    long getEndTimeNano();

    int getEndTimeTicks();

    default long getNanoDuration() {
        return this.getEndTimeNano() - this.getStartTimeNano();
    }

    default int getTickDuration() {
        return this.getEndTimeTicks() - this.getStartTimeTicks();
    }

    String getProfilerResults();

    static String demanglePath(String path) {
        return path.replace('\u001e', '.');
    }
}
