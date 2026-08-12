package com.itsgeorge.performanceoverlay.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BenchmarkSummaryTest {
    private static final long NS_PER_MS = 1_000_000L;

    private FpsTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new FpsTracker(new OverlayConfig());
        pushDistribution();
    }

    @Test
    void completeSummaryUsesAllRetainedFrames() {
        FpsTracker.BenchmarkSummary summary = tracker.buildBenchmarkSummaryFullRun(
                OverlayConfig.LowMethod.PERCENTILE,
                20
        );

        assertEquals(98.13542688910697, summary.avg(), 0.000_001);
        assertEquals(50.0, summary.low1(), 0.000_001);
        assertEquals(50.0, summary.low01(), 0.000_001);
        assertEquals(11, summary.stutters());
        assertEquals(1, summary.stutterPercent());
        assertEquals(100.0, summary.maxSpikeMs(), 0.000_001);
    }

    @Test
    void completeSummaryUsesSelectedMeanWorstLowMethod() {
        FpsTracker.BenchmarkSummary summary = tracker.buildBenchmarkSummaryFullRun(
                OverlayConfig.LowMethod.MEAN_WORST,
                20
        );

        assertEquals(1_000.0 / 28.0, summary.low1(), 0.000_001);
        assertEquals(10.0, summary.low01(), 0.000_001);
    }

    @Test
    void footerWritesEverySummaryFieldAndFrameCount() throws Exception {
        FpsTracker.BenchmarkSummary summary = tracker.buildBenchmarkSummaryFullRun(
                OverlayConfig.LowMethod.PERCENTILE,
                20
        );
        StringWriter output = new StringWriter();

        FpsTracker.writeBenchmarkFooter(
                output,
                FpsTracker.BenchmarkEndReason.MANUAL,
                1000,
                1000,
                summary
        );

        assertEquals(
                "# EndReason: MANUAL\n" +
                        "# SUMMARY\n" +
                        "# FramesLogged: 1000\n" +
                        "# FramesSummary: 1000\n" +
                        "# AvgFPS: 98.1\n" +
                        "# Low1FPS: 50.0\n" +
                        "# Low01FPS: 50.0\n" +
                        "# Stutters: 11\n" +
                        "# StutterPercent: 1\n" +
                        "# MaxSpikeMs: 100.0\n",
                output.toString()
        );
    }

    private void pushDistribution() {
        for (int i = 0; i < 989; i++) {
            tracker.benchPushFrame(ns(10));
        }
        for (int i = 0; i < 10; i++) {
            tracker.benchPushFrame(ns(20));
        }
        tracker.benchPushFrame(ns(100));
    }

    private static long ns(long milliseconds) {
        return milliseconds * NS_PER_MS;
    }
}
