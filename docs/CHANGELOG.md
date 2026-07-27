# Changelog Notes

## Unreleased

- Added an owner-only G4AR root research status and readiness assessment API.
- Added a large permanent-brick warning, verified/unverified evidence, hard
  stops, and a receive-only research checklist to the web Settings page.
- Added the G4AR Owner Root Research Guide with exact-board identification,
  voltage-safe serial discovery, backup, recovery, and unsafe-tool rejection
  requirements.
- Root execution remains disabled until a reproducible G4AR-specific chain and
  exact-device recovery process are independently verified.

This project started as a cleaned copy of the earlier TMHI watchdog codebase.

The current scaffold keeps the backend behavior but resets project identity:

- Canonical backend package: `backend/src/tmhi_control_center/`
- Local Docker image: `tmhi-control-center:local`
- Local Compose service: `tmhi-control-center`
- Active GitHub Actions Docker Hub publish workflow
- No copied app icon assets
