package com.itsgeorge.performanceoverlay.gametest;

import com.itsgeorge.performanceoverlay.PerformanceOverlayClient;
import com.itsgeorge.performanceoverlay.client.FpsTracker;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

@SuppressWarnings("UnstableApiUsage")
public final class PerformanceOverlayStartupClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            assertModLoaded("performanceoverlay");
            assertModLoaded("fabric-api");
            assertModLoaded("cloth-config");

            if (PerformanceOverlayClient.getConfig() == null) {
                throw new AssertionError("Performance Overlay client entrypoint did not initialize its configuration");
            }
        });

        FpsTracker tracker = context.computeOnClient(client -> getActiveTracker());
        context.getInput().pressKey(GLFW.GLFW_KEY_F10);
        context.waitTicks(2);
        if (tracker.isBenchmarkActive()) {
            throw new AssertionError("F10 started a benchmark while no world was loaded");
        }
    }

    private static void assertModLoaded(String modId) {
        if (!FabricLoader.getInstance().isModLoaded(modId)) {
            throw new AssertionError("Required mod was not loaded: " + modId);
        }
    }

    private static FpsTracker getActiveTracker() {
        try {
            Field trackerField = PerformanceOverlayClient.class.getDeclaredField("tracker");
            trackerField.setAccessible(true);
            return (FpsTracker) trackerField.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not inspect the active Performance Overlay tracker", e);
        }
    }
}
