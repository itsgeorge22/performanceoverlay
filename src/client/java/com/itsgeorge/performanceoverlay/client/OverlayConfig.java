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
