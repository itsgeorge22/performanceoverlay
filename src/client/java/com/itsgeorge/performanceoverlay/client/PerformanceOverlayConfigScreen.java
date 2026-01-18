package com.itsgeorge.performanceoverlay.client;

import com.itsgeorge.performanceoverlay.PerformanceOverlayClient;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PerformanceOverlayConfigScreen {
    private PerformanceOverlayConfigScreen() {
    }

    private static Component label(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }

    private static Component section(String text) {
        return Component.literal(text).withStyle(ChatFormatting.WHITE);
    }

    private static Component hint(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }

    public static Screen build(Screen parent) {
        OverlayConfig current = PerformanceOverlayClient.getConfig();
        OverlayConfig working = copy(current);
        OverlayConfig defaults = new OverlayConfig();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Performance Overlay"))
                .setSavingRunnable(() -> {
                    applyPresetOnSave(working);
                    PerformanceOverlayClient.setConfig(working);
                    ConfigIO.save(working);
                });

        ConfigEntryBuilder eb = builder.entryBuilder();

        // ---------------- Overlay ----------------
        ConfigCategory overlay = builder.getOrCreateCategory(Component.literal("Overlay"));

        overlay.addEntry(eb.startTextDescription(section("— Overlay —")).build());

        overlay.addEntry(eb.startBooleanToggle(label("Enabled"), working.enabled)
                .setDefaultValue(defaults.enabled)
                .setTooltip(
                        Component.literal("Show or hide the overlay."),
                        Component.literal("Off = no overlay and no tracking.")
                )
                .setSaveConsumer(v -> working.enabled = v)
                .build());

        overlay.addEntry(eb.startTextDescription(section("— Controls —")).build());

        overlay.addEntry(eb.startTextDescription(
                hint("Toggle: " + PerformanceOverlayClient.getToggleKeyDisplayName())
        ).build());

        overlay.addEntry(eb.startTextDescription(
                hint("Cycle layout: " + PerformanceOverlayClient.getCycleLayoutKeyDisplayName())
        ).build());

        overlay.addEntry(eb.startTextDescription(
                hint("Reset stats: " + PerformanceOverlayClient.getResetKeyDisplayName())
        ).build());

        overlay.addEntry(eb.startTextDescription(
                hint("Benchmark: " + PerformanceOverlayClient.getBenchmarkKeyDisplayName())
        ).build());

        overlay.addEntry(eb.startTextDescription(section("— Metrics to show —")).build());

        overlay.addEntry(eb.startBooleanToggle(label("Show FPS"), working.showFps)
                .setDefaultValue(defaults.showFps)
                .setTooltip(Component.literal("Show current FPS (smoothed)."))
                .setSaveConsumer(v -> working.showFps = v)
                .build());

        overlay.addEntry(eb.startBooleanToggle(label("Show Avg FPS"), working.showAvg)
                .setDefaultValue(defaults.showAvg)
                .setTooltip(Component.literal("Show average FPS over the Avg window."))
                .setSaveConsumer(v -> working.showAvg = v)
                .build());

        overlay.addEntry(eb.startBooleanToggle(label("Show 1% Low"), working.show1Low)
                .setDefaultValue(defaults.show1Low)
                .setTooltip(Component.literal("Show 1% Low FPS (stutter indicator)."))
                .setSaveConsumer(v -> working.show1Low = v)
                .build());

        overlay.addEntry(eb.startBooleanToggle(label("Show 0.1% Low"), working.show01Low)
                .setDefaultValue(defaults.show01Low)
                .setTooltip(Component.literal("Show 0.1% Low FPS (micro-freeze indicator)."))
                .setSaveConsumer(v -> working.show01Low = v)
                .build());

        overlay.addEntry(eb.startBooleanToggle(label("Show Frametime (ms)"), working.showFrametime)
                .setDefaultValue(defaults.showFrametime)
                .setTooltip(Component.literal("Show frametime in milliseconds. Lower = smoother."))
                .setSaveConsumer(v -> working.showFrametime = v)
                .build());

        overlay.addEntry(eb.startBooleanToggle(label("Show Stutters (count)"), working.showStutters)
                .setDefaultValue(defaults.showStutters)
                .setTooltip(Component.literal("Count stutters in the last window (frame >= threshold ms)."))
                .setSaveConsumer(v -> working.showStutters = v)
                .build());

        overlay.addEntry(eb.startBooleanToggle(label("Show Max Spike (ms)"), working.showMaxSpike)
                .setDefaultValue(defaults.showMaxSpike)
                .setTooltip(Component.literal("Show max frametime spike (ms) within the stutter window."))
                .setSaveConsumer(v -> working.showMaxSpike = v)
                .build());
        overlay.addEntry(eb.startBooleanToggle(label("Show GC pauses"), working.showGc)
            .setDefaultValue(defaults.showGc)
            .setTooltip(Component.literal("Show garbage collection pauses (ms)."))
            .setSaveConsumer(v -> working.showGc = v)
            .build());

        overlay.addEntry(eb.startBooleanToggle(label("Show Memory usage"), working.showMemory)
            .setDefaultValue(defaults.showMemory)
            .setTooltip(Component.literal("Show used / max JVM memory (MB)."))
            .setSaveConsumer(v -> working.showMemory = v)
            .build());

        // ---------------- Layout ----------------
        ConfigCategory layout = builder.getOrCreateCategory(Component.literal("Layout"));

        layout.addEntry(eb.startTextDescription(section("— Layout —")).build());

        layout.addEntry(eb.startFloatField(label("Scale"), working.scale)
                .setDefaultValue(defaults.scale)
                .setMin(0.50f)
                .setMax(2.00f)
                .setTooltip(Component.literal("Text scale for the overlay."))
                .setSaveConsumer(v -> working.scale = clampFloat(v, 0.50f, 2.00f))
                .build());

        layout.addEntry(eb.startEnumSelector(label("Position"), OverlayConfig.OverlayPosition.class, working.position)
                .setDefaultValue(defaults.position)
                .setSaveConsumer(v -> working.position = v)
                .build());

        layout.addEntry(eb.startIntField(label("Offset X"), working.offsetX)
                .setDefaultValue(defaults.offsetX)
                .setMin(0)
                .setMax(5000)
                .setSaveConsumer(v -> working.offsetX = v)
                .build());

        layout.addEntry(eb.startIntField(label("Offset Y"), working.offsetY)
                .setDefaultValue(defaults.offsetY)
                .setMin(0)
                .setMax(5000)
                .setSaveConsumer(v -> working.offsetY = v)
                .build());

        layout.addEntry(eb.startEnumSelector(label("Text layout"), OverlayConfig.TextLayout.class, working.textLayout)
                .setDefaultValue(defaults.textLayout)
                .setTooltip(
                        Component.literal("ONE_LINE = compact."),
                        Component.literal("THREE_LINES = balanced."),
                        Component.literal("COLUMN = easiest to scan.")
                )
                .setSaveConsumer(v -> working.textLayout = v)
                .build());

        layout.addEntry(eb.startIntField(label("Line spacing (px)"), working.lineSpacingPx)
                .setDefaultValue(defaults.lineSpacingPx)
                .setMin(0)
                .setMax(30)
                .setTooltip(Component.literal("Spacing between lines in THREE_LINES and COLUMN modes."))
                .setSaveConsumer(v -> working.lineSpacingPx = clamp(v, 0, 30))
                .build());

        // ---------------- Advanced ----------------
        ConfigCategory advanced = builder.getOrCreateCategory(Component.literal("Advanced"));

        advanced.addEntry(eb.startTextDescription(section("— Behaviour —")).build());

        advanced.addEntry(eb.startEnumSelector(label("Preset"), OverlayConfig.Preset.class, working.preset)
                .setDefaultValue(defaults.preset)
                .setTooltip(
                        Component.literal("DEFAULT = balanced."),
                        Component.literal("RESPONSIVE = reacts faster, more jitter."),
                        Component.literal("SMOOTH = steadier numbers."),
                        Component.literal("CUSTOM = your values.")
                )
                .setSaveConsumer(v -> {
                    working.preset = v;
                    if (v != OverlayConfig.Preset.CUSTOM) {
                        applyPreset(working, v);
                    }
                })
                .build());

        advanced.addEntry(eb.startEnumSelector(label("Pause handling"), OverlayConfig.PauseHandling.class, working.pauseHandling)
                .setDefaultValue(defaults.pauseHandling)
                .setTooltip(
                        Component.literal("RESET = clear stats."),
                        Component.literal("FREEZE = stop sampling, keep values."),
                        Component.literal("TRACK = keep sampling (may distort).")
                )
                .setSaveConsumer(v -> {
                    working.pauseHandling = v;
                    working.preset = OverlayConfig.Preset.CUSTOM;
                })
                .build());

        advanced.addEntry(eb.startEnumSelector(label("Low calculation method"), OverlayConfig.LowMethod.class, working.lowMethod)
                .setDefaultValue(defaults.lowMethod)
                .setTooltip(
                        Component.literal("MEAN_WORST = average of worst frames (stricter)."),
                        Component.literal("PERCENTILE = percentile frametime (more standard).")
                )
                .setSaveConsumer(v -> {
                    working.lowMethod = v;
                    working.preset = OverlayConfig.Preset.CUSTOM;
                })
                .build());

        advanced.addEntry(eb.startTextDescription(section("— Benchmark —")).build());

        advanced.addEntry(eb.startIntField(label("Auto benchmark duration (sec)"), working.autoBenchmarkDurationSec)
                .setDefaultValue(defaults.autoBenchmarkDurationSec)
                .setMin(0)
                .setMax(3600)
                .setTooltip(
                        Component.literal("0 = disable auto stop."),
                        Component.literal("If > 0, benchmark auto-stops after N seconds.")
                )
                .setSaveConsumer(v -> working.autoBenchmarkDurationSec = clamp(v, 0, 3600))
                .build());

        advanced.addEntry(eb.startTextDescription(section("— Update rates (ms) —")).build());

        advanced.addEntry(eb.startIntField(label("FPS update (ms)"), working.fpsUpdateMs)
                .setDefaultValue(defaults.fpsUpdateMs)
                .setMin(50)
                .setMax(5000)
                .setTooltip(Component.literal("How often FPS updates (smoothed)."))
                .setSaveConsumer(v -> {
                    working.fpsUpdateMs = clamp(v, 50, 5000);
                    working.preset = OverlayConfig.Preset.CUSTOM;
                })
                .build());

        advanced.addEntry(eb.startIntField(label("Frametime update (ms)"), working.frametimeUpdateMs)
                .setDefaultValue(defaults.frametimeUpdateMs)
                .setMin(50)
                .setMax(5000)
                .setTooltip(Component.literal("How often frametime updates."))
                .setSaveConsumer(v -> {
                    working.frametimeUpdateMs = clamp(v, 50, 5000);
                    working.preset = OverlayConfig.Preset.CUSTOM;
                })
                .build());

        advanced.addEntry(eb.startIntField(label("Avg update (ms)"), working.avgUpdateMs)
                .setDefaultValue(defaults.avgUpdateMs)
                .setMin(100)
                .setMax(10000)
                .setTooltip(Component.literal("How often Avg recalculates."))
                .setSaveConsumer(v -> {
                    working.avgUpdateMs = clamp(v, 100, 10000);
                    working.preset = OverlayConfig.Preset.CUSTOM;
                })
                .build());

        advanced.addEntry(eb.startIntField(label("1% Low update (ms)"), working.low1UpdateMs)
                .setDefaultValue(defaults.low1UpdateMs)
                .setMin(100)
                .setMax(10000)
                .setTooltip(Component.literal("How often 1% Low recalculates."))
                .setSaveConsumer(v -> {
                    working.low1UpdateMs = clamp(v, 100, 10000);
                    working.preset = OverlayConfig.Preset.CUSTOM;
                })
                .build());

        advanced.addEntry(eb.startIntField(label("0.1% Low update (ms)"), working.low01UpdateMs)
                .setDefaultValue(defaults.low01UpdateMs)
                .setMin(100)
                .setMax(10000)
                .setTooltip(Component.literal("How often 0.1% Low recalculates."))
                .setSaveConsumer(v -> {
                    working.low01UpdateMs = clamp(v, 100, 10000);
                    working.preset = OverlayConfig.Preset.CUSTOM;
                })
                .build());

        advanced.addEntry(eb.startIntField(label("Stutters update (ms)"), working.stuttersUpdateMs)
                .setDefaultValue(defaults.stuttersUpdateMs)
                .setMin(100)
                .setMax(10000)
                .setTooltip(Component.literal("How often stutter count recalculates."))
                .setSaveConsumer(v -> {
                    working.stuttersUpdateMs = clamp(v, 100, 10000);
                    working.preset = OverlayConfig.Preset.CUSTOM;
                })
                .build());

        advanced.addEntry(eb.startTextDescription(section("— Windows —")).build());

        advanced.addEntry(eb.startIntField(label("FPS smoothing (ms)"), working.fpsWindowMs)
                .setDefaultValue(defaults.fpsWindowMs)
                .setMin(50)
                .setMax(2000)
                .setTooltip(Component.literal("Time window used to smooth displayed FPS."))
                .setSaveConsumer(v -> {
                    working.fpsWindowMs = clamp(v, 50, 2000);
                    working.preset = OverlayConfig.Preset.CUSTOM;
                })
                .build());

        advanced.addEntry(eb.startIntField(label("Avg window (sec)"), working.avgWindowSec)
                .setDefaultValue(defaults.avgWindowSec)
                .setMin(1)
                .setMax(30)
                .setTooltip(Component.literal("History window used to calculate Avg FPS."))
                .setSaveConsumer(v -> {
                    working.avgWindowSec = clamp(v, 1, 30);
                    working.preset = OverlayConfig.Preset.CUSTOM;
                })
                .build());

        advanced.addEntry(eb.startIntField(label("1% Low window (sec)"), working.low1WindowSec)
                .setDefaultValue(defaults.low1WindowSec)
                .setMin(1)
                .setMax(60)
                .setTooltip(Component.literal("History window used to calculate 1% Low."))
                .setSaveConsumer(v -> {
                    working.low1WindowSec = clamp(v, 1, 60);
                    working.preset = OverlayConfig.Preset.CUSTOM;
                })
                .build());

        advanced.addEntry(eb.startIntField(label("0.1% Low window (sec)"), working.low01WindowSec)
                .setDefaultValue(defaults.low01WindowSec)
                .setMin(1)
                .setMax(60)
                .setTooltip(Component.literal("History window used to calculate 0.1% Low."))
                .setSaveConsumer(v -> {
                    working.low01WindowSec = clamp(v, 1, 60);
                    working.preset = OverlayConfig.Preset.CUSTOM;
                })
                .build());

        advanced.addEntry(eb.startTextDescription(section("— Stutters —")).build());

        advanced.addEntry(eb.startIntField(label("Stutter threshold (ms)"), working.stutterThresholdMs)
                .setDefaultValue(defaults.stutterThresholdMs)
                .setMin(5)
                .setMax(500)
                .setTooltip(Component.literal("Frames >= this frametime count as stutters."))
                .setSaveConsumer(v -> {
                    working.stutterThresholdMs = clamp(v, 5, 500);
                    working.preset = OverlayConfig.Preset.CUSTOM;
                })
                .build());

        advanced.addEntry(eb.startIntField(label("Stutter window (sec)"), working.stutterWindowSec)
                .setDefaultValue(defaults.stutterWindowSec)
                .setMin(1)
                .setMax(60)
                .setTooltip(Component.literal("History window used to count stutters."))
                .setSaveConsumer(v -> {
                    working.stutterWindowSec = clamp(v, 1, 60);
                    working.preset = OverlayConfig.Preset.CUSTOM;
                })
                .build());

        advanced.addEntry(eb.startTextDescription(section("— Colouring —")).build());

        advanced.addEntry(eb.startBooleanToggle(label("Color thresholds"), working.colorThresholds)
                .setDefaultValue(defaults.colorThresholds)
                .setTooltip(Component.literal("Color the overlay when performance is low."))
                .setSaveConsumer(v -> working.colorThresholds = v)
                .build());

        advanced.addEntry(eb.startEnumSelector(label("Color target"), OverlayConfig.ColorTarget.class, working.colorTarget)
                .setDefaultValue(defaults.colorTarget)
                .setTooltip(Component.literal("Use FPS, 1% Low, or 0.1% Low for colouring."))
                .setSaveConsumer(v -> working.colorTarget = v)
                .build());

        advanced.addEntry(eb.startIntField(label("Warning FPS"), working.warningFps)
                .setDefaultValue(defaults.warningFps)
                .setMin(1)
                .setMax(500)
                .setTooltip(Component.literal("Below this = yellow."))
                .setSaveConsumer(v -> working.warningFps = clamp(v, 1, 500))
                .build());

        advanced.addEntry(eb.startIntField(label("Danger FPS"), working.dangerFps)
                .setDefaultValue(defaults.dangerFps)
                .setMin(1)
                .setMax(500)
                .setTooltip(Component.literal("Below this = red."))
                .setSaveConsumer(v -> working.dangerFps = clamp(v, 1, 500))
                .build());

        return builder.build();
    }

    private static void applyPresetOnSave(OverlayConfig cfg) {
        if (cfg.preset == OverlayConfig.Preset.DEFAULT) {
            applyPreset(cfg, OverlayConfig.Preset.DEFAULT);
        } else if (cfg.preset == OverlayConfig.Preset.RESPONSIVE) {
            applyPreset(cfg, OverlayConfig.Preset.RESPONSIVE);
        } else if (cfg.preset == OverlayConfig.Preset.SMOOTH) {
            applyPreset(cfg, OverlayConfig.Preset.SMOOTH);
        }
    }

    private static void applyPreset(OverlayConfig cfg, OverlayConfig.Preset preset) {
        if (preset == OverlayConfig.Preset.DEFAULT) {
            cfg.fpsUpdateMs = 250;
            cfg.frametimeUpdateMs = 250;
            cfg.avgUpdateMs = 1000;
            cfg.low1UpdateMs = 1000;
            cfg.low01UpdateMs = 1500;
            cfg.stuttersUpdateMs = 1000;

            cfg.fpsWindowMs = 500;
            cfg.avgWindowSec = 3;
            cfg.low1WindowSec = 10;
            cfg.low01WindowSec = 10;

            cfg.pauseHandling = OverlayConfig.PauseHandling.FREEZE;
            return;
        }

        if (preset == OverlayConfig.Preset.RESPONSIVE) {
            cfg.fpsUpdateMs = 100;
            cfg.frametimeUpdateMs = 100;
            cfg.avgUpdateMs = 500;
            cfg.low1UpdateMs = 750;
            cfg.low01UpdateMs = 1000;
            cfg.stuttersUpdateMs = 500;

            cfg.fpsWindowMs = 250;
            cfg.avgWindowSec = 2;
            cfg.low1WindowSec = 8;
            cfg.low01WindowSec = 8;

            cfg.pauseHandling = OverlayConfig.PauseHandling.FREEZE;
            return;
        }

        if (preset == OverlayConfig.Preset.SMOOTH) {
            cfg.fpsUpdateMs = 500;
            cfg.frametimeUpdateMs = 500;
            cfg.avgUpdateMs = 2000;
            cfg.low1UpdateMs = 2000;
            cfg.low01UpdateMs = 2500;
            cfg.stuttersUpdateMs = 2000;

            cfg.fpsWindowMs = 1000;
            cfg.avgWindowSec = 5;
            cfg.low1WindowSec = 15;
            cfg.low01WindowSec = 15;

            cfg.pauseHandling = OverlayConfig.PauseHandling.FREEZE;
        }
    }

    private static int clamp(int v, int min, int max) {
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }

    private static float clampFloat(float v, float min, float max) {
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }

    private static OverlayConfig copy(OverlayConfig src) {
        OverlayConfig c = new OverlayConfig();

        c.enabled = src.enabled;

        c.showFps = src.showFps;
        c.showAvg = src.showAvg;
        c.show1Low = src.show1Low;
        c.show01Low = src.show01Low;

        c.showFrametime = src.showFrametime;
        c.showStutters = src.showStutters;

        c.showGc = src.showGc;
        c.showMemory = src.showMemory;

        c.position = src.position;
        c.offsetX = src.offsetX;
        c.offsetY = src.offsetY;

        c.scale = src.scale;

        c.textLayout = (src.textLayout != null) ? src.textLayout : c.textLayout;
        c.lineSpacingPx = src.lineSpacingPx;

        c.preset = src.preset;

        c.fpsUpdateMs = src.fpsUpdateMs;
        c.frametimeUpdateMs = src.frametimeUpdateMs;
        c.avgUpdateMs = src.avgUpdateMs;
        c.low1UpdateMs = src.low1UpdateMs;
        c.low01UpdateMs = src.low01UpdateMs;
        c.stuttersUpdateMs = src.stuttersUpdateMs;

        c.fpsWindowMs = src.fpsWindowMs;
        c.avgWindowSec = src.avgWindowSec;
        c.low1WindowSec = src.low1WindowSec;
        c.low01WindowSec = src.low01WindowSec;

        c.stutterThresholdMs = src.stutterThresholdMs;
        c.stutterWindowSec = src.stutterWindowSec;

        c.lowMethod = src.lowMethod;

        c.pauseHandling = src.pauseHandling;

        c.colorThresholds = src.colorThresholds;
        c.warningFps = src.warningFps;
        c.dangerFps = src.dangerFps;
        c.colorTarget = src.colorTarget;

        return c;
    }
}