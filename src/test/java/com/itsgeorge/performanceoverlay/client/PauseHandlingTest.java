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

        assertEquals(2, tracker.countAboveThreshold(ns(1120), ns(2000), 1));
        assertEquals(ns(10), tracker.maxFrameInWindow(ns(1120), ns(2000)));
    }

    @Test
    void resetClearsRollingFramesWhenPauseBegins() {
        FpsTracker tracker = trackerWith(OverlayConfig.PauseHandling.RESET);

        tracker.onFrame(false, ns(100));
        tracker.onFrame(false, ns(150));
        tracker.onFrame(false, ns(200));
        tracker.onFrame(true, ns(1000));
        tracker.onFrame(false, ns(1010));

        assertEquals(1, tracker.countAboveThreshold(ns(1010), ns(2000), 1));
        assertEquals(ns(10), tracker.maxFrameInWindow(ns(1010), ns(2000)));
    }

    @Test
    void trackMeasuresPausedCallbacksNormally() {
        FpsTracker tracker = trackerWith(OverlayConfig.PauseHandling.TRACK);

        tracker.onFrame(false, ns(100));
        tracker.onFrame(true, ns(150));
        tracker.onFrame(true, ns(200));
        tracker.onFrame(false, ns(210));

        assertEquals(3, tracker.countAboveThreshold(ns(210), ns(1000), 1));
        assertEquals(ns(50), tracker.maxFrameInWindow(ns(210), ns(1000)));
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
