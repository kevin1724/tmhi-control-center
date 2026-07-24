# TMHI Control Center

A fresh control-center scaffold for T-Mobile Home Internet gateway management.

This repo keeps the working backend pieces from the earlier TMHI backend project,
but it is no longer wired to that original GitHub or container image. Publishing
is intentionally disabled until the new repository and image names are chosen.

## Current Scope

- FastAPI backend and built-in browser dashboard
- Gateway reachability, login testing, and reboot request support
- Connectivity probes and reboot safety logic from the previous project
- SQLite event history
- Dockerfile and source-build Docker Compose setup
- Clean backend package layout under `backend/src/tmhi_control_center/`

## Project Layout

```text
tmhi-control-center/
├── backend/
│   ├── src/
│   │   └── tmhi_control_center/
│   │       ├── static/
│   │       ├── main.py
│   │       ├── gateway.py
│   │       ├── watchdog.py
│   │       ├── connectivity.py
│   │       ├── config.py
│   │       ├── credentials.py
│   │       ├── storage.py
│   │       ├── models.py
│   │       └── cli.py
│   ├── tests/
│   ├── pyproject.toml
│   ├── requirements.txt
│   └── requirements-dev.txt
├── android/
├── web/
├── deploy/
├── docs/
├── scripts/
├── .github/
├── .env.example
├── .gitignore
├── Dockerfile
├── docker-compose.yml
├── LICENSE
├── README.md
├── SECURITY.md
├── CONTRIBUTING.md
├── CHANGELOG.md
└── ACKNOWLEDGEMENTS.md
```

The Python app now lives under `backend/`; the root is reserved for orchestration,
docs, scripts, Android, and future standalone web work.

## Run Locally With Docker

```bash
docker compose up -d --build
docker compose logs -f tmhi-control-center
```

Open:

```text
http://localhost:8088/
```

From another device on your LAN, use the Docker host's LAN IP instead of
`localhost`.

## Diagnostic Commands

Inside the container:

```bash
docker compose exec tmhi-control-center \
  python -m tmhi_control_center.cli connectivity

docker compose exec tmhi-control-center \
  python -m tmhi_control_center.cli gateway-test
```

## GitHub Setup

The repo includes an active GitHub Actions workflow at
`.github/workflows/docker-publish.yml`.

On pull requests, it runs backend tests and verifies the Docker image builds. On
pushes to `main`, version tags like `v0.1.1`, or manual runs, it builds and
publishes a multi-architecture Docker Hub image.

When you are ready to create the new GitHub repository:

```powershell
.\scripts\create-github-repo.ps1 -RepoName tmhi-control-center -Visibility public
```

Or from a Bash shell:

```bash
./scripts/create-github-repo.sh tmhi-control-center public
```

The helper scripts refuse to reuse an existing `origin` remote. That is
deliberate so the new project does not get pushed to the old repo by mistake.

## Docker Hub Publishing

Create a Docker Hub repository named:

```text
tmhi-control-center
```

Then configure the GitHub repository with:

- Actions variable: `DOCKERHUB_USERNAME`
- Actions secret: `DOCKERHUB_TOKEN`

The PowerShell helper can set both values after the GitHub repo exists:

```powershell
.\scripts\configure-dockerhub-github.ps1 -DockerHubUsername YOUR_DOCKERHUB_USERNAME
```

Or from a Bash shell:

```bash
./scripts/configure-dockerhub-github.sh YOUR_DOCKERHUB_USERNAME
```

The workflow publishes to:

```text
YOUR_DOCKERHUB_USERNAME/tmhi-control-center
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
uvicorn tmhi_control_center.main:app --host 0.0.0.0 --port 8088
```

## App Icon

The copied favicon, Apple touch icon, web manifest, and PNG app icons were
removed. Add new branded assets later when the replacement app identity is ready.

## Disclaimer

TMHI Control Center is an unofficial community project and is not affiliated with,
endorsed by, or supported by T-Mobile.
