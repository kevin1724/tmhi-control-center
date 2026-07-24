# TMHI Control Center Android

This folder contains the native Android app for TMHI Control Center.

The Android app runs fully on the phone. It talks directly to the local gateway
over the LAN while the user has the app open. It does not run a background
watchdog, scheduled internet monitor, or 24/7 reboot service.

## Current Features

- Gateway dashboard with local API reachability, device details, cellular
  connection details, and signal quality.
- Gateway login saved locally on the phone.
- Connected-device list loaded directly from the gateway.
- Wi-Fi SSID and gateway Wi-Fi radio controls when supported by the gateway API.
- Tower map screen using OpenStreetMap/Leaflet in a WebView and optional
  OpenCellID lookups.
- Homelab tab with setup readiness score, next-best-action guidance, signal and
  antenna coaching, router offload/SQM playbook, adapter URL guide, and backup
  status.
- Manual diagnostics, raw gateway data sections, and a manual reboot action.
- G4AR Unlock / Radio Lab settings for owner-controlled G4AR units.
- Local adapter stock-backup request for G4AR lab users, saved in the app's
  private storage.

## What Is Intentionally Not Included

- No background watchdog loop.
- No always-on connectivity monitoring.
- No automatic reboot logic while the app is closed.
- No firmware download source.
- No firmware flashing implementation.
- No transmit-power override controls.

## Open In Android Studio

1. Open Android Studio.
2. Choose `Open`.
3. Select the `android/` folder.
4. Let Gradle sync.
5. Run the `app` configuration on a device connected to the same network as the
   gateway.

The phone must be on the gateway LAN or on a network route that can reach the
gateway IP, usually `192.168.12.1`.

## Build From CLI

Install a local Android toolchain first:

- JDK 17 or newer.
- Android SDK with API 35.
- Gradle compatible with Android Gradle Plugin 8.7.x.

Then run:

```bash
cd android
gradle assembleDebug
```

The debug APK will be created under:

```text
android/app/build/outputs/apk/debug/
```

## GitHub APK Artifact

The repository includes a `Build Android APK` workflow. When Android files are
pushed to `main`, GitHub Actions builds a debug APK and uploads it as the
`tmhi-control-center-debug-apk` artifact.

## First-Time Use

1. Install and open the app.
2. Go to `Settings`.
3. Confirm the gateway host and API port.
4. Save the gateway admin password.
5. Tap `Test`.
6. Go to `Dashboard` and refresh.
7. Open `Homelab` to review the setup checklist and next best action.
8. Optional: add an OpenCellID key on the `Map` screen.

Most gateways use:

```text
Host: 192.168.12.1
Port: 8080
Username: admin
```

## G4AR Lab

The G4AR lab is for owner-controlled Arcadyan TMO-G4AR gateways only. The app can
store lab intent, radio-profile preference, local adapter URL, risk
acknowledgement, and local stock-backup manifests. Device-specific firmware
operations must be implemented by a trusted local adapter and require verified
backup/recovery first.

The local adapter URL is not the stock gateway login page. It is a LAN-only HTTP
service running on hardware the user controls, such as OpenWrt/ROOTer, a
Raspberry Pi, a mini PC, or a Linux host attached to the lab hardware.

The Docker web app can leave this field blank and automatically use its internal
`http://127.0.0.1:8000` adapter default. The Android app should not use that
localhost value unless the adapter is actually running on the phone. For Android,
enter the Docker host's LAN URL or the LAN URL of the hardware adapter.
