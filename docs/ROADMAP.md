# Roadmap

This roadmap defines the planned development order. It does not assign release dates or guarantee that every later item will be implemented.

## Working rules

- Complete the current phase before starting planned work from a later phase.
- Confirmed serious bugs may interrupt any phase.
- Minecraft compatibility work may be handled when new versions appear.
- Measure performance problems before implementing large optimizations.
- Record completed release changes in `CHANGELOG.md`; keep this document focused on direction and status.

## Phase 1 — Stabilize the current mod [COMPLETE]

- [x] Fix Max Spike visibility and benchmark duration resetting when settings are saved.
- [x] Fix benchmark Minecraft-version metadata.
- [x] Exclude pre-benchmark GC activity from each run.
- [x] Clarify GC time-delta reporting.
- [x] Refresh the GC measurement baseline whenever rolling statistics are reset.
- [x] Fix the missing Max Spike separator in the three-line layout.
- [x] Add validation for loaded configuration values.
- [x] Prevent the Danger FPS color threshold from exceeding Warning FPS.
- [x] Standardize configuration option labels so capitalization, spacing, and enum-value names follow one consistent user-facing style.
- [x] Prevent benchmark filename collisions from overwriting existing CSV files.
- [x] Report benchmark CSV write and finalization failures immediately and clear stale progress state.
- [x] Limit benchmark CSV finalization waits to five seconds.
- [x] Prevent same-tick F10 presses from restarting a benchmark while a write error is reported.
- [x] Finalize active benchmark CSV files during normal Minecraft shutdown.
- [x] Make benchmark start, measurement, and stopping independent of overlay visibility.
- [x] Make F9 inactive while the overlay is hidden and prevent rolling-stat resets during active benchmarks.
- [x] Fix preset selection persistence and align the Default preset with the 10-second average window.
- [x] Correct overlay-disabled and GC metric tooltips.
- [x] Make automatic-stop progress reach 100% and prevent F10 from immediately restarting the benchmark.
- [x] Prevent cached GC deltas from being duplicated across benchmark CSV rows.
- [x] Prevent duplicate-heavy frametime data from degrading low-percentile selection.
- [x] Restrict benchmarks to loaded worlds and finalize them when their world or server is left.
- [x] Remove inactive template mixin remnants from the project and built JAR.
- [x] Pin Fabric Loom to a fixed version for repeatable builds.
- [x] Complete a final stability review with no remaining confirmed Phase 1 bugs.

Phase 1 is complete when no known confirmed stability or persistence bugs remain and loaded configuration values are validated safely.

## Phase 2 — Lock down current measurement behavior [COMPLETE]

- [x] Test average FPS.
- [x] Test percentile and mean-worst 1% and 0.1% lows.
- [x] Test stutter count, percentage, threshold inclusion, and maximum spike.
- [x] Test rolling-window boundaries.
- [x] Test frametime calculations.
- [x] Test complete benchmark summaries.
- [x] Test pause handling and benchmark lifecycle behavior.
- [x] Make benchmark collection independent of overlay visibility.
- [x] Capture measurement settings when each benchmark starts.
- [x] Start each benchmark from a fresh frame-timing boundary.
- [x] Add benchmark CSV tests for metadata, columns, summaries, and captured settings.

## Phase 3 — Measure and reduce benchmark self-overhead [COMPLETE]

- [x] Manually compare benchmark-on versus benchmark-off behavior with no noticeable performance impact.
- [x] Decide that moving CSV formatting and writing off the measured render path is sufficient for the current release.
- [x] Move CSV formatting and writing away from the measured render path using bounded storage and safe failure handling.
- [x] Combine rolling stutter count, percentage, and maximum-spike work into one history scan.
- [x] Cache overlay text measurements until the displayed snapshot or font changes.
- [x] Leave further per-frame optimization until measurement shows meaningful overhead.

Any I/O redesign must preserve current CSV data, support long runs safely, report failures, and avoid a large freeze when a benchmark stops.

## Phase 4 — Maintain Minecraft compatibility [NEXT]

- [x] Define the current supported range as Minecraft 1.21.9–1.21.11.
- [x] Manually verify Minecraft 1.21.9, 1.21.10, and 1.21.11.
- [x] Defer Minecraft 1.21–1.21.8 unless user demand justifies maintaining additional builds.
- [x] Create a separate Java 25, unobfuscated build line targeting Minecraft 26.1.2.
- [x] Test the 26.1.2 JAR on Minecraft 26.1, 26.1.1, and 26.1.2; declare one 26.1-26.1.2 range only after every version passes.
- [x] Port the 26.1.2 build line to Minecraft 26.2 and verify it as a separate 26.2 JAR.
- [ ] Combine modern version ranges only when the same built JAR passes the complete client suite on every declared version.
- [ ] Continue porting and verifying later stable releases as they become available.
- [x] Add and run the Minecraft 1.21.11 client integration suite defined in `docs/TESTING.md`.
- [x] Run the release JAR through the complete client behavior suite on Minecraft 1.21.9, 1.21.10, and 1.21.11 with one local command.
- [ ] Run compatibility smoke tests in CI for every version declared by each build.

The declared compatibility range of each JAR must remain limited to versions verified with that build. Supporting future releases is an ongoing maintenance target, not a claim that existing JARs will automatically work on them.

## Phase 5 — Review metric definitions

- [ ] Review frametime semantics.
- [ ] Review 1% and 0.1% low definitions.
- [ ] Review stutter detection and decide whether redesign is justified.
- [x] Rename and document GC reporting according to its actual time-delta behavior.
- [ ] Clarify memory and heap terminology throughout the UI, CSV, and documentation.

Metric definitions must not change without explicit approval, updated tests, and updated documentation.

## Phase 6 — Improve benchmark UX

- [ ] Add a post-run results screen.
- [ ] Add run-to-run comparison.
- [ ] Capture relevant benchmark environment information automatically.
- [ ] Add a warm-up, countdown, and fixed-run flow.

## Configuration UX

- [ ] Add a way to view and edit the mod's key bindings from its Mod Menu configuration screen.
- [ ] Keep key-binding changes synchronized with Minecraft's standard Controls configuration.

## Phase 7 — Investigate repeatable scenarios

- [ ] Define what “repeatable enough” means for real Minecraft worlds.
- [ ] Investigate recorded movement or camera playback.
- [ ] Evaluate repeatability, compatibility, maintenance cost, and measurement overhead before implementation.

## Not currently planned

- A broad architectural rewrite.
- Claims of GPU frametime or presentation-time measurement without a new validated sampling source.
- Major visual features before correctness and benchmark stability are established.
