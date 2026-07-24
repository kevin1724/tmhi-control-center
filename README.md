# TMHI Control Center

A fresh control-center scaffold for T-Mobile Home Internet gateway management.

This repo keeps the working backend pieces from the earlier TMHI backend project,
but it is no longer wired to that original GitHub or container image. The GitHub
Actions workflow now builds and publishes this project as its own Docker image.

## Current Scope

- FastAPI backend and built-in browser dashboard
- Gateway overview API for device, cellular signal, network, Wi-Fi, and redacted
  advanced gateway data
- Wi-Fi SSID/radio controls and connected-device discovery with best-effort
  device identification
- G4AR Unlock / Radio Lab settings for owned secondhand gateways, with safety
  gates for adapter-based LTE/NSA, band/cell, backup, and firmware workflows
- Gateway reachability, login testing, and reboot request support
- Connectivity probes and reboot safety logic from the previous project
- SQLite event history
- Dockerfile and source-build Docker Compose setup
- Clean backend package layout under `backend/src/tmhi_control_center/`

## Guides

- [G4AR Firmware Lab Guide](docs/G4AR_FIRMWARE_LAB_GUIDE.md) - setup,
  stock-backup workflow, recovery checklist, and LTE/NSA testing guide.

## Project Layout

```text
tmhi-control-center/
+-- backend/
|   +-- src/
|   |   +-- tmhi_control_center/
|   |       +-- static/
|   |       +-- main.py
|   |       +-- gateway.py
|   |       +-- watchdog.py
|   |       +-- connectivity.py
|   |       +-- config.py
|   |       +-- credentials.py
|   |       +-- storage.py
|   |       +-- models.py
|   |       +-- cli.py
|   +-- tests/
|   +-- pyproject.toml
|   +-- requirements.txt
|   +-- requirements-dev.txt
+-- android/
+-- web/
+-- deploy/
+-- docs/
+-- .github/
+-- .env.example
+-- .gitignore
+-- Dockerfile
+-- docker-compose.yml
+-- LICENSE
+-- README.md
+-- SECURITY.md
+-- CONTRIBUTING.md
+-- CHANGELOG.md
+-- ACKNOWLEDGEMENTS.md
```

The Python app now lives under `backend/`; the root is reserved for orchestration,
docs, Android, and future standalone web work.

## Run Locally With Docker

```bash
docker compose up -d --build
docker compose logs -f tmhi-control-center
```

Open:

```text
http://localhost:8095/
```

From another device on your LAN, use the Docker host's LAN IP instead of
`localhost`.

## Diagnostic Commands

Inside the container:

```bash
docker compose exec tmhi-control-center \
  python -m tmhi_control_center.cli connectivity

docker compose exec tmhi-control-center \
  python -m tmhi_control_center.cli gateway-test
```

## GitHub Setup

The repo includes an active GitHub Actions workflow at
`.github/workflows/docker-publish.yml`.

On pull requests, it runs backend tests and verifies the Docker image builds. On
pushes to `main`, version tags like `v0.1.1`, or manual runs, it builds and
publishes a multi-architecture Docker Hub image.

The GitHub repository is:

```text
https://github.com/kevin1724/tmhi-control-center
```

## Docker Hub Publishing

Create a Docker Hub repository named:

```text
tmhi-control-center
```

Then configure the GitHub repository with:

- Actions variable: `DOCKERHUB_USERNAME`
- Actions secret: `DOCKERHUB_TOKEN`

From PowerShell, set the repository variable:

```powershell
gh variable set DOCKERHUB_USERNAME --repo kevin1724/tmhi-control-center --body kevina1724
```

Then set the repository secret. Paste your Docker Hub access token when prompted:

```powershell
gh secret set DOCKERHUB_TOKEN --repo kevin1724/tmhi-control-center
```

The workflow publishes to:

```text
YOUR_DOCKERHUB_USERNAME/tmhi-control-center
```

## Development

```bash
python -m venv .venv
. .venv/bin/activate
pip install -r backend/requirements-dev.txt
pytest -q backend
```

Run without Docker:

```bash
export PYTHONPATH=backend/src
export DATABASE_PATH=/tmp/tmhi-control-center.db
export WATCHDOG_ENV_PATH=/tmp/tmhi-control-center.env
export WATCHDOG_ENABLED=false
uvicorn tmhi_control_center.main:app --host 0.0.0.0 --port 8095
```

## App Icon

The copied favicon, Apple touch icon, web manifest, and PNG app icons were
removed. Add new branded assets later when the replacement app identity is ready.

## G4AR Unlock / Radio Lab

G4AR Unlock / Radio Lab is for owner-controlled Arcadyan TMO-G4AR gateways,
such as secondhand eBay or Amazon units. The goal is to support research around
stock backup, recovery, firmware override, and radio-mode experiments like
restoring or preferring LTE anchor / 5G NSA when that performs better than pure
5G Standalone.

Start with the [G4AR Firmware Lab Guide](docs/G4AR_FIRMWARE_LAB_GUIDE.md) before
enabling this mode.

The app intentionally does not provide transmit-power override controls. Upload
and download improvements should come from antenna aiming, tower comparison,
supported LTE/NSA or band/cell profiles, SQM/QoS, and repeated speed/latency
tests.

For upload preference, the project uses an adapter-facing QoS/SQM profile. That
means shaping and prioritizing traffic so upload stays responsive under load; it
does not increase cellular transmit power.

Firmware override is guarded by stock backup, calibration/identity backup,
SHA-256 hashes, verified recovery path, and exact typed consent before any future
local adapter is allowed to attempt a flash. The current app validates the
safety gate but does not write firmware.

T-Mobile publishes G4AR firmware history but not public firmware image
downloads. The app therefore focuses on creating a local stock backup from the
user's own gateway through a trusted adapter. In G4AR Unlock / Radio Lab mode,
the Settings page can request `POST /g4ar/firmware/backup` from the configured
adapter and saves the returned manifest/artifacts under `FIRMWARE_BACKUP_DIR`
(`/data/firmware-backups` by default). Adapter-returned inline artifacts must
include base64 content and, when provided, matching SHA-256 hashes.

## Disclaimer

TMHI Control Center is an unofficial community project and is not affiliated with,
endorsed by, or supported by T-Mobile. Custom firmware, modem commands, and
external antenna modifications may void warranty, brick hardware, break carrier
terms, or create RF compliance problems.
