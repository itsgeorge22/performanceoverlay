# Testing

## Quick command guide

Coding agents run the relevant tests after making changes, so these commands are only needed for independent manual verification.

```powershell
# Fast logic tests without opening Minecraft
.\gradlew.bat test

# Real Minecraft test on the 1.21.11 development target
.\gradlew.bat runClientGameTest

# Real Minecraft tests on every supported version
.\gradlew.bat testSupportedVersions

# Clean build of the JAR plus logic tests
.\gradlew.bat clean build

# Clean build of the Minecraft 26.1–26.1.2 JAR plus logic tests
.\versions\26.1.2\gradlew.bat -p versions\26.1.2 clean build

# Clean build of the separate Minecraft 26.2 JAR plus logic tests
.\versions\26.2\gradlew.bat -p versions\26.2 clean build
```

Use the root `testSupportedVersions` task for the most complete automated check before a release. Minecraft opens and closes once for each of the seven supported versions across all three JARs. Success is reported as `BUILD SUCCESSFUL`; any failed assertion or crash fails the Gradle task. The red benchmark-write error shown during the client test is intentionally generated to verify error handling.

GitHub runs `build` on every push and pull request. The complete real-client compatibility matrix can also be started manually from GitHub without using the local computer.

### Run the client tests on GitHub

1. Open the repository's **Actions** tab.
2. Select **Minecraft client tests**.
3. Select **Run workflow**, keep the `main` branch selected, then confirm **Run workflow**.
4. Open the run to see one job for each of the seven supported Minecraft versions.

All seven jobs run independently and mostly in parallel. A green check means that version completed the asserted client behavior suite. A red cross identifies the failed version; its **Artifacts** section contains available logs, screenshots, crash reports, and incomplete benchmark files for seven days.

Each CI job retries the complete client test once if its first attempt fails. This covers occasional fixed-timeout world-loading failures on slower virtual runners; a repeatable mod or assertion failure still makes the job red.

The GitHub clients render through a virtual display with software graphics. They validate startup and asserted behavior, but they do not replace the manual smoke test for visual quality, real GPU performance, or compatibility with other mods.

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

## Minecraft client integration tests

The regular JUnit suite does not launch Minecraft. Fabric client game tests live in a separate test source set so test-only code is not packaged in release JARs.

### 1. Startup

- [x] Launch the real Minecraft 1.21.11 client with Performance Overlay and its required dependencies.
- [x] Confirm the Fabric client entrypoint initializes without crashing.
- [x] Fail the test when Performance Overlay or a required dependency does not initialize.

Run this client test locally with:

```powershell
.\gradlew.bat runClientGameTest
```

The test opens a Minecraft window, completes automatically, closes the game, and reports a failed Gradle task if startup validation fails.

Run the same client behavior suite against every currently supported Minecraft version with:

```powershell
.\gradlew.bat testSupportedVersions
```

Individual version commands are `test12109`, `test12110`, and `test12111`. The 1.21.9 and 1.21.10 tasks build the normal release JAR first, then load that JAR with the target Minecraft version and matching dependencies. The combined command runs versions sequentially to keep their client-test files separate.

Run only the Java 25 JAR's complete compatibility matrix from the repository root with:

```powershell
.\gradlew.bat test261SupportedVersions
```

Its individual client commands are `test261`, `test2611`, and `test2612` in the `versions/26.1.2` build. Gradle automatically obtains a Java 25 toolchain when needed. Every task loads the same built 26.1–26.1.2 JAR rather than recompiling a different mod for each patch version.

Run only the packaged Minecraft 26.2 JAR's complete client suite with:

```powershell
.\gradlew.bat test262SupportedVersion
```

The version build also exposes `test262`. Both commands load the built 26.2 JAR as the mod under test.

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
- [x] Wait for rolling Average FPS to become available before validating hidden-overlay CSV completeness.
- [x] Confirm write failures produce an error instead of leaving stale benchmark state.

### 7. World and shutdown lifecycle

- [x] Confirm a real-client dimension change keeps the benchmark active.
- [x] Confirm leaving the client test world ends the real benchmark CSV with `WORLD_LEFT`.
- [x] Confirm the normal Minecraft shutdown handler finalizes an active CSV with `GAME_SHUTDOWN`.

### 8. Supported-version matrix

- [x] Run the complete client behavior suite against the release JAR on Minecraft 1.21.9.
- [x] Run the complete client behavior suite against the release JAR on Minecraft 1.21.10.
- [x] Run the complete client integration suite on the 1.21.11 build target.
- [x] Add a Java 25 client integration target for the Minecraft 26.1.2 build line.
- [x] Run its built JAR through the complete client suite on Minecraft 26.1, 26.1.1, and 26.1.2.
- [x] Add a separate Minecraft 26.2 build and packaged-JAR client integration target.
- [x] Provide one command that requires every declared supported version to pass before release.

### Reliability rules

- Prefer state assertions and checking that expected text is present over exact screenshot equality.
- Use screenshots only for broad presence checks because fonts, graphics drivers, and CI environments can produce small visual differences.
- Do not use CI frame rates for performance comparisons; shared runners are not stable benchmark machines.
- Upload screenshots, logs, crash reports, and failed benchmark files as CI artifacts when a client test fails.
- Keep the existing manual smoke test for visual quality, real performance, and mod-conflict checks.

## Manual smoke test

To launch the Minecraft 26.2 development client directly:

```powershell
.\versions\26.2\gradlew.bat -p versions\26.2 runClient
```

After metric or benchmark changes:

1. Launch the development client with `runClient` in VS Code.
2. Confirm the overlay appears and updates.
3. Run a short benchmark with F10.
4. Confirm the CSV is saved and ends with `# SUMMARY`.
5. Confirm the per-frame rows appear before the summary and can be opened normally in Excel or Google Sheets.
6. Confirm F10 does nothing in the main menu, dimension changes keep a run active, and leaving the world saves it with `# EndReason: WORLD_LEFT`.

The automated Minecraft 26.2 suite currently uses the default OpenGL backend. The experimental Vulkan backend remains manually unverified.
