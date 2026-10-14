# Lineage Updater & Downloader

A native Android utility for LineageOS device/build discovery, historical ROM archives, verified downloads, community GSI discovery, recovery release tracking, and root-only flashing tools.

## Current version

- Latest published release before this update: [2.0.0](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v2.0.0)
- Source version being prepared: **2.0.1** (version code `2001`). It is not published until the GitHub Actions release workflow succeeds.
- Application ID: `com.tahmidgaming.lineageupdater`
- Minimum Android version: Android 6.0 (API 23). Android 5.0/5.0.2 is not supported.
- Package migration: 2.x installs separately from the legacy `com.tahmidgaming.lineagearchive` package; it does not upgrade that package in place.

## GSI discovery, downloading and DD installation

GSI Discovery lists public release assets from community Treble/LineageOS projects. It filters assets by filename-based CPU architecture hints and displays device properties such as ABI, Android API level, Treble status, A/B property and dynamic-partition property when available.

1. Open **Tools → GSI Discovery** and choose the architecture filter matching the device and GSI build.
2. Read the release notes and confirm the Android release, ABI, vendor interface/VNDK, required variant (for example, vndklite), and partition layout. App-detected properties are hints, not an automated compatibility certification.
3. Download the asset. When the source provides a SHA-256 digest, the app checks the downloaded bytes against it. If no digest is published, the download is marked as not checksum-verified.
4. For a raw `.img` asset, open **Tools → dd image flasher** after the download completes. Raw GSI images may be offered for `system`, `system_a`, or `system_b`. Confirm the exact partition, image size, root access and device-specific instructions before writing.
5. Compressed `.img.xz` and `.img.gz` assets must be extracted to raw `.img` before DD flashing. A `.zip` release archive is not itself a raw partition image and must not be sent to DD.

**DD flashing is device-specific and destructive.** It requires root access and a valid writable block-device target. The flasher checks that the image fits the selected target before writing. Devices using dynamic partitions commonly require `fastbootd` and the device/GSI project's documented procedure instead of direct `dd` writes. Do not assume a partition name is safe or compatible. Back up important data and confirm bootloader-unlock requirements before proceeding. A wrong image or target can cause data loss or prevent the device from booting.

### What “compatible” means here

The app helps filter by architecture and show selected device properties. It cannot guarantee that a GSI will boot or that camera, cellular, audio, encryption or other hardware will work. Filename filters can miss or misclassify unusual naming conventions. Confirm compatibility with the device community and GSI maintainer before flashing.

## Existing functionality

- Discovers devices using LineageOS device metadata and queries the official LineageOS updater API.
- Displays build metadata, filenames, file sizes and published checksums when available.
- Downloads ROM packages with background progress notifications and resumable transfers where the server supports HTTP range requests.
- Calculates SHA-256 after download and compares it with a trusted expected digest when available.
- Caches public device/build/archive metadata and falls back to cached results when network requests fail.
- Separates TimSchumi historical builds from official current releases.
- Tracks app updates and notifies users; app installation remains user-confirmed and signature-checked.
- Provides root-only recovery ZIP handoff, DD image flashing, GApps/Magisk discovery and a configurable Harmony Recovery release tracker.
- Includes a Harmony Recovery build-script planner. It does not compile or guarantee a bootable recovery image on the phone.
- Provides light, dark and system theme options, changelog, diagnostics and support information.

## Sources and integrity

- Official LineageOS device metadata: [LineageOS hudson](https://github.com/LineageOS/hudson)
- Current-build API: `https://download.lineageos.org/api/v2/devices/{codename}/builds`
- LineageOS installation guidance: [LineageOS Wiki](https://wiki.lineageos.org/)
- Community GSI sources: [Treble builds](https://github.com/ponces/treble_build_los/releases) and [Unified LineageOS builds](https://github.com/AndyYan/lineage_build_unified/releases)

The TimSchumi archive is an independent, unofficial archive. Historical packages may be unsupported or vulnerable. SHA-256 confirms that bytes match a supplied digest; it does not by itself prove the publisher's identity. Follow source signature-verification guidance where available.

## Build from source

Requirements: Android SDK, JDK 17 and Gradle 8.11.1 (or use the repository's configured GitHub Actions workflow).

```bash
gradle :app:assembleDebug --stacktrace
gradle :app:testDebugUnitTest --stacktrace
```

The release workflow requires repository Actions secrets named `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` and `ANDROID_CERT_SHA256`. It refuses to publish if signing secrets are missing or the release certificate fingerprint does not match. Never commit a keystore, password, private signing key or access token.

See [CHANGELOG.md](CHANGELOG.md), [GitHub Actions](https://github.com/tahmidgaming700/lineageos-archive-downloader/actions) and [Releases](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases) for version history and actual build/release status.

## Contributing and support

Read [CONTRIBUTING.md](CONTRIBUTING.md). Security reports should follow [SECURITY.md](SECURITY.md). No donation destination is verified/configured in this repository; use [GitHub issues and pull requests](https://github.com/tahmidgaming700/lineageos-archive-downloader) to contribute.

## Licence

Confirm the repository's licence file and third-party notices before redistributing. Do not infer a licence solely from README text.
