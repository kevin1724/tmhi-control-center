from __future__ import annotations

import base64
import hashlib
import json
import re
from pathlib import Path
from typing import Any

import httpx

from .models import utc_now


class FirmwareBackupError(RuntimeError):
    pass


SAFE_FILENAME_PATTERN = re.compile(r"[^A-Za-z0-9._-]+")


async def create_g4ar_firmware_backup(
    settings: Any,
    *,
    reason: str = "ui_request",
) -> dict[str, Any]:
    adapter_url = settings.advanced_modem_control_url.rstrip("/")
    request_payload = {
        "device": "Arcadyan TMO-G4AR",
        "reason": reason or "ui_request",
        "radio_profile": settings.advanced_radio_profile,
        "requested_at": utc_now().isoformat(),
        "expected_artifacts": [
            "stock-firmware.bin",
            "partition-table.txt",
            "calibration-and-identity-backup.tar",
            "restore-notes.md",
            "SHA256SUMS",
        ],
    }

    try:
        async with httpx.AsyncClient(
            timeout=httpx.Timeout(120.0),
            follow_redirects=True,
            trust_env=False,
        ) as client:
            response = await client.post(
                f"{adapter_url}/g4ar/firmware/backup",
                json=request_payload,
            )
    except httpx.HTTPError as exc:
        raise FirmwareBackupError(f"Local adapter backup request failed: {exc}") from exc

    if not response.is_success:
        detail = response.text.strip()[:240] or response.reason_phrase
        raise FirmwareBackupError(f"Local adapter backup failed: {detail}")

    try:
        adapter_payload = response.json()
    except ValueError as exc:
        raise FirmwareBackupError("Local adapter returned non-JSON backup data") from exc
    if not isinstance(adapter_payload, dict):
        raise FirmwareBackupError("Local adapter backup response must be a JSON object")

    return save_g4ar_firmware_backup(
        settings.firmware_backup_dir,
        adapter_payload,
        adapter_url=adapter_url,
    )


def save_g4ar_firmware_backup(
    backup_root: str,
    adapter_payload: dict[str, Any],
    *,
    adapter_url: str,
) -> dict[str, Any]:
    created_at = utc_now()
    backup_id = created_at.strftime("g4ar-%Y%m%d-%H%M%S-%f")
    backup_dir = Path(backup_root).expanduser() / backup_id
    backup_dir.mkdir(parents=True, exist_ok=False)

    artifacts = _normalise_artifacts(adapter_payload)
    saved_artifacts: list[dict[str, Any]] = []
    for index, artifact in enumerate(artifacts, start=1):
        if not isinstance(artifact, dict):
            continue
        saved_artifacts.append(_save_artifact(backup_dir, artifact, index))

    manifest = {
        "id": backup_id,
        "created_at": created_at.isoformat(),
        "device": adapter_payload.get("device") or "Arcadyan TMO-G4AR",
        "firmware_version": adapter_payload.get("firmware_version"),
        "hardware_revision": adapter_payload.get("hardware_revision"),
        "adapter_url": adapter_url,
        "status": "saved",
        "artifact_count": len(saved_artifacts),
        "artifacts": saved_artifacts,
        "notes": adapter_payload.get("notes") or [],
        "metadata": adapter_payload.get("metadata") or {},
    }
    (backup_dir / "backup-manifest.json").write_text(
        json.dumps(manifest, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    return manifest


def list_g4ar_firmware_backups(backup_root: str) -> dict[str, Any]:
    root = Path(backup_root).expanduser()
    backups: list[dict[str, Any]] = []
    if not root.exists():
        return {"backup_dir": str(root), "count": 0, "backups": backups}

    for manifest_path in root.glob("*/backup-manifest.json"):
        try:
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            continue
        if isinstance(manifest, dict):
            manifest["path"] = str(manifest_path.parent)
            backups.append(manifest)

    backups.sort(key=lambda item: str(item.get("created_at", "")), reverse=True)
    return {"backup_dir": str(root), "count": len(backups), "backups": backups}


def _normalise_artifacts(adapter_payload: dict[str, Any]) -> list[dict[str, Any]]:
    artifacts = adapter_payload.get("artifacts")
    if isinstance(artifacts, list):
        return artifacts
    if "content_base64" in adapter_payload:
        return [
            {
                "name": adapter_payload.get("name") or "stock-firmware.bin",
                "content_base64": adapter_payload.get("content_base64"),
                "sha256": adapter_payload.get("sha256"),
            }
        ]
    return []


def _save_artifact(
    backup_dir: Path,
    artifact: dict[str, Any],
    index: int,
) -> dict[str, Any]:
    name = _safe_artifact_name(str(artifact.get("name") or ""), index)
    summary: dict[str, Any] = {
        "name": name,
        "saved": False,
        "declared_sha256": _clean_sha256(artifact.get("sha256")),
        "declared_size_bytes": artifact.get("size_bytes"),
    }

    encoded_content = artifact.get("content_base64")
    if not isinstance(encoded_content, str) or not encoded_content.strip():
        summary["note"] = "Adapter did not include inline content for this artifact."
        return summary

    try:
        content = base64.b64decode(encoded_content, validate=True)
    except ValueError as exc:
        raise FirmwareBackupError(f"Backup artifact {name} is not valid base64") from exc

    artifact_path = backup_dir / name
    artifact_path.write_bytes(content)
    actual_sha256 = hashlib.sha256(content).hexdigest()
    declared_sha256 = summary["declared_sha256"]
    if declared_sha256 and declared_sha256 != actual_sha256:
        raise FirmwareBackupError(f"Backup artifact {name} SHA-256 does not match")

    summary.update(
        {
            "saved": True,
            "sha256": actual_sha256,
            "size_bytes": len(content),
            "path": str(artifact_path),
        }
    )
    return summary


def _safe_artifact_name(raw_name: str, index: int) -> str:
    basename = raw_name.replace("\\", "/").split("/")[-1].strip()
    cleaned = SAFE_FILENAME_PATTERN.sub("_", basename).strip("._")
    if not cleaned:
        return f"artifact-{index}.bin"
    return cleaned[:120]


def _clean_sha256(value: Any) -> str | None:
    text = str(value or "").strip().lower()
    if len(text) == 64 and all(character in "0123456789abcdef" for character in text):
        return text
    return None
