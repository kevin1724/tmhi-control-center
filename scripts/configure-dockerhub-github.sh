#!/usr/bin/env bash
set -euo pipefail

DOCKERHUB_USERNAME="${1:-}"

if ! command -v gh >/dev/null 2>&1; then
  echo "Error: GitHub CLI (gh) is not installed." >&2
  echo "Install it from https://cli.github.com/ and run: gh auth login" >&2
  exit 1
fi

if [[ -z "$DOCKERHUB_USERNAME" ]]; then
  read -r -p "Docker Hub username: " DOCKERHUB_USERNAME
fi

if [[ -z "$DOCKERHUB_USERNAME" ]]; then
  echo "Error: Docker Hub username is required." >&2
  exit 1
fi

echo "Paste a Docker Hub access token. It will be saved as GitHub secret DOCKERHUB_TOKEN."
read -r -s -p "Docker Hub token: " DOCKERHUB_TOKEN
echo

if [[ -z "$DOCKERHUB_TOKEN" ]]; then
  echo "Error: Docker Hub token is required." >&2
  exit 1
fi

gh variable set DOCKERHUB_USERNAME --body "$DOCKERHUB_USERNAME"
printf '%s' "$DOCKERHUB_TOKEN" | gh secret set DOCKERHUB_TOKEN --body-file -

echo "Configured GitHub Actions Docker Hub variable and secret."
