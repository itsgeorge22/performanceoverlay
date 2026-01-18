package com.itsgeorge.performanceoverlay.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class PerformanceOverlayModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> PerformanceOverlayConfigScreen.build(parent);
    }
}