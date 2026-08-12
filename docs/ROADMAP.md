# Roadmap

This roadmap defines the planned development order. It does not assign release dates or guarantee that every later item will be implemented.

## Working rules

- Complete the current phase before starting planned work from a later phase.
- Confirmed serious bugs may interrupt any phase.
- Minecraft compatibility work may be handled when new versions appear.
- Measure performance problems before implementing large optimizations.
- Record completed release changes in `CHANGELOG.md`; keep this document focused on direction and status.

## Phase 1 — Stabilize the current mod [CURRENT]

- [x] Fix Max Spike visibility and benchmark duration resetting when settings are saved.
- [x] Fix benchmark Minecraft-version metadata.
- [x] Exclude pre-benchmark GC activity from each run.
- [x] Clarify GC time-delta reporting.
- [x] Fix the missing Max Spike separator in the three-line layout.
- [x] Add validation for loaded configuration values.
- [x] Standardize configuration option labels so capitalization, spacing, and enum-value names follow one consistent user-facing style.
- [x] Prevent benchmark filename collisions from overwriting existing CSV files.
- [ ] Fix any confirmed benchmark lifecycle, CSV/write, shutdown, UI-formatting, or persistence bugs.

Phase 1 is complete when no known confirmed stability or persistence bugs remain and loaded configuration values are validated safely.

## Phase 2 — Lock down current measurement behavior

- [x] Test average FPS.
- [x] Test percentile and mean-worst 1% and 0.1% lows.
- [x] Test stutter count, percentage, threshold inclusion, and maximum spike.
- [x] Test rolling-window boundaries.
- [ ] Test frametime calculations.
- [ ] Test complete benchmark summaries.
- [ ] Test pause handling and benchmark lifecycle behavior.
- [x] Make benchmark collection independent of overlay visibility.
- [x] Capture measurement settings when each benchmark starts.
- [x] Start each benchmark from a fresh frame-timing boundary.
- [ ] Add benchmark CSV tests for metadata, columns, summaries, and captured settings.

## Phase 3 — Measure and reduce benchmark self-overhead

- [ ] Measure benchmark-on versus benchmark-off overhead.
- [ ] Decide whether benchmark I/O changes are justified by the measurements.
- [ ] If justified, move CSV formatting and writing away from the measured render path using bounded storage and safe failure handling.
- [ ] Remove other per-frame work only where measurement shows meaningful overhead.

Any I/O redesign must preserve current CSV data, support long runs safely, report failures, and avoid a large freeze when a benchmark stops.

## Phase 4 — Maintain Minecraft compatibility

- [ ] Support every stable Fabric-compatible Minecraft Java release from 1.21 through 26.2 using separate version-specific builds where one JAR cannot remain compatible.
- [x] Define the current supported range as Minecraft 1.21.9–1.21.11.
- [x] Manually verify Minecraft 1.21.9, 1.21.10, and 1.21.11.
- [ ] Port and verify the missing 1.21–1.21.8 releases individually or in proven-compatible build groups.
- [ ] Port and verify Minecraft 26.1 and 26.2.
- [ ] Continue porting and verifying later stable releases as they become available.
- [ ] Improve build or CI validation for supported versions where practical.

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
