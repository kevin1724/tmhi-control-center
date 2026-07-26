# TMHI Control Center

Self-hosted dashboard and control app for T-Mobile Home Internet gateways.

TMHI Control Center runs locally in Docker and gives users a cleaner way to view
gateway status, signal quality, cellular details, connected devices, tower map
data, Wi-Fi settings, diagnostics, and advanced owner-controlled G4AR lab
workflows.

It also includes a Homelab control room with a setup score, next-best-action
guidance, signal and antenna coaching, router offload/SQM planning, redacted
snapshot export, and a plain-English local adapter guide for owned G4AR lab
hardware.

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
  SINR, and conditional gateway-temperature charts.
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

## Volumes

The app stores settings, event history, and G4AR backup metadata under:

```text
/data
```

Keep this volume if you want saved gateway login, map settings, OpenCellID
configuration, event history, 14-day telemetry history, and firmware-lab backup
manifests to persist.

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

The app also creates `/data/control-center.env` and can update settings from the
web UI.

## G4AR Unlock / Radio Lab

The G4AR lab is for owner-controlled Arcadyan TMO-G4AR gateways only, such as
secondhand units purchased outside of a carrier lease.

The lab can store:

- Local adapter URL.
- LTE anchor / 5G NSA, LTE-only, 5G SA, or scan-only intent.
- Stock firmware backup metadata.
- Optional stock-backup reminder suppression while exploring the UI.
- SHA-256 backup and firmware hashes.
- Risk acknowledgement and consent-gate state.

The local adapter URL is not the stock gateway login page. When this Docker
image is running, the app automatically uses its built-in Docker adapter URL,
`http://127.0.0.1:8000`, so most users can leave the field blank.

The built-in Docker adapter is useful for health checks and default setup. Real
firmware backup, scan, tower lock, or radio-profile commands still require a
trusted hardware bridge controlled by the user, such as an OpenWrt/ROOTer
router, Raspberry Pi, mini PC, or Linux host. The adapter is expected to expose
narrow endpoints like `GET /health` and `POST /g4ar/firmware/backup`.

Skipping the stock-backup reminder does not unlock flashing. Firmware override
still requires verified backup, recovery, hashes, and exact consent.

Important limitations:

- This image does not provide firmware downloads.
- This image does not write firmware by itself.
- This image does not increase transmit power.
- Stock gateway firmware may not expose tower locking or LTE/NSA controls.
- Custom firmware, modem commands, and antenna modifications can brick hardware,
  void warranty, break service terms, or create RF compliance problems.

## Security Notes

- Run this dashboard only on a trusted LAN.
- Do not expose it directly to the public internet.
- Keep `/data` backed up if using advanced lab features.
- Store gateway credentials only on systems you control.

## Links

- GitHub: https://github.com/kevin1724/tmhi-control-center
- Docker Hub namespace: https://hub.docker.com/repositories/kevina1724
