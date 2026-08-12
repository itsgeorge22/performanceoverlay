# METRICS.md

## Scope

This document defines the metric semantics implemented in the current repository snapshot. It describes what Performance Overlay measures, not what an ideal profiler could measure.

## Frame sample

For each eligible HUD render callback:

```text
nowNs = System.nanoTime()
dtNs = nowNs - previousEligibleCallbackNs
```

The first eligible callback after initialization or reset has no previous timestamp and is discarded.

`dtNs` is therefore the interval between eligible Performance Overlay HUD callbacks. It is not GPU presentation time and is not read from Minecraft's internal frame timer.

The sample is stored with the timestamp captured near the start of `FpsTracker.onFrame()`, which the tracker uses as its window timestamp.

If the overlay is disabled, the HUD callback returns before `FpsTracker.onFrame()` is called, so no frame samples are collected.

## Pause handling

### `FREEZE`

Paused callbacks are excluded from measurement. While paused, the previous-frame timestamp is advanced to the current time so the pause duration does not become the next measured frame.

Cached statistics remain visible.

### `RESET`

On entering pause, rolling statistics are reset. Paused callbacks are excluded and the previous-frame timestamp is advanced while paused.

### `TRACK`

Paused callback intervals continue to be measured normally.

## Instantaneous FPS

Used in benchmark CSV rows:

```text
instantaneousFps = 1_000_000_000 / dtNs
```

## Displayed FPS

At the configured FPS refresh cadence, samples with timestamps inside `fpsWindowMs` are scanned.

With at least two samples:

```text
displayedFps = sampleCount * 1_000_000_000 / sumFrameNs
```

With fewer than two samples, displayed FPS falls back to instantaneous FPS from the latest frame.

This is a throughput calculation over elapsed frame time, not the arithmetic mean of individual FPS values.

## Displayed frametime

The frametime display uses the same `fpsWindowMs` sample set as displayed FPS.

With at least two samples:

```text
frametimeMs = (sumFrameNs / sampleCount) / 1_000_000
```

With fewer than two samples, it uses the latest `dtNs`.

The displayed value is therefore normally a rolling mean frametime rather than the latest raw frame interval.

## Average FPS

Within `avgWindowSec`:

```text
averageFps = sampleCount * 1_000_000_000 / sumFrameNs
```

The current implementation returns `0` until at least two samples exist in the window.

## 1% low and 0.1% low

The tracker copies frame durations from the configured rolling window into a reusable scratch buffer.

Two methods are implemented.

### `PERCENTILE`

Default method.

For 1% low:

```text
p = 0.99
index = ceil(N * p) - 1
selectedFrameNs = Nth frame duration at index
lowFps = 1_000_000_000 / selectedFrameNs
```

For 0.1% low:

```text
p = 0.999
index = ceil(N * p) - 1
selectedFrameNs = Nth frame duration at index
lowFps = 1_000_000_000 / selectedFrameNs
```

Selection uses in-place quickselect on the scratch array rather than full sorting.

### `MEAN_WORST`

For 1% low:

```text
k = max(1, ceil(N * 0.01))
```

For 0.1% low:

```text
k = max(1, ceil(N * 0.001))
```

The arithmetic mean of the `k` largest frame durations is calculated, then converted to FPS:

```text
lowFps = 1_000_000_000 / meanWorstFrameNs
```

This is not the arithmetic mean of the worst individual FPS values.

## Stutters

Within `stutterWindowSec`, a sample is counted as a stutter when:

```text
frameNs >= max(1, stutterThresholdMs) * 1_000_000
```

Stutter percentage is:

```text
round(stutterCount * 100 / sampleCount)
```

## Maximum spike

Within the same stutter window:

```text
maxSpikeMs = maximumFrameNs / 1_000_000
```

## Garbage collection metric

When GC display is enabled, the tracker polls approximately once per second.

It sums positive `getCollectionTime()` values from all `GarbageCollectorMXBean` instances and subtracts the previously observed total.

Conceptually:

```text
currentTotalGcMs = sum(all collector cumulative collection times)
gcDeltaMs = max(0, currentTotalGcMs - previousTotalGcMs)
```

The displayed value is therefore aggregate JVM GC collection time accumulated between polls. It is not the duration of the most recent individual GC pause.

Current limitation: the first positive sample can include GC time accumulated before Performance Overlay's first poll because the previous total starts at zero.

A non-positive result is stored internally as unavailable and currently renders as `GC: NaN`.

## Memory metric

When memory display is enabled, the tracker polls every 250 ms.

```text
usedBytes = Runtime.totalMemory() - Runtime.freeMemory()
maxBytes = Runtime.maxMemory()
```

Both values are divided by `1024 * 1024` using integer division.

These values represent JVM heap usage and configured maximum heap in MiB-equivalent binary units. They do not represent total process memory, native memory, GPU memory, direct buffers, or thread stacks.

## Rolling history

The tracker stores parallel circular arrays for sample timestamp and frame duration plus a scratch array for selection work.

Capacity is derived from the largest configured history window:

```text
desiredCapacity = largestWindowSeconds * 1200
```

then clamped to:

```text
6000 <= capacity <= 240000 samples
```

Arrays grow when required and do not shrink. A growth reallocates the arrays and resets rolling statistics.

Samples older than the largest configured time window are pruned. If the ring becomes full first, the oldest sample is overwritten.

## Update cadence

Metric values are cached and refreshed independently.

Configured update intervals are clamped by the tracker:

- FPS and frametime: 50–5000 ms
- Average, lows, stutters: 100–10000 ms
- GC: fixed 1000 ms poll
- Memory: fixed 250 ms poll

A benchmark CSV row can therefore contain rolling values that were calculated on an earlier frame.

## Benchmark raw frame capture

When a benchmark is active, every measured `dtNs` is appended to a dedicated full-run frame array in addition to the rolling history.

The array starts at 6000 samples and doubles up to a hard maximum of 5,000,000 retained samples.

CSV logging can continue after this in-memory summary cap is reached, but additional frames are not retained for the final full-run summary.

## Benchmark CSV row semantics

Columns are:

```text
elapsed_ms
frame_ms
inst_fps
fps_smoothed
avg_fps
low1_fps
low01_fps
stutters
stutter_percent
max_spike_ms
gc_pause_ms
mem_used_mb
mem_max_mb
```

`frame_ms` and `inst_fps` are derived from the current raw frame sample.

`fps_smoothed`, `avg_fps`, lows, stutters, spike, GC, and memory use the tracker's cached rolling values.

Current limitation: several cached values are only refreshed when their overlay metric is visible or required for color selection. CSV completeness therefore currently depends on UI/configuration state.

GC and memory sampling occurs after the benchmark row is written in `onFrame()`, so a row contains the previously cached GC and memory values.

## Benchmark full-run summary

At stop, summary metrics are recalculated from the retained full-run frame durations.

Average FPS:

```text
avgFps = retainedFrameCount * 1_000_000_000 / retainedTotalFrameNs
```

1% low and 0.1% low use the currently configured low method over all retained benchmark frames.

Stutters use the currently configured stutter threshold over all retained benchmark frames.

Maximum spike is the maximum retained benchmark frame duration.

Current limitation: the benchmark does not snapshot all metric-defining settings at start. A live settings change during a run can therefore make final-summary semantics differ from the metadata written at benchmark start.

## Reset semantics

`FpsTracker.reset()` clears rolling history, metric update timestamps, cached values, and visible snapshot state.

It does not clear an active benchmark's dedicated full-run frame collection.
