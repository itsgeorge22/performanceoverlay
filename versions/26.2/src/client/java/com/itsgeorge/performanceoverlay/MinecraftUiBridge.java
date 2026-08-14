package com.itsgeorge.performanceoverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class MinecraftUiBridge {
    private MinecraftUiBridge() {
    }

    static void setOverlayMessage(Minecraft client, Component message) {
        client.gui.hud.setOverlayMessage(message, false);
    }
}
