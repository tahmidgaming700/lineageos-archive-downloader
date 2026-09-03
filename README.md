# LineageOS Archive Downloader

An Android downloader and SHA-256 verifier for official LineageOS builds.

## Project status

Early development. The first milestone is a functional Android app that discovers LineageOS devices dynamically, retrieves build metadata from official LineageOS infrastructure, downloads official ZIP files, and verifies their SHA-256 checksums.

## Important

This project does **not** host, mirror, modify, flash, or install LineageOS ROMs. Download URLs point to official LineageOS infrastructure. Installing LineageOS remains device-specific and must follow the official LineageOS installation instructions for the exact device and variant.

## Build with GitHub Actions

Every push to `main` and pull request runs the APK build workflow. The debug APK is uploaded as a workflow artifact.

## License

Apache License 2.0.
