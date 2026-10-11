# Changelog

This file distinguishes confirmed repository history from planned work. An entry is not evidence that a version was published unless a matching GitHub release or tag exists.

## [2.0.0] — Development branch; not released

### Development foundation
- Added a release gate document covering signing identity, upgrade compatibility, testing, verification and publication.
- Added contribution and security-reporting guidance.
- Set the development version to 2.0.0 (version code 2000) on the `release/2.0.0-foundation` branch.
- Updated project documentation to distinguish implemented functionality from planned work.

### Not yet claimed
The GSI discovery, full Harmony Recovery build orchestration, secure self-updater, broader recovery tracking and all requested device compatibility testing are not claimed complete by this foundation commit. Do not publish a release until implementation and validation gates are met.

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
