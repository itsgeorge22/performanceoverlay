package com.itsgeorge.performanceoverlay.client;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class BenchmarkShutdownTest {
    @Test
    void shutdownFooterContainsEndReasonAndCompleteSummary() throws Exception {
        StringWriter output = new StringWriter();
        FpsTracker.BenchmarkSummary summary = new FpsTracker.BenchmarkSummary(
                120.0,
                80.0,
                60.0,
                2,
                1,
                42.5
        );

        FpsTracker.writeBenchmarkFooter(
                output,
                FpsTracker.BenchmarkEndReason.GAME_SHUTDOWN,
                240,
                240,
                summary
        );

        String csv = output.toString();
        assertTrue(csv.contains("# EndReason: GAME_SHUTDOWN\n"));
        assertTrue(csv.contains("# SUMMARY\n"));
        assertTrue(csv.contains("# FramesLogged: 240\n"));
        assertTrue(csv.contains("# AvgFPS: 120.0\n"));
        assertTrue(csv.contains("# MaxSpikeMs: 42.5\n"));
    }
}
