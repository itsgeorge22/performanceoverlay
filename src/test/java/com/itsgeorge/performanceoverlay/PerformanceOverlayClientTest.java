package com.itsgeorge.performanceoverlay;

import com.itsgeorge.performanceoverlay.client.FpsTracker;
import com.itsgeorge.performanceoverlay.client.OverlayConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PerformanceOverlayClientTest {
    @TempDir
    Path directory;

    @Test
    void benchmarkKeyIsIgnoredOnAutoStopTick() {
        assertFalse(PerformanceOverlayClient.shouldHandleBenchmarkKey(true, false, false, true));
    }

    @Test
    void benchmarkKeyIsIgnoredWhenWriteErrorIsReportedOnSameTick() {
        assertFalse(PerformanceOverlayClient.shouldHandleBenchmarkKey(false, true, false, true));
    }

    @Test
    void benchmarkKeyWorksOnNormalTicks() {
        assertTrue(PerformanceOverlayClient.shouldHandleBenchmarkKey(false, false, false, true));
    }

    @Test
    void benchmarkKeyDoesNothingInMenus() {
        assertFalse(PerformanceOverlayClient.shouldHandleBenchmarkKey(false, false, false, false));
    }

    @Test
    void activeBenchmarkCanStillBeStoppedIfWorldStateDisappears() {
        assertTrue(PerformanceOverlayClient.shouldHandleBenchmarkKey(false, false, true, false));
    }

    @Test
    void completedDurationDisplaysOneHundredPercent() {
        assertEquals(100, PerformanceOverlayClient.benchmarkProgressPercent(7, 7));
    }

    @Test
    void automaticProgressIsBoundedFromZeroToOneHundredPercent() {
        assertEquals(0, PerformanceOverlayClient.benchmarkProgressPercent(0, 7));
        assertEquals(43, PerformanceOverlayClient.benchmarkProgressPercent(3, 7));
        assertEquals(100, PerformanceOverlayClient.benchmarkProgressPercent(8, 7));
    }

    @Test
    void clientShutdownFinalizesAnActiveBenchmark() throws Exception {
        FpsTracker tracker = new FpsTracker(new OverlayConfig());
        startBenchmark(tracker);
        tracker.onFrame(false);

        FpsTracker.BenchmarkStatus status = PerformanceOverlayClient.finalizeActiveBenchmark(
                tracker,
                FpsTracker.BenchmarkEndReason.GAME_SHUTDOWN
        );

        assertTrue(status.stopped());
        assertFalse(tracker.isBenchmarkActive());
        assertTrue(Files.readString(Path.of(status.filePath())).contains("# EndReason: GAME_SHUTDOWN\n"));
    }

    private void startBenchmark(FpsTracker tracker) throws Exception {
        Method method = FpsTracker.class.getDeclaredMethod(
                "startBenchmark",
                Path.class,
                LocalDateTime.class,
                LongSupplier.class
        );
        method.setAccessible(true);
        method.invoke(
                tracker,
                directory,
                LocalDateTime.of(2026, 8, 14, 1, 0),
                (LongSupplier) () -> 100_000_000L
        );
    }
}
