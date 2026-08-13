package com.itsgeorge.performanceoverlay.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ConfigPresetTest {
    @Test
    void defaultPresetUsesTenSecondAverageWindow() {
        OverlayConfig config = new OverlayConfig();

        PerformanceOverlayConfigScreen.applyPreset(config, OverlayConfig.Preset.DEFAULT);

        assertEquals(10, config.avgWindowSec);
    }

    @Test
    void everyPresetAppliesItsCompleteControlledValueSet() {
        assertPreset(OverlayConfig.Preset.DEFAULT,
                250, 250, 1000, 1000, 1500, 1000, 500, 10, 10, 10);
        assertPreset(OverlayConfig.Preset.RESPONSIVE,
                100, 100, 500, 750, 1000, 500, 250, 2, 8, 8);
        assertPreset(OverlayConfig.Preset.SMOOTH,
                500, 500, 2000, 2000, 2500, 2000, 1000, 5, 15, 15);
    }

    @Test
    void newlySelectedPresetAppliesAndRemainsSelected() {
        OverlayConfig config = new OverlayConfig();
        config.preset = OverlayConfig.Preset.RESPONSIVE;

        PerformanceOverlayConfigScreen.finalizePresetOnSave(config, OverlayConfig.Preset.DEFAULT);

        assertEquals(OverlayConfig.Preset.RESPONSIVE, config.preset);
        assertEquals(100, config.fpsUpdateMs);
        assertEquals(250, config.fpsWindowMs);
        assertEquals(2, config.avgWindowSec);
        assertEquals(8, config.low1WindowSec);
    }

    @Test
    void editingPresetControlledValueChangesPresetToCustom() {
        OverlayConfig config = new OverlayConfig();
        config.avgWindowSec = 7;

        PerformanceOverlayConfigScreen.finalizePresetOnSave(config, OverlayConfig.Preset.DEFAULT);

        assertEquals(OverlayConfig.Preset.CUSTOM, config.preset);
        assertEquals(7, config.avgWindowSec);
    }

    @Test
    void unchangedPresetRemainsSelected() {
        OverlayConfig config = new OverlayConfig();

        PerformanceOverlayConfigScreen.finalizePresetOnSave(config, OverlayConfig.Preset.DEFAULT);

        assertEquals(OverlayConfig.Preset.DEFAULT, config.preset);
    }

    private static void assertPreset(
            OverlayConfig.Preset preset,
            int fpsUpdateMs,
            int frametimeUpdateMs,
            int avgUpdateMs,
            int low1UpdateMs,
            int low01UpdateMs,
            int stuttersUpdateMs,
            int fpsWindowMs,
            int avgWindowSec,
            int low1WindowSec,
            int low01WindowSec
    ) {
        OverlayConfig config = new OverlayConfig();

        PerformanceOverlayConfigScreen.applyPreset(config, preset);
        config.preset = preset;
        PerformanceOverlayConfigScreen.finalizePresetOnSave(config, preset);

        assertEquals(preset, config.preset);
        assertEquals(fpsUpdateMs, config.fpsUpdateMs);
        assertEquals(frametimeUpdateMs, config.frametimeUpdateMs);
        assertEquals(avgUpdateMs, config.avgUpdateMs);
        assertEquals(low1UpdateMs, config.low1UpdateMs);
        assertEquals(low01UpdateMs, config.low01UpdateMs);
        assertEquals(stuttersUpdateMs, config.stuttersUpdateMs);
        assertEquals(fpsWindowMs, config.fpsWindowMs);
        assertEquals(avgWindowSec, config.avgWindowSec);
        assertEquals(low1WindowSec, config.low1WindowSec);
        assertEquals(low01WindowSec, config.low01WindowSec);
        assertEquals(OverlayConfig.PauseHandling.FREEZE, config.pauseHandling);
    }
}
