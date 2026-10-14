# Lineage Updater & Downloader {{VERSION}}

A native Android utility for LineageOS device/build discovery, historical ROM archives, verified downloads, GSI release discovery, recovery tracking, and root-only flashing tools.

## Installation

1. Download `app-updater.apk` from the Assets section below.
2. Verify the APK with the published `app-updater.sha256` file before installation.
3. Open the APK and follow Android's package-installer prompts.
4. Launch **Lineage Updater & Downloader**.

**This is a new app package:** `com.tahmidgaming.lineageupdater`. It installs separately and does not replace or upgrade the legacy LineageOS Archive Downloader package.

## Highlights

- Native LineageOS discovery, historical archives, resumable downloads and SHA-256 checks when a trusted digest is available.
- GSI discovery with architecture filters, device-property hints, and raw-image versus compressed/archive labels.
- Raw GSI `.img` downloads can be offered to the root-only DD Flasher; missing upstream checksums are explicitly warned about.
- DD preflight checks validate the target block device and image size before writing.
- Recovery release tracking and a Harmony Recovery build-script planner.
- Offline metadata cache, app-update notifications, diagnostics, and light/dark/system themes.

## GSI and DD safety

- Architecture and device properties are hints, not a compatibility guarantee.
- Compressed `.img.xz`/`.img.gz` assets must be extracted to raw `.img`; ZIP archives are not directly flashable with DD.
- Direct DD writes require root access and the exact writable partition. Many dynamic-partition devices require fastbootd and the GSI/device project's documented process instead.
- DD flashing can permanently destroy data or make a device unbootable. Confirm the target, image size and bootloader requirements; back up first.
- A download without a publisher-provided SHA-256 digest is not checksum-verified.
- The Harmony Recovery planner prepares a Linux build script; it does not guarantee a bootable image or compile a recovery on the phone.

See [CHANGELOG.md](https://github.com/tahmidgaming700/lineageos-archive-downloader/blob/main/CHANGELOG.md) for version history. Report issues with app/Android version, device model and non-sensitive diagnostics. Never post passwords, tokens, private keystores or personal files.
