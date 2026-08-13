package com.itsgeorge.performanceoverlay.gametest;

import com.itsgeorge.performanceoverlay.PerformanceOverlayClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;

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
    }

    private static void assertModLoaded(String modId) {
        if (!FabricLoader.getInstance().isModLoaded(modId)) {
            throw new AssertionError("Required mod was not loaded: " + modId);
        }
    }
}
