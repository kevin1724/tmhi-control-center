# TMHI Control Center

TMHI Control Center is a self-hosted dashboard and control app for T-Mobile Home
Internet gateways. It is designed for users who want clearer gateway status,
signal visibility, connected-device information, tower mapping, Wi-Fi controls,
and safer advanced-lab workflows for owner-controlled hardware.

The app runs locally in Docker and opens in a web browser. It keeps settings and
event history in a local `/data` volume, so gateway credentials and lab settings
stay on the user's own system.

> TMHI Control Center is an unofficial community project. It is not affiliated
> with, endorsed by, or supported by T-Mobile.

## Download The Android App

Download the latest Android APK:

- [tmhi-control-center-debug.apk](https://github.com/kevin1724/tmhi-control-center/releases/download/android-latest/tmhi-control-center-debug.apk)

This is an early debug APK for testing. Android may ask the user to allow
installs from unknown sources. A signed release build will be added later.

## What It Does

- Shows live gateway health, internet status, cellular connection details, and
  signal quality.
- Separates 4G LTE and 5G NR measurements into radio cards with RSRP, RSRQ,
  SINR, RSSI, bars, CQI, band, bandwidth, antenna source, PCI, channel number,
  cell ID, and eNB/gNB identity when the gateway exposes them.
- Stores up to 14 days of compact telemetry in SQLite and graphs LTE/5G RSRP,
  SINR, and real gateway temperature readings across 1-hour, 6-hour, 24-hour,
  and 7-day ranges.
- Tracks download, upload, latency, and jitter with optional daily, weekly, or
  monthly low-impact speed tests. Scheduled samples rotate through four
  dayparts, run sequentially, and default to a roughly 12.6 MB data budget.
- Gives a Homelab readiness score with next-best-action guidance for setup,
  signal tuning, tower data, LAN inventory, watchdog safety, and G4AR backups.
- Displays useful gateway data such as model, firmware, uptime, WAN info,
  cellular band, PCI, TAC/LAC, cell ID, and radio state when available.
- Lists connected LAN/Wi-Fi devices with best-effort vendor and device guesses.
- Provides Wi-Fi controls for SSID changes and gateway Wi-Fi radio toggling when
  supported by the gateway API.
- Maps the connected serving cell and nearby towers using Leaflet,
  OpenStreetMap, and optional OpenCellID data.
- Uses public-IP location estimates or saved map coordinates to help center
  tower searches.
- Exports a redacted troubleshooting snapshot for placement notes, support
  requests, and before/after tuning records.
- Includes homelab workflows for router offload mode, SQM/QoS planning, antenna
  placement, tower notes, recovery discipline, and device inventory.
- Runs connectivity probes and records events for troubleshooting outages.
- Includes reboot safeguards from the original watchdog workflow.
- Includes a G4AR Unlock / Radio Lab for owned Arcadyan TMO-G4AR gateways, with
  explicit warnings, backup requirements, adapter-based controls, and consent
  gates.
- Adds a read-only G4AR USB-C 2.5GbE lab that checks USB host mode, 5Gbps
  negotiation, Ethernet adapter detection, driver binding, interface creation,
  carrier, link speed, and LAN bridge membership through a gateway-side adapter.

## Who It Is For

This app is useful for:

- Home Internet users who want a cleaner dashboard than the stock gateway UI.
- Users troubleshooting signal, tower, antenna, or placement issues.
- Users who want local logs and connectivity checks for unstable service.
- Advanced users working with owned secondhand G4AR units and external router or
  modem-lab setups.

This app is not a magic signal booster. It does not increase cellular transmit
power, bypass carrier network rules, or guarantee tower locking on stock
firmware.

## Current Status

TMHI Control Center is under active development. The web dashboard and backend
are usable today, but some gateway features depend on the model, firmware, and
what the local gateway API exposes.

Planned project areas:

- Standalone web frontend package.
- Additional gateway adapters.
- Safer local adapter tooling for owner-controlled modem labs.

The native Android app lives under [android/](android/). It runs locally on the
phone while the user has the app open and intentionally does not include the
web/Docker watchdog's 24/7 background internet monitoring.

Docker Hub page copy is available in
[docs/DOCKER_HUB_README.md](docs/DOCKER_HUB_README.md).

## Quick Start With Docker

Use the published Docker image:

```bash
docker run -d \
  --name tmhi-control-center \
  --restart unless-stopped \
  -p 8095:8000 \
  -v tmhi_control_center_data:/data \
  kevina1724/tmhi-control-center:latest
```

Open the dashboard:

```text
http://localhost:8095/
```

From another device on the same LAN, replace `localhost` with the Docker host's
LAN IP address.

## Run From Source

Clone the repo and build locally:

```bash
git clone https://github.com/kevin1724/tmhi-control-center.git
cd tmhi-control-center
docker compose up -d --build
docker compose logs -f tmhi-control-center
```

The included `docker-compose.yml` builds the image from source and maps the
dashboard to port `8095` by default. Change `WEB_PORT` in your environment if
another service already uses that port.

## First-Time Setup

1. Open `http://localhost:8095/`.
2. Go to `Settings`.
3. Save the gateway admin password.
4. Click `Test` to confirm the app can reach the gateway.
5. Go to `Dashboard` and click `Refresh`.
6. Review the `Setup Coach` card and open `Homelab` for the full readiness
   checklist.
7. Optional: add an OpenCellID API key under `Tower Data` for nearby tower
   lookups.
8. Optional: save a map center or use browser location so tower searches start
   near the gateway.

Most T-Mobile Home Internet gateways use:

```text
Gateway host: 192.168.12.1
Gateway API port: 8080
Username: admin
```

Some models expose the API differently. The app tries common same-host gateway
API variants when possible.

## Configuration

The app creates and updates a managed settings file inside the Docker data
volume:

```text
/data/control-center.env
```

Common settings:

| Setting | Default | Purpose |
| --- | --- | --- |
| `WEB_PORT` | `8095` | Host port used by Docker Compose |
| `GATEWAY_HOST` | `192.168.12.1` | Gateway LAN IP |
| `GATEWAY_PORT` | `8080` | Gateway local API port |
| `GATEWAY_USERNAME` | `admin` | Gateway API username |
| `MAP_RADIUS_KM` | `0.8` | Tower search radius |
| `PUBLIC_IP_LOCATION_ENABLED` | `true` | Use public IP as a rough map fallback |
| `OPENCELLID_API_KEY` | empty | Optional tower lookup key |
| `ADVANCED_SKIP_STOCK_BACKUP` | `false` | Suppress the G4AR stock-backup setup reminder without unlocking firmware work |
| `FIRMWARE_BACKUP_DIR` | `/data/firmware-backups` | Local G4AR backup storage |
| `SPEEDTEST_CADENCE` | `disabled` | Optional `daily`, `weekly`, or `monthly` speed history |
| `SPEEDTEST_PROFILE` | `gentle` | Per-run data budget: `gentle` or `standard` |
| `DRY_RUN` | `true` | Prevent automatic reboot actions while testing |

See [.env.example](.env.example) for the full reference.

## Dashboard Pages

`Dashboard` gives a live overview of internet status, gateway status, signal
quality, radio mode, gateway uptime, conditional temperature data, separate
LTE/5G telemetry, historical charts, low-impact speed history, cellular
details, and quick actions.

Speed history is disabled by default. The dashboard can schedule one test per
day, week, or month and rotates scheduled tests through night, morning,
afternoon, and evening. Download and upload samples run one after the other so
the feature does not create parallel test traffic. The gentle profile transfers
at most about 12.6 MB per run. Test traffic and measurement metadata go to
Cloudflare; saved results stay in the local SQLite database. See
[Low-Impact Speed History](docs/SPEED_TEST_HISTORY.md) for details.

Temperature is shown only when the gateway firmware returns a real sensor
reading. Stock G4AR firmware commonly omits it, so the dashboard removes the
temperature tile and graph instead of showing an unavailable measurement.

See [Gateway Telemetry](docs/GATEWAY_TELEMETRY.md) for the full field and
history reference.

`Devices` shows Wi-Fi configuration and connected LAN/Wi-Fi clients.

`Map` shows the serving cell, gateway/map center, nearby OpenCellID results, and
important notes about tower locking limitations.

`Homelab` shows the setup score, next action, signal and antenna coach, router
offload/SQM playbook, G4AR backup status, and a plain-English local adapter URL
guide.

`Diagnostics` shows connectivity probes, event history, raw gateway sections,
and repeated probe sweeps.

`Settings` stores gateway login, theme preference, tower data settings, watchdog
settings, and advanced G4AR lab settings.

## G4AR Unlock / Radio Lab

The G4AR Unlock / Radio Lab is for owner-controlled Arcadyan TMO-G4AR gateways,
such as secondhand units purchased outside of a carrier lease. It exists to make
advanced work safer and more organized, not to hide risk.

Use the lab for:

- Recording a local adapter URL.
- Creating and listing stock firmware backups through a trusted local adapter.
- Skipping the stock-backup setup reminder for now when you are only exploring
  the UI.
- Saving SHA-256 hashes for backup and firmware artifacts.
- Tracking LTE anchor / 5G NSA, LTE-only, 5G SA, and scan-only profile intent.
- Keeping firmware override work locked behind backup, recovery, hash, and
  consent requirements.
- Assessing owner-only G4AR root research readiness without pretending a
  verified root chain or OpenWrt image exists.
- Probing the G4AR USB-C data port before attempting a temporary 2.5GbE LAN
  bridge.

Read the guide before enabling this mode:

- [G4AR Firmware Lab Guide](docs/G4AR_FIRMWARE_LAB_GUIDE.md)
- [G4AR Owner Root Research Guide](docs/G4AR_ROOT_RESEARCH_GUIDE.md)
- [G4AR USB-C 2.5GbE Lab Guide](docs/G4AR_USB_C_2_5GBE_LAB.md)

The local adapter URL is not the stock gateway login page. It is the base URL of
a small HTTP service. When the Docker app is running, TMHI Control Center
automatically uses its built-in Docker adapter URL, `http://127.0.0.1:8000`, so
most users can leave this field blank.

The built-in Docker adapter is useful for health checks and default setup. Real
firmware backup, scan, tower lock, or radio-profile commands still require a
trusted hardware bridge controlled by the user, such as an OpenWrt/ROOTer
router, Raspberry Pi, mini PC, or Linux host attached to the modem lab hardware.
TMHI Control Center only calls narrow endpoints such as `GET /health` and
`POST /g4ar/firmware/backup`. The USB-C lab expects the gateway-side adapter to
provide a read-only `GET /g4ar/usb/probe` endpoint. Docker running on another
computer cannot inspect devices connected inside the G4AR.

The `Skip stock backup reminder for now` option only suppresses the readiness
checklist reminder. It does not unlock firmware override. The override gate
still requires verified backup, recovery, hashes, and exact consent.

Important limitations:

- The app does not provide firmware downloads.
- The app does not write firmware by itself.
- The app does not currently root the G4AR. No public, reproducible G4AR root
  chain, complete restore path, or supported OpenWrt image has been verified.
- The app does not provide transmit-power override controls.
- Stock gateway firmware may not expose tower locking or LTE/NSA controls.
- USB 3 and 2.5GbE are hardware-capable research targets, not a promise that
  stock firmware includes the required USB Ethernet driver or bridge support.
- Any custom firmware, modem commands, or external antenna modifications can
  brick hardware, void warranty, break service terms, or create RF compliance
  problems.

## Tower Data

The map uses Leaflet and OpenStreetMap tiles for display. Nearby tower searches
use OpenCellID when an API key is configured.

The app may estimate the map center using public IP location if no saved map
center is available. Public-IP location is approximate and may point to the ISP
or carrier exit location instead of the real gateway location. For best results,
save the gateway's actual latitude and longitude in `Map Center`.

## Watchdog Behavior

The watchdog checks internet connectivity with multiple probes and records the
results. Reboot actions are guarded by grace periods, cooldowns, daily reboot
limits, and `DRY_RUN`.

Keep `DRY_RUN=true` until gateway login and reboot behavior have been tested.

## Diagnostic Commands

Run these inside the container:

```bash
docker compose exec tmhi-control-center \
  python -m tmhi_control_center.cli connectivity

docker compose exec tmhi-control-center \
  python -m tmhi_control_center.cli gateway-test
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

## Docker Publishing

GitHub Actions builds and publishes the Docker image on pushes to `main`, version
tags, and manual workflow runs.

Published image:

```text
kevina1724/tmhi-control-center:latest
```

Repository settings required for publishing:

- Actions variable: `DOCKERHUB_USERNAME`
- Actions secret: `DOCKERHUB_TOKEN`

## Project Layout

```text
tmhi-control-center/
+-- backend/                  FastAPI app, gateway clients, tests, static UI
+-- android/                  Native Android app
+-- web/                      Future standalone web app placeholder
+-- deploy/                   Optional deployment examples
+-- docs/                     User and developer documentation
+-- .github/                  GitHub Actions workflows and templates
+-- Dockerfile                Production container image
+-- docker-compose.yml        Source-build local deployment
+-- README.md
```

The backend uses a `src/` layout under `backend/src/tmhi_control_center/`.

## Security And Safety

- Keep the dashboard on a trusted LAN.
- Do not expose the app directly to the public internet.
- Store gateway credentials only on systems you control.
- Keep backups of `/data` if using advanced lab features.
- Do not use unverified firmware images.
- Do not continue with custom firmware work unless a stock backup and recovery
  path have both been verified.

## License

See [LICENSE](LICENSE).
