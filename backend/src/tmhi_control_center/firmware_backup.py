from __future__ import annotations

import hashlib
import json
import re
import zipfile
from pathlib import Path
from typing import Any

from .models import utc_now


class FirmwareBackupError(RuntimeError):
    pass


BACKUP_ID_PATTERN = re.compile(r"g4ar-\d{8}-\d{6}-\d{6}")


async def create_g4ar_firmware_backup(
    settings: Any,
    gateway: Any,
    *,
    reason: str = "ui_request",
) -> dict[str, Any]:
    try:
        overview = await gateway.overview()
    except Exception as exc:
        raise FirmwareBackupError(f"Gateway inventory could not be read: {exc}") from exc

    detection_value = overview.get("detection") if isinstance(overview, dict) else {}
    detection = detection_value if isinstance(detection_value, dict) else {}
    if not isinstance(overview, dict) or detection.get("reachable") is False:
        raise FirmwareBackupError("Gateway must be reachable before creating a recovery bundle")

    warnings: list[str] = []
    try:
        wifi = await gateway.wifi_config()
    except Exception as exc:
        wifi = {
            "supported": False,
            "error": str(exc),
        }
        warnings.append(f"Wi-Fi configuration was unavailable: {exc}")

    return save_g4ar_recovery_bundle(
        settings.firmware_backup_dir,
        overview=overview,
        wifi=wifi,
        reason=reason,
        warnings=warnings,
    )


def save_g4ar_recovery_bundle(
    backup_root: str,
    *,
    overview: dict[str, Any],
    wifi: dict[str, Any],
    reason: str,
    warnings: list[str] | None = None,
) -> dict[str, Any]:
    created_at = utc_now()
    backup_id = created_at.strftime("g4ar-%Y%m%d-%H%M%S-%f")
    root = Path(backup_root).expanduser()
    backup_dir = root / backup_id
    backup_dir.mkdir(parents=True, exist_ok=False)

    device = overview.get("device") if isinstance(overview.get("device"), dict) else {}
    firmware_version = device.get("firmware")
    hardware_revision = device.get("hardware")
    bundle_payload = {
        "created_at": created_at.isoformat(),
        "reason": reason or "ui_request",
        "device": device,
        "connection": overview.get("connection") or {},
        "signal": overview.get("signal") or {},
        "radios": overview.get("radios") or [],
        "system": overview.get("system") or {},
        "telemetry": overview.get("telemetry") or {},
        "sections": overview.get("sections") or [],
    }

    artifact_contents = {
        "gateway-snapshot.json": _json_bytes(bundle_payload),
        "wifi-configuration.json": _json_bytes(wifi),
        "restore-notes.md": _restore_notes(
            created_at=created_at.isoformat(),
            firmware_version=firmware_version,
            hardware_revision=hardware_revision,
            warnings=warnings or [],
        ).encode("utf-8"),
    }
    artifacts = [
        _write_artifact(backup_dir, name, content)
        for name, content in artifact_contents.items()
    ]
    checksums = "".join(
        f'{artifact["sha256"]}  {artifact["name"]}\n' for artifact in artifacts
    ).encode("ascii")
    artifacts.append(_write_artifact(backup_dir, "SHA256SUMS", checksums))

    manifest = {
        "id": backup_id,
        "created_at": created_at.isoformat(),
        "device": "Arcadyan TMO-G4AR",
        "firmware_version": firmware_version,
        "hardware_revision": hardware_revision,
        "backup_type": "docker_stock_recovery_bundle",
        "source": "docker_direct_gateway_api",
        "status": "saved",
        "raw_firmware_included": False,
        "flash_backup_requirement_satisfied": False,
        "artifact_count": len(artifacts),
        "artifacts": artifacts,
        "warnings": warnings or [],
        "notes": [
            "Created directly by TMHI Control Center using the stock gateway API.",
            "Includes recoverable settings and inventory exposed by the gateway.",
            "Does not contain raw eMMC partitions, calibration partitions, or a flashable firmware image.",
        ],
        "download_url": f"/api/g4ar/firmware/backups/{backup_id}/download",
    }
    (backup_dir / "backup-manifest.json").write_bytes(_json_bytes(manifest))
    _create_archive(root, backup_id)
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
            backup_id = str(manifest.get("id") or manifest_path.parent.name)
            manifest.setdefault(
                "download_url",
                f"/api/g4ar/firmware/backups/{backup_id}/download",
            )
            manifest.setdefault("raw_firmware_included", False)
            manifest.setdefault("flash_backup_requirement_satisfied", False)
            backups.append(manifest)

    backups.sort(key=lambda item: str(item.get("created_at", "")), reverse=True)
    return {"backup_dir": str(root), "count": len(backups), "backups": backups}


def get_g4ar_backup_archive(backup_root: str, backup_id: str) -> Path:
    if not BACKUP_ID_PATTERN.fullmatch(backup_id):
        raise FileNotFoundError("Invalid G4AR backup ID")
    root = Path(backup_root).expanduser()
    backup_dir = root / backup_id
    if not backup_dir.is_dir() or not (backup_dir / "backup-manifest.json").is_file():
        raise FileNotFoundError("G4AR backup was not found")
    archive_path = root / f"{backup_id}.zip"
    if not archive_path.is_file():
        _create_archive(root, backup_id)
    return archive_path


def _write_artifact(backup_dir: Path, name: str, content: bytes) -> dict[str, Any]:
    path = backup_dir / name
    path.write_bytes(content)
    return {
        "name": name,
        "saved": True,
        "sha256": hashlib.sha256(content).hexdigest(),
        "size_bytes": len(content),
    }


def _create_archive(root: Path, backup_id: str) -> Path:
    backup_dir = root / backup_id
    archive_path = root / f"{backup_id}.zip"
    with zipfile.ZipFile(
        archive_path,
        mode="w",
        compression=zipfile.ZIP_DEFLATED,
    ) as archive:
        for path in sorted(backup_dir.iterdir()):
            if path.is_file():
                archive.write(path, arcname=path.name)
    return archive_path


def _json_bytes(value: Any) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True, default=str) + "\n").encode(
        "utf-8"
    )


def _restore_notes(
    *,
    created_at: str,
    firmware_version: Any,
    hardware_revision: Any,
    warnings: list[str],
) -> str:
    warning_lines = "\n".join(f"- {warning}" for warning in warnings) or "- None"
    return f"""# G4AR Docker Recovery Bundle

Created: {created_at}
Firmware: {firmware_version or "Unknown"}
Hardware: {hardware_revision or "Unknown"}

## What This Bundle Can Restore

- Gateway and radio inventory for comparison after a reset or firmware update.
- SSID, radio, and Wi-Fi configuration values exposed by the stock API.
- Signal, serving-cell, and system snapshots for troubleshooting.

## Important Limitation

This is not a raw firmware image. Stock G4AR firmware does not expose eMMC,
boot, calibration, identity, or NVRAM partitions through its local network API.
This bundle must not be used to satisfy a custom-firmware flash backup gate.

## Warnings

{warning_lines}
"""
