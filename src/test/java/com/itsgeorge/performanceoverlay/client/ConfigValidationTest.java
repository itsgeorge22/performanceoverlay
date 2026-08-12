package com.itsgeorge.performanceoverlay.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigValidationTest {
    @Test
    void malformedJsonFallsBackToDefaults() {
        OverlayConfig config = ConfigIO.parse("{not valid json");

        assertDefaultCoreValues(config);
    }

    @Test
    void nullJsonFallsBackToDefaults() {
        OverlayConfig config = ConfigIO.parse("null");

        assertDefaultCoreValues(config);
    }

    @Test
    void missingValuesKeepDefaults() {
        OverlayConfig config = ConfigIO.parse("{}");

        assertDefaultCoreValues(config);
        assertTrue(config.showFps);
    }

    @Test
    void nullEnumsAreReplacedWithDefaults() {
        OverlayConfig config = new OverlayConfig();
        config.position = null;
        config.textLayout = null;
        config.preset = null;
        config.lowMethod = null;
        config.pauseHandling = null;
        config.colorTarget = null;

        config.validate();

        assertEquals(OverlayConfig.OverlayPosition.TOP_LEFT, config.position);
        assertEquals(OverlayConfig.TextLayout.ONE_LINE, config.textLayout);
        assertEquals(OverlayConfig.Preset.DEFAULT, config.preset);
        assertEquals(OverlayConfig.LowMethod.PERCENTILE, config.lowMethod);
        assertEquals(OverlayConfig.PauseHandling.FREEZE, config.pauseHandling);
        assertEquals(OverlayConfig.ColorTarget.LOW_01, config.colorTarget);
    }

    @Test
    void numericValuesAreClampedToConfigScreenLimits() {
        OverlayConfig config = new OverlayConfig();
        config.offsetX = -1;
        config.offsetY = 6000;
        config.scale = Float.NaN;
        config.lineSpacingPx = 31;
        config.autoBenchmarkDurationSec = -5;
        config.fpsUpdateMs = 1;
        config.frametimeUpdateMs = 6000;
        config.avgUpdateMs = 1;
        config.low1UpdateMs = 20000;
        config.low01UpdateMs = 1;
        config.stuttersUpdateMs = 20000;
        config.fpsWindowMs = 1;
        config.avgWindowSec = 100;
        config.low1WindowSec = 0;
        config.low01WindowSec = 100;
        config.stutterThresholdMs = 1;
        config.stutterWindowSec = 100;
        config.warningFps = 0;
        config.dangerFps = 1000;

        config.validate();

        assertEquals(0, config.offsetX);
        assertEquals(5000, config.offsetY);
        assertEquals(1.0f, config.scale);
        assertEquals(30, config.lineSpacingPx);
        assertEquals(0, config.autoBenchmarkDurationSec);
        assertEquals(50, config.fpsUpdateMs);
        assertEquals(5000, config.frametimeUpdateMs);
        assertEquals(100, config.avgUpdateMs);
        assertEquals(10000, config.low1UpdateMs);
        assertEquals(100, config.low01UpdateMs);
        assertEquals(10000, config.stuttersUpdateMs);
        assertEquals(50, config.fpsWindowMs);
        assertEquals(30, config.avgWindowSec);
        assertEquals(1, config.low1WindowSec);
        assertEquals(60, config.low01WindowSec);
        assertEquals(5, config.stutterThresholdMs);
        assertEquals(60, config.stutterWindowSec);
        assertEquals(1, config.warningFps);
        assertEquals(500, config.dangerFps);
    }

    @Test
    void validValuesRemainUnchanged() {
        OverlayConfig config = new OverlayConfig();
        config.offsetX = 42;
        config.scale = 1.25f;
        config.autoBenchmarkDurationSec = 7;
        config.lowMethod = OverlayConfig.LowMethod.MEAN_WORST;

        config.validate();

        assertEquals(42, config.offsetX);
        assertEquals(1.25f, config.scale);
        assertEquals(7, config.autoBenchmarkDurationSec);
        assertEquals(OverlayConfig.LowMethod.MEAN_WORST, config.lowMethod);
    }

    @Test
    void enumLabelsUseReadableSentenceCase() {
        assertEquals("Top left", OverlayConfig.OverlayPosition.TOP_LEFT.toString());
        assertEquals("Three lines", OverlayConfig.TextLayout.THREE_LINES.toString());
        assertEquals("Responsive", OverlayConfig.Preset.RESPONSIVE.toString());
        assertEquals("Freeze", OverlayConfig.PauseHandling.FREEZE.toString());
        assertEquals("Mean worst", OverlayConfig.LowMethod.MEAN_WORST.toString());
        assertEquals("0.1% low", OverlayConfig.ColorTarget.LOW_01.toString());
    }

    private static void assertDefaultCoreValues(OverlayConfig config) {
        assertNotNull(config);
        assertEquals(1.0f, config.scale);
        assertEquals(250, config.fpsUpdateMs);
        assertEquals(OverlayConfig.TextLayout.ONE_LINE, config.textLayout);
        assertEquals(OverlayConfig.LowMethod.PERCENTILE, config.lowMethod);
    }
}
