package com.itsgeorge.performanceoverlay.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BenchmarkFailureTest {
    @Test
    void writeFailureQueuesOneImmediateErrorStatus() {
        FpsTracker tracker = new FpsTracker(new OverlayConfig());

        tracker.handleBenchmarkWriteFailure(new IOException("disk full"));

        FpsTracker.BenchmarkStatus status = tracker.consumePendingBenchmarkStatus();
        assertTrue(status.error());
        assertTrue(status.message().contains("disk full"));
        assertFalse(tracker.isBenchmarkActive());
        assertNull(tracker.consumePendingBenchmarkStatus());
    }

    @Test
    void commonIoErrorsHaveFriendlyDescriptions() {
        assertTrue(FpsTracker.describeBenchmarkIoError(
                new FileAlreadyExistsException("benchmarks")
        ).contains("blocked by a file"));
        assertTrue(FpsTracker.describeBenchmarkIoError(
                new AccessDeniedException("benchmarks")
        ).contains("permission"));
        assertTrue(FpsTracker.describeBenchmarkIoError(
                new IOException("disk full")
        ).contains("disk full"));
    }
}
