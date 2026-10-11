# Contributing

Thank you for helping improve Lineage Updater & Downloader.

## Before opening a pull request

1. Open or search an issue describing the bug or proposed feature.
2. Keep changes focused and preserve existing downloader behaviour.
3. Do not add device, build, checksum, compatibility, or release data that has not been verified from its source.
4. Never commit keystores, passwords, access tokens, private vendor files, or other secrets.
5. Add or update tests for changed behaviour.
6. Run the available Gradle checks and report the exact commands and results. Do not describe checks that were not run as passing.

## Safety requirements

- Downloading must remain useful without root.
- Do not add silent bootloader unlocking, data wiping, partition flashing, or package removal.
- Treat all firmware metadata and remote JSON as untrusted input.
- Clearly label official, community, and historical sources.
- A checksum only confirms equality with a trusted expected digest; it does not independently establish publisher identity.
- Flashing and recovery-development features must state their device-specific prerequisites and require explicit confirmation for destructive actions.

## Pull request checklist

- [ ] The change is scoped and documented.
- [ ] Tests cover the change or the missing test coverage is explained.
- [ ] Failure and offline states are handled.
- [ ] Accessibility and light/dark themes are considered.
- [ ] Source attribution and licensing are correct.
- [ ] No secrets or fabricated test results are included.
