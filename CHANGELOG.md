# Changelog

Entries distinguish source changes from published releases. A source version is not published until a matching GitHub Release exists.

## [2.0.1] — Preparing release; not yet published

- Bumped Android version to `2.0.1` (version code `2001`).
- Fixed DD Flasher command separation so preflight checks, block-device size checks, image writes and success reporting execute in the intended order.
- Added a guard that rejects images larger than the selected target block partition.
- Connected completed raw GSI `.img` downloads to the DD Flasher, including a clear warning when the source does not publish a SHA-256 digest.
- Limited GSI DD target choices to `system`, `system_a` and `system_b`; other image types retain the general DD target list.
- Clarified GSI asset labels for raw images, compressed images and ZIP archives.
- Compressed `.img.xz`/`.img.gz` assets are download-only until extracted to raw `.img`; ZIP archives are not raw DD images.

### Release gates still required

- GitHub Actions unit tests and release build must pass on the final commit.
- Publish `v2.0.1` using the release workflow and verify that the signed APK and checksum are attached.
- Test on supported hardware before describing any GSI/partition combination as device-validated. No physical-device test is claimed here.

## [2.0.0] — Published

- Introduced the application ID `com.tahmidgaming.lineageupdater`, so 2.x installs separately from the legacy package.
- Added live GSI release discovery with architecture filters and compatibility warnings.
- Added a configurable Harmony Recovery release tracker and build-script planner; neither fabricates a recovery image.
- Added APK update checks requiring source SHA-256, matching package ID, matching signing-certificate fingerprint and a newer version before opening Android's installer.
- Added daily connected-network app update checks and local cache fallback for public LineageOS device/build and archive metadata.
- Added architecture filters for ARM64, ARM, x86_64 and x86 GSI assets, plus root-only recovery ZIP handoff and DD image flashing.
- Added native changelog, diagnostics and Support Development screens.
- Changed release automation to require production signing secrets and verify the release certificate before publishing.

## [1.0.37] — Unreleased on the previous default branch

- Reject malformed or non-HTTP(S) download URLs before starting network work.
- Reject unsafe download filenames containing path separators or control characters.
- Reject unsolicited HTTP 206 Partial Content when no resume range was requested.
- Queue each download as unique work to reduce duplicate workers for the same download ID.

## Historical published releases

- [2.0.0](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v2.0.0) — published release.
- [1.0.36](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.36) — published release.
- [1.0.35](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.35) — published release.
- [1.0.32](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.32) — published release.
- [1.0.31](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.31) — published release.
- [1.0.3](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.3), [1.0.1](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.1) and [1.0.0](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.0) — published releases.

Versions without a published tag/release or verified commit history remain undocumented rather than being assigned invented features or dates. The skipped 1.0.3.4 entry is recorded only as the project owner's account, not as a verified release.
