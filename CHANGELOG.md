# Changelog

This file distinguishes confirmed repository history from planned work. An entry is not evidence that a version was published unless a matching GitHub release or tag exists.

## [2.0.0] — Development branch; not released

- Introduced a new Android application ID, `com.tahmidgaming.lineageupdater`, so 2.0.0 installs as a separate app instead of replacing the legacy package. Existing 1.0.35/1.0.36 installations will not update in place.

### Implemented in the development branch
- Added live GSI release discovery from community Treble/LineageOS GitHub sources, with clear compatibility warnings and source links.
- Added a configurable Harmony Recovery release tracker using real GitHub release assets; it does not fabricate or build recovery images.
- Added APK update checks that require a source SHA-256, matching package ID, matching signing-certificate fingerprint, and a newer version before opening the Android installer.
- Added a Harmony Recovery Build Lab that generates a user-configured Linux build script; it does not fabricate an image on the phone.
- Added a daily connected-network app update check that notifies users when a newer release is available; installation remains user-confirmed and signature-checked.
- Added local cache fallback for public LineageOS device/build metadata and archive metadata when the network request fails.
- Added architecture filters for ARM64, ARM, x86_64, and x86 GSI assets, plus explicit boot/system DD Flasher targets.
- Added native changelog, diagnostics with copyable privacy-conscious reports, and a Support Development screen.
- Added unit tests for numeric version comparison and update detection.
- Fixed duplicate worker declaration and invalid Kotlin escape sequence that blocked compilation.
- Changed release automation to require production signing secrets and verify the release certificate before publishing.
- Corrected minimum-Android-version text and APK MIME metadata.

### Still required before release
- CI must pass on the latest commit, including the unit tests.
- Confirm signing compatibility with the actual installed app and test upgrades on devices.
- Validate GSI compatibility and recovery/DD flashing behaviour on supported hardware.
- Validate generated Harmony Recovery build commands against each supported source tree and real Linux build hosts.
- Configure the release signing secrets and verify the production release workflow.
- Configure a verified funding link if financial support is to be offered.

This is still a development branch, not a published 2.0.0 release.

## [1.0.37] — Unreleased on the previous default branch

- Reject malformed or non-HTTP(S) download URLs before starting network work.
- Reject unsafe download filenames containing path separators or control characters.
- Reject unsolicited HTTP 206 Partial Content when no resume range was requested.
- Queue each download as unique work to reduce duplicate workers for the same download ID.

The repository's latest CI release workflow for the default branch reported a build failure on 2026-10-11. Check [Actions](https://github.com/tahmidgaming700/lineageos-archive-downloader/actions) for current results. No physical-device test is claimed here.

## Historical published releases

- [1.0.36](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.36) — published release.
- [1.0.35](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.35) — published release.
- [1.0.32](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.32) — published release.
- [1.0.31](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.31) — published release.
- [1.0.3](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.3), [1.0.1](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.1), and [1.0.0](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.0) — published releases.

Versions not supported by a published tag, release, or verified commit history remain undocumented rather than being assigned invented features or dates. The skipped 1.0.3.4 entry is recorded only as the project owner's account, not as a verified release.
