package com.itsgeorge.performanceoverlay.client;

public class OverlayConfig {
    // Overlay
    public boolean enabled = true;

    // Metrics to show
    public boolean showFps = true;
    public boolean showAvg = true;
    public boolean show1Low = true;
    public boolean show01Low = true;

    public boolean showFrametime = true;
    public boolean showStutters = true;
    public boolean showMaxSpike = true;

    public boolean showGc = true;
    public boolean showMemory = true;

    // Layout
    public OverlayPosition position = OverlayPosition.TOP_LEFT;
    public int offsetX = 8;
    public int offsetY = 8;

    public float scale = 1.0f;

    public TextLayout textLayout = TextLayout.ONE_LINE;
    public int lineSpacingPx = 4;

    // Presets / Advanced
    public Preset preset = Preset.DEFAULT;

    // Benchmark
    public int autoBenchmarkDurationSec = 30;

    // Update rates (ms)
    public int fpsUpdateMs = 250;
    public int frametimeUpdateMs = 250;
    public int avgUpdateMs = 1000;
    public int low1UpdateMs = 1000;
    public int low01UpdateMs = 1500;
    public int stuttersUpdateMs = 1000;

    // Windows
    public int fpsWindowMs = 500;
    public int avgWindowSec = 10;
    public int low1WindowSec = 10;
    public int low01WindowSec = 10;

    // Stutters (threshold in ms)
    public int stutterThresholdMs = 40;
    public int stutterWindowSec = 10;

    // Low calculation
    public LowMethod lowMethod = LowMethod.PERCENTILE;

    // Pause
    public PauseHandling pauseHandling = PauseHandling.FREEZE;

    // Colour thresholds
    public boolean colorThresholds = true;

    public ColorTarget colorTarget = ColorTarget.LOW_01;

    public int warningFps = 50;
    public int dangerFps = 25;

    public OverlayConfig validate() {
        OverlayConfig defaults = new OverlayConfig();

        if (position == null) position = defaults.position;
        if (textLayout == null) textLayout = defaults.textLayout;
        if (preset == null) preset = defaults.preset;
        if (lowMethod == null) lowMethod = defaults.lowMethod;
        if (pauseHandling == null) pauseHandling = defaults.pauseHandling;
        if (colorTarget == null) colorTarget = defaults.colorTarget;

        offsetX = clamp(offsetX, 0, 5000);
        offsetY = clamp(offsetY, 0, 5000);
        scale = Float.isFinite(scale) ? clamp(scale, 0.5f, 2.0f) : defaults.scale;
        lineSpacingPx = clamp(lineSpacingPx, 0, 30);

        autoBenchmarkDurationSec = clamp(autoBenchmarkDurationSec, 0, 3600);

        fpsUpdateMs = clamp(fpsUpdateMs, 50, 5000);
        frametimeUpdateMs = clamp(frametimeUpdateMs, 50, 5000);
        avgUpdateMs = clamp(avgUpdateMs, 100, 10000);
        low1UpdateMs = clamp(low1UpdateMs, 100, 10000);
        low01UpdateMs = clamp(low01UpdateMs, 100, 10000);
        stuttersUpdateMs = clamp(stuttersUpdateMs, 100, 10000);

        fpsWindowMs = clamp(fpsWindowMs, 50, 2000);
        avgWindowSec = clamp(avgWindowSec, 1, 30);
        low1WindowSec = clamp(low1WindowSec, 1, 60);
        low01WindowSec = clamp(low01WindowSec, 1, 60);
        stutterThresholdMs = clamp(stutterThresholdMs, 5, 500);
        stutterWindowSec = clamp(stutterWindowSec, 1, 60);

        warningFps = clamp(warningFps, 1, 500);
        dangerFps = clamp(dangerFps, 1, 500);

        return this;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum OverlayPosition {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT
    }

    public enum TextLayout {
        ONE_LINE("One line"),
        THREE_LINES("Three lines"),
        COLUMN("Column");

        private final String label;

        TextLayout(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum Preset {
        DEFAULT,
        RESPONSIVE,
        SMOOTH,
        CUSTOM
    }

    public enum PauseHandling {
        RESET,
        FREEZE,
        TRACK
    }

    public enum LowMethod {
        MEAN_WORST,
        PERCENTILE
    }

    public enum ColorTarget {
        FPS,
        LOW_1,
        LOW_01
    }
}
