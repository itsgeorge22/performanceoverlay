package com.itsgeorge.performanceoverlay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PerformanceOverlayClientTest {
    @Test
    void benchmarkKeyIsIgnoredOnAutoStopTick() {
        assertFalse(PerformanceOverlayClient.shouldHandleBenchmarkKey(true, false, true));
    }

    @Test
    void benchmarkKeyWorksOnNormalTicks() {
        assertTrue(PerformanceOverlayClient.shouldHandleBenchmarkKey(false, false, true));
    }

    @Test
    void benchmarkKeyDoesNothingInMenus() {
        assertFalse(PerformanceOverlayClient.shouldHandleBenchmarkKey(false, false, false));
    }

    @Test
    void activeBenchmarkCanStillBeStoppedIfWorldStateDisappears() {
        assertTrue(PerformanceOverlayClient.shouldHandleBenchmarkKey(false, true, false));
    }

    @Test
    void completedDurationDisplaysOneHundredPercent() {
        assertEquals(100, PerformanceOverlayClient.benchmarkProgressPercent(7, 7));
    }
}
