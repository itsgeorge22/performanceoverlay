package com.itsgeorge.performanceoverlay.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FpsTrackerMetricTest {
    private static final long NS_PER_MS = 1_000_000L;
    private static final long NS_PER_SEC = 1_000_000_000L;

    private FpsTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new FpsTracker(new OverlayConfig());
    }

    @Test
    void averageFpsUsesTotalFrameTime() {
        push(10, 10);
        push(20, 10);
        push(30, 10);

        assertEquals(100.0, tracker.windowFps(ns(30), NS_PER_SEC), 0.000_001);
    }

    @Test
    void averageFpsRequiresAtLeastTwoSamples() {
        push(10, 10);

        assertEquals(0.0, tracker.windowFps(ns(10), NS_PER_SEC));
    }

    @Test
    void frametimeUsesLatestFrameWhenOnlyOneSampleExists() {
        push(10, 12);

        FpsTracker.Smoothed smoothed = tracker.computeSmoothed(ns(10), ns(12), NS_PER_SEC);

        assertEquals(12.0, smoothed.ftMs(), 0.000_001);
    }

    @Test
    void frametimeAveragesMultipleFramesInTheWindow() {
        push(10, 10);
        push(20, 20);
        push(30, 30);

        FpsTracker.Smoothed smoothed = tracker.computeSmoothed(ns(30), ns(30), NS_PER_SEC);

        assertEquals(20.0, smoothed.ftMs(), 0.000_001);
    }

    @Test
    void frametimeIncludesTheWindowBoundaryAndExcludesOlderFrames() {
        push(899, 500);
        push(900, 20);
        push(950, 30);
        push(1000, 10);

        FpsTracker.Smoothed smoothed = tracker.computeSmoothed(ns(1000), ns(10), ns(100));

        assertEquals(20.0, smoothed.ftMs(), 0.000_001);
    }

    @Test
    void resetClearsFramesUsedByFrametime() {
        push(10, 50);
        push(20, 50);
        tracker.reset();
        push(30, 10);

        FpsTracker.Smoothed smoothed = tracker.computeSmoothed(ns(30), ns(10), NS_PER_SEC);

        assertEquals(10.0, smoothed.ftMs(), 0.000_001);
    }

    @Test
    void percentileLowsSelectConfiguredTailBoundaries() {
        pushLowDistribution();

        double low1 = tracker.lowValue(ns(1000), NS_PER_SEC, 0.01, OverlayConfig.LowMethod.PERCENTILE);
        double low01 = tracker.lowValue(ns(1000), NS_PER_SEC, 0.001, OverlayConfig.LowMethod.PERCENTILE);

        assertEquals(50.0, low1, 0.000_001);
        assertEquals(50.0, low01, 0.000_001);
    }

    @Test
    void meanWorstLowsAverageTheSlowestFrames() {
        pushLowDistribution();

        double low1 = tracker.lowValue(ns(1000), NS_PER_SEC, 0.01, OverlayConfig.LowMethod.MEAN_WORST);
        double low01 = tracker.lowValue(ns(1000), NS_PER_SEC, 0.001, OverlayConfig.LowMethod.MEAN_WORST);

        assertEquals(1_000.0 / 28.0, low1, 0.000_001);
        assertEquals(10.0, low01, 0.000_001);
    }

    @Test
    void stuttersIncludeFramesEqualToTheThreshold() {
        push(10, 10);
        push(20, 40);
        push(30, 50);
        push(40, 100);

        int stutters = tracker.countAboveThreshold(ns(40), NS_PER_SEC, ns(40));

        assertEquals(3, stutters);
        assertEquals(75, FpsTracker.calculateStutterPercent(stutters, 4));
        assertEquals(67, FpsTracker.calculateStutterPercent(2, 3));
        assertEquals(0, FpsTracker.calculateStutterPercent(0, 0));
    }

    @Test
    void maximumSpikeUsesTheLargestFrameInTheWindow() {
        push(10, 10);
        push(20, 40);
        push(30, 100);
        push(40, 50);

        assertEquals(ns(100), tracker.maxFrameInWindow(ns(40), NS_PER_SEC));
    }

    @Test
    void windowIncludesExactBoundaryAndExcludesOlderSamples() {
        push(899, 500);
        push(900, 20);
        push(950, 30);
        push(1000, 10);

        long now = ns(1000);
        long window = ns(100);

        assertEquals(3, tracker.countAboveThreshold(now, window, 1));
        assertEquals(ns(30), tracker.maxFrameInWindow(now, window));
    }

    private void pushLowDistribution() {
        for (int i = 1; i <= 989; i++) {
            push(i, 10);
        }
        for (int i = 990; i <= 999; i++) {
            push(i, 20);
        }
        push(1000, 100);
    }

    private void push(long timestampMs, long frameMs) {
        tracker.push(ns(timestampMs), ns(frameMs));
    }

    private static long ns(long milliseconds) {
        return milliseconds * NS_PER_MS;
    }
}
