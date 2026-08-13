package com.itsgeorge.performanceoverlay.gametest;

import com.itsgeorge.performanceoverlay.PerformanceOverlayClient;
import com.itsgeorge.performanceoverlay.client.FpsTracker;
import com.itsgeorge.performanceoverlay.client.OverlayConfig;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.lwjgl.glfw.GLFW;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@SuppressWarnings("UnstableApiUsage")
public final class PerformanceOverlayWorldClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        OverlayConfig config = context.computeOnClient(client -> PerformanceOverlayClient.getConfig());
        FpsTracker tracker = context.computeOnClient(client -> {
            config.enabled = true;
            config.position = OverlayConfig.OverlayPosition.TOP_LEFT;
            config.offsetX = 8;
            config.offsetY = 8;
            config.scale = 1.0f;
            config.textLayout = OverlayConfig.TextLayout.ONE_LINE;
            config.colorThresholds = false;
            config.showFps = true;
            config.showAvg = true;
            config.show1Low = true;
            config.show01Low = false;
            config.showFrametime = true;
            config.showStutters = false;
            config.showMaxSpike = false;
            config.showGc = true;
            config.showMemory = false;
            PerformanceOverlayClient.setConfig(config);
            return getActiveTracker();
        });

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientWorld().waitForChunksRender();

            context.waitFor(client -> client.level != null
                    && client.player != null
                    && containsPositiveFps(tracker.getText()));

            verifyLayout(context, tracker, config, OverlayConfig.TextLayout.ONE_LINE, 1, "overlay-one-line");
            verifyLayout(context, tracker, config, OverlayConfig.TextLayout.THREE_LINES, 3, "overlay-three-lines");
            verifyLayout(context, tracker, config, OverlayConfig.TextLayout.COLUMN, 5, "overlay-column");

            preparePositionChecks(context, config);
            for (OverlayConfig.OverlayPosition position : OverlayConfig.OverlayPosition.values()) {
                verifyPosition(context, tracker, config, position);
            }

            verifyScaleLimits(context, tracker, config);
            verifyToggleKey(context, tracker, config);
            verifyCycleLayoutKey(context, tracker, config);
            verifyResetKey(context, tracker, config);
            verifyBenchmarkKey(context, tracker, config);
        }
    }

    private static void verifyBenchmarkKey(
            ClientGameTestContext context,
            FpsTracker tracker,
            OverlayConfig config
    ) {
        context.runOnClient(client -> {
            config.enabled = true;
            config.autoBenchmarkDurationSec = 0;
            PerformanceOverlayClient.setConfig(config);
        });

        context.getInput().pressKey(GLFW.GLFW_KEY_F10);
        context.waitFor(client -> tracker.isBenchmarkActive());
        String benchmarkFilePath = context.computeOnClient(client -> getStringField(tracker, "benchmarkFilePath"));
        Path benchmarkFile = Path.of(benchmarkFilePath);

        context.waitTicks(20);
        long capturedFrames = context.computeOnClient(client -> getLongField(tracker, "benchmarkFrameCount"));
        if (capturedFrames <= 0) {
            throw new AssertionError("F10 benchmark did not capture any rendered frames");
        }
        context.takeScreenshot("overlay-f10-benchmark-running");

        int benchmarkHistoryBeforeF9 = context.computeOnClient(client -> getHistorySize(tracker));
        context.getInput().pressKey(GLFW.GLFW_KEY_F9);
        int benchmarkHistoryAfterF9 = context.computeOnClient(client -> getHistorySize(tracker));
        if (benchmarkHistoryAfterF9 < benchmarkHistoryBeforeF9) {
            throw new AssertionError("F9 reset rolling history while a benchmark was active");
        }
        if (!tracker.isBenchmarkActive()) {
            throw new AssertionError("F9 unexpectedly stopped the active benchmark");
        }
        context.takeScreenshot("overlay-f9-blocked-during-benchmark");

        context.getInput().pressKey(GLFW.GLFW_KEY_F10);
        context.waitFor(client -> !tracker.isBenchmarkActive());

        if (!Files.isRegularFile(benchmarkFile)) {
            throw new AssertionError("F10 benchmark did not save its CSV: " + benchmarkFile);
        }

        try {
            String csv = Files.readString(benchmarkFile, StandardCharsets.UTF_8);
            if (!csv.contains("# EndReason: MANUAL") || !csv.contains("# SUMMARY")) {
                throw new AssertionError("Manually stopped F10 benchmark did not finalize its CSV correctly");
            }
        } catch (IOException e) {
            throw new AssertionError("Could not read the F10 benchmark CSV", e);
        }

        if (tracker.getBenchmarkSummary().avg() <= 0) {
            throw new AssertionError("F10 benchmark produced an empty final summary");
        }
        context.takeScreenshot("overlay-f10-benchmark-stopped");

        verifyResetIgnoredWhileHidden(context, tracker, config);
    }

    private static void verifyResetIgnoredWhileHidden(
            ClientGameTestContext context,
            FpsTracker tracker,
            OverlayConfig config
    ) {
        context.waitTicks(5);
        context.getInput().pressKey(GLFW.GLFW_KEY_F7);
        context.waitFor(client -> !config.enabled);

        int hiddenHistoryBeforeF9 = context.computeOnClient(client -> getHistorySize(tracker));
        context.getInput().pressKey(GLFW.GLFW_KEY_F9);
        int hiddenHistoryAfterF9 = context.computeOnClient(client -> getHistorySize(tracker));
        if (hiddenHistoryAfterF9 != hiddenHistoryBeforeF9) {
            throw new AssertionError("F9 changed rolling history while the overlay was hidden");
        }
        context.takeScreenshot("overlay-f9-ignored-while-hidden");

        context.getInput().pressKey(GLFW.GLFW_KEY_F7);
        context.waitFor(client -> config.enabled);
    }

    private static void verifyResetKey(
            ClientGameTestContext context,
            FpsTracker tracker,
            OverlayConfig config
    ) {
        context.runOnClient(client -> {
            config.enabled = true;
            config.textLayout = OverlayConfig.TextLayout.ONE_LINE;
            config.fpsUpdateMs = 250;
            PerformanceOverlayClient.setConfig(config);
        });
        context.waitTicks(30);

        int historyBefore = context.computeOnClient(client -> getHistorySize(tracker));
        if (historyBefore < 20) {
            throw new AssertionError("Not enough frame history was collected before testing F9: " + historyBefore);
        }

        context.getInput().pressKey(GLFW.GLFW_KEY_F9);
        int historyAfter = context.computeOnClient(client -> getHistorySize(tracker));
        if (historyAfter >= historyBefore / 2 || historyAfter > 10) {
            throw new AssertionError(
                    "F9 did not reset frame history; samples before=" + historyBefore + ", after=" + historyAfter
            );
        }
        context.takeScreenshot("overlay-f9-stats-reset");

        context.waitTicks(5);
        int resumedHistory = context.computeOnClient(client -> getHistorySize(tracker));
        if (resumedHistory <= historyAfter) {
            throw new AssertionError("Frame sampling did not resume after the F9 reset");
        }
    }

    private static void verifyCycleLayoutKey(
            ClientGameTestContext context,
            FpsTracker tracker,
            OverlayConfig config
    ) {
        context.runOnClient(client -> {
            config.enabled = true;
            config.position = OverlayConfig.OverlayPosition.TOP_LEFT;
            config.offsetX = 12;
            config.offsetY = 60;
            config.scale = 1.0f;
            config.textLayout = OverlayConfig.TextLayout.ONE_LINE;
            config.showFps = true;
            config.showAvg = true;
            config.show1Low = true;
            config.showFrametime = true;
            config.showGc = true;
            PerformanceOverlayClient.setConfig(config);
        });
        context.waitTick();
        assertLayoutState(context, tracker, config, OverlayConfig.TextLayout.ONE_LINE, 1, "overlay-f8-one-line-before");

        context.getInput().pressKey(GLFW.GLFW_KEY_F8);
        assertLayoutState(context, tracker, config, OverlayConfig.TextLayout.THREE_LINES, 3, "overlay-f8-three-lines");

        context.getInput().pressKey(GLFW.GLFW_KEY_F8);
        assertLayoutState(context, tracker, config, OverlayConfig.TextLayout.COLUMN, 5, "overlay-f8-column");

        context.getInput().pressKey(GLFW.GLFW_KEY_F8);
        assertLayoutState(context, tracker, config, OverlayConfig.TextLayout.ONE_LINE, 1, "overlay-f8-one-line-after");
    }

    private static void assertLayoutState(
            ClientGameTestContext context,
            FpsTracker tracker,
            OverlayConfig config,
            OverlayConfig.TextLayout expectedLayout,
            int expectedLineCount,
            String screenshotName
    ) {
        context.waitFor(client -> config.textLayout == expectedLayout
                && tracker.getSnapshot().count() == expectedLineCount
                && containsPositiveFps(tracker.getText()));
        OverlayBounds bounds = context.computeOnClient(client -> calculateOverlayBounds(client, tracker, config));
        Path screenshot = context.takeScreenshot(screenshotName);
        assertOverlayPixelsPresent(screenshot, bounds);
    }

    private static void verifyToggleKey(
            ClientGameTestContext context,
            FpsTracker tracker,
            OverlayConfig config
    ) {
        context.runOnClient(client -> {
            config.enabled = true;
            config.position = OverlayConfig.OverlayPosition.TOP_LEFT;
            config.offsetX = 12;
            config.offsetY = 60;
            config.scale = 1.0f;
            PerformanceOverlayClient.setConfig(config);
        });
        context.waitTick();
        context.waitFor(client -> config.enabled && containsPositiveFps(tracker.getText()));

        OverlayBounds bounds = context.computeOnClient(client -> calculateOverlayBounds(client, tracker, config));
        Path visibleBefore = context.takeScreenshot("overlay-f7-visible-before");
        assertOverlayPixelsPresent(visibleBefore, bounds);

        context.getInput().pressKey(GLFW.GLFW_KEY_F7);
        context.waitFor(client -> !config.enabled);
        Path hidden = context.takeScreenshot("overlay-f7-hidden");
        assertOverlayPixelsAbsent(hidden, bounds);

        context.getInput().pressKey(GLFW.GLFW_KEY_F7);
        context.waitFor(client -> config.enabled && containsPositiveFps(tracker.getText()));
        Path visibleAfter = context.takeScreenshot("overlay-f7-visible-after");
        assertOverlayPixelsPresent(visibleAfter, bounds);
    }

    private static void verifyScaleLimits(
            ClientGameTestContext context,
            FpsTracker tracker,
            OverlayConfig config
    ) {
        context.runOnClient(client -> {
            config.position = OverlayConfig.OverlayPosition.TOP_LEFT;
            config.offsetX = 12;
            config.offsetY = 60;
            config.fpsUpdateMs = 5000;
            PerformanceOverlayClient.setConfig(config);
        });

        int minimumScalePixels = captureScale(context, tracker, config, 0.5f, "overlay-scale-minimum");
        int maximumScalePixels = captureScale(context, tracker, config, 2.0f, "overlay-scale-maximum");

        if (maximumScalePixels < minimumScalePixels * 4) {
            throw new AssertionError("Maximum overlay scale did not render materially larger than minimum scale");
        }
    }

    private static int captureScale(
            ClientGameTestContext context,
            FpsTracker tracker,
            OverlayConfig config,
            float scale,
            String screenshotName
    ) {
        context.runOnClient(client -> {
            config.scale = scale;
            PerformanceOverlayClient.setConfig(config);
        });
        context.waitTick();
        context.waitFor(client -> tracker.getSnapshot().count() == 1
                && containsPositiveFps(tracker.getText()));

        OverlayBounds bounds = context.computeOnClient(client -> calculateOverlayBounds(client, tracker, config));
        Path screenshot = context.takeScreenshot(screenshotName);
        int brightPixels = countBrightPixels(screenshot, bounds);
        if (brightPixels < 5) {
            throw new AssertionError("Overlay text was not visible at scale " + scale);
        }
        return brightPixels;
    }

    private static void preparePositionChecks(ClientGameTestContext context, OverlayConfig config) {
        context.runOnClient(client -> {
            config.textLayout = OverlayConfig.TextLayout.ONE_LINE;
            config.offsetX = 12;
            config.offsetY = 60;
            config.showFps = true;
            config.showAvg = false;
            config.show1Low = false;
            config.show01Low = false;
            config.showFrametime = false;
            config.showStutters = false;
            config.showMaxSpike = false;
            config.showGc = false;
            config.showMemory = false;
            PerformanceOverlayClient.setConfig(config);
        });
    }

    private static void verifyPosition(
            ClientGameTestContext context,
            FpsTracker tracker,
            OverlayConfig config,
            OverlayConfig.OverlayPosition position
    ) {
        context.runOnClient(client -> {
            config.position = position;
            PerformanceOverlayClient.setConfig(config);
        });
        context.waitTick();
        context.waitFor(client -> tracker.getSnapshot().count() == 1
                && containsPositiveFps(tracker.getText()));

        OverlayBounds bounds = context.computeOnClient(client -> calculateOverlayBounds(client, tracker, config));

        String screenshotName = "overlay-position-" + position.name().toLowerCase(Locale.ROOT).replace('_', '-');
        Path screenshot = context.takeScreenshot(screenshotName);
        assertOverlayPixelsPresent(screenshot, bounds);
    }

    private static OverlayBounds calculateOverlayBounds(
            net.minecraft.client.Minecraft client,
            FpsTracker tracker,
            OverlayConfig config
    ) {
        String line = tracker.getSnapshot().lines()[0];
        int width = Math.round(client.font.width(line) * config.scale);
        int height = Math.round(client.font.lineHeight * config.scale);
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int x = switch (config.position) {
            case TOP_LEFT, BOTTOM_LEFT -> config.offsetX;
            case TOP_RIGHT, BOTTOM_RIGHT -> Math.max(0, screenWidth - config.offsetX - width);
            case TOP_CENTER, BOTTOM_CENTER -> Math.max(0, (screenWidth - width) / 2 + config.offsetX);
        };
        int y = switch (config.position) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> config.offsetY;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> Math.max(0, screenHeight - config.offsetY - height);
        };
        return new OverlayBounds(x, y, width, height, screenWidth, screenHeight);
    }

    private static void verifyLayout(
            ClientGameTestContext context,
            FpsTracker tracker,
            OverlayConfig config,
            OverlayConfig.TextLayout layout,
            int expectedLineCount,
            String screenshotName
    ) {
        context.runOnClient(client -> {
            config.textLayout = layout;
            PerformanceOverlayClient.setConfig(config);
        });
        context.waitTick();
        context.waitFor(client -> tracker.getSnapshot().count() == expectedLineCount
                && containsPositiveFps(tracker.getText()));

        Path screenshot = context.takeScreenshot(screenshotName);
        assertOverlayPixelsPresent(screenshot);
    }

    private static void assertOverlayPixelsPresent(Path screenshot) {
        try {
            BufferedImage image = ImageIO.read(screenshot.toFile());
            if (image == null) {
                throw new AssertionError("Could not read the overlay test screenshot");
            }

            int brightPixels = 0;
            int regionWidth = Math.min(200, image.getWidth());
            int regionHeight = Math.min(80, image.getHeight());

            for (int y = 0; y < regionHeight; y++) {
                for (int x = 0; x < regionWidth; x++) {
                    int rgb = image.getRGB(x, y);
                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = rgb & 0xFF;

                    if (red >= 235 && green >= 235 && blue >= 235) {
                        brightPixels++;
                    }
                }
            }

            if (brightPixels < 20) {
                throw new AssertionError("Expected FPS text was not visible in the overlay screenshot");
            }
        } catch (IOException e) {
            throw new AssertionError("Could not inspect the overlay test screenshot", e);
        }
    }

    private static void assertOverlayPixelsPresent(Path screenshot, OverlayBounds bounds) {
        int brightPixels = countBrightPixels(screenshot, bounds);
        if (brightPixels < 20) {
            throw new AssertionError("Overlay text was not visible at the expected position: " + screenshot);
        }
    }

    private static void assertOverlayPixelsAbsent(Path screenshot, OverlayBounds bounds) {
        int brightPixels = countBrightPixels(screenshot, bounds);
        if (brightPixels >= 5) {
            throw new AssertionError("Overlay text remained visible after F7 disabled it: " + screenshot);
        }
    }

    private static int countBrightPixels(Path screenshot, OverlayBounds bounds) {
        try {
            BufferedImage image = ImageIO.read(screenshot.toFile());
            if (image == null) {
                throw new AssertionError("Could not read the overlay position screenshot");
            }

            double scaleX = image.getWidth() / (double) bounds.screenWidth();
            double scaleY = image.getHeight() / (double) bounds.screenHeight();
            int padding = 12;
            int minX = Math.max(0, (int) Math.floor((bounds.x() - padding) * scaleX));
            int minY = Math.max(0, (int) Math.floor((bounds.y() - padding) * scaleY));
            int maxX = Math.min(image.getWidth(), (int) Math.ceil((bounds.x() + bounds.width() + padding) * scaleX));
            int maxY = Math.min(image.getHeight(), (int) Math.ceil((bounds.y() + bounds.height() + padding) * scaleY));

            int brightPixels = 0;
            for (int y = minY; y < maxY; y++) {
                for (int x = minX; x < maxX; x++) {
                    int rgb = image.getRGB(x, y);
                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = rgb & 0xFF;
                    if (red >= 235 && green >= 235 && blue >= 235) {
                        brightPixels++;
                    }
                }
            }

            return brightPixels;
        } catch (IOException e) {
            throw new AssertionError("Could not inspect the overlay screenshot", e);
        }
    }

    private static FpsTracker getActiveTracker() {
        try {
            Field trackerField = PerformanceOverlayClient.class.getDeclaredField("tracker");
            trackerField.setAccessible(true);
            FpsTracker tracker = (FpsTracker) trackerField.get(null);

            if (tracker == null) {
                throw new AssertionError("Performance Overlay tracker was not initialized");
            }
            return tracker;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not inspect the active Performance Overlay tracker", e);
        }
    }

    private static int getHistorySize(FpsTracker tracker) {
        try {
            Field sizeField = FpsTracker.class.getDeclaredField("size");
            sizeField.setAccessible(true);
            return sizeField.getInt(tracker);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not inspect Performance Overlay frame history", e);
        }
    }

    private static long getLongField(FpsTracker tracker, String fieldName) {
        try {
            Field field = FpsTracker.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getLong(tracker);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not inspect Performance Overlay field: " + fieldName, e);
        }
    }

    private static String getStringField(FpsTracker tracker, String fieldName) {
        try {
            Field field = FpsTracker.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (String) field.get(tracker);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not inspect Performance Overlay field: " + fieldName, e);
        }
    }

    private static boolean containsPositiveFps(String text) {
        int valueStart = text.indexOf("FPS: ");
        if (valueStart < 0) {
            return false;
        }

        valueStart += "FPS: ".length();
        int valueEnd = valueStart;
        while (valueEnd < text.length() && Character.isDigit(text.charAt(valueEnd))) {
            valueEnd++;
        }

        if (valueEnd == valueStart) {
            return false;
        }
        return Integer.parseInt(text.substring(valueStart, valueEnd)) > 0;
    }

    private record OverlayBounds(int x, int y, int width, int height, int screenWidth, int screenHeight) {
    }
}
