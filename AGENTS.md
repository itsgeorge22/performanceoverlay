# AGENTS.md

## Purpose

Performance Overlay is a Minecraft Java client-side performance monitoring and benchmarking mod.

Coding agents must prioritize:

1. measurement correctness,
2. low measurement overhead,
3. simple, reviewable changes,
4. practical value for modpack creators,
5. preservation of documented metric semantics.

Do not perform broad rewrites unless the task explicitly requires one and the existing implementation has been inspected first.

## Required workflow

Before changing code:

- Read the relevant repository code, not only this document.
- Read `docs/METRICS.md` before changing sampling, aggregation, benchmark export, pause handling, or metric calculations.
- Read `docs/ARCHITECTURE.md` before changing runtime flow or class responsibilities.
- Read `docs/TESTING.md` when it exists and follow the validation requirements relevant to the change.
- Read `docs/ROADMAP.md` before planning new features or selecting the next development task.
- Treat documentation as a specification only where it describes accepted behavior. If code and documentation disagree, report the discrepancy instead of silently choosing one.

When changing behavior:

- Keep changes scoped to the current task.
- Do not change metric definitions implicitly as part of refactoring or optimization.
- Do not add dependencies unless they provide clear value that cannot reasonably be achieved with the existing stack.
- Avoid moving additional work onto the render path.
- Avoid allocations, blocking I/O, repeated scans, or expensive formatting on per-frame paths unless justified and measured.
- Preserve client-only behavior unless a task explicitly changes that constraint.

After changing behavior:

- Validate the affected logic.
- Tell the user how to manually test the completed change, using concise step-by-step instructions and the expected result.
- Update `CHANGELOG.md` in the same piece of work for every meaningful bug fix, feature, behavior change, compatibility change, or performance improvement.
- Add new changelog entries directly under the current `## X.X.X [WIP]` heading, with no blank line after the heading; do not wait until release preparation to reconstruct completed work.
- Keep changelog entries concise and user-facing. Do not add entries for trivial edits that do not affect the project or its users.
- Update persistent documentation when the accepted behavior, metric contract, architecture, or testing requirements change.
- Maintain `docs/ROADMAP.md` as planned work is completed, added, removed, or reprioritized; keep release-specific details in `CHANGELOG.md`.
- Explicitly call out known limitations that remain relevant to the task.

## Measurement rules

The current primary frame sample is the elapsed nanoseconds between eligible HUD render callbacks, measured with `System.nanoTime()`.

Do not describe this value as GPU frame time, presentation time, or Minecraft internal frame time. It is a client-side HUD-callback interval.

Any proposal to change the sampling boundary must be treated as a metric-contract change and reviewed against `METRICS.md` before implementation.

Benchmark results must not silently depend on whether a metric is visible in the overlay. The current implementation has this limitation for several exported rolling columns; do not propagate it into new behavior.

Benchmark settings that affect summary semantics should be treated as part of the benchmark's measurement contract. Do not allow a run to silently change meaning because mutable live settings changed mid-run.

## Performance-sensitive code

Treat the following as performance-sensitive:

- `FpsTracker.onFrame()`
- frame-history operations
- rolling metric updates
- percentile / low calculations
- benchmark per-frame capture and export
- snapshot construction
- `OverlayRenderer.render()`

For these paths, prefer fewer passes, bounded memory, reusable buffers, and work scheduled outside the frame-critical path where possible. Optimization must not change metric semantics unless the change is explicitly approved.

## Scope discipline

Do not introduce unrelated features while fixing correctness or performance issues.

Do not replace the existing architecture simply because another architecture appears cleaner. First establish the concrete problem, measurement impact, migration cost, and smallest effective change.

Template remnants or unused code may be removed only when the task includes cleanup or their removal is necessary for the requested change.

## Git authorization

- A request to `commit` means commit the intended current changes locally without pushing.
- A request to `push` means commit any intended pending changes, then push all local commits to `origin/main`.
- A request to `commit and push` means commit the intended current changes, then push all local commits to `origin/main`.
- A request to `release vX.X.X` authorizes the complete release workflow documented in `docs/RELEASE.md`, including the release commit, push, version tag, tag push, and release verification.
- Never create or push a release tag from a normal commit or push request.

## Release preparation

Before preparing, committing, tagging, or publishing a new version:

- Read and follow `docs/RELEASE.md`.
- Review all maintained Markdown documentation for statements affected by the release.
- Update the changelog, version references, supported Minecraft versions, feature descriptions, architecture, metric semantics, testing instructions, and release instructions wherever relevant.
- Ensure documentation describes the final implemented behavior and does not retain outdated claims.
- Do not create commits, push changes, create tags, or publish releases without the corresponding explicit instruction described above.

## Source of truth

Implementation facts come from the current repository.

Product direction and accepted future behavior come from explicit project decisions and the maintained product/roadmap documentation.

Recommendations, hypotheses, and possible future improvements are not implementation facts until accepted and implemented.
