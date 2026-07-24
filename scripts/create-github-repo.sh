#!/usr/bin/env bash
set -euo pipefail

REPO_NAME="${1:-tmhi-control-center}"
VISIBILITY="${2:-public}"
DESCRIPTION="Docker-hosted control center for supported T-Mobile Home Internet gateways."

REPO_ROOT="$(pwd -P)"
if command -v cygpath >/dev/null 2>&1; then
  REPO_ROOT="$(cygpath -aw .)"
fi
GIT=(git -c "safe.directory=${REPO_ROOT}")

if ! command -v git >/dev/null 2>&1; then
  echo "Error: git is not installed." >&2
  exit 1
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "Error: GitHub CLI (gh) is not installed." >&2
  echo "Install it from https://cli.github.com/ and run: gh auth login" >&2
  exit 1
fi

gh auth status >/dev/null

case "$VISIBILITY" in
  public|private|internal) ;;
  *)
    echo "Usage: $0 [repository-name] [public|private|internal]" >&2
    exit 1
    ;;
esac

if [[ ! -d .git ]]; then
  "${GIT[@]}" init -b main
fi

"${GIT[@]}" add .
if ! "${GIT[@]}" diff --cached --quiet; then
  "${GIT[@]}" commit -m "Initial TMHI Control Center scaffold"
fi

if "${GIT[@]}" remote get-url origin >/dev/null 2>&1; then
  existing_origin="$("${GIT[@]}" remote get-url origin)"
  echo "Error: an origin remote already exists: $existing_origin" >&2
  echo "Remove or rename that remote before creating the new GitHub repository." >&2
  exit 1
fi

GITHUB_OWNER="$(gh api user --jq .login)"
visibility_flag="--${VISIBILITY}"
gh repo create "$REPO_NAME" \
  "$visibility_flag" \
  --description "$DESCRIPTION"

"${GIT[@]}" remote add origin "https://github.com/${GITHUB_OWNER}/${REPO_NAME}.git"
"${GIT[@]}" push -u origin main

gh repo edit "${GITHUB_OWNER}/${REPO_NAME}" \
  --add-topic t-mobile \
  --add-topic tmhi \
  --add-topic home-internet \
  --add-topic control-center \
  --add-topic docker \
  --add-topic python \
  --add-topic fastapi

echo
echo "Repository created successfully:"
gh repo view "${GITHUB_OWNER}/${REPO_NAME}" --web
