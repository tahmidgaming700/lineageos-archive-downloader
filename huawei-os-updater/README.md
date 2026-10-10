# Huawei OS Updater

An Android companion app for browsing Huawei firmware listings hosted by [Huawei Firm Finder](https://professorjtj.github.io/). This project follows the simple downloader-first approach of the LineageOS Archive Downloader, adapted for Huawei firmware archives.

## Features

- Opens Huawei Firm Finder and its v2, Base Archive, and Downgrade Archive pages inside the app.
- Uses the live website as the source of firmware listings; it does not invent or mirror firmware metadata.
- Supports the site's model, region (CXXX), vendor/country, and target-version searches.
- Opens firmware downloads using Android's download-capable browser/app handler.
- Includes refresh and Android back navigation.
- Downloader/browser only: it does not flash firmware, unlock bootloaders, or modify partitions.

## Source

- Main finder: https://professorjtj.github.io/
- Firm Finder v2: https://professorjtj.github.io/v2/
- Base archive: https://professorjtj.github.io/BaseArchive/
- Downgrade archive: https://professorjtj.github.io/Downgrade/

## Build

Open this `huawei-os-updater` folder as an Android Gradle project, or use the GitHub Actions workflow in `.github/workflows/huawei-os-updater.yml` to build a debug APK.

## Safety

Firmware compatibility depends on exact device model, region, vendor, and build. Verify all details and checksums when available. Firmware installation can erase data or brick a device. The app only browses and downloads files; installation is always a separate, user-controlled process.

## Attribution

This app is inspired by the user's LineageOS Archive Downloader project. It is not affiliated with Huawei or the Huawei Firm Finder website operator.