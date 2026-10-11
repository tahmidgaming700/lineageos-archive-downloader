# Version 2.0.0 release gate

This document tracks the intended major release. A checklist item is not complete until there is repository or test evidence.

## Identity and compatibility

- [x] Preserve application ID: `com.tahmidgaming.lineagearchive`.
- [x] Set development version to `2.0.0` / version code `2000`.
- [ ] Confirm the signing certificate of the currently installed/published app.
- [ ] Configure a secure release keystore outside the public repository.
- [ ] Build and test an upgrade from a real 1.0.36 (and latest 1.0.x) installation.
- [ ] Publish only after signature compatibility is established.

## Core application

- [ ] Audit existing ROM discovery and archive flows for regressions.
- [ ] Validate the GSI architecture filters and generated Harmony Recovery build scripts against real source repositories.
- [ ] Verify current-build metadata and source attribution.
- [ ] Verify download cancellation/resume, duplicate prevention, and error handling.
- [ ] Verify checksum behaviour when the expected digest is missing or malformed.
- [x] Add offline fallback for public LineageOS device/build and archive metadata, with cache freshness surfaced in diagnostics.
- [ ] Exercise offline/stale cache behaviour and last-sync timestamp on physical devices.
- [ ] Exercise light/dark theme and navigation on supported Android versions.

## Planned feature areas

- [x] Community GSI release discovery and source links; device-specific compatibility remains a manual check and must not be implied by discovery.
- [x] Configurable GitHub recovery release tracker; official/community status is determined by the configured source, not guessed.
- [x] Root-only DD Flasher controls with an explicit destructive-action confirmation; device/partition validation still requires physical-device testing.
- [x] Harmony Recovery release tracking through a user-configured GitHub repository and a build-script planner for a real Linux host. The app does not build or fabricate recovery images on-device; generated commands still require validation against the chosen source tree.
- [x] APK self-update checks require a SHA-256 digest, matching package ID, matching signing certificate, and newer version before invoking Android's installer.
- [x] Schedule a daily connected-network app update check with user-visible notifications; installation remains user-confirmed.
- [x] Support-development page links to the project and issue tracker; no financial link is shown until a real destination is verified.

## Release automation

- [ ] CI build and unit tests succeed on the latest proposed commit.
- [ ] APK exists and is non-empty.
- [ ] APK package ID/version/version code are inspected.
- [ ] APK signing certificate is compared with the intended update identity.
- [ ] SHA-256 is calculated from the actual release artifact.
- [ ] Release notes describe only implemented changes.
- [ ] GitHub release is published only after all required gates pass.

## Current caveat

The release variant now requires an externally supplied production keystore. The release workflow requires repository secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, and `ANDROID_CERT_SHA256`, then compares the APK signer against that fingerprint before publishing. These secrets and the installed app's expected signer have not been confirmed here. Do not publish or install over an existing app until the fingerprint is verified.
