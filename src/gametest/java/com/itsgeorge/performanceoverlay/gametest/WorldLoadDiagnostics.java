package com.itsgeorge.performanceoverlay.gametest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

final class WorldLoadDiagnostics implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("performanceoverlay-client-tests");
    private static final int[] CAPTURE_AFTER_SECONDS = {30, 60, 120};

    private final long startedNanos = System.nanoTime();
    private final Thread watchdog;
    private volatile boolean closed;

    private WorldLoadDiagnostics() {
        watchdog = new Thread(this::run, "Performance Overlay world-load diagnostics");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    static WorldLoadDiagnostics start() {
        return new WorldLoadDiagnostics();
    }

    private void run() {
        for (int elapsedSeconds : CAPTURE_AFTER_SECONDS) {
            if (!waitUntil(elapsedSeconds)) {
                return;
            }

            LOGGER.error(buildReport(elapsedSeconds));
        }
    }

    private boolean waitUntil(int elapsedSeconds) {
        long deadlineNanos = startedNanos + TimeUnit.SECONDS.toNanos(elapsedSeconds);

        while (!closed) {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                return true;
            }

            try {
                Thread.sleep(Math.max(1, TimeUnit.NANOSECONDS.toMillis(remainingNanos)));
            } catch (InterruptedException ignored) {
                return false;
            }
        }

        return false;
    }

    private static String buildReport(int elapsedSeconds) {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        enableThreadCpuTime(threadBean);

        ThreadInfo[] threadInfos = threadBean.dumpAllThreads(true, true);
        Arrays.sort(threadInfos, Comparator.comparing(ThreadInfo::getThreadName));

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long[] deadlockedThreadIds = threadBean.findDeadlockedThreads();

        StringBuilder report = new StringBuilder(32_768);
        report.append("\n=== PERFORMANCE OVERLAY WORLD-LOAD DIAGNOSTIC: ")
                .append(elapsedSeconds)
                .append(" seconds ===\n")
                .append("Memory: used=").append(toMiB(usedMemory)).append(" MiB")
                .append(", committed=").append(toMiB(runtime.totalMemory())).append(" MiB")
                .append(", max=").append(toMiB(runtime.maxMemory())).append(" MiB\n")
                .append("Processors: ").append(runtime.availableProcessors())
                .append(", systemLoadAverage=")
                .append(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage())
                .append('\n')
                .append("Deadlocked thread IDs: ")
                .append(deadlockedThreadIds == null ? "none" : Arrays.toString(deadlockedThreadIds))
                .append("\n\n");

        for (ThreadInfo threadInfo : threadInfos) {
            if (threadInfo == null) {
                continue;
            }

            report.append('"').append(threadInfo.getThreadName()).append('"')
                    .append(" id=").append(threadInfo.getThreadId())
                    .append(" state=").append(threadInfo.getThreadState())
                    .append(" cpuMs=").append(threadCpuMillis(threadBean, threadInfo.getThreadId()))
                    .append(" blocked=").append(threadInfo.getBlockedCount())
                    .append(" waited=").append(threadInfo.getWaitedCount())
                    .append('\n');

            if (threadInfo.getLockName() != null) {
                report.append("    waitingOn=").append(threadInfo.getLockName())
                        .append(" ownedBy=").append(threadInfo.getLockOwnerName())
                        .append('\n');
            }

            for (StackTraceElement frame : threadInfo.getStackTrace()) {
                report.append("    at ").append(frame).append('\n');
            }

            report.append('\n');
        }

        report.append("=== END PERFORMANCE OVERLAY WORLD-LOAD DIAGNOSTIC ===");
        return report.toString();
    }

    private static void enableThreadCpuTime(ThreadMXBean threadBean) {
        if (!threadBean.isThreadCpuTimeSupported() || threadBean.isThreadCpuTimeEnabled()) {
            return;
        }

        try {
            threadBean.setThreadCpuTimeEnabled(true);
        } catch (UnsupportedOperationException | SecurityException ignored) {
            // Thread states and stacks remain useful when CPU timing is unavailable.
        }
    }

    private static long threadCpuMillis(ThreadMXBean threadBean, long threadId) {
        if (!threadBean.isThreadCpuTimeSupported() || !threadBean.isThreadCpuTimeEnabled()) {
            return -1;
        }

        long cpuNanos = threadBean.getThreadCpuTime(threadId);
        return cpuNanos < 0 ? -1 : TimeUnit.NANOSECONDS.toMillis(cpuNanos);
    }

    private static long toMiB(long bytes) {
        return bytes / (1024L * 1024L);
    }

    @Override
    public void close() {
        closed = true;
        watchdog.interrupt();
    }
}
