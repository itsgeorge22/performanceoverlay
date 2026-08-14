# Git and release workflow

## Commits

- Keep commit messages descriptive and concise, using one sentence to explain what changed.
- Create a commit for each meaningful, coherent change.
- Group small related edits into the same commit instead of creating noise in the history.
- Review and validate changes before committing them.
- `Commit` means commit the intended current changes locally without pushing.
- `Push` means commit any intended pending changes, then push all local commits to `origin/main`.
- `Commit and push` means commit the intended current changes, then push all local commits to `origin/main`.
- A normal commit or push must never create a release tag.

## Maintaining the changelog

- Record every meaningful bug fix, feature, behavior change, compatibility change, and performance improvement in `CHANGELOG.md` as the work is completed.
- Add ongoing work under the current `## X.X.X [WIP]` heading instead of waiting until release preparation.
- Use one concise, user-facing bullet for each meaningful change.
- Group closely related changes into one bullet and omit trivial edits with no user-visible or project-level impact.
- Place changelog bullets directly below the version heading with no blank line between them.
- When preparing a release, remove `[WIP]` from the version heading after its entries are final.

## Before a release

- Confirm the release version and whether it is Alpha, Beta, or Release.
- Review the accumulated entries under the current `[WIP]` version heading and remove `[WIP]` when they are final.
- Bump `mod_version` in `gradle.properties`, `versions/26.1.2/gradle.properties`, and `versions/26.2/gradle.properties`.
- Review all three JAR names and their Minecraft compatibility ranges.
- Update relevant README, architecture, metrics, testing, release, and issue-template documentation.
- Build the Java 21 JAR with `./gradlew clean build` or `.\gradlew.bat clean build` on Windows.
- Build the Java 25 JAR with `bash ./versions/26.1.2/gradlew -p versions/26.1.2 clean build` or `.\versions\26.1.2\gradlew.bat -p versions\26.1.2 clean build` on Windows.
- Build the separate 26.2 JAR with `bash ./versions/26.2/gradlew -p versions/26.2 clean build` or `.\versions\26.2\gradlew.bat -p versions\26.2 clean build` on Windows.
- Run the root `testSupportedVersions` task. It must pass on 1.21.9–1.21.11, 26.1–26.1.2, and 26.2.
- Verify startup, overlay toggle, layouts, settings persistence, benchmark lifecycle, and CSV output.
- Review the complete Git diff and confirm the working tree contains only intended changes.

## GitHub release

Only proceed after explicit release approval.

1. Commit the complete release changes with a concise one-sentence message.
2. Push the release commit to `main`.
3. Create a tag using `vX.X.X`, for example `v1.0.1`.
4. Push the tag to trigger the GitHub release workflow.
5. Monitor the seven-version Minecraft client-test gate. Release building begins only after every version passes.
6. Monitor the release build until it succeeds. A failed client test prevents the GitHub Release from being created.
7. Verify that the GitHub Release exists and contains all three correct non-sources JARs.
8. Include a full changelog link in the release description:

   ```markdown
   **Full Changelog**: [https://github.com/itsgeorge22/performanceoverlay/compare/vX.X.X](https://github.com/itsgeorge22/performanceoverlay/compare/vX.X.X)
   ```

## Modrinth

- Upload each non-sources JAR from the GitHub Release as a separate Modrinth version entry with the same mod version number.
- Use the same version number and select the appropriate Alpha, Beta, or Release channel.
- Select 1.21.9–1.21.11 for the Java 21 JAR, 26.1–26.1.2 for the first Java 25 JAR, and only 26.2 for the separate 26.2 JAR.
- Verify the Fabric loader and required dependencies.
- Add concise release notes matching `CHANGELOG.md`.
