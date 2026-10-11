# Security policy

## Reporting a vulnerability

Please do not publish exploit details or private user data in a public issue. Use GitHub's private vulnerability reporting feature for this repository if it is enabled. Otherwise, contact the repository maintainer privately through a verified GitHub contact method.

Include the affected version, Android version/device where relevant, reproduction steps, expected behaviour, actual behaviour, and any safe proof-of-concept. Do not include passwords, signing keys, access tokens, or personal data.

## Security expectations

- The app must not bypass Android package-signature verification.
- Never trust a checksum or update manifest fetched from an untrusted or unauthenticated source.
- Never silently perform destructive device operations.
- Root-dependent functionality must be opt-in and clearly identified.
- Firmware, ROM, recovery and GSI downloads must show their source and available verification metadata.
- A successful compile is not proof that a recovery image is safe or has booted successfully.
