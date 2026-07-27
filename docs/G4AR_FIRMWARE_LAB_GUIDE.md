# G4AR Firmware Lab Guide

This guide is for owner-controlled Arcadyan TMO-G4AR gateways only, such as
secondhand units purchased outside of a carrier lease. Custom firmware, modem
commands, radio overrides, and recovery work can permanently brick the gateway,
erase calibration data, void warranty, break service terms, or create RF
compliance problems.

TMHI Control Center does not download old firmware images, bypass carrier
protections, or write firmware by itself. The app gives you a safer workflow:
save gateway login, configure a trusted local adapter, create a stock backup,
record hashes, choose a radio-profile intent, and keep flash controls locked
behind explicit consent.

This firmware workflow is not a verified G4AR rooting method. For the current
root status, equipment requirements, receive-only discovery order, and hard
stops, read the [G4AR Owner Root Research Guide](G4AR_ROOT_RESEARCH_GUIDE.md).

## What You Need

- A G4AR gateway that you own and can afford to recover or replace.
- Current gateway admin password saved in TMHI Control Center.
- The built-in Docker adapter URL, or a trusted hardware adapter running on your
  LAN for real modem operations.
- A complete stock backup from this exact gateway.
- A recovery method verified on this exact gateway before any firmware writing.
- SHA-256 hashes for every backup and firmware artifact.
- Basic test notes: location, antenna setup, serving cell, band, SINR, RSRP,
  RSRQ, ping, packet loss, download, and upload.

Do not use random firmware files from forums or file hosts. T-Mobile publishes
G4AR firmware history, but not official public firmware images. If old firmware
is not from your own verified backup or an authorized source, treat it as unsafe.

## What The Local Adapter Must Do

The Docker app automatically provides a built-in adapter URL:

```text
http://127.0.0.1:8000
```

You can leave `Local adapter URL` blank in the web UI and TMHI Control Center
will save that Docker default for you. The built-in adapter answers health
checks, removes setup guesswork, and keeps the UI consistent.

For real stock firmware backup, cell scan, tower lock, or radio-profile changes,
you still need a trusted hardware-specific adapter. That adapter is the bridge
between TMHI Control Center and your owned gateway or router/modem environment.
Keep it local-only.

Required backup endpoint:

```text
POST /g4ar/firmware/backup
```

TMHI Control Center sends JSON like this:

```json
{
  "device": "Arcadyan TMO-G4AR",
  "reason": "ui_request",
  "radio_profile": "prefer_lte_anchor_nsa",
  "requested_at": "2026-07-24T00:00:00+00:00",
  "expected_artifacts": [
    "stock-firmware.bin",
    "partition-table.txt",
    "calibration-and-identity-backup.tar",
    "restore-notes.md",
    "SHA256SUMS"
  ]
}
```

The adapter should return a manifest with artifact metadata. Inline artifacts
can be saved by the app when they include base64 content:

```json
{
  "device": "Arcadyan TMO-G4AR",
  "firmware_version": "1.00.12",
  "hardware_revision": "your-unit",
  "artifacts": [
    {
      "name": "stock-firmware.bin",
      "content_base64": "base64-encoded-backup",
      "sha256": "64-character-sha256"
    }
  ],
  "notes": [
    "Backup created from this exact gateway."
  ]
}
```

The app saves the returned manifest and inline artifacts under
`FIRMWARE_BACKUP_DIR`, which defaults to `/data/firmware-backups`.

## Step 1: Save Gateway Login

1. Open TMHI Control Center.
2. Go to `Settings`.
3. Enter the G4AR admin password.
4. Keep `Remember login` enabled.
5. Click `Save Login`.
6. Click `Test` and make sure the gateway responds.

If login is not saved, the dashboard can still show some basic status, but Wi-Fi
controls and deeper gateway data may not work.

## Step 2: Configure G4AR Unlock / Radio Lab

1. Go to `Settings`.
2. Find `G4AR Unlock / Radio Lab`.
3. Set `Control mode` to `G4AR unlock / radio lab`.
4. Leave `Local adapter URL` blank to use the built-in Docker default, or set it
   to your hardware adapter, for example:

```text
http://192.168.12.50:8080
```

5. Choose a `G4AR radio profile`.
6. Read the warning.
7. Check the ownership/risk acknowledgement.
8. Optionally check `Skip the stock backup reminder for now` if you are only
   exploring the UI and do not want the readiness checklist to keep prompting.
9. Click `Save Unlock Lab`.

The app stores the selected profile and adapter URL. If the field was blank, the
saved URL becomes `http://127.0.0.1:8000`. A hardware adapter is responsible for
any real device-specific command support.

Skipping the reminder does not count as a backup and does not unlock firmware
override. The flash gate still requires backup, recovery, hashes, and exact
typed consent.

## Step 3: Create A Stock Backup

1. Stay on `Settings`.
2. In `Stock firmware backup`, click `Create Stock Backup`.
3. Wait for the adapter to finish.
4. Confirm the backup appears in the backup history list.
5. Confirm the saved manifest contains the current firmware version and hashes.
6. Keep an offline copy of the backup folder.

If the adapter returns an inline artifact with a valid SHA-256 hash, the app can
auto-fill the `Stock backup SHA-256` field in the firmware override gate.

Do not move forward until you have backed up:

- Stock firmware image.
- Partition layout.
- Calibration and identity data.
- MAC/config/NVRAM-style data.
- Recovery notes.
- SHA-256 checksums.

## Step 4: Verify Recovery First

Before any custom firmware work, prove that recovery works on the exact unit.

Minimum recovery checklist:

- You can reach the gateway or adapter after a failed boot attempt.
- You can restore the stock backup.
- You know where the backup folder is saved.
- You have verified backup hashes.
- You have written notes for the physical steps required.

If recovery is not tested, stop here.

## Step 5: Try To Get LTE / 4G Back Safely

The goal is not to increase transmit power. The safer goal is to compare whether
LTE anchor / 5G NSA performs better than 5G SA in your location.

1. Go to `Settings`.
2. Set `G4AR radio profile` to `Prefer LTE anchor / 5G NSA`.
3. Click `Save Unlock Lab`.
4. Use your local adapter to apply the stored radio-profile intent if the adapter
   supports that modem and firmware.
5. Go to the `Dashboard` and `Map`.
6. Watch the active band, radio type, serving cell, signal, ping, upload, and
   download.
7. Test for at least a few minutes before changing anything else.

If LTE/NSA is not stable, try `LTE-only test` for diagnostics, then return to
`Auto` or `5G Standalone`.

Suggested test order:

1. `Auto`
2. `Prefer LTE anchor / 5G NSA`
3. `LTE-only test`
4. `5G Standalone`
5. Back to the best stable profile

Record each result with the same antenna position and the same speed-test server
when possible.

## Step 6: Keep The Flash Gate Locked Until Ready

The `G4AR firmware override gate` should stay locked until all of this is true:

- Stock backup exists.
- Recovery has been tested.
- Stock backup SHA-256 is recorded.
- Custom firmware SHA-256 is recorded.
- You understand the brick risk.
- You type the exact consent phrase shown in the UI.

Even after the consent gate validates, TMHI Control Center currently returns
`501 Not Implemented` for firmware writing. That is intentional until a tested
local adapter can verify backup and recovery on real owned hardware.

## Quick Troubleshooting

`Create Stock Backup` is disabled:

- Select `G4AR unlock / radio lab`.
- Check the ownership/risk acknowledgement.
- Click `Save Unlock Lab`.
- The web UI will use the Docker adapter URL automatically if the adapter field
  is blank.

Backup fails:

- Confirm whether you are using the built-in Docker adapter or a real hardware
  adapter.
- The built-in Docker adapter is reachable but cannot create real firmware
  backups without hardware bridge tooling.
- If using a hardware adapter, confirm it is reachable from the Docker host.
- Confirm the adapter implements `POST /g4ar/firmware/backup`.
- Check adapter logs.
- Make sure the adapter returns JSON.

LTE / 4G does not come back:

- The current stock firmware, SIM, modem, or network may reject LTE/NSA.
- Try `Auto` again before testing another profile.
- Check tower map, antenna aim, and signal metrics.
- Compare SINR and upload stability, not just bars.

## Hard Stop Conditions

Stop and return to stock behavior if:

- You do not have a verified backup.
- Recovery is untested.
- Hashes do not match.
- The gateway loses identity/calibration data.
- The gateway becomes unstable after a profile change.
- You are not sure the gateway is yours to modify.
