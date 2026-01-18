package com.itsgeorge.performanceoverlay.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class FpsTracker {
    private static final long NS_PER_SEC = 1_000_000_000L;
    private static final long NS_PER_MS = 1_000_000L;

    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_YELLOW = 0xFFFFFF55;
    private static final int COLOR_RED = 0xFFFF5555;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter TS_HUMAN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private OverlayConfig config;

    private long[] timeNs;
    private long[] frameNs;
    private long[] scratch;

    private int head = 0;
    private int size = 0;

    private long lastFrameStartNs = 0;

    private long lastFpsUpdateNs = 0;
    private long lastFtUpdateNs = 0;
    private long lastAvgUpdateNs = 0;
    private long lastLow1UpdateNs = 0;
    private long lastLow01UpdateNs = 0;
    private long lastStuttersUpdateNs = 0;

    private double cachedFps = 0;
    private double cachedFtMs = 0;
    private double cachedAvg = 0;
    private double cachedLow1 = 0;
    private double cachedLow01 = 0;

    private int cachedStutters = 0;
    private int cachedStutterPercent = 0;
    private double cachedMaxSpikeMs = 0;

    private Snapshot cached = Snapshot.empty();

    private boolean wasEnabled = false;
    private boolean wasPaused = false;

    // GC / Memory
    private long cachedGcPauseMs = 0;
    private long lastGcUpdateNs = 0;

    private long cachedMemUsedMb = 0;
    private long cachedMemMaxMb = 0;
    private long lastMemUpdateNs = 0;

    // Benchmark
    private boolean benchmarkActive = false;
    private long benchmarkStartNs = 0;
    private BufferedWriter benchmarkWriter = null;
    private String benchmarkFileName = "";
    private String benchmarkFilePath = "";
    private int benchmarkFlushCounter = 0;
    private long benchmarkFrameCount = 0;

    private boolean benchmarkHadWriteError = false;

    // Benchmark (full-run stats)
    private long[] benchmarkFramesNs = null;
    private int benchmarkFramesSize = 0;

    private long benchmarkTotalNs = 0;
    private long benchmarkMaxFrameNs = 0;

    private BenchmarkSummary lastBenchmarkSummary = new BenchmarkSummary(0, 0, 0, 0, 0, 0);

    public FpsTracker(OverlayConfig config) {
        setConfig(config, true);
    }

    public void setConfig(OverlayConfig cfg, boolean forceReset) {
        boolean enabledChangedToTrue = cfg.enabled && !wasEnabled;
        wasEnabled = cfg.enabled;

        this.config = cfg;

        ensureCapacity();

        if (forceReset || enabledChangedToTrue) {
            reset();
        }
    }

    public void reset() {
        head = 0;
        size = 0;

        lastFrameStartNs = 0;

        lastFpsUpdateNs = 0;
        lastFtUpdateNs = 0;
        lastAvgUpdateNs = 0;
        lastLow1UpdateNs = 0;
        lastLow01UpdateNs = 0;
        lastStuttersUpdateNs = 0;

        cachedFps = 0;
        cachedFtMs = 0;
        cachedAvg = 0;
        cachedLow1 = 0;
        cachedLow01 = 0;

        cachedStutters = 0;
        cachedStutterPercent = 0;
        cachedMaxSpikeMs = 0;

        cachedGcPauseMs = -1;
        lastGcUpdateNs = 0;

        cachedMemUsedMb = 0;
        cachedMemMaxMb = 0;
        lastMemUpdateNs = 0;

        wasPaused = false;

        cached = buildSnapshot(COLOR_WHITE);
    }

    public Snapshot getSnapshot() {
        return cached;
    }

    public String getText() {
        if (cached == null || cached.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(96);
        for (int i = 0; i < cached.count; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(cached.lines[i]);
        }
        return sb.toString();
    }

    // ---------- Benchmark API (for PerformanceOverlayClient) ----------

    public boolean isBenchmarkActive() {
        return benchmarkActive;
    }

    public BenchmarkStatus toggleBenchmark() {
        if (benchmarkActive) {
            return stopBenchmark();
        }

        // If we had a write error and the writer is already gone, don't silently start a new run.
        if (benchmarkHadWriteError && benchmarkWriter == null) {
            clearBenchmarkState();
            return BenchmarkStatus.error("Benchmark aborted: write failed");
        }

        return startBenchmark();
    }

    public BenchmarkSummary getBenchmarkSummary() {
        return lastBenchmarkSummary;
    }

    private BenchmarkStatus startBenchmark() {
        // Defensive: close any leftover writer
        if (benchmarkWriter != null) {
            try {
                benchmarkWriter.close();
            } catch (IOException ignored) {
            }
            benchmarkWriter = null;
        }

        benchmarkHadWriteError = false;

        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve("performanceoverlay").resolve("benchmarks");
            Files.createDirectories(dir);

            LocalDateTime now = LocalDateTime.now();

            benchmarkFileName = "benchmark_" + now.format(TS) + ".csv";
            Path file = dir.resolve(benchmarkFileName);
            benchmarkFilePath = file.toAbsolutePath().toString();

            benchmarkWriter = Files.newBufferedWriter(file, StandardCharsets.UTF_8);

            benchmarkWriter.write("# PerformanceOverlay Benchmark\n");
            benchmarkWriter.write("# Date: " + now.format(TS_HUMAN) + "\n");
            benchmarkWriter.write("# ModVersion: " + getModVersion() + "\n");
            benchmarkWriter.write("# Minecraft: " + getMinecraftVersion() + "\n");
            benchmarkWriter.write("# DurationSec: " + Math.max(0, config.autoBenchmarkDurationSec) + "\n");
            benchmarkWriter.write("# PauseHandling: " + config.pauseHandling.name() + "\n");
            benchmarkWriter.write("# StutterThresholdMs: " + config.stutterThresholdMs + "\n");
            benchmarkWriter.write("# AvgWindowSec: " + config.avgWindowSec + "\n");
            benchmarkWriter.write("# Low1WindowSec: " + config.low1WindowSec + "\n");
            benchmarkWriter.write("# Low01WindowSec: " + config.low01WindowSec + "\n");
            benchmarkWriter.write("# FpsWindowMs: " + config.fpsWindowMs + "\n");

            benchmarkWriter.write(
                    "elapsed_ms,frame_ms,inst_fps,fps_smoothed,avg_fps,low1_fps,low01_fps,stutters,stutter_percent,max_spike_ms,gc_pause_ms,mem_used_mb,mem_max_mb\n"
            );

            benchmarkActive = true;
            benchmarkStartNs = System.nanoTime();
            benchmarkFlushCounter = 0;
            benchmarkFrameCount = 0;

            benchmarkFramesSize = 0;
            benchmarkTotalNs = 0;
            benchmarkMaxFrameNs = 0;

            if (benchmarkFramesNs == null) {
                benchmarkFramesNs = new long[6000];
            }

            lastBenchmarkSummary = new BenchmarkSummary(0, 0, 0, 0, 0, 0);

            return BenchmarkStatus.started(benchmarkFileName, benchmarkFilePath);
        } catch (IOException e) {
            clearBenchmarkState();
            return BenchmarkStatus.error("Failed to start benchmark: " + e.getMessage());
        }
    }

    private BenchmarkStatus stopBenchmark() {
        try {
            benchmarkActive = false;

            String name = benchmarkFileName;
            String path = benchmarkFilePath;

            lastBenchmarkSummary = buildBenchmarkSummaryFullRun();

            if (benchmarkWriter != null) {
                benchmarkWriter.write("# SUMMARY\n");

                // Avoid lying if we hit the in-memory cap:
                benchmarkWriter.write("# FramesLogged: " + benchmarkFrameCount + "\n");
                benchmarkWriter.write("# FramesSummary: " + benchmarkFramesSize + "\n");

                benchmarkWriter.write("# AvgFPS: " + f1(lastBenchmarkSummary.avg()) + "\n");
                benchmarkWriter.write("# Low1FPS: " + f1(lastBenchmarkSummary.low1()) + "\n");
                benchmarkWriter.write("# Low01FPS: " + f1(lastBenchmarkSummary.low01()) + "\n");
                benchmarkWriter.write("# Stutters: " + lastBenchmarkSummary.stutters() + "\n");
                benchmarkWriter.write("# MaxSpikeMs: " + ms1(lastBenchmarkSummary.maxSpikeMs()) + "\n");
                benchmarkWriter.flush();
                benchmarkWriter.close();
            }

            // FIX #1: on successful stop, clear the write-error flag
            benchmarkHadWriteError = false;

            clearBenchmarkStateKeepSummary();

            return BenchmarkStatus.stopped(name, path);
        } catch (IOException e) {
            clearBenchmarkStateKeepSummary();
            return BenchmarkStatus.error("Failed to stop benchmark: " + e.getMessage());
        }
    }

    private void clearBenchmarkState() {
        benchmarkActive = false;
        benchmarkHadWriteError = false;

        if (benchmarkWriter != null) {
            try {
                benchmarkWriter.close();
            } catch (IOException ignored) {
            }
        }

        benchmarkWriter = null;
        benchmarkFileName = "";
        benchmarkFilePath = "";
        benchmarkStartNs = 0;
        benchmarkFlushCounter = 0;
        benchmarkFrameCount = 0;

        benchmarkFramesSize = 0;
        benchmarkTotalNs = 0;
        benchmarkMaxFrameNs = 0;

        lastBenchmarkSummary = new BenchmarkSummary(0, 0, 0, 0, 0, 0);
    }

    private void clearBenchmarkStateKeepSummary() {
        benchmarkActive = false;

        benchmarkWriter = null;
        benchmarkFileName = "";
        benchmarkFilePath = "";
        benchmarkStartNs = 0;
        benchmarkFlushCounter = 0;
        benchmarkFrameCount = 0;

        benchmarkFramesSize = 0;
        benchmarkTotalNs = 0;
        benchmarkMaxFrameNs = 0;
    }

    public void onFrame(boolean paused) {
        long nowNs = System.nanoTime();

        if (!config.enabled) {
            lastFrameStartNs = nowNs;
            wasPaused = paused;
            return;
        }

        if (paused) {
            if (config.pauseHandling == OverlayConfig.PauseHandling.RESET) {
                if (!wasPaused) {
                    reset();
                }
                wasPaused = true;
                lastFrameStartNs = nowNs;
            } else if (config.pauseHandling == OverlayConfig.PauseHandling.FREEZE) {
                wasPaused = true;
                lastFrameStartNs = nowNs;
            } else {
                // TRACK
                wasPaused = true;
            }

            if (config.pauseHandling != OverlayConfig.PauseHandling.TRACK) {
                return;
            }
        } else {
            wasPaused = false;
        }

        if (lastFrameStartNs == 0) {
            lastFrameStartNs = nowNs;
            return;
        }

        long dtNs = nowNs - lastFrameStartNs;
        lastFrameStartNs = nowNs;

        if (dtNs <= 0) {
            return;
        }

        push(nowNs, dtNs);
        pruneOld(nowNs);

        boolean changed = false;

        boolean needFpsForColor = config.colorThresholds && config.colorTarget == OverlayConfig.ColorTarget.FPS;
        boolean needLow1ForColor = config.colorThresholds && config.colorTarget == OverlayConfig.ColorTarget.LOW_1;
        boolean needLow01ForColor = config.colorThresholds && config.colorTarget == OverlayConfig.ColorTarget.LOW_01;

        boolean dueFps = (config.showFps || needFpsForColor) && due(nowNs, lastFpsUpdateNs, clamp(config.fpsUpdateMs, 50, 5000));
        boolean dueFt = config.showFrametime && due(nowNs, lastFtUpdateNs, clamp(config.frametimeUpdateMs, 50, 5000));

        if (dueFps || dueFt) {
            Smoothed s = computeSmoothed(nowNs, dtNs);

            if (dueFps) {
                cachedFps = s.fps;
                lastFpsUpdateNs = nowNs;
                changed = true;
            }
            if (dueFt) {
                cachedFtMs = s.ftMs;
                lastFtUpdateNs = nowNs;
                changed = true;
            }
        }

        if (config.showAvg && due(nowNs, lastAvgUpdateNs, clamp(config.avgUpdateMs, 100, 10000))) {
            cachedAvg = windowFps(nowNs, (long) config.avgWindowSec * NS_PER_SEC);
            lastAvgUpdateNs = nowNs;
            changed = true;
        }

        if ((config.show1Low || needLow1ForColor) && due(nowNs, lastLow1UpdateNs, clamp(config.low1UpdateMs, 100, 10000))) {
            cachedLow1 = lowValue(nowNs, (long) config.low1WindowSec * NS_PER_SEC, 0.01);
            lastLow1UpdateNs = nowNs;
            changed = true;
        }

        if ((config.show01Low || needLow01ForColor) && due(nowNs, lastLow01UpdateNs, clamp(config.low01UpdateMs, 100, 10000))) {
            cachedLow01 = lowValue(nowNs, (long) config.low01WindowSec * NS_PER_SEC, 0.001);
            lastLow01UpdateNs = nowNs;
            changed = true;
        }

        if ((config.showStutters || config.showMaxSpike) && due(nowNs, lastStuttersUpdateNs, clamp(config.stuttersUpdateMs, 100, 10000))) {
            long windowNs = (long) config.stutterWindowSec * NS_PER_SEC;
            long thresholdNs = (long) Math.max(1, config.stutterThresholdMs) * NS_PER_MS;

            WindowStats w = windowStats(nowNs, windowNs);
            int frames = w.count;

            cachedStutters = countAboveThreshold(nowNs, windowNs, thresholdNs);
            cachedStutterPercent = (frames > 0) ? (int) Math.round((cachedStutters * 100.0) / frames) : 0;

            long maxFrameNs = maxFrameInWindow(nowNs, windowNs);
            cachedMaxSpikeMs = nsToMs(maxFrameNs);

            lastStuttersUpdateNs = nowNs;
            changed = true;
        }

        // Benchmark write (per-frame)
        if (benchmarkActive && benchmarkWriter != null) {
            try {
                long elapsedMs = (nowNs - benchmarkStartNs) / NS_PER_MS;
                double frameMs = (double) dtNs / (double) NS_PER_MS;
                double instFps = (double) NS_PER_SEC / (double) dtNs;

                // Collect full-run frametimes for end-of-run summary
                benchPushFrame(dtNs);

                benchmarkWriter.write(
                        elapsedMs + "," +
                                ms3(frameMs) + "," +
                                f1(instFps) + "," +
                                f1(cachedFps) + "," +
                                f1(cachedAvg) + "," +
                                f1(cachedLow1) + "," +
                                f1(cachedLow01) + "," +
                                cachedStutters + "," +
                                cachedStutterPercent + "," +
                                ms3(cachedMaxSpikeMs) + "," +
                                ms1((double) cachedGcPauseMs) + "," +
                                cachedMemUsedMb + "," +
                                cachedMemMaxMb + "\n"
                );

                benchmarkFrameCount++;

                benchmarkFlushCounter++;
                if (benchmarkFlushCounter >= 120) {
                    benchmarkWriter.flush();
                    benchmarkFlushCounter = 0;
                }
            } catch (IOException e) {
                // FIX #2: don't half-reset fields; use the shared cleanup
                benchmarkHadWriteError = true;
                clearBenchmarkStateKeepSummary();
            }
        }

        // GC pause (once per second)
        if (config.showGc && due(nowNs, lastGcUpdateNs, 1000)) {
            long gcMs = readLastGcPauseMs();
            cachedGcPauseMs = (gcMs > 0) ? gcMs : -1;
            lastGcUpdateNs = nowNs;
            changed = true;
        }

        // Memory (once per second)
        if (config.showMemory && due(nowNs, lastMemUpdateNs, 250)) {
            Runtime rt = Runtime.getRuntime();
            long used = rt.totalMemory() - rt.freeMemory();

            cachedMemUsedMb = used / (1024 * 1024);
            cachedMemMaxMb = rt.maxMemory() / (1024 * 1024);

            lastMemUpdateNs = nowNs;
            changed = true;
        }

        if (changed) {
            int color = pickColor(cachedFps, cachedLow1, cachedLow01);
            cached = buildSnapshot(color);
        }
    }

    private Snapshot buildSnapshot(int color) {
        String[] lines = new String[16];
        int n = 0;

        OverlayConfig.TextLayout mode = config.textLayout;

        if (mode == OverlayConfig.TextLayout.ONE_LINE) {
            StringBuilder sb = new StringBuilder(120);

            if (config.showFps) {
                sb.append("FPS: ").append(roundInt(cachedFps));
            }
            if (config.showAvg) {
                appendSep(sb);
                sb.append("Avg: ").append(roundInt(cachedAvg));
            }
            if (config.show1Low) {
                appendSep(sb);
                sb.append("1%: ").append(roundInt(cachedLow1));
            }
            if (config.show01Low) {
                appendSep(sb);
                sb.append("0.1%: ").append(roundInt(cachedLow01));
            }
            if (config.showFrametime) {
                appendSep(sb);
                sb.append("FT: ").append(ms1(cachedFtMs)).append("ms");
            }
            if (config.showStutters) {
                appendSep(sb);
                sb.append("St: ").append(cachedStutters).append(" (").append(cachedStutterPercent).append("%)");
            }
            if (config.showMaxSpike) {
                appendSep(sb);
                sb.append("Spike: ").append(ms1(cachedMaxSpikeMs)).append("ms");
            }
            if (config.showGc) {
                appendSep(sb);
                sb.append("GC: ").append(cachedGcPauseMs > 0 ? cachedGcPauseMs + "ms" : "NaN");
            }
            if (config.showMemory) {
                appendSep(sb);
                sb.append("Mem: ")
                        .append(cachedMemUsedMb)
                        .append(" / ")
                        .append(cachedMemMaxMb)
                        .append("M");
            }

            String t = sb.toString();
            if (t.isEmpty()) {
                return Snapshot.empty();
            }

            lines[0] = t;
            return new Snapshot(lines, 1, color);
        }

        if (mode == OverlayConfig.TextLayout.THREE_LINES) {
            StringBuilder a = new StringBuilder(80); // FPS Avg FT
            StringBuilder b = new StringBuilder(80); // 1% 0.1% St
            StringBuilder c = new StringBuilder(80); // Spike GC Mem

            // --- LINE 1 ---
            if (config.showFps) {
                a.append("FPS: ").append(roundInt(cachedFps));
            }
            if (config.showAvg) {
                appendSep(a);
                a.append("Avg: ").append(roundInt(cachedAvg));
            }
            if (config.showFrametime) {
                appendSep(a);
                a.append("FT: ").append(ms1(cachedFtMs)).append("ms");
            }

            // --- LINE 2 ---
            if (config.show1Low) {
                b.append("1%: ").append(roundInt(cachedLow1));
            }
            if (config.show01Low) {
                appendSep(b);
                b.append("0.1%: ").append(roundInt(cachedLow01));
            }
            if (config.showStutters) {
                appendSep(b);
                b.append("St: ").append(cachedStutters)
                        .append(" (").append(cachedStutterPercent).append("%)");
            }
            if (config.showMaxSpike) {
                b.append("Spike: ").append(ms1(cachedMaxSpikeMs)).append("ms");
            }

            // --- LINE 3 ---
            if (config.showGc) {
                appendSep(c);
                c.append("GC: ").append(cachedGcPauseMs > 0 ? cachedGcPauseMs + "ms" : "NaN");
            }
            if (config.showMemory) {
                appendSep(c);
                c.append("Mem: ")
                        .append(cachedMemUsedMb)
                        .append(" / ")
                        .append(cachedMemMaxMb)
                        .append("M");
            }

            if (a.length() == 0 && b.length() == 0 && c.length() == 0) {
                return Snapshot.empty();
            }

            if (a.length() > 0) {
                lines[n++] = a.toString();
            }
            if (b.length() > 0) {
                lines[n++] = b.toString();
            }
            if (c.length() > 0) {
                lines[n++] = c.toString();
            }

            return new Snapshot(lines, n, color);
        }

        // COLUMN
        if (config.showFps) {
            lines[n++] = "FPS: " + roundInt(cachedFps);
        }
        if (config.showAvg) {
            lines[n++] = "Avg: " + roundInt(cachedAvg);
        }
        if (config.show1Low) {
            lines[n++] = "1%: " + roundInt(cachedLow1);
        }
        if (config.show01Low) {
            lines[n++] = "0.1%: " + roundInt(cachedLow01);
        }
        if (config.showStutters) {
            lines[n++] = "St: " + cachedStutters + " (" + cachedStutterPercent + "%)";
        }
        if (config.showMaxSpike) {
            lines[n++] = "Spike: " + ms1(cachedMaxSpikeMs) + "ms";
        }
        if (config.showFrametime) {
            lines[n++] = "FT: " + ms1(cachedFtMs) + "ms";
        }
        if (config.showGc) {
            lines[n++] = "GC: " + (cachedGcPauseMs > 0 ? cachedGcPauseMs + "ms" : "NaN");
        }
        if (config.showMemory) {
            lines[n++] = "Mem: " + cachedMemUsedMb + " / " + cachedMemMaxMb + "M";
        }

        if (n == 0) {
            return Snapshot.empty();
        }

        return new Snapshot(lines, n, color);
    }

    private int pickColor(double fps, double low1, double low01) {
        if (!config.colorThresholds) {
            return COLOR_WHITE;
        }

        double metric;

        if (config.colorTarget == OverlayConfig.ColorTarget.LOW_01) {
            metric = (low01 > 0) ? low01 : ((low1 > 0) ? low1 : fps);
        } else if (config.colorTarget == OverlayConfig.ColorTarget.LOW_1) {
            metric = (low1 > 0) ? low1 : fps;
        } else {
            metric = fps;
        }

        if (metric <= 0 || Double.isNaN(metric) || Double.isInfinite(metric)) {
            return COLOR_WHITE;
        }

        if (metric < config.dangerFps) {
            return COLOR_RED;
        }
        if (metric < config.warningFps) {
            return COLOR_YELLOW;
        }
        return COLOR_WHITE;
    }

    private Smoothed computeSmoothed(long nowNs, long lastDtNs) {
        double fps = nsToFps(lastDtNs);
        double ftMs = nsToMs(lastDtNs);

        long windowNs = (long) config.fpsWindowMs * NS_PER_MS;
        WindowStats w = windowStats(nowNs, windowNs);

        if (w.count >= 2 && w.sumNs > 0) {
            fps = (double) w.count * (double) NS_PER_SEC / (double) w.sumNs;
            ftMs = ((double) w.sumNs / (double) w.count) / (double) NS_PER_MS;
        }

        return new Smoothed(fps, ftMs);
    }

    private double lowValue(long nowNs, long windowNs, double worstPercent) {
        int n = copyFramesToScratch(nowNs, windowNs);
        if (n <= 0) {
            return 0;
        }

        if (config.lowMethod == OverlayConfig.LowMethod.MEAN_WORST) {
            int k = Math.max(1, (int) Math.ceil(n * worstPercent));
            long meanWorst = meanWorstK(scratch, n, k);
            return nsToFps(meanWorst);
        }

        int index = percentileIndex(n, 1.0 - worstPercent);
        long dt = selectNth(scratch, 0, n - 1, index);
        return nsToFps(dt);
    }

    private static int percentileIndex(int n, double p) {
        if (n <= 1) {
            return 0;
        }
        double clamped = Math.max(0.0, Math.min(1.0, p));
        int idx = (int) Math.ceil(n * clamped) - 1;
        if (idx < 0) {
            return 0;
        }
        if (idx > n - 1) {
            return n - 1;
        }
        return idx;
    }

    private int countAboveThreshold(long nowNs, long windowNs, long thresholdNs) {
        long minNs = nowNs - windowNs;

        int cap = frameNs.length;
        int count = 0;

        for (int i = 0; i < size; i++) {
            int idx = (head + size - 1 - i + cap) % cap;
            if (timeNs[idx] < minNs) {
                break;
            }
            if (frameNs[idx] >= thresholdNs) {
                count++;
            }
        }

        return count;
    }

    private long maxFrameInWindow(long nowNs, long windowNs) {
        long minNs = nowNs - windowNs;

        int cap = frameNs.length;
        long max = 0;

        for (int i = 0; i < size; i++) {
            int idx = (head + size - 1 - i + cap) % cap;
            if (timeNs[idx] < minNs) {
                break;
            }
            long v = frameNs[idx];
            if (v > max) {
                max = v;
            }
        }

        return max;
    }

    private double windowFps(long nowNs, long windowNs) {
        WindowStats w = windowStats(nowNs, windowNs);
        if (w.count < 2 || w.sumNs <= 0) {
            return 0;
        }
        return (double) w.count * (double) NS_PER_SEC / (double) w.sumNs;
    }

    private WindowStats windowStats(long nowNs, long windowNs) {
        long minNs = nowNs - windowNs;

        long sum = 0;
        int count = 0;

        int cap = frameNs.length;

        for (int i = 0; i < size; i++) {
            int idx = (head + size - 1 - i + cap) % cap;
            if (timeNs[idx] < minNs) {
                break;
            }
            sum += frameNs[idx];
            count++;
        }

        return new WindowStats(sum, count);
    }

    private int copyFramesToScratch(long nowNs, long windowNs) {
        long minNs = nowNs - windowNs;

        int cap = frameNs.length;
        int n = 0;

        for (int i = 0; i < size; i++) {
            int idx = (head + size - 1 - i + cap) % cap;
            if (timeNs[idx] < minNs) {
                break;
            }
            scratch[n] = frameNs[idx];
            n++;
        }

        return n;
    }

    private void ensureCapacity() {
        int maxSec = 1;

        maxSec = Math.max(maxSec, config.avgWindowSec);
        maxSec = Math.max(maxSec, config.low1WindowSec);
        maxSec = Math.max(maxSec, config.low01WindowSec);
        maxSec = Math.max(maxSec, config.stutterWindowSec);

        int fpsSec = (int) Math.ceil((double) config.fpsWindowMs / 1000.0);
        maxSec = Math.max(maxSec, fpsSec);

        int desired = maxSec * 1200;
        desired = Math.max(6000, Math.min(240000, desired));

        if (timeNs != null && timeNs.length >= desired) {
            return;
        }

        timeNs = new long[desired];
        frameNs = new long[desired];
        scratch = new long[desired];

        reset();
    }

    private void push(long tNs, long dtNs) {
        int cap = timeNs.length;

        if (size == cap) {
            head = (head + 1) % cap;
            size--;
        }

        int tail = (head + size) % cap;
        timeNs[tail] = tNs;
        frameNs[tail] = dtNs;
        size++;
    }

    private void pruneOld(long nowNs) {
        int maxSec = 1;

        maxSec = Math.max(maxSec, config.avgWindowSec);
        maxSec = Math.max(maxSec, config.low1WindowSec);
        maxSec = Math.max(maxSec, config.low01WindowSec);
        maxSec = Math.max(maxSec, config.stutterWindowSec);

        int fpsSec = (int) Math.ceil((double) config.fpsWindowMs / 1000.0);
        maxSec = Math.max(maxSec, fpsSec);

        long minNs = nowNs - (long) maxSec * NS_PER_SEC;

        int cap = frameNs.length;

        while (size > 0 && timeNs[head] < minNs) {
            head = (head + 1) % cap;
            size--;
        }
    }

    private static boolean due(long nowNs, long lastUpdateNs, int intervalMs) {
        if (lastUpdateNs == 0) {
            return true;
        }
        long intervalNs = (long) intervalMs * NS_PER_MS;
        return nowNs - lastUpdateNs >= intervalNs;
    }

    private static void appendSep(StringBuilder sb) {
        if (sb.length() > 0) {
            sb.append(" | ");
        }
    }

    private static int roundInt(double v) {
        if (v <= 0 || Double.isNaN(v) || Double.isInfinite(v)) {
            return 0;
        }
        return (int) Math.round(v);
    }

    private static double nsToFps(long dtNs) {
        if (dtNs <= 0) {
            return 0;
        }
        return (double) NS_PER_SEC / (double) dtNs;
    }

    private static double nsToMs(long ns) {
        if (ns <= 0) {
            return 0;
        }
        return (double) ns / (double) NS_PER_MS;
    }

    private static String ms1(double ms) {
        if (ms <= 0 || Double.isNaN(ms) || Double.isInfinite(ms)) {
            return "0.0";
        }

        long t = Math.round(ms * 10.0);
        long whole = t / 10;
        long frac = Math.abs(t % 10);
        return whole + "." + frac;
    }

    private static String ms3(double ms) {
        long t = Math.round(ms * 1000.0);
        long whole = t / 1000;
        long frac = Math.abs(t % 1000);
        String f = String.valueOf(frac);
        if (frac < 10) {
            f = "00" + f;
        } else if (frac < 100) {
            f = "0" + f;
        }
        return whole + "." + f;
    }

    private static String f1(double v) {
        if (v <= 0 || Double.isNaN(v) || Double.isInfinite(v)) {
            return "0.0";
        }
        long t = Math.round(v * 10.0);
        long whole = t / 10;
        long frac = Math.abs(t % 10);
        return whole + "." + frac;
    }

    private static String getModVersion() {
        return FabricLoader.getInstance()
                .getModContainer("performanceoverlay")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private static String getMinecraftVersion() {
        try {
            Object v = SharedConstants.getCurrentVersion();

            try {
                return String.valueOf(v.getClass().getMethod("getName").invoke(v));
            } catch (ReflectiveOperationException ignored) {
            }

            try {
                return String.valueOf(v.getClass().getMethod("getId").invoke(v));
            } catch (ReflectiveOperationException ignored) {
            }

            return String.valueOf(v);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static long meanWorstK(long[] a, int n, int k) {
        int target = n - k;
        long threshold = selectNth(a, 0, n - 1, target);

        long sum = 0;
        int countAbove = 0;

        for (int i = 0; i < n; i++) {
            if (a[i] > threshold) {
                sum += a[i];
                countAbove++;
            }
        }

        int remaining = k - countAbove;
        if (remaining > 0) {
            sum += (long) remaining * threshold;
        }

        return sum / k;
    }

    private static long selectNth(long[] a, int left, int right, int n) {
        while (true) {
            if (left == right) {
                return a[left];
            }

            int pivotIndex = (left + right) >>> 1;
            pivotIndex = partition(a, left, right, pivotIndex);

            if (n == pivotIndex) {
                return a[n];
            }
            if (n < pivotIndex) {
                right = pivotIndex - 1;
            } else {
                left = pivotIndex + 1;
            }
        }
    }

    private static int partition(long[] a, int left, int right, int pivotIndex) {
        long pivotValue = a[pivotIndex];
        swap(a, pivotIndex, right);

        int storeIndex = left;

        for (int i = left; i < right; i++) {
            if (a[i] < pivotValue) {
                swap(a, storeIndex, i);
                storeIndex++;
            }
        }

        swap(a, right, storeIndex);
        return storeIndex;
    }

    private static void swap(long[] a, int i, int j) {
        long t = a[i];
        a[i] = a[j];
        a[j] = t;
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

    private void benchPushFrame(long dtNs) {
        if (dtNs <= 0) {
            return;
        }

        if (benchmarkFramesNs == null) {
            benchmarkFramesNs = new long[6000];
        }

        if (benchmarkFramesSize >= benchmarkFramesNs.length) {
            int next = benchmarkFramesNs.length * 2;
            next = Math.min(next, 5_000_000);
            if (next <= benchmarkFramesNs.length) {
                return;
            }

            long[] grown = new long[next];
            System.arraycopy(benchmarkFramesNs, 0, grown, 0, benchmarkFramesNs.length);
            benchmarkFramesNs = grown;
        }

        benchmarkFramesNs[benchmarkFramesSize++] = dtNs;
        benchmarkTotalNs += dtNs;

        if (dtNs > benchmarkMaxFrameNs) {
            benchmarkMaxFrameNs = dtNs;
        }
    }

    private BenchmarkSummary buildBenchmarkSummaryFullRun() {
        int n = benchmarkFramesSize;
        if (n <= 0 || benchmarkTotalNs <= 0) {
            return new BenchmarkSummary(0, 0, 0, 0, 0, 0);
        }

        double avgFps = (double) n * (double) NS_PER_SEC / (double) benchmarkTotalNs;

        double low1Fps;
        double low01Fps;

        if (config.lowMethod == OverlayConfig.LowMethod.MEAN_WORST) {
            long meanWorst1 = meanWorstKFullRun(n, 0.01);
            long meanWorst01 = meanWorstKFullRun(n, 0.001);

            low1Fps = nsToFps(meanWorst1);
            low01Fps = nsToFps(meanWorst01);
        } else {
            long p99Ns = percentileFrameNs(benchmarkFramesNs, n, 0.99);
            long p999Ns = percentileFrameNs(benchmarkFramesNs, n, 0.999);

            low1Fps = nsToFps(p99Ns);
            low01Fps = nsToFps(p999Ns);
        }

        long thresholdNs = (long) Math.max(1, config.stutterThresholdMs) * NS_PER_MS;
        int stutters = 0;
        for (int i = 0; i < n; i++) {
            if (benchmarkFramesNs[i] >= thresholdNs) {
                stutters++;
            }
        }
        int stutterPercent = (n > 0) ? (int) Math.round((stutters * 100.0) / n) : 0;

        double maxSpikeMs = nsToMs(benchmarkMaxFrameNs);

        return new BenchmarkSummary(avgFps, low1Fps, low01Fps, stutters, stutterPercent, maxSpikeMs);
    }

    private long percentileFrameNs(long[] src, int n, double p) {
        if (n <= 0) {
            return 0;
        }

        if (scratch == null || scratch.length < n) {
            scratch = new long[Math.max(n, 6000)];
        }

        System.arraycopy(src, 0, scratch, 0, n);

        int idx = percentileIndex(n, p);
        return selectNth(scratch, 0, n - 1, idx);
    }

    private long meanWorstKFullRun(int n, double worstPercent) {
        if (n <= 0) {
            return 0;
        }

        int k = Math.max(1, (int) Math.ceil(n * worstPercent));

        if (scratch == null || scratch.length < n) {
            scratch = new long[Math.max(n, 6000)];
        }

        System.arraycopy(benchmarkFramesNs, 0, scratch, 0, n);

        return meanWorstK(scratch, n, k);
    }

    private record Smoothed(double fps, double ftMs) {
    }

    private record WindowStats(long sumNs, int count) {
    }

    public static final class BenchmarkStatus {
        private final boolean started;
        private final boolean stopped;
        private final boolean error;
        private final String message;
        private final String fileName;
        private final String filePath;

        private BenchmarkStatus(boolean started, boolean stopped, boolean error, String message, String fileName, String filePath) {
            this.started = started;
            this.stopped = stopped;
            this.error = error;
            this.message = message;
            this.fileName = fileName;
            this.filePath = filePath;
        }

        public static BenchmarkStatus started(String fileName, String filePath) {
            return new BenchmarkStatus(true, false, false, "Benchmark started", fileName, filePath);
        }

        public static BenchmarkStatus stopped(String fileName, String filePath) {
            return new BenchmarkStatus(false, true, false, "Benchmark saved: " + fileName, fileName, filePath);
        }

        public static BenchmarkStatus error(String message) {
            return new BenchmarkStatus(false, false, true, message, "", "");
        }

        public boolean started() {
            return started;
        }

        public boolean stopped() {
            return stopped;
        }

        public boolean error() {
            return error;
        }

        public String message() {
            return message;
        }

        public String fileName() {
            return fileName;
        }

        public String filePath() {
            return filePath;
        }
    }

    public static record BenchmarkSummary(
            double avg,
            double low1,
            double low01,
            int stutters,
            int stutterPercent,
            double maxSpikeMs
    ) {
    }

    public static record Snapshot(String[] lines, int count, int color) {
        public static Snapshot empty() {
            return new Snapshot(new String[0], 0, COLOR_WHITE);
        }

        public boolean isEmpty() {
            return count <= 0;
        }
    }

    private long lastTotalGcTimeMs = 0;

    private long readLastGcPauseMs() {
        long total = 0;

        for (var bean : java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            long t = bean.getCollectionTime();
            if (t > 0) total += t;
        }

        long delta = total - lastTotalGcTimeMs;
        lastTotalGcTimeMs = total;

        return Math.max(delta, 0);
    }
}