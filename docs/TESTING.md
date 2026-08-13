# Testing

## Automated tests

Run the complete build, including metric tests:

```powershell
.\gradlew.bat clean build
```

Run only automated tests:

```powershell
.\gradlew.bat test
```

The automated suite covers benchmark lifecycle and key guarding, same-tick automatic-stop and write-error key suppression, menu start rejection, world-departure finalization, background CSV formatting, sparse GC samples, GC baseline reset behavior, bounded-queue overflow, writer failures and finalization timeouts, CSV metadata and columns, captured settings, filename collision, shutdown-footer handling, complete summary calculations and formatting, all three pause modes, complete configuration-screen copying, configuration presets, configuration fallback and validation, color-threshold ordering, overlay width-cache invalidation, and deterministic metric calculations for average FPS, frametime, percentile and mean-worst lows, duplicate-heavy low selection, combined stutter statistics, threshold inclusion and percentage, maximum spike, reset behavior, and rolling-window boundary inclusion.

## Planned Minecraft client integration tests

The regular JUnit suite does not launch Minecraft. Fabric client game tests are being added gradually in a separate test source set so test-only code is not packaged in release JARs.

### 1. Startup

- [x] Launch the real Minecraft 1.21.11 client with Performance Overlay and its required dependencies.
- [x] Confirm the Fabric client entrypoint initializes without crashing.
- [x] Fail the test when Performance Overlay or a required dependency does not initialize.

Run this client test locally with:

```powershell
.\gradlew.bat runClientGameTest
```

The test opens a Minecraft window, completes automatically, closes the game, and reports a failed Gradle task if startup validation fails.

### 2. Overlay rendering

- [x] Create and enter a test world.
- [x] Wait for chunks and the HUD to render.
- [x] Confirm the overlay produces visible metric text with a positive FPS value and capture a diagnostic screenshot.
- [x] Render and verify the expected line structure of all three text layouts without crashing.
- [x] Render all six supported screen positions and verify text appears at each expected anchor.
- [x] Render at the minimum and maximum supported scale and verify the maximum is materially larger.

### 3. Key bindings

- [x] Press F7 and confirm both configuration state and rendered overlay visibility hide and reappear.
- [x] Press F8 and confirm state, line structure, and rendering cycle through One line, Three lines, Column, then One line.
- [x] Press F9 and confirm accumulated frame history is cleared and sampling resumes afterward.
- [x] Press F10 to start and manually stop a benchmark, confirm frames are captured, and verify the CSV is finalized.
- [x] Confirm F9 is ignored while the overlay is hidden and blocked without interrupting an active benchmark.
- [x] Confirm queued F10 presses cannot start another run on the same tick as automatic stop or write-error reporting.

### 4. Settings

- [x] Open the Performance Overlay configuration screen through its Mod Menu integration and confirm its title loads successfully.
- [x] Verify every switch, number field, and enum value is copied into the working configuration.
- [x] Save every supported setting and confirm it persists after reopening the screen.
- [x] Confirm Default, Responsive, and Smooth presets apply their complete values and remain selected.
- [x] Confirm editing a preset-controlled value changes the preset to Custom.
- [x] Confirm invalid loaded values are corrected safely.

Prefer direct state and persistence assertions for the full settings matrix. Use simulated UI clicks only for representative controls so layout changes in Cloth Config do not make the entire suite fragile.

### 5. Metric smoke checks

- [x] Confirm FPS and frametime become non-zero after rendered frames.
- [x] Confirm enabled metrics appear and disabled metrics disappear.
- [x] Exercise all 512 metric-visibility combinations through snapshot construction, with real-client rendering checks for representative layouts.
- [x] Confirm reset and overlay re-enable establish fresh frame and GC baselines.

These tests validate integration only. Exact metric calculations remain covered by deterministic unit tests.

### 6. Benchmark lifecycle and export

- [x] Confirm F10 does nothing in menus and starts a benchmark inside a world.
- [x] Confirm a benchmark continues when the overlay is hidden.
- [x] Confirm automatic progress reaches 100%.
- [x] Confirm manual and automatic stops both work.
- [x] Confirm the CSV contains metadata, ordered frame rows, an end reason, and `# SUMMARY`.
- [x] Confirm the CSV remains complete when overlay metrics are hidden.
- [x] Confirm write failures produce an error instead of leaving stale benchmark state.

### 7. World and shutdown lifecycle

- [x] Confirm a real-client dimension change keeps the benchmark active.
- [x] Confirm leaving the client test world ends the real benchmark CSV with `WORLD_LEFT`.
- [x] Confirm the normal Minecraft shutdown handler finalizes an active CSV with `GAME_SHUTDOWN`.

### 8. Supported-version matrix

- [ ] Run at least the startup, world-load, overlay-render, and short-benchmark tests on Minecraft 1.21.9.
- [ ] Run the same compatibility smoke tests on Minecraft 1.21.10.
- [x] Run the complete client integration suite on the 1.21.11 build target.
- [ ] Add a separate client integration target for Minecraft 26.2 when that port exists.
- [ ] Require each declared supported version to pass before release.

### Reliability rules

- Prefer state assertions and checking that expected text is present over exact screenshot equality.
- Use screenshots only for broad presence checks because fonts, graphics drivers, and CI environments can produce small visual differences.
- Do not use CI frame rates for performance comparisons; shared runners are not stable benchmark machines.
- Upload screenshots, logs, crash reports, and failed benchmark files as CI artifacts when a client test fails.
- Keep the existing manual smoke test for visual quality, real performance, and mod-conflict checks.

## Manual smoke test

After metric or benchmark changes:

1. Launch the development client with `runClient` in VS Code.
2. Confirm the overlay appears and updates.
3. Run a short benchmark with F10.
4. Confirm the CSV is saved and ends with `# SUMMARY`.
5. Confirm the per-frame rows appear before the summary and can be opened normally in Excel or Google Sheets.
6. Confirm F10 does nothing in the main menu, dimension changes keep a run active, and leaving the world saves it with `# EndReason: WORLD_LEFT`.
