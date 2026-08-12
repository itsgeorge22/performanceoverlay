package com.itsgeorge.performanceoverlay.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PauseHandlingTest {
    private static final long NS_PER_MS = 1_000_000L;

    @Test
    void freezeIgnoresPausedCallbacksAndThePauseGap() {
        FpsTracker tracker = trackerWith(OverlayConfig.PauseHandling.FREEZE);

        tracker.onFrame(false, ns(100));
        tracker.onFrame(false, ns(110));
        tracker.onFrame(true, ns(1110));
        tracker.onFrame(false, ns(1120));

        assertStats(tracker.stutterStats(ns(1120), ns(2000), 1), 2, ns(10));
    }

    @Test
    void resetClearsRollingFramesWhenPauseBegins() {
        FpsTracker tracker = trackerWith(OverlayConfig.PauseHandling.RESET);

        tracker.onFrame(false, ns(100));
        tracker.onFrame(false, ns(150));
        tracker.onFrame(false, ns(200));
        tracker.onFrame(true, ns(1000));
        tracker.onFrame(false, ns(1010));

        assertStats(tracker.stutterStats(ns(1010), ns(2000), 1), 1, ns(10));
    }

    @Test
    void trackMeasuresPausedCallbacksNormally() {
        FpsTracker tracker = trackerWith(OverlayConfig.PauseHandling.TRACK);

        tracker.onFrame(false, ns(100));
        tracker.onFrame(true, ns(150));
        tracker.onFrame(true, ns(200));
        tracker.onFrame(false, ns(210));

        assertStats(tracker.stutterStats(ns(210), ns(1000), 1), 3, ns(50));
    }

    private static void assertStats(FpsTracker.StutterStats stats, int frames, long maxFrameNs) {
        assertEquals(frames, stats.frames());
        assertEquals(frames, stats.stutters());
        assertEquals(maxFrameNs, stats.maxFrameNs());
    }

    private static FpsTracker trackerWith(OverlayConfig.PauseHandling pauseHandling) {
        OverlayConfig config = new OverlayConfig();
        config.pauseHandling = pauseHandling;
        return new FpsTracker(config);
    }

    private static long ns(long milliseconds) {
        return milliseconds * NS_PER_MS;
    }
}
