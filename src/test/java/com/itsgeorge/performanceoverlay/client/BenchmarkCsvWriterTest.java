package com.itsgeorge.performanceoverlay.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BenchmarkCsvWriterTest {
    @Test
    void formatsRowsAndFooterOnDedicatedWriterThread() throws Exception {
        TrackingWriter output = new TrackingWriter();
        BenchmarkCsvWriter writer = new BenchmarkCsvWriter(output, 4);

        writer.enqueue(row(25, 16_666_667));
        writer.finish(
                FpsTracker.BenchmarkEndReason.MANUAL,
                1,
                1,
                new FpsTracker.BenchmarkSummary(60, 55, 50, 1, 100, 16.666667)
        );

        assertEquals(
                "25,16.667,60.0,59.5,58.5,55.5,50.5,1,25,16.667,2.0,512,8192\n" +
                        "# EndReason: MANUAL\n" +
                        "# SUMMARY\n" +
                        "# FramesLogged: 1\n" +
                        "# FramesSummary: 1\n" +
                        "# AvgFPS: 60.0\n" +
                        "# Low1FPS: 55.0\n" +
                        "# Low01FPS: 50.0\n" +
                        "# Stutters: 1\n" +
                        "# StutterPercent: 100\n" +
                        "# MaxSpikeMs: 16.7\n",
                output.toString()
        );
        assertEquals("PerformanceOverlay-BenchmarkWriter", output.threadName());
    }

    @Test
    void fullQueueFailsInsteadOfBlockingFrameCapture() throws Exception {
        BlockingWriter output = new BlockingWriter();
        BenchmarkCsvWriter writer = new BenchmarkCsvWriter(output, 1);

        try {
            writer.enqueue(row(1, 16_000_000));
            assertTrue(output.awaitWriteStarted());
            writer.enqueue(row(2, 16_000_000));

            IOException error = assertThrows(IOException.class, () -> writer.enqueue(row(3, 16_000_000)));
            assertTrue(error.getMessage().contains("could not keep up"));
        } finally {
            output.release();
            writer.abort();
        }
    }

    @Test
    void workerWriteFailureIsReturnedWhenBenchmarkFinishes() throws Exception {
        BenchmarkCsvWriter writer = new BenchmarkCsvWriter(new FailingWriter(), 4);
        writer.enqueue(row(1, 16_000_000));

        IOException error = assertThrows(
                IOException.class,
                () -> writer.finish(
                        FpsTracker.BenchmarkEndReason.MANUAL,
                        1,
                        1,
                        new FpsTracker.BenchmarkSummary(0, 0, 0, 0, 0, 0)
                )
        );

        assertTrue(error.getMessage().contains("disk full"));
    }

    private static BenchmarkCsvWriter.BenchmarkRow row(long elapsedMs, long frameNs) {
        return new BenchmarkCsvWriter.BenchmarkRow(
                elapsedMs,
                frameNs,
                59.5,
                58.5,
                55.5,
                50.5,
                1,
                25,
                16.666667,
                2,
                512,
                8192
        );
    }

    private static final class TrackingWriter extends StringWriter {
        private volatile String threadName;

        @Override
        public void write(String str) {
            threadName = Thread.currentThread().getName();
            super.write(str);
        }

        String threadName() {
            return threadName;
        }
    }

    private static final class BlockingWriter extends Writer {
        private final CountDownLatch writeStarted = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        @Override
        public void write(char[] buffer, int offset, int length) throws IOException {
            writeStarted.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("interrupted", e);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        boolean awaitWriteStarted() throws InterruptedException {
            return writeStarted.await(2, TimeUnit.SECONDS);
        }

        void release() {
            release.countDown();
        }
    }

    private static final class FailingWriter extends Writer {
        @Override
        public void write(char[] buffer, int offset, int length) throws IOException {
            throw new IOException("disk full");
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
