package com.itsgeorge.performanceoverlay.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BenchmarkLifecycleTest {
    private static final long NS_PER_MS = 1_000_000L;
    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 8, 12, 22, 0, 0);

    @TempDir
    Path directory;

    @Test
    void manualStopExcludesFirstCallbackFinalizesCsvAndClearsState() throws Exception {
        FpsTracker tracker = new FpsTracker(new OverlayConfig());
        FpsTracker.BenchmarkStatus started = tracker.startBenchmark(directory, START_TIME, () -> ns(100));

        assertTrue(started.started());
        assertTrue(tracker.isBenchmarkActive());

        tracker.onFrame(false, ns(110));
        tracker.onFrame(false, ns(120));

        FpsTracker.BenchmarkStatus stopped = tracker.stopBenchmark(FpsTracker.BenchmarkEndReason.MANUAL);
        String csv = Files.readString(Path.of(stopped.filePath()));

        assertTrue(stopped.stopped());
        assertFalse(tracker.isBenchmarkActive());
        assertTrue(csv.contains("# EndReason: MANUAL\n"));
        assertTrue(csv.contains("# FramesLogged: 1\n"));
        assertTrue(csv.contains("# FramesSummary: 1\n"));
    }

    @Test
    void automaticStopRecordsItsEndReason() throws Exception {
        FpsTracker tracker = new FpsTracker(new OverlayConfig());
        tracker.startBenchmark(directory, START_TIME, () -> ns(100));

        FpsTracker.BenchmarkStatus stopped = tracker.stopBenchmark(FpsTracker.BenchmarkEndReason.AUTO_DURATION);
        String csv = Files.readString(Path.of(stopped.filePath()));

        assertTrue(stopped.stopped());
        assertFalse(tracker.isBenchmarkActive());
        assertTrue(csv.contains("# EndReason: AUTO_DURATION\n"));
        assertTrue(csv.endsWith("# MaxSpikeMs: 0.0\n"));
    }

    @Test
    void benchmarkKeepsPauseSettingCapturedAtStart() throws Exception {
        OverlayConfig config = new OverlayConfig();
        config.pauseHandling = OverlayConfig.PauseHandling.FREEZE;
        FpsTracker tracker = new FpsTracker(config);
        tracker.startBenchmark(directory, START_TIME, () -> ns(100));

        config.pauseHandling = OverlayConfig.PauseHandling.TRACK;
        tracker.setConfig(config, false);

        tracker.onFrame(false, ns(110));
        tracker.onFrame(true, ns(1110));
        tracker.onFrame(false, ns(1120));

        FpsTracker.BenchmarkStatus stopped = tracker.stopBenchmark(FpsTracker.BenchmarkEndReason.MANUAL);
        String csv = Files.readString(Path.of(stopped.filePath()));

        assertTrue(csv.contains("# PauseHandling: FREEZE\n"));
        assertTrue(csv.contains("# FramesLogged: 1\n"));
    }

    private static long ns(long milliseconds) {
        return milliseconds * NS_PER_MS;
    }
}
