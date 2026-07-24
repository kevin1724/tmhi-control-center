#!/usr/bin/env bash
set -euo pipefail

MESSAGE="${1:-Update TMHI Control Center}"

REPO_ROOT="$(pwd -P)"
if command -v cygpath >/dev/null 2>&1; then
  REPO_ROOT="$(cygpath -aw .)"
fi
GIT=(git -c "safe.directory=${REPO_ROOT}")

if [[ ! -d .git ]]; then
  echo "Error: this directory is not a Git repository." >&2
  echo "Run ./scripts/create-github-repo.sh first." >&2
  exit 1
fi

ORIGIN_URL="$("${GIT[@]}" remote get-url origin 2>/dev/null || true)"
if [[ -z "$ORIGIN_URL" ]]; then
  echo "Error: no origin remote is configured." >&2
  echo "Run ./scripts/create-github-repo.sh after choosing the new repository name." >&2
  exit 1
fi

if [[ "$ORIGIN_URL" == *"tmhi-watchdog"* ]]; then
  echo "Error: origin still points at the old tmhi-watchdog repository: $ORIGIN_URL" >&2
  echo "Set origin to the new TMHI Control Center repository before pushing." >&2
  exit 1
fi

"${GIT[@]}" add .

if "${GIT[@]}" diff --cached --quiet; then
  echo "No changes to commit."
  exit 0
fi

"${GIT[@]}" commit -m "$MESSAGE"
"${GIT[@]}" push
