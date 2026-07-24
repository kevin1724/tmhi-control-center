# Changelog

All notable changes to this new project will be documented here.

## [Unreleased]

### Added

- Added a Homelab readiness engine and `/api/homelab/snapshot` endpoint for
  setup scoring, next-best-action guidance, signal coaching, local adapter
  instructions, playbook cards, and redacted troubleshooting export.
- Added a dedicated web Homelab page with setup checklist, signal/antenna coach,
  router offload/SQM playbook, G4AR local adapter guide, and snapshot download.
- Added a native Android Homelab tab with matching setup, signal, playbook,
  adapter, and stock-backup guidance.

### Changed

- Updated the dashboard and settings copy to make the local adapter URL easier
  to understand for owner-controlled G4AR firmware/radio lab workflows.
- Renamed the Python package path to `backend/src/tmhi_control_center/`.
- Moved the Python app, tests, and package metadata under `backend/`.
- Removed the duplicate root-level Python files and legacy `app/` copy.
- Removed copied favicon, Apple touch icon, web manifest, and PNG app icons.
- Removed the old prebuilt-image and publish workflow references.
- Added a new active GitHub Actions workflow for tests, Docker builds, and Docker Hub publishing.
- Updated Docker Compose, tests, and docs for the new project name.
