package com.itsgeorge.performanceoverlay.client;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class BenchmarkCsvWriter {
    static final int DEFAULT_QUEUE_CAPACITY = 4096;

    private final Writer writer;
    private final ArrayBlockingQueue<Command> queue;
    private final CountDownLatch completed = new CountDownLatch(1);
    private final Thread worker;

    private volatile IOException failure;
    private volatile boolean acceptingRows = true;

    BenchmarkCsvWriter(Writer writer) {
        this(writer, DEFAULT_QUEUE_CAPACITY);
    }

    BenchmarkCsvWriter(Writer writer, int queueCapacity) {
        this.writer = writer;
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.worker = new Thread(this::writeLoop, "PerformanceOverlay-BenchmarkWriter");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    void enqueue(BenchmarkRow row) throws IOException {
        IOException currentFailure = failure;
        if (currentFailure != null) {
            throw currentFailure;
        }
        if (!acceptingRows) {
            throw new IOException("The benchmark writer is no longer accepting frames");
        }
        if (!queue.offer(row)) {
            throw new IOException("The benchmark writer could not keep up with frame capture");
        }
    }

    void finish(
            FpsTracker.BenchmarkEndReason endReason,
            long framesLogged,
            int framesSummary,
            FpsTracker.BenchmarkSummary summary
    ) throws IOException {
        acceptingRows = false;
        FooterCommand footer = new FooterCommand(endReason, framesLogged, framesSummary, summary);

        try {
            while (!queue.offer(footer, 50, TimeUnit.MILLISECONDS)) {
                IOException currentFailure = failure;
                if (currentFailure != null) {
                    throw currentFailure;
                }
                if (completed.getCount() == 0) {
                    throw completedWithoutResult();
                }
            }
            completed.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            abort();
            throw new IOException("Interrupted while finalizing the benchmark CSV", e);
        }

        IOException currentFailure = failure;
        if (currentFailure != null) {
            throw currentFailure;
        }
    }

    void abort() {
        acceptingRows = false;
        worker.interrupt();
    }

    IOException failure() {
        return failure;
    }

    private void writeLoop() {
        int flushCounter = 0;
        try {
            while (true) {
                Command command = queue.take();
                if (command instanceof BenchmarkRow row) {
                    writeRow(row);
                    flushCounter++;
                    if (flushCounter >= 120) {
                        writer.flush();
                        flushCounter = 0;
                    }
                    continue;
                }

                FooterCommand footer = (FooterCommand) command;
                FpsTracker.writeBenchmarkFooter(
                        writer,
                        footer.endReason(),
                        footer.framesLogged(),
                        footer.framesSummary(),
                        footer.summary()
                );
                writer.flush();
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            failure = e;
        } finally {
            acceptingRows = false;
            try {
                writer.close();
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                }
            }
            completed.countDown();
        }
    }

    private void writeRow(BenchmarkRow row) throws IOException {
        double frameMs = (double) row.frameNs() / 1_000_000.0;
        double instFps = 1_000_000_000.0 / (double) row.frameNs();

        writer.write(
                row.elapsedMs() + "," +
                        FpsTracker.ms3(frameMs) + "," +
                        FpsTracker.f1(instFps) + "," +
                        FpsTracker.f1(row.fpsSmoothed()) + "," +
                        FpsTracker.f1(row.avgFps()) + "," +
                        FpsTracker.f1(row.low1Fps()) + "," +
                        FpsTracker.f1(row.low01Fps()) + "," +
                        row.stutters() + "," +
                        row.stutterPercent() + "," +
                        FpsTracker.ms3(row.maxSpikeMs()) + "," +
                        (row.gcTimeDeltaSampled() ? FpsTracker.ms1((double) row.gcTimeDeltaMs()) : "") + "," +
                        row.memUsedMb() + "," +
                        row.memMaxMb() + "\n"
        );
    }

    private IOException completedWithoutResult() {
        IOException currentFailure = failure;
        return currentFailure != null
                ? currentFailure
                : new IOException("The benchmark writer stopped before finalizing the CSV");
    }

    record BenchmarkRow(
            long elapsedMs,
            long frameNs,
            double fpsSmoothed,
            double avgFps,
            double low1Fps,
            double low01Fps,
            int stutters,
            int stutterPercent,
            double maxSpikeMs,
            long gcTimeDeltaMs,
            boolean gcTimeDeltaSampled,
            long memUsedMb,
            long memMaxMb
    ) implements Command {
    }

    private sealed interface Command permits BenchmarkRow, FooterCommand {
    }

    private record FooterCommand(
            FpsTracker.BenchmarkEndReason endReason,
            long framesLogged,
            int framesSummary,
            FpsTracker.BenchmarkSummary summary
    ) implements Command {
    }
}
