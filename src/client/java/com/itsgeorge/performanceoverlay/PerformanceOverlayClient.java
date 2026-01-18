package com.itsgeorge.performanceoverlay;

import com.itsgeorge.performanceoverlay.client.ConfigIO;
import com.itsgeorge.performanceoverlay.client.FpsTracker;
import com.itsgeorge.performanceoverlay.client.OverlayConfig;
import com.itsgeorge.performanceoverlay.client.OverlayRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class PerformanceOverlayClient implements ClientModInitializer {
    public static final String MOD_ID = "performanceoverlay";

    private static final long NS_PER_SEC = 1_000_000_000L;
    private static final long NS_PER_MS = 1_000_000L;

    private static OverlayConfig config;
    private static FpsTracker tracker;

    private static KeyMapping toggleKey;
    private static KeyMapping resetKey;
    private static KeyMapping benchmarkKey;
    private static KeyMapping cycleLayoutKey;

    private static long benchmarkAutoStopAtNs = 0;

    // Progress state (captured at start so duration display/auto-stop stays consistent during a run)
    private static long benchmarkStartedAtNs = 0;
    private static int benchmarkDurationSecActive = 0;
    private static long lastBenchmarkActionbarUpdateNs = 0;

    public static OverlayConfig getConfig() {
        return config;
    }

    public static void setConfig(OverlayConfig cfg) {
        config = cfg;

        if (tracker != null) {
            tracker.setConfig(cfg, false);
        }
    }

    public static String getToggleKeyDisplayName() {
        return keyNameOrFallback(toggleKey, "F7");
    }

    public static String getCycleLayoutKeyDisplayName() {
        return keyNameOrFallback(cycleLayoutKey, "F8");
    }

    public static String getResetKeyDisplayName() {
        return keyNameOrFallback(resetKey, "F9");
    }

    public static String getBenchmarkKeyDisplayName() {
        return keyNameOrFallback(benchmarkKey, "F10");
    }

    private static String keyNameOrFallback(KeyMapping key, String fallback) {
        if (key == null) {
            return fallback;
        }
        return key.getTranslatedKeyMessage().getString();
    }

    @Override
    public void onInitializeClient() {
        config = ConfigIO.load();
        tracker = new FpsTracker(config);

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "category")
        );

        // IMPORTANT: order of registration = order in Controls
        toggleKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.performanceoverlay.toggle",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_F7,
                        category
                )
        );

        cycleLayoutKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.performanceoverlay.cycle_layout",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_F8,
                        category
                )
        );

        resetKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.performanceoverlay.reset",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_F9,
                        category
                )
        );

        benchmarkKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.performanceoverlay.benchmark",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_F10,
                        category
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long now = System.nanoTime();

            // ActionBar progress while benchmark is active (no chat spam)
            if (tracker.isBenchmarkActive()) {
                if (dueNs(now, lastBenchmarkActionbarUpdateNs, 500)) {
                    lastBenchmarkActionbarUpdateNs = now;
                    showBenchmarkProgressActionbar(client, now);
                }
            }

            // Auto-stop benchmark
            if (tracker.isBenchmarkActive() && benchmarkAutoStopAtNs > 0 && now >= benchmarkAutoStopAtNs) {
                benchmarkAutoStopAtNs = 0;
                FpsTracker.BenchmarkStatus s = tracker.toggleBenchmark();
                if (!s.error()) {
                    clearBenchmarkProgressState();
                    showBenchmarkStopped(client, s);
                }
            }

            while (toggleKey.consumeClick()) {
                config.enabled = !config.enabled;

                if (!config.enabled && tracker.isBenchmarkActive()) {
                    tracker.toggleBenchmark();
                    benchmarkAutoStopAtNs = 0;
                    clearBenchmarkProgressState();
                }

                tracker.setConfig(config, false);
                ConfigIO.save(config);

                showToggleActionbar(client, config.enabled);
            }

            while (resetKey.consumeClick()) {
                tracker.reset();
                showResetActionbar(client);
            }

            while (benchmarkKey.consumeClick()) {
                if (!config.enabled) {
                    showActionbarPlain(client, Component.literal("Enable overlay first").withStyle(ChatFormatting.WHITE));
                    continue;
                }

                FpsTracker.BenchmarkStatus s = tracker.toggleBenchmark();
                if (s.error()) {
                    showActionbarPlain(client, Component.literal(s.message()).withStyle(ChatFormatting.WHITE));
                    benchmarkAutoStopAtNs = 0;
                    clearBenchmarkProgressState();
                    continue;
                }

                if (s.started()) {
                    int durSec = Math.max(0, config.autoBenchmarkDurationSec);

                    benchmarkStartedAtNs = System.nanoTime();
                    benchmarkDurationSecActive = durSec;
                    lastBenchmarkActionbarUpdateNs = 0;

                    benchmarkAutoStopAtNs = (durSec > 0) ? (benchmarkStartedAtNs + (long) durSec * NS_PER_SEC) : 0;

                    showBenchmarkStarted(client, durSec);
                    showBenchmarkProgressActionbar(client, benchmarkStartedAtNs);
                } else if (s.stopped()) {
                    benchmarkAutoStopAtNs = 0;
                    clearBenchmarkProgressState();
                    showBenchmarkStopped(client, s);
                }
            }

            while (cycleLayoutKey.consumeClick()) {
                config.textLayout = nextLayout(config.textLayout);
                tracker.setConfig(config, false);
                ConfigIO.save(config);

                showActionbarPlain(
                        client,
                        Component.literal("Layout: ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(config.textLayout.toString()).withStyle(ChatFormatting.WHITE))
                );
            }
        });

        // Render BEFORE chat, so chat stays on top and never gets covered.
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "overlay"),
                (guiGraphics, deltaTracker) -> {
                    if (!config.enabled) {
                        return;
                    }

                    boolean paused = Minecraft.getInstance().isPaused();
                    tracker.onFrame(paused);

                    OverlayRenderer.render(guiGraphics, config, tracker.getSnapshot());
                }
        );
    }

    private static void clearBenchmarkProgressState() {
        benchmarkStartedAtNs = 0;
        benchmarkDurationSecActive = 0;
        lastBenchmarkActionbarUpdateNs = 0;
    }

    private static OverlayConfig.TextLayout nextLayout(OverlayConfig.TextLayout current) {
        if (current == OverlayConfig.TextLayout.ONE_LINE) {
            return OverlayConfig.TextLayout.THREE_LINES;
        }
        if (current == OverlayConfig.TextLayout.THREE_LINES) {
            return OverlayConfig.TextLayout.COLUMN;
        }
        return OverlayConfig.TextLayout.ONE_LINE;
    }

    private static void showToggleActionbar(Minecraft client, boolean enabled) {
        if (client == null || client.player == null) {
            return;
        }

        Component msg = Component.literal("Overlay: ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(enabled ? "ON" : "OFF")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));

        client.player.displayClientMessage(msg, true);
    }

    private static void showResetActionbar(Minecraft client) {
        showActionbarPlain(client, Component.literal("Stats reset").withStyle(ChatFormatting.WHITE));
    }

    private static void showActionbarPlain(Minecraft client, Component msg) {
        if (client == null || client.player == null) {
            return;
        }
        client.player.displayClientMessage(msg, true);
    }

    private static void showChat(Minecraft client, Component msg) {
        if (client == null || client.player == null) {
            return;
        }
        client.player.displayClientMessage(msg, false);
    }

    private static void showBenchmarkProgressActionbar(Minecraft client, long nowNs) {
        if (client == null || client.player == null) {
            return;
        }

        long start = benchmarkStartedAtNs;
        if (start <= 0) {
            start = nowNs;
            benchmarkStartedAtNs = nowNs;
        }

        long elapsedNs = Math.max(0, nowNs - start);
        long elapsedSec = elapsedNs / NS_PER_SEC;

        MutableComponent msg = Component.literal("Benchmark running… ").withStyle(ChatFormatting.GRAY);

        if (benchmarkDurationSecActive > 0) {
            long dur = benchmarkDurationSecActive;
            long clampedElapsed = Math.min(elapsedSec, dur);

            int pct = (dur > 0) ? (int) Math.round((clampedElapsed * 100.0) / dur) : 0;
            pct = clamp(pct, 0, 100);

            msg = msg.append(Component.literal(pct + "% ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("(" + clampedElapsed + "s / " + dur + "s)").withStyle(ChatFormatting.GRAY));
        } else {
            msg = msg.append(Component.literal("(" + elapsedSec + "s)").withStyle(ChatFormatting.GRAY));
        }

        client.player.displayClientMessage(msg, true);
    }

    private static void showBenchmarkStarted(Minecraft client, int durationSec) {
        String key = getBenchmarkKeyDisplayName();

        MutableComponent msg = Component.empty();

        msg = msg.append(Component.literal("------------------------------").withStyle(ChatFormatting.WHITE));
        msg = msg.append(Component.literal("\nPerformance Overlay — Benchmark Started").withStyle(ChatFormatting.WHITE));
        msg = msg.append(Component.literal("\n------------------------------\n").withStyle(ChatFormatting.WHITE));

        msg = msg.append(Component.literal("\nDuration: ").withStyle(ChatFormatting.GRAY));
        if (durationSec > 0) {
            msg = msg.append(Component.literal(durationSec + "s").withStyle(ChatFormatting.WHITE));
        } else {
            msg = msg.append(Component.literal("Manual stop").withStyle(ChatFormatting.WHITE));
        }
        msg = msg.append(Component.literal(" (Mods → Performance Overlay → Advanced)").withStyle(ChatFormatting.GRAY));

        msg = msg.append(Component.literal("\nStop: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Press " + key).withStyle(ChatFormatting.WHITE));

        showChat(client, msg);
    }

    private static void showBenchmarkStopped(Minecraft client, FpsTracker.BenchmarkStatus s) {
        String path = (s.filePath() != null && !s.filePath().isEmpty()) ? s.filePath() : s.fileName();

        FpsTracker.BenchmarkSummary sum = tracker.getBenchmarkSummary();

        MutableComponent msg = Component.empty();

        msg = msg.append(Component.literal("------------------------------").withStyle(ChatFormatting.WHITE));
        msg = msg.append(Component.literal("\nPerformance Overlay — Benchmark Finished").withStyle(ChatFormatting.WHITE));
        msg = msg.append(Component.literal("\n------------------------------").withStyle(ChatFormatting.WHITE));

        msg = msg.append(Component.literal("\n\nAvg FPS: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(roundInt(sum.avg()))).withStyle(ChatFormatting.WHITE));

        msg = msg.append(Component.literal("\n1% Low: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(roundInt(sum.low1()))).withStyle(ChatFormatting.WHITE));

        msg = msg.append(Component.literal("\n0.1% Low: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(roundInt(sum.low01()))).withStyle(ChatFormatting.WHITE));

        msg = msg.append(Component.literal("\nMax Spike: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(ms1(sum.maxSpikeMs()) + " ms").withStyle(ChatFormatting.WHITE));

        msg = msg.append(Component.literal("\n\nSaved to:").withStyle(ChatFormatting.GRAY));
        msg = msg.append(Component.literal("\n" + path).withStyle(ChatFormatting.WHITE));

        msg = msg.append(Component.literal("\n\nHint: Open the .csv in Excel / Google Sheets or send it to AI").withStyle(ChatFormatting.GRAY));

        showChat(client, msg);
    }

    private static boolean dueNs(long nowNs, long lastUpdateNs, int intervalMs) {
        if (lastUpdateNs == 0) {
            return true;
        }
        long intervalNs = (long) intervalMs * NS_PER_MS;
        return nowNs - lastUpdateNs >= intervalNs;
    }

    private static int roundInt(double v) {
        if (v <= 0 || Double.isNaN(v) || Double.isInfinite(v)) {
            return 0;
        }
        return (int) Math.round(v);
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

    private static int clamp(int v, int min, int max) {
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }
}
