# ARCHITECTURE.md

## Scope

This document describes the confirmed architecture of the current Performance Overlay repository snapshot.

Performance Overlay is a Fabric client-only mod. Runtime behavior is small, static, and event-driven.

## Source layout

Main runtime code is under:

```text
src/client/java/com/itsgeorge/performanceoverlay/
```

Important classes:

- `BenchmarkCsvWriter` — bounded background queue, CSV row formatting, ordered writes, flushing, and finalization.
- `PerformanceOverlayClient` — Fabric client entry point and runtime orchestration.
- `FpsTracker` — frame sampling, rolling history, metric calculation, cached display snapshots, benchmark capture/export, GC sampling, and memory sampling.
- `OverlayRenderer` — overlay measurement, anchoring, scaling, and text drawing.
- `OverlayConfig` — mutable configuration model and enums.
- `ConfigIO` — Gson JSON persistence.
- `PerformanceOverlayConfigScreen` — Cloth Config UI.
- `PerformanceOverlayModMenu` — Mod Menu integration.

The repository also contains example mixin source/config remnants. `fabric.mod.json` currently declares no `mixins` property, so those mixins are not registered by the mod metadata.

Automated tests are under `src/test/java`. They exercise production metric calculations, pause handling, benchmark lifecycle behavior, and CSV structure with deterministic frame timestamps and durations through JUnit Jupiter.

## Runtime initialization

`PerformanceOverlayClient.onInitializeClient()`:

1. Loads `OverlayConfig` through `ConfigIO`.
2. Creates one `FpsTracker`.
3. Registers four key bindings.
4. Registers an `END_CLIENT_TICK` callback for user actions and benchmark lifecycle control.
5. Attaches the Performance Overlay HUD element immediately before vanilla chat.
6. Registers play-connection disconnect and client-shutdown benchmark finalization.

## Runtime flow

### HUD/render path

```text
Fabric HUD callback
    -> return immediately if both overlay and benchmark are inactive
    -> read Minecraft pause state
    -> FpsTracker.onFrame(paused)
        -> measure callback interval
        -> push/prune rolling frame history
        -> refresh due cached metrics
        -> poll GC/memory when due
        -> queue benchmark frame values if active
        -> rebuild cached Snapshot if a visible/cached metric changed
    -> render Snapshot only if overlay is enabled
```

Normal sampling is coupled to the enabled overlay. An active benchmark continues sampling through the HUD callback if the overlay is disabled through F7 or the settings screen.

Frame measurement, rolling metric work, lightweight benchmark value capture, snapshot generation, and overlay rendering are executed synchronously on the client/render path used by the HUD callback. Benchmark CSV formatting and file writes run on a dedicated background writer thread.

### Client-tick path

```text
END_CLIENT_TICK
    -> benchmark progress action bar
    -> benchmark auto-stop
    -> F7 overlay toggle
    -> F9 rolling-stat reset
    -> F10 benchmark start/stop
    -> F8 layout cycle
```

Configuration changes made through these key actions are persisted with `ConfigIO.save()` where applicable.

Resetting rolling statistics also establishes a fresh cumulative GC baseline, so the next GC delta excludes collection time from before the reset or while normal overlay sampling was disabled.

## Key bindings

Default keys:

- F7 — enable/disable overlay
- F8 — cycle text layout
- F9 — reset rolling statistics
- F10 — start/stop benchmark

Layouts cycle:

```text
ONE_LINE -> THREE_LINES -> COLUMN -> ONE_LINE
```

F10 starts or stops benchmarks regardless of whether the overlay is visible, but an inactive benchmark can start only while a world is loaded. Pressing F10 in menus does nothing and displays no message.

Leaving a single-player world or multiplayer server finalizes an active benchmark with `WORLD_LEFT`. Joining another world does not restart it. Dimension changes keep the same play connection, so benchmarks continue across Overworld, Nether, and End transitions.

Automatic stops force a final 100% progress update. If an automatic stop or benchmark write-error report and an F10 press occur during the same client tick, the F10 press is consumed without starting another benchmark.

F9 does nothing while the overlay is disabled. While the overlay is visible, F9 is blocked with a warning during an active benchmark because resetting only the rolling statistics would make the CSV's rolling columns inconsistent with its uninterrupted full-run data. It works normally after the benchmark ends.

Disabling the overlay through F7 or the settings screen hides only the display during an active benchmark. Sampling, progress, F10 manual stop, and automatic stop continue normally.

## `FpsTracker`

`FpsTracker` is the central measurement component.

It currently owns four related responsibilities:

1. frame interval collection,
2. rolling metric calculation and display cache,
3. benchmark capture/export/summary,
4. GC and JVM heap sampling.

### Rolling storage

Parallel circular arrays store:

```text
timeNs[]   sample timestamp
frameNs[]  sampled callback interval
scratch[]  reusable calculation workspace
```

History is time-pruned using the largest configured metric window. Capacity is also bounded by a sample-count heuristic documented in `METRICS.md`.

### Cached metrics

Metrics are not recalculated every frame. Each metric has its own last-update timestamp and refresh cadence.

`Snapshot` is a record containing formatted display lines, line count, and one color for the whole overlay. Its `String[]` line array is exposed by the record accessor and is therefore not deeply immutable.

Snapshot text is rebuilt when cached values change.

### Benchmark state

Benchmark state is stored directly in `FpsTracker`:

- active flag,
- start timestamp,
- bounded background CSV writer queue,
- file name/path,
- frame/write counters,
- dedicated full-run frame array,
- retained total duration and maximum frame,
- an immutable snapshot of measurement settings for the active run,
- last completed summary.

Each measured benchmark frame creates one numeric row snapshot and submits it to a bounded 4,096-row queue without waiting. A dedicated daemon thread formats queued values as CSV and writes them in capture order.

The background writer is flushed every 120 logged frames. Normal stop waits for the bounded queue to drain, writes the summary footer, flushes, and closes the file before reporting that it was saved. If the queue fills, the benchmark aborts instead of blocking frame rendering.

If row submission fails or the background writer reports an I/O failure, the tracker stops the run and queues one error status. The client consumes that status on the next client tick, clears benchmark progress and auto-stop state, consumes any F10 press from that tick, and shows the player an error with the incomplete file path when available. Start, manual-stop, and auto-stop finalization failures use the same visible error presentation.

The Fabric play-connection disconnect event finalizes an active benchmark when its world or server is left. The client-stopping event finalizes any run still active during normal game shutdown. Every successfully finalized CSV records an `EndReason` value for manual stop, automatic duration, world departure, or game shutdown. Forced process termination, power loss, and crashes that bypass lifecycle events can still leave a partial file.

## Benchmark files

Benchmark files are created under:

```text
<config-dir>/performanceoverlay/benchmarks/
```

with names of the form:

```text
benchmark_yyyyMMdd_HHmmss.csv
```

If that name already exists, the tracker atomically creates a numbered variant such as `benchmark_yyyyMMdd_HHmmss_2.csv` rather than overwriting the earlier file.

The file contains start-time metadata, per-frame CSV rows, then an end reason and summary footer on successful finalization.

The start metadata records the captured duration, pause handling, low method, stutter settings, rolling windows, and metric update intervals. Those measurement settings remain fixed until the run stops, while display-only configuration remains live.

The CSV writes `gc_time_delta_ms` only on frames with a fresh GC poll. Intermediate rows leave the cell blank so cached display values are not duplicated in exported data.

## Rendering

`OverlayRenderer.render()`:

1. obtains scaled GUI dimensions and the Minecraft font,
2. clamps configured scale to 0.5–2.0,
3. reuses the cached maximum text width while the snapshot and font instances are unchanged, otherwise measures every non-empty line with `font.width()`,
4. calculates total rendered dimensions,
5. computes an anchor from `OverlayPosition`, offsets, and dimensions,
6. applies a pose scale,
7. draws each line with shadow,
8. restores the pose.

Supported anchors:

- top-left
- top-center
- top-right
- bottom-left
- bottom-center
- bottom-right

The renderer currently has no background panel or clipping stage. One threshold-derived color is applied to every line in the snapshot.

Screen dimensions, scale, spacing, offsets, and anchoring remain live every frame; only the font-dependent maximum line width is cached.

The HUD element is attached before vanilla chat so chat is rendered after it.

## Configuration

`OverlayConfig` is a mutable public-field configuration object.

It contains:

- overlay enable state,
- metric visibility,
- position/layout/scale,
- presets,
- benchmark duration,
- metric refresh intervals,
- metric history windows,
- stutter settings,
- low calculation method,
- pause handling,
- threshold color configuration.

`ConfigIO` persists this object as pretty-printed Gson JSON at:

```text
<config-dir>/performanceoverlay.json
```

If the file does not exist, defaults are created and saved.

Reads and writes use the whole JSON file. Loaded values are validated against the same ranges used by the configuration screen, missing enum values receive defaults, and malformed JSON falls back to the default configuration. Write I/O errors are still ignored, and no schema migration layer is implemented.

Configuration UI text uses sentence case, retains uppercase technical acronyms such as FPS and GC, and displays enum choices as readable words with spaces rather than serialized identifiers. Serialized JSON enum names remain unchanged for compatibility.

Default, Responsive, and Smooth presets apply their complete controlled value set when newly selected and remain selected after saving. Editing a value controlled by the active preset changes it to Custom. The Default preset uses a 10-second average FPS window.

## Threading model

A dedicated daemon thread owns benchmark CSV row formatting and file writes. The client/render path communicates with it through a bounded thread-safe queue. Benchmark start writes and flushes metadata before measurement begins; benchmark stop waits for queued rows and finalization before reporting success.

`FpsTracker`, `OverlayConfig`, and other benchmark state remain client-thread-owned. Only immutable numeric row snapshots cross to the writer thread.

## Build/runtime metadata

Confirmed from the repository snapshot:

- Java 21
- Minecraft build target: 1.21.11
- Fabric Loader: 0.18.4
- Fabric API: 0.141.1+1.21.11
- mod version: 1.0.1
- declared Minecraft compatibility in `fabric.mod.json`: `>=1.21.9 <=1.21.11`
- environment: `client`
- Cloth Config is required
- Mod Menu is recommended

The build targets Minecraft 1.21.11; the same JAR has been manually verified on Minecraft 1.21.9 and 1.21.10. Minecraft 1.21.8 fails during client initialization because its key-binding API is incompatible.

## Current architectural constraints / known risks

These are confirmed characteristics of the current implementation, not automatically approved redesign targets:

- Normal frame measurement depends on the enabled HUD callback; an active benchmark keeps the callback's measurement path running without drawing the overlay.
- Benchmark capture still allocates one numeric row snapshot and performs one non-blocking queue submission per measured frame.
- Rolling benchmark CSV columns reuse cached overlay metrics.
- Outside benchmarks, some cached metric calculation is conditional on visibility or color-target needs.
