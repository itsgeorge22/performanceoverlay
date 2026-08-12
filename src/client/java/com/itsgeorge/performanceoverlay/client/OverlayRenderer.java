package com.itsgeorge.performanceoverlay.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class OverlayRenderer {
    private static final WidthCache WIDTH_CACHE = new WidthCache();

    private OverlayRenderer() {
    }

    public static void render(GuiGraphics g, OverlayConfig cfg, FpsTracker.Snapshot snapshot) {
        if (!cfg.enabled) {
            return;
        }
        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        float scale = clamp(cfg.scale, 0.50f, 2.00f);

        int lineCount = snapshot.count();
        int lineH = font.lineHeight;

        int maxW;
        if (WIDTH_CACHE.matches(font, snapshot)) {
            maxW = WIDTH_CACHE.maxWidth();
        } else {
            maxW = 0;
            for (int i = 0; i < lineCount; i++) {
                String line = snapshot.lines()[i];
                if (line != null && !line.isEmpty()) {
                    maxW = Math.max(maxW, font.width(line));
                }
            }
            WIDTH_CACHE.update(font, snapshot, maxW);
        }

        int spacingPx = (lineCount > 1) ? Math.max(0, cfg.lineSpacingPx) : 0;

        float totalWPx = maxW * scale;
        float totalHPx = lineCount * (lineH * scale) + (lineCount - 1) * spacingPx;

        int wPx = Math.round(totalWPx);
        int hPx = Math.round(totalHPx);

        int xPx = switch (cfg.position) {
            case TOP_LEFT, BOTTOM_LEFT -> cfg.offsetX;
            case TOP_RIGHT, BOTTOM_RIGHT -> Math.max(0, screenW - cfg.offsetX - wPx);
            case TOP_CENTER, BOTTOM_CENTER -> Math.max(0, (screenW - wPx) / 2 + cfg.offsetX);
        };

        int yPx = switch (cfg.position) {
            case TOP_LEFT, TOP_RIGHT, TOP_CENTER -> cfg.offsetY;
            case BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM_CENTER -> Math.max(0, screenH - cfg.offsetY - hPx);
        };

        float x = xPx / scale;
        float y = yPx / scale;
        float spacingUnscaled = spacingPx / scale;

        g.pose().pushMatrix();
        g.pose().scale(scale, scale);

        int color = snapshot.color();

        for (int i = 0; i < lineCount; i++) {
            String line = snapshot.lines()[i];
            if (line == null || line.isEmpty()) {
                continue;
            }

            float yy = y + i * (lineH + spacingUnscaled);
            g.drawString(font, line, Math.round(x), Math.round(yy), color, true);
        }

        g.pose().popMatrix();
    }

    private static float clamp(float v, float min, float max) {
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }

    static final class WidthCache {
        private Object fontIdentity;
        private Object snapshotIdentity;
        private int maxWidth;

        boolean matches(Object font, Object snapshot) {
            return fontIdentity == font && snapshotIdentity == snapshot;
        }

        void update(Object font, Object snapshot, int width) {
            fontIdentity = font;
            snapshotIdentity = snapshot;
            maxWidth = width;
        }

        int maxWidth() {
            return maxWidth;
        }
    }
}
