# G4AR Docker Lab Guide

This guide is for Arcadyan TMO-G4AR gateways that the user owns outright, such
as a secondhand unit purchased outside a carrier lease. Opening the gateway,
changing radio behavior, or writing firmware can permanently brick it, erase
device-specific data, void warranty, break service terms, or create RF
compliance problems.

TMHI Control Center does not download firmware, bypass verified boot, root the
gateway, or write flash partitions. Its Docker lab records the stock state that
the gateway exposes and keeps unsupported firmware operations locked.

## What You Need

- TMHI Control Center running in Docker on the same LAN as the gateway.
- A G4AR that you own and can afford to replace.
- The gateway IP address and current admin password.
- A mounted `/data` Docker volume so bundles survive container replacement.

No second service or control URL is required.

## Step 1: Save The Gateway Login

1. Open `Settings`.
2. Enter the gateway IP address, usually `192.168.12.1`.
3. Enter the current admin password.
4. Keep `Remember login` enabled.
5. Select `Save Login`, then select `Test`.

The recovery button remains disabled until the login is saved and the gateway
can be read.

## Step 2: Enable The Owner Lab

1. In `Settings`, find `G4AR Docker Lab`.
2. Turn on `Owner lab enabled`.
3. Read and accept the ownership and hardware-risk warning.
4. Optionally choose upload-priority and radio-profile labels for comparison
   notes. Stock firmware may not expose commands to apply those choices.
5. Select `Save Lab Settings`.

The app now talks directly to the configured gateway from the Docker container.

## Step 3: Create And Download A Recovery Bundle

1. Select `Create Recovery Bundle`.
2. Wait for the bundle to appear in the list.
3. Select `Download ZIP`.
4. Keep the ZIP with the gateway's purchase record and physical revision notes.

Docker also keeps the bundle under `/data/firmware-backups`.

Each ZIP contains:

- `gateway-snapshot.json` with firmware, hardware, connection, radio, signal,
  system, and telemetry inventory exposed by the stock API.
- `wifi-configuration.json` with the redacted Wi-Fi configuration exposed by the
  stock API.
- `restore-notes.md` explaining what the bundle can and cannot recover.
- `SHA256SUMS` for integrity checking.
- `backup-manifest.json` with bundle metadata and limitations.

## Important Backup Limitation

The ZIP is a Docker recovery bundle, not a raw firmware image. Stock G4AR
firmware does not expose eMMC, bootloader, calibration, identity, or NVRAM
partitions through its local network API. The bundle cannot restore those
partitions and never unlocks the firmware override gate.

Before any future firmware-writing research, a separate hardware-specific
process would need to produce and verify a complete raw partition backup and a
tested offline recovery path for that exact unit. TMHI Control Center does not
currently provide that process because no reproducible G4AR writer and recovery
method has been verified by this project.

## Advanced Research Area

The root and firmware controls are collapsed under `Advanced root and firmware
research`. Open this area only after downloading the Docker bundle and reading
the warnings.

The radio profile is stored as research intent for controlled before-and-after
comparisons. It does not prove that the stock G4AR API can force LTE, NSA, SA,
band, or tower selection. Unsupported commands remain visibly unavailable.

The `Skip this reminder for now` checkbox only hides the setup reminder. It does
not turn the Docker bundle into a raw backup and does not weaken the flash gate.

## Troubleshooting

`Create Recovery Bundle` is disabled:

- Save and test the gateway login.
- Enable the owner lab and accept the warning.
- Select `Save Lab Settings` before creating the bundle.

The bundle request fails:

- Confirm the Docker host can reach the configured gateway IP.
- Confirm the admin password is current.
- Check `docker logs tmhi-control-center` for the gateway API error.
- Confirm `/data` is writable and has free space.

The ZIP is missing Wi-Fi data:

- Some firmware versions do not expose authenticated Wi-Fi configuration.
- The bundle will still save available gateway inventory and record the Wi-Fi
  error in its manifest and recovery notes.

## Hard Stops

Stop if the gateway is leased, financed, carrier-owned, service-critical, or
not replaceable. Do not treat a downloaded recovery ZIP as proof that raw flash
recovery is possible. Firmware writing stays unavailable until an exact-device,
reproducible, independently reviewed recovery method exists.
