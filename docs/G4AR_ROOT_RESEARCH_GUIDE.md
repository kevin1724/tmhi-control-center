# G4AR Owner Root Research Guide

> **DANGER: ROOT RESEARCH CAN PERMANENTLY BRICK THIS GATEWAY.**
>
> Continue only with an Arcadyan TMO-G4AR that you own outright and can replace.
> Never use this workflow on leased, financed, carrier-owned, or
> service-critical hardware. Opening the enclosure, probing test pads, bypassing
> verified boot, or writing storage can destroy calibration and identity data,
> void warranty, interrupt service, and leave the unit without a recovery path.

## Current Finding

As of July 26, 2026, TMHI Control Center has not found a public, reproducible
G4AR root chain, bootloader unlock, complete restore procedure, or supported
OpenWrt/ROOTer image. The app therefore does not include a `Root Gateway`
button, generic bootloader commands, or an unverified firmware download.

The G4AR stock `/TMI/v1` API can expose gateway information and selected
administrative controls. An administrator token for that API is not Linux or
Android root access.

The related Arcadyan KVD21 research project found 1.8 V receive-only serial
output on its own hardware, but the project was shelved while tooling for the
MediaTek T75 platform remained incomplete. A KVD21 observation is a research
clue only. It does not prove the G4AR uses the same pad order, voltage, boot
chain, or recovery path.

## What The App Provides

The `G4AR Root Readiness` section in **Settings** provides:

- A large ownership, brick, warranty, service, and recovery warning.
- Confirmed evidence separated from unverified claims.
- A receive-only hardware research order.
- Hard stops for unsafe voltage, copied pinouts, unknown images, and scripts
  that send gateway identifiers off the LAN.
- A readiness assessment that can approve only read-only research.

The assessment always returns `root_execution_enabled: false`. Completing it
does not unlock rooting or firmware writing.

## Required Equipment

- A spare G4AR owned outright and not required for internet service.
- ESD mat, ESD wrist strap, good lighting, and board-safe probes.
- The G4AR FCC internal photos for board-layout comparison.
- Continuity-capable digital multimeter.
- High-impedance logic analyzer.
- Isolated, **1.8 V-safe** USB-UART adapter with receive-only wiring available.
- A separate Linux computer with encrypted local storage for logs and backups.
- A verified exact-device readback and offline recovery tool before any write.

Do not connect an ordinary 3.3 V or 5 V UART to an unknown pad. Do not connect
the USB-UART VCC pin to the gateway at all.

## Step 1: Prove Ownership And Record The Unit

1. Confirm the gateway is owned outright.
2. Confirm it is not leased, financed, carrier-owned, or pending return.
3. Use a spare unit that can be replaced.
4. Photograph the external label for your private records.
5. Record the model, hardware revision, current firmware, and visible board
   markings.
6. Redact serial number, MAC addresses, IMEI, SIM identifiers, tokens, and
   credentials from anything shared publicly.

Stop here if ownership is uncertain or the gateway is service-critical.

## Step 2: Preserve Stock State

1. Export every backup the stock UI offers.
2. In TMHI Control Center, open **Settings > G4AR Unlock / Radio Lab**.
3. Configure a trusted, LAN-only hardware adapter if one exists for your exact
   unit.
4. Use **Create Stock Backup** only when that adapter performs real readback.
5. Preserve the partition table, boot partitions, root filesystems, modem
   firmware, calibration data, identity data, configuration/NVRAM, restore
   notes, and SHA-256 hashes.
6. Keep at least two offline copies.

The built-in Docker adapter coordinates the workflow but cannot read eMMC or
internal flash through the stock network API. A manifest without the underlying
artifacts is not a complete backup.

## Step 3: Match The Exact Board

1. Disconnect power and allow the unit to discharge.
2. Use ESD protection while opening an owned unit.
3. Compare both sides of the board with the FCC internal photos.
4. Record every candidate test-pad group without assigning a function yet.
5. Do not copy a KVD21, Nokia, Sagemcom, or unrelated Arcadyan pinout.

Hardware revisions can move pads or change voltage domains while retaining the
same retail model name.

## Step 4: Measure Before Connecting

1. With power disconnected, use continuity testing to identify a ground
   candidate against a known shield ground.
2. Inspect the remaining candidates with a high-impedance meter or logic
   analyzer during boot.
3. Record idle voltage and activity for each candidate.
4. Treat any unmeasured pad as unsafe.
5. Stop if measurements do not clearly support a 1.8 V serial interface.

Do not inject voltage and do not attach adapter TX or VCC during discovery.

## Step 5: Capture Receive-Only Boot Output

1. Configure an isolated interface for the measured logic level.
2. Connect only gateway ground and gateway transmit to adapter receive.
3. Leave adapter transmit and VCC physically disconnected.
4. Begin with a passive capture during a normal boot.
5. Save the raw log locally, calculate its SHA-256 hash, and make a redacted
   copy for analysis.
6. Look for the SoC, boot stages, secure-boot state, storage type, partition
   names, kernel command line, operating system, and recovery messages.

Receiving a boot log is not root access. A console that prints text may ignore
input or require signed authentication.

## Step 6: Establish Recovery Before Rooting

A future root method is not ready to test until all of these are independently
proven on the exact spare unit:

- Complete storage readback with stable hashes.
- Preservation of calibration, radio identity, MAC, and NVRAM partitions.
- A documented recovery entry method that works when the normal OS does not.
- A restore tool that supports the exact SoC, storage, preloader, and board
  revision.
- A successful offline restoration of the original images.
- Continued access to the recovery interface after the test.

Do not call a backup complete merely because the stock UI exported a settings
file.

## Step 7: Requirements For A Real Root Method

TMHI Control Center can consider a G4AR root adapter only after researchers can
provide all of the following:

1. Exact supported G4AR hardware and firmware revisions.
2. Reproducible entry path with no guessed pinout or voltage.
3. Explanation of secure-boot and signature behavior.
4. Full backup and exact-device restore instructions.
5. Repeatable results on more than one owner-controlled spare device.
6. Public source code that can be reviewed and built locally.
7. SHA-256 hashes and provenance for every binary artifact.
8. No off-LAN collection of MAC, IMEI, serial, token, password, or SIM data.

Until then, the correct result is `No verified root path`.

## Reject Unsafe Tools

Do not use a script merely because its filename says `flash`, `root`, or
`unlock`. Reject it if it:

- Only logs into the stock REST API while claiming to root the OS.
- Contains hard-coded passwords, MAC addresses, IMEIs, serials, or remote URLs.
- Registers the gateway with an unknown cloud, tunnel, mesh, or heartbeat
  service.
- Downloads binaries without source, provenance, signatures, and hashes.
- Uses commands or partition names from another gateway model.
- Cannot explain and demonstrate exact-device recovery.

## Sources

- [FCC G4AR internal photos](https://fccid.io/RAXTMOG4AR/Internal-Photos/Internal-Photos-1-rev-6551767)
- [G4AR local API research](https://github.com/joaovorocha/tmobile-g4ar-local-api)
- [Related Arcadyan KVD21 hardware research](https://github.com/chainofexecution/Arcadyan-KVD21)

These sources support hardware identification, stock API behavior, and related
platform research. None currently supplies a verified G4AR root chain.
