package com.itsgeorge.performanceoverlay.client;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

final class ConfigScreenCopyTest {
    @Test
    void configurationScreenCopiesEveryConfigField() throws IllegalAccessException {
        OverlayConfig source = new OverlayConfig();
        assignNonDefaultValues(source);

        OverlayConfig copy = PerformanceOverlayConfigScreen.copy(source);

        assertNotSame(source, copy);
        for (Field field : OverlayConfig.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                assertEquals(field.get(source), field.get(copy), "Field was not copied: " + field.getName());
            }
        }
    }

    private static void assignNonDefaultValues(OverlayConfig config) throws IllegalAccessException {
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
}
