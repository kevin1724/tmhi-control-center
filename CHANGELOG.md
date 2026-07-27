# Changelog

All notable changes to this new project will be documented here.

## [Unreleased]

### Added

- Added user-selectable speed-history retention for 30 days, 90 days, 6
  months, 1 year, or 2 years, plus an `All` chart range for retained results.
- Added 5, 10, 15, and 30-minute plus hourly speed-test schedules, a 125.8 MB
  Accurate profile, and server-side daily and 30-day usage estimates.
- Added 24-hour, 7-day, 30-day, and 1-year speed-history chart ranges with
  explicit high-volume confirmation in the web dashboard.
- Added separate LTE and 5G NR dashboard telemetry with signal power and
  quality, antenna selection, CQI, bandwidth, PCI, ARFCN, TAC, and cell/node
  identity where supported by gateway firmware.
- Added authenticated `/network/telemetry?get=cell` enrichment without making
  basic gateway telemetry depend on a saved login.
- Added SQLite-backed gateway telemetry snapshots with 14-day retention and a
  range-based `/api/gateway/telemetry/history` endpoint.
- Added responsive RSRP, SINR, and conditional temperature trend charts with
  1-hour, 6-hour, 24-hour, and 7-day views.
- Added a Homelab readiness engine and `/api/homelab/snapshot` endpoint for
  setup scoring, next-best-action guidance, signal coaching, Docker lab
  instructions, playbook cards, and redacted troubleshooting export.
- Added a dedicated web Homelab page with setup checklist, signal/antenna coach,
  router offload/SQM playbook, G4AR Docker workflow, and snapshot download.
- Added a native Android Lab tab with setup, signal, playbook, and encrypted
  Wi-Fi recovery guidance.
- Added a direct Docker recovery bundle with stock API inventory, Wi-Fi
  configuration, recovery notes, SHA-256 checksums, and ZIP download.
- Added a saved option to skip the G4AR recovery-bundle setup reminder while
  keeping firmware override locked behind backup, recovery, hash, and consent
  checks.
- Added a read-only G4AR USB-C 2.5GbE lab with normalized host, SuperSpeed,
  Ethernet-device, driver, interface, carrier, link-speed, and bridge checks.
- Added a gateway-side `GET /g4ar/usb/probe` adapter contract, privacy filtering,
  backend tests, and a staged USB-C hardware and recovery guide.
- Added a phone-local Android Wi-Fi recovery vault for gateway-exposed SSIDs and
  unmasked credentials, protected with Android Keystore encryption.

### Changed

- Moved automatic speed-test scheduling, test size, retention, traffic
  estimates, and save controls from the dashboard into Settings.
- Matched the Android interface to the web dashboard's visual system with the
  same neutral surfaces, dark navigation, magenta section labels, compact
  metric grids, and 8px bordered panels.
- Reduced Android screen density with expandable connection, radio, Wi-Fi,
  tower, setup, playbook, security, and owner-lab sections plus compact
  connected-device rows.
- Reworked the web dashboard into a denser health view with radio mode,
  temperature availability, uptime, connected-device count, live radio cards,
  and compact mobile navigation.
- Removed the web local-service URL setup and simplified the G4AR lab into three
  Docker-only steps.
- Reworked Android around a five-destination icon navigation bar, moved manual
  diagnostics into Profile, encrypted saved secrets, and removed the obsolete
  Android local-adapter workflow.
- Replaced the 30-second full-page refresh with a visibility-aware, one-minute
  status and gateway telemetry poll.
- Renamed the Python package path to `backend/src/tmhi_control_center/`.
- Moved the Python app, tests, and package metadata under `backend/`.
- Removed the duplicate root-level Python files and legacy `app/` copy.
- Removed copied favicon, Apple touch icon, web manifest, and PNG app icons.
- Removed the old prebuilt-image and publish workflow references.
- Added a new active GitHub Actions workflow for tests, Docker builds, and Docker Hub publishing.
- Updated Docker Compose, tests, and docs for the new project name.
