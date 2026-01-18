package com.itsgeorge.performanceoverlay.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "performanceoverlay.json";

    private ConfigIO() {
    }

    public static OverlayConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

        if (!Files.exists(path)) {
            OverlayConfig cfg = new OverlayConfig();
            save(cfg);
            return cfg;
        }

        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            OverlayConfig cfg = GSON.fromJson(json, OverlayConfig.class);
            return cfg != null ? cfg : new OverlayConfig();
        } catch (IOException e) {
            return new OverlayConfig();
        }
    }

    public static void save(OverlayConfig cfg) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(cfg), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}