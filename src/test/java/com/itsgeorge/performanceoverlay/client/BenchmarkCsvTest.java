package com.itsgeorge.performanceoverlay.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BenchmarkCsvTest {
    private static final long NS_PER_MS = 1_000_000L;
    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 8, 12, 22, 30, 45);
    private static final String COLUMNS =
            "elapsed_ms,frame_ms,inst_fps,fps_smoothed,avg_fps,low1_fps,low01_fps," +
                    "stutters,stutter_percent,max_spike_ms,gc_time_delta_ms,mem_used_mb,mem_max_mb";

    @TempDir
    Path directory;

    @Test
    void csvContainsCompleteMetadataColumnsRowsAndFooterFromCapturedSettings() throws Exception {
        OverlayConfig config = configuredSettings();
        FpsTracker tracker = new FpsTracker(config);
        FpsTracker.BenchmarkStatus started = tracker.startBenchmark(
                directory,
                START_TIME,
                () -> ns(100),
                "1.0.2-test",
                "1.21.11"
        );

        changeSettingsAfterStart(config);
        tracker.setConfig(config, false);

        tracker.onFrame(false, ns(110));
        tracker.onFrame(false, ns(120));
        tracker.onFrame(false, ns(140));

        FpsTracker.BenchmarkStatus stopped = tracker.stopBenchmark(FpsTracker.BenchmarkEndReason.MANUAL);
        List<String> lines = Files.readAllLines(Path.of(stopped.filePath()));

        assertTrue(started.started());
        assertTrue(stopped.stopped());
        assertEquals("# PerformanceOverlay Benchmark", lines.get(0));
        assertEquals("# Date: 2026-08-12 22:30:45", lines.get(1));
        assertEquals("# ModVersion: 1.0.2-test", lines.get(2));
        assertEquals("# Minecraft: 1.21.11", lines.get(3));

        assertTrue(lines.contains("# DurationSec: 17"));
        assertTrue(lines.contains("# PauseHandling: FREEZE"));
        assertTrue(lines.contains("# LowMethod: MEAN_WORST"));
        assertTrue(lines.contains("# StutterThresholdMs: 15"));
        assertTrue(lines.contains("# StutterWindowSec: 12"));
        assertTrue(lines.contains("# FpsWindowMs: 750"));
        assertTrue(lines.contains("# AvgWindowSec: 11"));
        assertTrue(lines.contains("# Low1WindowSec: 12"));
        assertTrue(lines.contains("# Low01WindowSec: 13"));
        assertTrue(lines.contains("# FpsUpdateMs: 100"));
        assertTrue(lines.contains("# FrametimeUpdateMs: 200"));
        assertTrue(lines.contains("# AvgUpdateMs: 300"));
        assertTrue(lines.contains("# Low1UpdateMs: 400"));
        assertTrue(lines.contains("# Low01UpdateMs: 500"));
        assertTrue(lines.contains("# StuttersUpdateMs: 600"));
        assertTrue(lines.contains("# GcMetric: TIME_DELTA_SINCE_PREVIOUS_POLL"));

        int headerIndex = lines.indexOf(COLUMNS);
        int footerIndex = lines.indexOf("# EndReason: MANUAL");
        assertTrue(headerIndex >= 0);
        assertEquals(headerIndex + 3, footerIndex);

        String[] firstRow = lines.get(headerIndex + 1).split(",", -1);
        String[] secondRow = lines.get(headerIndex + 2).split(",", -1);
        assertEquals(13, firstRow.length);
        assertEquals(13, secondRow.length);
        assertEquals("20", firstRow[0]);
        assertEquals("10.000", firstRow[1]);
        assertEquals("100.0", firstRow[2]);
        assertEquals("0.0", firstRow[10]);
        assertEquals("40", secondRow[0]);
        assertEquals("20.000", secondRow[1]);
        assertEquals("50.0", secondRow[2]);
        assertEquals("", secondRow[10]);

        assertTrue(lines.contains("# SUMMARY"));
        assertTrue(lines.contains("# FramesLogged: 2"));
        assertTrue(lines.contains("# FramesSummary: 2"));
        assertTrue(lines.contains("# Stutters: 1"));
        assertTrue(lines.contains("# StutterPercent: 50"));

        assertFalse(lines.contains("# DurationSec: 99"));
        assertFalse(lines.contains("# PauseHandling: TRACK"));
        assertFalse(lines.contains("# StutterThresholdMs: 5"));
    }

    private static OverlayConfig configuredSettings() {
        OverlayConfig config = new OverlayConfig();
        config.autoBenchmarkDurationSec = 17;
        config.pauseHandling = OverlayConfig.PauseHandling.FREEZE;
        config.lowMethod = OverlayConfig.LowMethod.MEAN_WORST;
        config.stutterThresholdMs = 15;
        config.stutterWindowSec = 12;
        config.fpsWindowMs = 750;
        config.avgWindowSec = 11;
        config.low1WindowSec = 12;
        config.low01WindowSec = 13;
        config.fpsUpdateMs = 100;
        config.frametimeUpdateMs = 200;
        config.avgUpdateMs = 300;
        config.low1UpdateMs = 400;
        config.low01UpdateMs = 500;
        config.stuttersUpdateMs = 600;
        return config;
    }

    private static void changeSettingsAfterStart(OverlayConfig config) {
        config.autoBenchmarkDurationSec = 99;
        config.pauseHandling = OverlayConfig.PauseHandling.TRACK;
        config.lowMethod = OverlayConfig.LowMethod.PERCENTILE;
        config.stutterThresholdMs = 5;
        config.stutterWindowSec = 30;
        config.fpsWindowMs = 1000;
        config.avgWindowSec = 30;
        config.low1WindowSec = 30;
        config.low01WindowSec = 30;
        config.fpsUpdateMs = 500;
        config.frametimeUpdateMs = 500;
        config.avgUpdateMs = 1000;
        config.low1UpdateMs = 1000;
        config.low01UpdateMs = 1000;
        config.stuttersUpdateMs = 1000;
    }

    private static long ns(long milliseconds) {
        return milliseconds * NS_PER_MS;
    }
}
