# LineageOS Archive Downloader

A free, open-source Android downloader for LineageOS builds with post-download SHA-256 verification.

## Features

- Dynamically discovers LineageOS-supported devices from the official LineageOS device list.
- Detects the Android device using manufacturer/model/product/device identifiers, with manual search as a fallback.
- Retrieves current build metadata from the official LineageOS updater API.
- Shows LineageOS version, build date, filename, size, Android SDK, patch level and SHA-256.
- Downloads ROM ZIPs to the public Android Downloads folder.
- Background downloads with progress notifications.
- Pause/resume support using a resumable partial file when the server supports HTTP Range requests.
- Calculates SHA-256 locally after download and reports PASS/FAIL.
- Includes older builds from the TimSchumi LineageOS Build Archive as a clearly separated archive source.
- Dark mode and an iOS 26-inspired Liquid Glass visual style adapted for Android.
- Downloader/checker only: no bootloader unlocking, flashing, recovery modification or partition changes.

## Sources

### Official LineageOS

- Device metadata: `LineageOS/hudson` (`updater/devices.json`)
- Current builds: `download.lineageos.org/api/v2/devices/{codename}/builds`
- Build verification guidance: LineageOS Wiki

### Older builds

TimSchumi's archive is an independent, unofficial archive of old LineageOS builds. Archived builds may contain security issues and are unsupported by the LineageOS team. Always verify the SHA-256 and LineageOS signature before using an archived build.

## Important limitations

LineageOS does not retain every historical build on its official download servers. This app therefore cannot promise every historical release. The Archive section is an additional, clearly labelled unofficial source and is not presented as an official LineageOS service.

SHA-256 proves that the downloaded bytes match the expected digest. For stronger authenticity assurance, users should also follow LineageOS's official build-signature verification procedure.

## Build

The repository includes a GitHub Actions workflow that builds a debug APK on every push to `main` and on pull requests. The APK is uploaded as a workflow artifact.

## License

Apache License 2.0.
