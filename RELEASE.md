# Release checklist

## Before release
- Update CHANGELOG.md
- Bump mod_version in gradle.properties
- (Recommended) Build: ./gradlew clean build
- Test:
  - earliest 1.21.x
  - one mid 1.21.x
  - latest 1.21.x
- Verify overlay, layouts, benchmark, CSV/TSV output

## Release
- git add CHANGELOG.md gradle.properties
- git commit -m "Release 1.x.x"
- git push
- git tag v1.x.x
- git push origin v1.x.x
- Download JAR from GitHub Release assets
- Upload single JAR to Modrinth (Version number = 1.x.x)
- Select all supported 1.21.x versions