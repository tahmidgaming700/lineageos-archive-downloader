# Lineage Updater & Downloader

A native Android utility for discovering LineageOS builds, browsing historical archives, downloading ROM packages, and checking file integrity. Version 2.0.0 uses a new Android package identity and installs separately from the legacy downloader.

> **Development build:** Version 2.0.0 is under development and is not yet published. GSI discovery, recovery release tracking, and APK installation safeguards are implemented in this branch, but they still need broader device testing. DD flashing is root-only and destructive. Harmony Recovery is a configurable GitHub release tracker; it does not build a recovery image.

## Project status

- Integrated branch: `main` (2.0.0 development build; public release is still blocked by missing signing secrets)
- Application ID: `com.tahmidgaming.lineageupdater`
- Development version: `2.0.0` (version code `2000`)
- Current Android minimum SDK: API 23 (Android 6.0). Android 5.0/5.0.2 is **not supported** by this build.
- Release status: **not published**
- Release signing: the release variant now requires an injected production keystore. CI refuses publication unless the configured certificate fingerprint matches `ANDROID_CERT_SHA256`. Those secrets have not been confirmed/configured here, and no production release has been published.

## Publishing a production release

The release workflow intentionally refuses to publish an unsigned or incorrectly signed APK. The first attempt after merging 2.0.0 stopped because the repository secret `ANDROID_KEYSTORE_BASE64` is missing. Configure all of these under **GitHub → Settings → Secrets and variables → Actions → New repository secret**:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded production keystore file.
- `ANDROID_KEYSTORE_PASSWORD`: keystore password.
- `ANDROID_KEY_ALIAS`: signing key alias.
- `ANDROID_KEY_PASSWORD`: key password.
- `ANDROID_CERT_SHA256`: SHA-256 certificate fingerprint expected from that key.

Do not commit a keystore, password, private signing key, or access token to the repository. The fingerprint must be from the exact key used to sign the APK. Once all five secrets are configured, open [the failed Publish Release workflow](https://github.com/tahmidgaming700/lineageos-archive-downloader/actions/runs/38142572425) and use **Re-run failed jobs**. The workflow will build the release APK, verify the certificate, and publish `app-updater.apk` plus its SHA-256 sidecar only if every gate passes.

## Existing functionality

The existing application includes these capabilities (check the current default branch and latest CI results for current status):

- Discovers devices using LineageOS device metadata.
- Queries the official LineageOS updater API for current build metadata.
- Displays available build details including filename, size and published checksum when provided.
- Downloads ROM ZIPs with background progress notifications.
- Supports resume when the server supports HTTP range requests.
- Caches public device/build/archive metadata locally and falls back to cached results when network requests fail.
- Checks for app updates daily when connected and can notify the user; it does not silently install updates.
- Calculates SHA-256 after download when a trusted expected digest is available.
- Separates TimSchumi historical builds from official current releases.
- Offers light, dark and system theme options.
- Includes root-only recovery ZIP handoff and DD image flashing with explicit destructive-action confirmation; partition compatibility remains device-specific.
- Discovers community GSI release assets from public Treble/LineageOS projects and links directly to their sources. GSI availability is not a device-compatibility guarantee.
- Tracks recovery releases from a user-configured GitHub repository (including Harmony Recovery projects) without hard-coding an unverified repository URL.
- Checks downloaded APK SHA-256, package ID, and signing certificate before opening Android's package installer for a self-update.
- Provides update tracking for the app, LineageOS, GApps, Magisk, and detected recovery properties.

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
- Extend GSI results with device-specific compatibility metadata; current results are discovery-only.
- Validate DD Flasher partition handling and recovery workflows on test devices before production use.
- Add a real Harmony Recovery build pipeline only if its source, build requirements, and a suitable Linux build environment are confirmed; this app currently tracks configured GitHub releases only.
- Configure the production signing secrets and test signer matching against the actual installed app certificate.
- Continue expanding unit tests, device testing, and CI.

Items in a plan are not claims that those features are already complete. See [CHANGELOG.md](CHANGELOG.md) for confirmed changes and [GitHub Actions](https://github.com/tahmidgaming700/lineageos-archive-downloader/actions) for actual build results.

## Build from source

Requirements: Android SDK, JDK 17, and Gradle 8.11.1 (or use the repository's configured GitHub Actions workflow).

```bash
gradle :app:assembleDebug --stacktrace
gradle :app:testDebugUnitTest --stacktrace
```

GitHub Actions runs the unit tests and builds a debug APK for pull requests. Debug APKs use a different signing identity from production and cannot safely replace a differently signed installation. The release workflow requires these repository secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, and `ANDROID_CERT_SHA256`. The final fingerprint must be the app-signing certificate expected on target devices (not merely an upload-key fingerprint).

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md). Security reports should follow [SECURITY.md](SECURITY.md).

## Support development

Voluntary support can help fund development time, test devices, build infrastructure, documentation and compatibility testing. No donation link has been configured or verified for this project yet; no payment link is intentionally displayed here. To contribute now, use issues and pull requests on [GitHub](https://github.com/tahmidgaming700/lineageos-archive-downloader).

## Licence

The repository has no licence metadata detected by the GitHub API at the time of this audit, although the existing README states Apache License 2.0. Confirm that the repository contains the appropriate LICENSE file and third-party notices before representing the licence as verified.
