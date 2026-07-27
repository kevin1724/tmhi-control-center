# Project Structure

```text
tmhi-control-center/
+-- backend/
|   +-- src/
|   |   +-- tmhi_control_center/
|   |       +-- static/        Built-in dashboard assets
|   |       +-- main.py        FastAPI app and API routes
|   |       +-- advanced_modem.py
|   |       +-- firmware_backup.py
|   |       +-- usb_lab.py     G4AR USB-C probe normalization and adapter client
|   |       +-- gateway.py     Gateway telemetry, login, detection, and reboot client
|   |       +-- watchdog.py    Outage state machine and reboot safeguards
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
+-- android/                   Native Android app
+-- web/                       Future standalone web app placeholder
+-- deploy/                    Optional deployment examples
+-- docs/                      Additional project documentation
|   +-- G4AR_FIRMWARE_LAB_GUIDE.md
|   +-- G4AR_USB_C_2_5GBE_LAB.md
|   +-- GATEWAY_TELEMETRY.md
|   +-- DOCKER_HUB_README.md
+-- .github/                   Issue/PR templates and active Actions workflows
+-- .env.example               Reference for generated settings
+-- .gitignore
+-- Dockerfile                 Production container image
+-- docker-compose.yml         Recommended source-build deployment
+-- LICENSE
+-- README.md
+-- SECURITY.md
+-- CONTRIBUTING.md
+-- CHANGELOG.md
+-- ACKNOWLEDGEMENTS.md
```

The backend keeps a `src/` layout inside `backend/` so imports resolve from the
installed application package instead of duplicate repository-root files.
