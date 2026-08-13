package com.itsgeorge.performanceoverlay.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ConfigPersistenceTest {
    @TempDir
    Path tempDir;

    @Test
    void everyConfigFieldPersistsAndReopensInTheScreenCopy() throws IllegalAccessException {
        OverlayConfig expected = new OverlayConfig();
        assignValidNonDefaultValues(expected);
        Path path = tempDir.resolve("nested/performanceoverlay.json");

        ConfigIO.save(path, expected);
        OverlayConfig loaded = ConfigIO.load(path);
        OverlayConfig reopenedScreenCopy = PerformanceOverlayConfigScreen.copy(loaded);

        assertAllFieldsEqual(expected, loaded);
        assertAllFieldsEqual(expected, reopenedScreenCopy);
    }

    private static void assignValidNonDefaultValues(OverlayConfig config) throws IllegalAccessException {
        for (Field field : OverlayConfig.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            Class<?> type = field.getType();
            if (type == boolean.class) {
                field.setBoolean(config, !field.getBoolean(config));
            } else if (type == int.class) {
                field.setInt(config, field.getInt(config) + 1);
            } else if (type == float.class) {
                field.setFloat(config, field.getFloat(config) + 0.25f);
            } else if (type.isEnum()) {
                Object[] values = type.getEnumConstants();
                Object current = field.get(config);
                int currentIndex = 0;
                while (values[currentIndex] != current) {
                    currentIndex++;
                }
                field.set(config, values[(currentIndex + 1) % values.length]);
            } else {
                throw new AssertionError("Unhandled configuration field type: " + field);
            }
        }
    }

    private static void assertAllFieldsEqual(OverlayConfig expected, OverlayConfig actual)
            throws IllegalAccessException {
        for (Field field : OverlayConfig.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                assertEquals(field.get(expected), field.get(actual), "Field did not persist: " + field.getName());
            }
        }
    }
}
