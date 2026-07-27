# TMHI Control Center for Android

The native Android app is a local companion for supported T-Mobile Home
Internet gateways. It connects directly to the gateway from the phone while the
app is open; no Docker server or cloud account is required.

The Android app intentionally has no background watchdog, scheduled speed test,
or automatic reboot service. Use the Docker application for 24/7 monitoring.

## Features

- Five-tab, icon-first navigation for Home, Devices, Map, Lab, and Profile.
- Gateway status, connection details, signal score, and exposed radio metrics.
- Connected-device inventory with best-effort vendor and device identification.
- Wi-Fi name and radio controls when the gateway API supports them.
- OpenStreetMap tower view with optional OpenCellID tower records.
- Signal and antenna coaching for repeatable placement tests.
- Manual gateway refresh, login test, and guarded reboot action.
- Android Keystore-encrypted gateway password and OpenCellID key.
- An encrypted Wi-Fi recovery vault stored only in the app's private storage.

## Wi-Fi Recovery Vault

The Lab tab can create a point-in-time backup of every Wi-Fi profile returned by
the authenticated gateway API. It always records exposed Wi-Fi names. It records
a password only when the gateway returns a real, unmasked credential.

Some firmware never returns Wi-Fi passwords, even after authentication. Those
profiles are clearly marked as name-only and the app does not claim that their
passwords can be restored.

Restore updates matching Wi-Fi names and available credential fields. It does
not change radio enabled state. Applying a restore can briefly disconnect the
phone while the gateway updates its Wi-Fi configuration.

Backups are encrypted with an Android Keystore key and saved in app-private
storage. They are bound to the current app installation and cannot be decrypted
after the app is uninstalled or its data is cleared. Android cloud backup is
disabled for the app.

## First-Time Setup

1. Connect the phone to the gateway Wi-Fi or another LAN that can reach it.
2. Open **Profile**.
3. Confirm the host, API port, and username.
4. Enter the gateway admin password, then tap **Save Login**.
5. Tap **Test**.
6. Open **Home** and refresh the gateway.
7. Open **Lab** and create an encrypted Wi-Fi backup.
8. Optionally add an OpenCellID key and home coordinates under **Map**.

Common G4AR values are:

```text
Host: 192.168.12.1
Port: 8080
Username: admin
```

## Download

The latest CI-built debug APK is published here:

- [tmhi-control-center-debug.apk](https://github.com/kevin1724/tmhi-control-center/releases/download/android-latest/tmhi-control-center-debug.apk)

This is a debug build for testing and is not a Play Store release. Android may
ask for permission to install apps from the browser or file manager used to open
the APK.

## Build

Requirements:

- JDK 17 or newer.
- Android SDK API 35 and build tools 35.0.0.
- Gradle 8.9.

Run:

```bash
gradle -p android :app:assembleDebug --no-daemon
```

The APK is written to:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

The `Build Android APK` GitHub Actions workflow also builds every Android change
pushed to `main`, uploads the `tmhi-control-center-debug-apk` artifact, and
replaces the `android-latest` release asset.

## G4AR Owner Lab

The Profile tab includes an owner-only G4AR lab acknowledgement and radio-profile
notes. It does not root, flash, unlock, increase transmit power, or force a tower.
The stock G4AR API does not expose supported commands for those operations.

Unofficial firmware work can permanently disable a gateway, erase calibration
data, violate carrier terms, or void a warranty. Keep the owner lab disabled on
leased hardware.
