package com.itsgeorge.performanceoverlay.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OverlayRendererTest {
    @Test
    void widthCacheMatchesOnlyTheMeasuredSnapshotAndFontInstances() {
        OverlayRenderer.WidthCache cache = new OverlayRenderer.WidthCache();
        Object font = new Object();
        Object snapshot = new Object();

        assertFalse(cache.matches(font, snapshot));

        cache.update(font, snapshot, 123);

        assertTrue(cache.matches(font, snapshot));
        assertEquals(123, cache.maxWidth());
        assertFalse(cache.matches(new Object(), snapshot));
        assertFalse(cache.matches(font, new Object()));
    }

    @Test
    void widthCacheAcceptsUpdatedMeasurementAfterInvalidation() {
        OverlayRenderer.WidthCache cache = new OverlayRenderer.WidthCache();
        Object font = new Object();

        cache.update(font, new Object(), 100);
        Object replacementSnapshot = new Object();
        cache.update(font, replacementSnapshot, 140);

        assertTrue(cache.matches(font, replacementSnapshot));
        assertEquals(140, cache.maxWidth());
    }
}
