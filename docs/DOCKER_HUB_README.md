# TMHI Control Center

Self-hosted dashboard and control app for T-Mobile Home Internet gateways.

TMHI Control Center runs locally in Docker and gives users a cleaner way to view
gateway status, signal quality, cellular details, connected devices, tower map
data, Wi-Fi settings, diagnostics, and advanced owner-controlled G4AR lab
workflows.

It also includes a Homelab control room with a setup score, next-best-action
guidance, signal and antenna coaching, router offload/SQM planning, redacted
snapshot export, and a direct Docker recovery workflow for owned G4AR lab hardware.

This is an unofficial community project. It is not affiliated with, endorsed by,
or supported by T-Mobile.

## Image

```text
kevina1724/tmhi-control-center:latest
```

Multi-architecture builds are published for:

- `linux/amd64`
- `linux/arm64`

## Quick Start

```bash
docker run -d \
  --name tmhi-control-center \
  --restart unless-stopped \
  -p 8095:8000 \
  -v tmhi_control_center_data:/data \
  kevina1724/tmhi-control-center:latest
```

Open:

```text
http://localhost:8095/
```

From another device on the LAN, replace `localhost` with the Docker host's LAN
IP address.

## Docker Compose

```yaml
services:
  tmhi-control-center:
    image: kevina1724/tmhi-control-center:latest
    container_name: tmhi-control-center
    restart: unless-stopped
    ports:
      - "8095:8000"
    volumes:
      - tmhi_control_center_data:/data
    security_opt:
      - no-new-privileges:true
    cap_drop:
      - ALL

volumes:
  tmhi_control_center_data:
```

## First-Time Setup

1. Open the dashboard.
2. Go to `Settings`.
3. Save the gateway admin password.
4. Click `Test`.
5. Return to `Dashboard` and click `Refresh`.
6. Optional: add an OpenCellID API key for tower lookups.
7. Optional: save a map center for more accurate nearby-tower searches.

Most T-Mobile Home Internet gateways use:

```text
Gateway host: 192.168.12.1
Gateway API port: 8080
Username: admin
```

## Main Features

- Live gateway and internet status dashboard.
- Cellular signal scoring with RSRP, RSRQ, SINR, RSSI, and bars when available.
- Separate LTE and 5G NR cards with antenna source, CQI, bandwidth, PCI,
  EARFCN/NR-ARFCN, TAC, cell ID, and eNB/gNB identity when available.
- SQLite-backed signal history with 1-hour, 6-hour, 24-hour, and 7-day RSRP,
  SINR, and real gateway-temperature charts that are omitted when unavailable.
- Optional speed history from 5-minute intervals through monthly schedules,
  with a 24-hour-first chart, three test sizes, and up-front data-use estimates.
- Gateway model, firmware, uptime, update state, WAN, radio mode, registration,
  roaming, and cellular identity details.
- Wi-Fi SSID and gateway Wi-Fi radio controls when supported by the gateway API.
- Connected-device list with best-effort vendor/device identification.
- Tower map using Leaflet, OpenStreetMap, and optional OpenCellID data.
- Public-IP or saved-location map centering.
- Homelab readiness checklist, setup coach, signal tuning tips, and router
  offload/SQM workflow guidance.
- Redacted troubleshooting snapshot export for before/after placement notes.
- Connectivity probes, event history, and diagnostic sweeps.
- Reboot safety logic with dry-run mode, grace periods, cooldowns, and daily
  reboot limits.
- G4AR Unlock / Radio Lab settings for owner-controlled Arcadyan TMO-G4AR units.
- Read-only G4AR USB-C 2.5GbE probing for USB host mode, 5Gbps negotiation,
  driver/interface readiness, carrier, link speed, and LAN bridge membership.
- Owner-only G4AR root research readiness with a large brick warning, verified
  versus unverified evidence, and read-only backup/recovery gates.

## Volumes

The app stores settings, event history, and G4AR backup metadata under:

```text
/data
```

Keep this volume if you want saved gateway login, map settings, OpenCellID
configuration, event history, 14-day telemetry history, and firmware-lab backup
manifests to persist. It also keeps up to two years of bounded speed-test
history when that feature is enabled.

## Common Environment Variables

| Variable | Default | Purpose |
| --- | --- | --- |
| `GATEWAY_HOST` | `192.168.12.1` | Gateway LAN IP |
| `GATEWAY_PORT` | `8080` | Gateway local API port |
| `GATEWAY_USERNAME` | `admin` | Gateway API username |
| `WEB_PORT` | `8095` | Host port when using Compose |
| `DRY_RUN` | `true` | Keeps reboot automation disabled until tested |
| `WATCHDOG_ENABLED` | `true` | Enables the Docker watchdog loop |
| `MAP_RADIUS_KM` | `0.8` | Tower lookup radius |
| `PUBLIC_IP_LOCATION_ENABLED` | `true` | Allows rough public-IP map fallback |
| `OPENCELLID_API_KEY` | empty | Optional tower lookup key |
| `ADVANCED_SKIP_STOCK_BACKUP` | `false` | Suppress the G4AR stock-backup setup reminder only |
| `FIRMWARE_BACKUP_DIR` | `/data/firmware-backups` | G4AR lab backup storage |
| `SPEEDTEST_CADENCE` | `disabled` | `every_5_minutes`, `every_10_minutes`, `every_15_minutes`, `every_30_minutes`, `hourly`, `daily`, `weekly`, or `monthly` |
| `SPEEDTEST_PROFILE` | `gentle` | `gentle` (12.6 MB), `standard` (31.5 MB), or `accurate` (125.8 MB) per run |

The app also creates `/data/control-center.env` and can update settings from the
web UI.

## G4AR Unlock / Radio Lab

The G4AR lab is for owner-controlled Arcadyan TMO-G4AR gateways only, such as
secondhand units purchased outside of a carrier lease.

The lab can store:

- LTE anchor / 5G NSA, LTE-only, 5G SA, or scan-only intent.
- A downloadable stock-API recovery bundle with gateway and Wi-Fi inventory.
- SHA-256 checksums, a manifest, and recovery notes.
- Optional recovery-bundle reminder suppression while exploring the UI.
- Risk acknowledgement and consent-gate state.

No second service or URL is required. Save the gateway IP and admin password,
enable the owner lab, then create and download the recovery ZIP from `Settings`.
Bundles are stored in `/data/firmware-backups`.

The bundle is not a raw firmware image. Stock G4AR firmware does not expose
eMMC, boot, calibration, identity, or NVRAM partitions through its local network
API, so a separate verified raw partition backup is still required before any
future firmware-writing research.

Skipping the stock-backup reminder does not unlock flashing. Firmware override
still requires verified backup, recovery, hashes, and exact consent.

Important limitations:

- This image does not provide firmware downloads.
- This image does not read or write raw firmware partitions.
- This image does not currently root the G4AR. No reproducible G4AR root chain,
  complete restore path, or supported OpenWrt image has been verified.
- This image does not increase transmit power.
- Stock gateway firmware may not expose tower locking or LTE/NSA controls.
- Custom firmware, modem commands, and antenna modifications can brick hardware,
  void warranty, break service terms, or create RF compliance problems.

## Security Notes

- Run this dashboard only on a trusted LAN.
- Do not expose it directly to the public internet.
- Keep `/data` backed up if using advanced lab features.
- Store gateway credentials only on systems you control.
- Speed tests use Cloudflare's public measurement endpoints. Test traffic and
  measurement metadata leave the LAN; the result history is stored locally.

## Links

- GitHub: https://github.com/kevin1724/tmhi-control-center
- Docker Hub namespace: https://hub.docker.com/repositories/kevina1724
