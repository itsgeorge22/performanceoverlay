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

The automated suite covers configuration fallback and validation plus deterministic metric calculations for average FPS, percentile lows, mean-worst lows, stutter threshold inclusion and percentage, maximum spike, and rolling-window boundary inclusion.

## Manual smoke test

After metric or benchmark changes:

1. Launch the development client with `runClient` in VS Code.
2. Confirm the overlay appears and updates.
3. Run a short benchmark with F10.
4. Confirm the CSV is saved and ends with `# SUMMARY`.
