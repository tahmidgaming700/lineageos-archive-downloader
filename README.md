# Lineage Updater & Downloader

A native Android utility for discovering LineageOS builds, browsing historical archives, downloading ROM packages, and checking file integrity.

> **Development build:** Version 2.0.0 is under development. Do not treat this branch as a published or production-ready release. Advanced GSI, DD Flasher, and Harmony Recovery work must be considered available only to the extent implemented and tested in the source.

## Project status

- Development branch: `release/2.0.0-foundation`
- Application ID: `com.tahmidgaming.lineagearchive`
- Development version: `2.0.0` (version code `2000`)
- Current Android minimum SDK: API 23 (Android 6.0). Android 5.0/5.0.2 is **not supported** by this build.
- Release status: **not published**
- Release signing: the current Gradle release variant still references the debug signing configuration. A production release must not be advertised as an in-place update until the signing certificate is confirmed compatible with the installed app.

## Existing functionality

The existing application includes these capabilities (check the current default branch and latest CI results for current status):

- Discovers devices using LineageOS device metadata.
- Queries the official LineageOS updater API for current build metadata.
- Displays available build details including filename, size and published checksum when provided.
- Downloads ROM ZIPs with background progress notifications.
- Supports resume when the server supports HTTP range requests.
- Calculates SHA-256 after download when a trusted expected digest is available.
- Separates TimSchumi historical builds from official current releases.
- Offers light, dark and system theme options.
- Includes tool screens for recovery/image workflows and add-on downloads; root and device limitations apply.

## Source labels and safety

### Official LineageOS sources
- Device metadata: [LineageOS hudson](https://github.com/LineageOS/hudson)
- Current build API: `https://download.lineageos.org/api/v2/devices/{codename}/builds`
- Verification instructions: [LineageOS Wiki](https://wiki.lineageos.org/)

### Historical archives
The TimSchumi archive is an independent, unofficial archive. Historical packages can be unsupported or vulnerable. Do not interpret their availability as official support.

### Integrity is not authenticity
SHA-256 can establish that downloaded bytes match a known expected digest. It does not, by itself, prove who produced the file. Follow the ROM project's signature-verification guidance where applicable.

## Version 2.0.0 development plan

See [the release gate and implementation checklist](docs/RELEASE_2.0.0_PLAN.md). The plan includes:

- Preserve the existing downloader and migrate data safely.
- Improve device/build discovery, caching and download verification.
- Add explainable GSI compatibility discovery and recovery update tracking.
- Develop DD Flasher only for supported workflows, with explicit destructive-action confirmation.
- Develop Harmony Recovery configuration/build orchestration without fabricating build artifacts.
- Add secure application-update metadata and release verification.
- Improve documentation, tests and CI.

Items in a plan are not claims that those features are already complete. See [CHANGELOG.md](CHANGELOG.md) for confirmed changes and [GitHub Actions](https://github.com/tahmidgaming700/lineageos-archive-downloader/actions) for actual build results.

## Build from source

Requirements: Android SDK, JDK 17, and Gradle 8.11.1 (or use the repository's configured GitHub Actions workflow).

```bash
gradle :app:assembleDebug --stacktrace
gradle :app:testDebugUnitTest --stacktrace
```

The commands above are instructions, not a claim that they have passed in this environment. Install the debug APK only after reviewing the build output and understanding its signing identity.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md). Security reports should follow [SECURITY.md](SECURITY.md).

## Support development

Voluntary support can help fund development time, test devices, build infrastructure, documentation and compatibility testing. No donation link has been configured or verified for this project yet; no payment link is intentionally displayed here. To contribute now, use issues and pull requests on [GitHub](https://github.com/tahmidgaming700/lineageos-archive-downloader).

## Licence

The repository has no licence metadata detected by the GitHub API at the time of this audit, although the existing README states Apache License 2.0. Confirm that the repository contains the appropriate LICENSE file and third-party notices before representing the licence as verified.
