package com.itsgeorge.performanceoverlay.client;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MetricVisibilityTest {
    private static final String[] LABELS = {
            "FPS:", "Avg:", "1%:", "0.1%:", "FT:", "St:", "Spike:", "GC:", "Mem:"
    };

    @Test
    void everyMetricVisibilityCombinationProducesOnlyEnabledMetrics() {
        OverlayConfig config = new OverlayConfig();
        config.textLayout = OverlayConfig.TextLayout.COLUMN;
        config.colorThresholds = false;
        FpsTracker tracker = new FpsTracker(config);
        tracker.onFrame(false, ns(100));
        tracker.onFrame(false, ns(110));
        tracker.onFrame(false, ns(120));

        for (int mask = 0; mask < 1 << LABELS.length; mask++) {
            setVisibility(config, mask);
            tracker.setConfig(config, false);
            String[] lines = tracker.getSnapshot().lines();

            for (int metric = 0; metric < LABELS.length; metric++) {
                boolean expected = (mask & (1 << metric)) != 0;
                String label = LABELS[metric];
                boolean present = Arrays.stream(lines).anyMatch(line -> line != null && line.startsWith(label));
                assertEquals(expected, present,
                        "Unexpected visibility for " + LABELS[metric] + " in mask " + mask);
            }
        }
    }

    private static void setVisibility(OverlayConfig config, int mask) {
        config.showFps = enabled(mask, 0);
        config.showAvg = enabled(mask, 1);
        config.show1Low = enabled(mask, 2);
        config.show01Low = enabled(mask, 3);
        config.showFrametime = enabled(mask, 4);
        config.showStutters = enabled(mask, 5);
        config.showMaxSpike = enabled(mask, 6);
        config.showGc = enabled(mask, 7);
        config.showMemory = enabled(mask, 8);
    }

    private static boolean enabled(int mask, int bit) {
        return (mask & (1 << bit)) != 0;
    }

    private static long ns(long milliseconds) {
        return milliseconds * 1_000_000L;
    }
}
