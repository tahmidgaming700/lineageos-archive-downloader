# Huawei OS Updater

A native Android application for searching Huawei/Honor firmware through the Huawei Firm Finder / compatible HiSuite Proxy workflow, then downloading packages with background progress and local integrity checks.

> **Status:** Native UI and download integration are under development. The website `https://professorjtj.github.io/` is a static JavaScript frontend. Its ROM-status and file-list actions call a compatible HiSuite Proxy service; it is not a documented public JSON firmware API. The app cannot query the site's search results unless a reachable compatible proxy is configured.

## Native app features

- Native Jetpack Compose interface; no WebView-based app shell.
- Firmware search fields for model, region, vendor/country and target version.
- ROM status and file-list requests through a configurable HiSuite Proxy base URL.
- Best-effort extraction of direct firmware package links from supported response formats.
- Background downloads using WorkManager, with progress notifications and HTTP Range resume where the server supports it.
- Local SHA-256 calculation after download, and comparison when a trusted checksum is supplied by the source.
- Persistent download history.
- Light, dark and system theme options.
- Shortcuts to Firm Finder, Firm Finder v2, BaseArchive and downgrade resources.

## Firmware search connection

Default proxy address: `http://127.0.0.1:7777`

That address means **the same device running the app**. It will only work if a compatible HiSuite Proxy service is running on the Android device itself. If the proxy runs on a computer or another device, set the base URL to that device's reachable LAN address, for example `http://192.168.1.20:7777`, and ensure the proxy accepts connections from the phone. Do not expose the proxy directly to the public internet.

The app uses the proxy's `/checkRom.txt` and `/getFile.txt` endpoints. Their request/response details may vary by proxy version. If the proxy returns an unfamiliar format, the app displays the raw response rather than claiming it found firmware.

## Download safety

- Downloading firmware is not the same as installing it.
- Verify the exact model, region, vendor and build before using a package.
- SHA-256 is checked only when a trusted expected digest is available; a locally calculated digest by itself does not prove authenticity.
- No automatic flashing, bootloader unlocking, partition modification or recovery installation is performed by the Finder screen.

## Build

Open this repository in Android Studio or run:

```bash
gradle :app:assembleDebug
```

GitHub Actions builds a debug APK for pushes to `main` and `huawei-os-updater`, pull requests, and manual workflow dispatches. The APK is a debug-signed development build, not a Play Store release.

## Original project

This project started from the native LineageOS Archive Downloader codebase. The Huawei branch switches the launcher experience to a Huawei firmware finder and reuses the existing download worker, history store, resumable transfer logic and SHA-256 verification implementation.

## License

Apache License 2.0.
