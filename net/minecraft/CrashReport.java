package net.minecraft;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletionException;
import net.minecraft.util.MemoryReserve;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CrashReport {
    private static final Logger LOGGER = LogManager.getLogger();
    private final String title;
    private final Throwable exception;
    private final List<CrashReportSystemDetails> details = Lists.newArrayList();
    private File saveFile;
    private boolean trackingStackTrace = true;
    private StackTraceElement[] uncategorizedStackTrace = new StackTraceElement[0];
    private final SystemReport systemReport = new SystemReport();

    public CrashReport(String message, Throwable cause) {
        this.title = message;
        this.exception = cause;
    }

    public String getTitle() {
        return this.title;
    }

    public Throwable getException() {
        return this.exception;
    }

    public String getDetails() {
        StringBuilder stringBuilder = new StringBuilder();
        this.getDetails(stringBuilder);
        return stringBuilder.toString();
    }

    public void getDetails(StringBuilder crashReportBuilder) {
        if ((this.uncategorizedStackTrace == null || this.uncategorizedStackTrace.length <= 0) && !this.details.isEmpty()) {
            this.uncategorizedStackTrace = ArrayUtils.subarray(this.details.get(0).getStacktrace(), 0, 1);
        }

        if (this.uncategorizedStackTrace != null && this.uncategorizedStackTrace.length > 0) {
            crashReportBuilder.append("-- Head --\n");
            crashReportBuilder.append("Thread: ").append(Thread.currentThread().getName()).append("\n");
            crashReportBuilder.append("Stacktrace:\n");

            for(StackTraceElement stackTraceElement : this.uncategorizedStackTrace) {
                crashReportBuilder.append("\t").append("at ").append((Object)stackTraceElement);
                crashReportBuilder.append("\n");
            }

            crashReportBuilder.append("\n");
        }

        for(CrashReportSystemDetails crashReportCategory : this.details) {
            crashReportCategory.getDetails(crashReportBuilder);
            crashReportBuilder.append("\n\n");
        }

        this.systemReport.appendToCrashReportString(crashReportBuilder);
    }

    public String getExceptionMessage() {
        StringWriter stringWriter = null;
        PrintWriter printWriter = null;
        Throwable throwable = this.exception;
        if (throwable.getMessage() == null) {
            if (throwable instanceof NullPointerException) {
                throwable = new NullPointerException(this.title);
            } else if (throwable instanceof StackOverflowError) {
                throwable = new StackOverflowError(this.title);
            } else if (throwable instanceof OutOfMemoryError) {
                throwable = new OutOfMemoryError(this.title);
            }

            throwable.setStackTrace(this.exception.getStackTrace());
        }

        String var4;
        try {
            stringWriter = new StringWriter();
            printWriter = new PrintWriter(stringWriter);
            throwable.printStackTrace(printWriter);
            var4 = stringWriter.toString();
        } finally {
            IOUtils.closeQuietly((Writer)stringWriter);
            IOUtils.closeQuietly((Writer)printWriter);
        }

        return var4;
    }

    public String getFriendlyReport() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("---- Minecraft Crash Report ----\n");
        stringBuilder.append("// ");
        stringBuilder.append(getErrorComment());
        stringBuilder.append("\n\n");
        stringBuilder.append("Time: ");
        stringBuilder.append((new SimpleDateFormat()).format(new Date()));
        stringBuilder.append("\n");
        stringBuilder.append("Description: ");
        stringBuilder.append(this.title);
        stringBuilder.append("\n\n");
        stringBuilder.append(this.getExceptionMessage());
        stringBuilder.append("\n\nA detailed walkthrough of the error, its code path and all known details is as follows:\n");

        for(int i = 0; i < 87; ++i) {
            stringBuilder.append("-");
        }

        stringBuilder.append("\n\n");
        this.getDetails(stringBuilder);
        return stringBuilder.toString();
    }

    public File getSaveFile() {
        return this.saveFile;
    }

    public boolean saveToFile(File file) {
        if (this.saveFile != null) {
            return false;
        } else {
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            Writer writer = null;

            boolean var4;
            try {
                writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
                writer.write(this.getFriendlyReport());
                this.saveFile = file;
                return true;
            } catch (Throwable var8) {
                LOGGER.error("Could not save crash report to {}", file, var8);
                var4 = false;
            } finally {
                IOUtils.closeQuietly(writer);
            }

            return var4;
        }
    }

    public SystemReport getSystemReport() {
        return this.systemReport;
    }

    public CrashReportSystemDetails addCategory(String name) {
        return this.addCategory(name, 1);
    }

    public CrashReportSystemDetails addCategory(String name, int ignoredStackTraceCallCount) {
        CrashReportSystemDetails crashReportCategory = new CrashReportSystemDetails(name);
        if (this.trackingStackTrace) {
            int i = crashReportCategory.fillInStackTrace(ignoredStackTraceCallCount);
            StackTraceElement[] stackTraceElements = this.exception.getStackTrace();
            StackTraceElement stackTraceElement = null;
            StackTraceElement stackTraceElement2 = null;
            int j = stackTraceElements.length - i;
            if (j < 0) {
                System.out.println("Negative index in crash report handler (" + stackTraceElements.length + "/" + i + ")");
            }

            if (stackTraceElements != null && 0 <= j && j < stackTraceElements.length) {
                stackTraceElement = stackTraceElements[j];
                if (stackTraceElements.length + 1 - i < stackTraceElements.length) {
                    stackTraceElement2 = stackTraceElements[stackTraceElements.length + 1 - i];
                }
            }

            this.trackingStackTrace = crashReportCategory.validateStackTrace(stackTraceElement, stackTraceElement2);
            if (i > 0 && !this.details.isEmpty()) {
                CrashReportSystemDetails crashReportCategory2 = this.details.get(this.details.size() - 1);
                crashReportCategory2.trimStacktrace(i);
            } else if (stackTraceElements != null && stackTraceElements.length >= i && 0 <= j && j < stackTraceElements.length) {
                this.uncategorizedStackTrace = new StackTraceElement[j];
                System.arraycopy(stackTraceElements, 0, this.uncategorizedStackTrace, 0, this.uncategorizedStackTrace.length);
            } else {
                this.trackingStackTrace = false;
            }
        }

        this.details.add(crashReportCategory);
        return crashReportCategory;
    }

    private static String getErrorComment() {
        String[] strings = new String[]{"Who set us up the TNT?", "Everything's going to plan. No, really, that was supposed to happen.", "Uh... Did I do that?", "Oops.", "Why did you do that?", "I feel sad now :(", "My bad.", "I'm sorry, Dave.", "I let you down. Sorry :(", "On the bright side, I bought you a teddy bear!", "Daisy, daisy...", "Oh - I know what I did wrong!", "Hey, that tickles! Hehehe!", "I blame Dinnerbone.", "You should try our sister game, Minceraft!", "Don't be sad. I'll do better next time, I promise!", "Don't be sad, have a hug! <3", "I just don't know what went wrong :(", "Shall we play a game?", "Quite honestly, I wouldn't worry myself about that.", "I bet Cylons wouldn't have this problem.", "Sorry :(", "Surprise! Haha. Well, this is awkward.", "Would you like a cupcake?", "Hi. I'm Minecraft, and I'm a crashaholic.", "Ooh. Shiny.", "This doesn't make any sense!", "Why is it breaking :(", "Don't do that.", "Ouch. That hurt :(", "You're mean.", "This is a token for 1 free hug. Redeem at your nearest Mojangsta: [~~HUG~~]", "There are four lights!", "But it works on my machine."};

        try {
            return strings[(int)(SystemUtils.getMonotonicNanos() % (long)strings.length)];
        } catch (Throwable var2) {
            return "Witty comment unavailable :(";
        }
    }

    public static CrashReport forThrowable(Throwable cause, String title) {
        while(cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }

        CrashReport crashReport;
        if (cause instanceof ReportedException) {
            crashReport = ((ReportedException)cause).getReport();
        } else {
            crashReport = new CrashReport(title, cause);
        }

        return crashReport;
    }

    public static void preload() {
        MemoryReserve.allocate();
        (new CrashReport("Don't panic!", new Throwable())).getFriendlyReport();
    }
}
