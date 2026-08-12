package com.itsgeorge.performanceoverlay.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BenchmarkFileTest {
    @TempDir
    Path directory;

    @Test
    void sameTimestampCreatesNumberedFilesWithoutOverwriting() throws Exception {
        LocalDateTime timestamp = LocalDateTime.of(2026, 8, 12, 21, 30, 45);

        try (FpsTracker.BenchmarkOutput first = FpsTracker.createBenchmarkOutput(directory, timestamp)) {
            first.writer().write("first benchmark");
            assertEquals("benchmark_20260812_213045.csv", first.path().getFileName().toString());
        }

        try (FpsTracker.BenchmarkOutput second = FpsTracker.createBenchmarkOutput(directory, timestamp)) {
            second.writer().write("second benchmark");
            assertEquals("benchmark_20260812_213045_2.csv", second.path().getFileName().toString());
        }

        assertEquals("first benchmark", Files.readString(directory.resolve("benchmark_20260812_213045.csv")));
        assertEquals("second benchmark", Files.readString(directory.resolve("benchmark_20260812_213045_2.csv")));
        assertTrue(Files.exists(directory.resolve("benchmark_20260812_213045.csv")));
    }
}
