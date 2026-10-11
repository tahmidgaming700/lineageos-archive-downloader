# Lineage Updater & Downloader {{VERSION}}

A native Android utility for LineageOS device/build discovery, historical ROM archives, verified downloads, GSI release discovery, recovery tracking, and root-only flashing tools.

## Installation

1. Download `app-updater.apk` from the Assets section below.
2. Verify the APK with the published `app-updater.sha256` file before installation.
3. Open the APK and follow Android's package-installer prompts. You may need to allow installation from the app you used to download it.
4. Launch **Lineage Updater & Downloader**.

**This is a new app package:** `com.tahmidgaming.lineageupdater`. It installs separately and does not replace or upgrade the legacy LineageOS Archive Downloader package.

## Highlights

- Native LineageOS device and current-build discovery.
- TimSchumi historical archive browsing.
- Background downloads, resumable transfers where supported, and SHA-256 checks when a trusted digest is available.
- GSI release discovery with architecture filters and compatibility warnings.
- Recovery release tracking and a Harmony Recovery build-script planner.
- Root-only DD flashing tools with destructive-action confirmation.
- Offline metadata cache, app-update notifications, diagnostics, and light/dark/system themes.

## Important safety notes

- GSI and recovery availability does not guarantee compatibility with your exact device.
- DD flashing can permanently destroy data or make a device unbootable. Confirm the exact partition and image before proceeding.
- The Harmony Recovery planner prepares a Linux build script; it does not guarantee a bootable image or compile a recovery on the phone.
- SHA-256 verifies byte-for-byte integrity against the supplied digest; it does not independently prove the publisher's identity.
- Root operations are optional and should only be used by users who understand their device's partition layout.

## Changelog

See [CHANGELOG.md](https://github.com/tahmidgaming700/lineageos-archive-downloader/blob/main/CHANGELOG.md) for version history and release status.

## Report an issue

Please include the app version, Android version, device codename/model, and relevant non-sensitive diagnostics. Never post passwords, tokens, private keystores, or personal files.
