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

The automated suite covers benchmark lifecycle and key guarding, background CSV formatting, sparse GC samples, bounded-queue overflow, writer failures, CSV metadata and columns, captured settings, filename collision, shutdown-footer handling, complete summary calculations and formatting, all three pause modes, configuration presets, configuration fallback and validation, overlay width-cache invalidation, and deterministic metric calculations for average FPS, frametime, percentile and mean-worst lows, duplicate-heavy low selection, combined stutter statistics, threshold inclusion and percentage, maximum spike, reset behavior, and rolling-window boundary inclusion.

## Manual smoke test

After metric or benchmark changes:

1. Launch the development client with `runClient` in VS Code.
2. Confirm the overlay appears and updates.
3. Run a short benchmark with F10.
4. Confirm the CSV is saved and ends with `# SUMMARY`.
5. Confirm the per-frame rows appear before the summary and can be opened normally in Excel or Google Sheets.
