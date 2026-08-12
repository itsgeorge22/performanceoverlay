# Changelog

## 1.0.2 [WIP]
- Corrected settings tooltips for hidden-overlay benchmarking and GC time reporting.
- Fixed configuration presets being overwritten on save and aligned Default with its 10-second average window.
- Added complete benchmark summary tests and included stutter percentage in the CSV summary.
- Made F9 inactive while the overlay is hidden and prevented rolling statistics from being reset during active benchmarks.
- Made benchmark start, measurement, and stopping independent of whether the overlay is visible.
- Finalized active benchmarks during normal game shutdown and recorded why each run ended.
- Added immediate, user-friendly notifications for benchmark CSV write and finalization failures.
- Prevented benchmarks started within the same second from overwriting existing CSV files.
- Standardized configuration labels and replaced internal enum names with readable options.
- Added validation and safe defaults for malformed, missing, or out-of-range configuration values.
- Fixed the missing separator before Max Spike in the three-line overlay layout.
- Clarified GC time-delta reporting and show zero when no collection time accumulated between polls.
- Added automated tests for metrics, pause handling, benchmark lifecycle, captured settings, and CSV structure.
- Fixed benchmark metadata to show the Minecraft version cleanly and exclude GC activity from before each run.
- Fixed benchmark CSV data to remain complete when metrics are hidden and use a fresh, fixed measurement configuration for each run.
- Fixed the settings screen resetting Max Spike visibility and benchmark duration when saving.

## 1.0.1
- Corrected supported Minecraft versions to 1.21.9–1.21.11.
- Confirmed compatibility with Minecraft 1.21.9, 1.21.10, and 1.21.11.
- Minecraft 1.21.8 is no longer listed as supported.

## 1.0.0.
- Initial release
- Real-time overlay: FPS, 1%/0.1% lows, frametime, spikes, stutters.
- GC pause detection and memory usage tracking.
- Benchmark mode with CSV export.
- Configurable layout and keybindings.
