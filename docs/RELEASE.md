# Release checklist

## Before release
- Update CHANGELOG.md
- Bump mod_version in gradle.properties
- (Recommended) Build: ./gradlew clean build
- Test:
  - 1.21.9
  - 1.21.10
  - 1.21.11
- Verify overlay, layouts, benchmark, CSV output

## Release
- git add CHANGELOG.md gradle.properties
- git commit -m "Release 1.x.x"
- git push
- git tag v1.x.x
- git push origin v1.x.x
- Download JAR from GitHub Release assets
- Upload single JAR to Modrinth (Version number = 1.x.x)
- Select Minecraft versions 1.21.9, 1.21.10, and 1.21.11
