# Changelog

All entries below describe changes confirmed by repository commits or releases. Older releases are linked to their GitHub release notes where available; this file does not infer undocumented historical features.

## [1.0.37] - Unreleased

### Fixed
- Reject malformed or non-HTTP(S) download URLs before starting network work.
- Reject unsafe download filenames containing path separators or control characters.
- Reject an unsolicited HTTP 206 Partial Content response when no resume range was requested, preventing a partial payload from being treated as a complete download.
- Queue each download as unique work so rapid taps or repeated resume actions do not start duplicate workers for the same download ID.

### Validation status
- Changes committed to the default branch; the GitHub Actions build and release workflow must finish successfully before this version is considered released.
- No physical-device test is claimed by this entry.

## Historical releases

- [1.0.36](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.36) — published release; consult the release and linked commit history for confirmed details.
- [1.0.35](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.35) — published release; consult the release and linked commit history for confirmed details.
- [1.0.32](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.32) — published release; consult the release and linked commit history for confirmed details.
- [1.0.31](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.31) — published release; consult the release and linked commit history for confirmed details.
- [1.0.3](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.3), [1.0.1](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.1), and [1.0.0](https://github.com/tahmidgaming700/lineageos-archive-downloader/releases/tag/v1.0.0) — published releases.
- Intermediate versions not represented by a published tag or confirmed commit are undocumented here rather than reconstructed from guesswork.
