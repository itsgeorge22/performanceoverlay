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
}
