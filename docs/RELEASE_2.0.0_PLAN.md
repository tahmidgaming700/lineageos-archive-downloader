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
- [ ] Verify current-build metadata and source attribution.
- [ ] Verify download cancellation/resume, duplicate prevention, and error handling.
- [ ] Verify checksum behaviour when the expected digest is missing or malformed.
- [ ] Verify offline/stale cache behaviour and last-sync timestamp.
- [ ] Exercise light/dark theme and navigation on supported Android versions.

## Planned feature areas

- [ ] GSI discovery with explainable, device-specific compatibility status.
- [ ] Recovery release tracking with official/community labels.
- [ ] DD Flasher: supported workflows only, explicit destructive-action confirmation.
- [ ] Harmony Recovery: configuration/build orchestration only where a real Linux build environment is available; never fabricate an image.
- [ ] Secure app update manifest and package-signature checks.
- [ ] Support Development page with real links only after eligibility and URLs are verified.

## Release automation

- [ ] CI build succeeds on the proposed commit.
- [ ] Unit tests and static checks succeed.
- [ ] APK exists and is non-empty.
- [ ] APK package ID/version/version code are inspected.
- [ ] APK signing certificate is compared with the intended update identity.
- [ ] SHA-256 is calculated from the actual release artifact.
- [ ] Release notes describe only implemented changes.
- [ ] GitHub release is published only after all required gates pass.

## Current caveat

The current build configuration uses the debug signing configuration for the release variant. This is not a safe basis for claiming in-place upgrade compatibility. Do not publish the resulting APK as a production upgrade until the signing identity has been confirmed.
