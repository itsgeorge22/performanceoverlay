package com.itsgeorge.performanceoverlay.gametest;

import com.itsgeorge.performanceoverlay.PerformanceOverlayClient;
import com.itsgeorge.performanceoverlay.client.FpsTracker;
import com.itsgeorge.performanceoverlay.client.OverlayConfig;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;

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
        }
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
}
