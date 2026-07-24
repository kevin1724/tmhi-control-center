# TMHI Control Center

Self-hosted dashboard and control app for T-Mobile Home Internet gateways.

TMHI Control Center runs locally in Docker and gives users a cleaner way to view
gateway status, signal quality, cellular details, connected devices, tower map
data, Wi-Fi settings, diagnostics, and advanced owner-controlled G4AR lab
workflows.

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
- Gateway model, firmware, WAN, cellular band, PCI, TAC/LAC, and cell ID details.
- Wi-Fi SSID and gateway Wi-Fi radio controls when supported by the gateway API.
- Connected-device list with best-effort vendor/device identification.
- Tower map using Leaflet, OpenStreetMap, and optional OpenCellID data.
- Public-IP or saved-location map centering.
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
configuration, event history, and firmware-lab backup manifests to persist.

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
- SHA-256 backup and firmware hashes.
- Risk acknowledgement and consent-gate state.

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
