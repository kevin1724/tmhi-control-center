from __future__ import annotations

from typing import Any

from .g4ar_root import g4ar_root_research_status


ADVANCED_MODEM_MODES = {
    "disabled": {
        "label": "Disabled",
        "description": "Use stock TMHI gateway APIs only.",
    },
    "openwrt_rooter": {
        "label": "Legacy mode",
        "description": "Retained only to migrate older saved settings to Docker-only mode.",
    },
    "modemmanager": {
        "label": "Legacy mode",
        "description": "Retained only to migrate older saved settings to Docker-only mode.",
    },
    "custom_adapter": {
        "label": "Legacy mode",
        "description": "Retained only to migrate older saved settings to Docker-only mode.",
    },
    "g4ar_unlock_lab": {
        "label": "G4AR Docker lab",
        "description": (
            "Docker-only recovery, radio research, and safety tools for an "
            "owner-controlled Arcadyan TMO-G4AR."
        ),
    },
    "g4ar_firmware_lab": {
        "label": "G4AR firmware lab (legacy)",
        "description": (
            "Legacy saved value for G4AR unlock / radio lab."
        ),
    },
}

BUILT_IN_DOCKER_ADAPTER_URL = "http://127.0.0.1:8000"
G4AR_LAB_MODES = {"g4ar_unlock_lab", "g4ar_firmware_lab"}

UPLOAD_PRIORITY_PROFILES = {
    "balanced": {
        "label": "Balanced",
        "description": "Keep download/upload shaping neutral.",
    },
    "prefer_upload": {
        "label": "Prefer upload",
        "description": "Bias QoS/SQM planning toward upload stability and lower uplink queue delay.",
    },
    "low_latency_upload": {
        "label": "Low-latency upload",
        "description": "Favor responsive uploads, video calls, gaming, and remote work over peak download.",
    },
}

G4AR_RADIO_PROFILES = {
    "auto": {
        "label": "Auto",
        "description": "Leave the gateway/modem in its current automatic radio mode.",
    },
    "prefer_lte_anchor_nsa": {
        "label": "Prefer LTE anchor / 5G NSA",
        "description": (
            "Research profile for trying LTE anchor plus 5G NR NSA "
            "when that performs better than 5G Standalone."
        ),
    },
    "lte_only_test": {
        "label": "LTE-only test",
        "description": "Temporary diagnostic profile for measuring LTE performance by itself.",
    },
    "nr_sa": {
        "label": "5G Standalone",
        "description": "Prefer NR SA where supported by firmware, modem, SIM, and network.",
    },
    "scan_only": {
        "label": "Scan only",
        "description": "Collect modem/radio data without applying an override.",
    },
}

RF_SAFETY_WARNING = (
    "G4AR unlock and radio override research is for hardware you own, such as a "
    "secondhand unit bought outside a carrier lease. Custom firmware, modem commands, "
    "and external antenna modifications can void warranty, brick hardware, break carrier "
    "or service terms, or create RF compliance problems."
)

TX_POWER_WARNING = (
    "Transmit-power override is intentionally unsupported. Cellular uplink power is controlled "
    "by the modem, network, antenna system, and regulatory limits. Use antenna placement, "
    "band preference, and tower comparison instead of power hacks."
)

G4AR_FLASH_CONSENT_PHRASE = "I OWN THIS G4AR - BACKUP VERIFIED - OVERRIDE RISK ACCEPTED"

G4AR_FIRMWARE_WARNING = (
    "G4AR firmware override can permanently brick the gateway or make it unusable on a "
    "carrier network. Do not flash until a complete stock firmware backup, device-specific "
    "restore path, and recovery console have been verified on that exact hardware."
)


def advanced_modem_summary(settings: Any) -> dict[str, Any]:
    mode = (
        "g4ar_unlock_lab"
        if settings.advanced_modem_mode in G4AR_LAB_MODES
        else "disabled"
    )
    mode_info = ADVANCED_MODEM_MODES.get(mode, ADVANCED_MODEM_MODES["disabled"])
    enabled = mode != "disabled" and settings.advanced_modem_acknowledged

    return {
        "mode": mode,
        "label": mode_info["label"],
        "description": mode_info["description"],
        "enabled": enabled,
        "acknowledged": settings.advanced_modem_acknowledged,
        "docker_direct": True,
        "skip_stock_backup": settings.advanced_skip_stock_backup,
        "available_modes": [
            {
                "value": key,
                "label": value["label"],
                "description": value["description"],
            }
            for key, value in ADVANCED_MODEM_MODES.items()
            if key in {"disabled", "g4ar_unlock_lab"}
        ],
        "capabilities": {
            "cell_lock": capability_status(enabled),
            "band_lock": capability_status(enabled),
            "cell_scan": capability_status(enabled),
            "lte_anchor_override": g4ar_capability_status(
                settings.advanced_modem_mode, enabled
            ),
            "radio_mode_override": g4ar_capability_status(
                settings.advanced_modem_mode, enabled
            ),
            "usb_hardware_probe": g4ar_capability_status(
                settings.advanced_modem_mode, enabled
            ),
            "usb_ethernet_bridge": g4ar_capability_status(
                settings.advanced_modem_mode, enabled
            ),
            "upload_priority_qos": capability_status(enabled),
            "stock_firmware_backup": firmware_capability_status(
                settings.advanced_modem_mode,
                enabled,
            ),
            "custom_firmware_flash": firmware_flash_status(
                settings.advanced_modem_mode,
                enabled,
            ),
            "root_access": {
                "supported": False,
                "status": "research_only_no_verified_chain",
                "reason": (
                    "No reproducible G4AR root chain or supported OpenWrt image is verified. "
                    "Only read-only owner hardware research is available."
                ),
            },
            "tx_power_override": {
                "supported": False,
                "status": "blocked",
                "reason": TX_POWER_WARNING,
            },
        },
        "upload_priority": {
            "profile": settings.advanced_upload_profile,
            "label": UPLOAD_PRIORITY_PROFILES[settings.advanced_upload_profile]["label"],
            "description": UPLOAD_PRIORITY_PROFILES[
                settings.advanced_upload_profile
            ]["description"],
            "available_profiles": [
                {
                    "value": key,
                    "label": value["label"],
                    "description": value["description"],
                }
                for key, value in UPLOAD_PRIORITY_PROFILES.items()
            ],
            "notes": [
                "This is QoS/SQM planning, not transmit-power control.",
                "Stock G4AR firmware does not expose a Docker command for applying this profile.",
                "The selection is saved as research intent for before/after comparisons.",
            ],
        },
        "g4ar_radio": g4ar_radio_summary(settings),
        "g4ar_unlock_lab": g4ar_firmware_lab_status(settings),
        "g4ar_firmware_lab": g4ar_firmware_lab_status(settings),
        "g4ar_root_research": g4ar_root_research_status(),
        "upload_optimization": [
            "Aim directional antennas using SINR and RSRP trends, not bars alone.",
            "Compare nearby cells on the tower map before applying any lock.",
            "Use SQM/QoS to keep upload queues short when download traffic is heavy.",
            "On owned G4AR lab units, compare Auto, LTE anchor/5G NSA, LTE-only, and NR SA profiles.",
            "Prefer supported band/cell locks from the modem vendor or router firmware.",
            "Docker recovery bundles preserve the stock API data before experiments.",
            "Retest upload, ping, and packet loss after each placement or antenna change.",
        ],
        "warnings": [
            RF_SAFETY_WARNING,
            TX_POWER_WARNING,
            G4AR_FIRMWARE_WARNING,
        ],
    }


def capability_status(
    enabled: bool,
) -> dict[str, Any]:
    if not enabled:
        return {
            "supported": False,
            "status": "disabled",
            "reason": "Enable the unlock/radio lab and acknowledge the risk warning first.",
        }
    return {
        "supported": False,
        "status": "not_exposed_by_stock_api",
        "reason": "Stock G4AR firmware does not expose this command to Docker.",
    }


def firmware_capability_status(
    mode: str,
    enabled: bool,
) -> dict[str, Any]:
    if mode not in G4AR_LAB_MODES:
        return {
            "supported": False,
            "status": "mode_required",
            "reason": "Select G4AR unlock / radio lab mode first.",
        }
    if not enabled:
        return {
            "supported": False,
            "status": "disabled",
            "reason": "Enable the G4AR Docker lab and acknowledge the warning first.",
        }
    return {
        "supported": True,
        "status": "docker_recovery_bundle_ready",
        "reason": (
            "Docker can save a downloadable stock-API recovery bundle. Raw firmware "
            "and flash partitions are not included."
        ),
    }


def firmware_flash_status(
    mode: str,
    enabled: bool,
) -> dict[str, Any]:
    if mode not in G4AR_LAB_MODES:
        return {
            "supported": False,
            "status": "mode_required",
            "reason": "Select G4AR unlock / radio lab mode first.",
        }
    if not enabled:
        return capability_status(enabled)
    return {
        "supported": False,
        "status": "unavailable_no_verified_writer",
        "reason": (
            "Docker has no verified G4AR firmware writer or raw partition recovery path."
        ),
    }


def g4ar_firmware_lab_status(settings: Any) -> dict[str, Any]:
    active = settings.advanced_modem_mode in G4AR_LAB_MODES
    docker_ready = active and settings.advanced_modem_acknowledged
    return {
        "device": "Arcadyan TMO-G4AR",
        "active": active,
        "docker_ready": docker_ready,
        "stock_backup_skipped": settings.advanced_skip_stock_backup,
        "consent_phrase": G4AR_FLASH_CONSENT_PHRASE,
        "flash_warning": G4AR_FIRMWARE_WARNING,
        "flash_enabled": False,
        "flash_status": (
            "blocked_until_backup_and_recovery_verified"
            if active
            else "select_g4ar_unlock_lab_mode"
        ),
        "required_before_flash": [
            "Identify exact TMO-G4AR hardware revision and current firmware version.",
            "Create a Docker recovery bundle for stock API settings and inventory.",
            "Obtain a separate complete raw partition backup before any future flashing.",
            "Skipping the setup reminder does not replace a verified stock backup.",
            "Back up calibration, modem identity, MAC addresses, IMEI-related metadata, and config/NVRAM.",
            "Store SHA-256 hashes for stock backup and custom firmware image.",
            "Verify a working recovery path on that exact gateway before writing firmware.",
            f'Type "{G4AR_FLASH_CONSENT_PHRASE}" immediately before any future flash operation.',
        ],
        "radio_research_goals": [
            "Determine whether the current firmware is forcing 5G Standalone where LTE anchor/5G NSA performs better.",
            "Test LTE-only and LTE anchor profiles without changing transmit-power behavior.",
            "Compare download, upload, ping, SINR, RSRP, RSRQ, band, PCI, and serving cell after each profile change.",
        ],
        "backup_artifacts": [
            "gateway-snapshot.json",
            "wifi-configuration.json",
            "restore-notes.md",
            "SHA256SUMS",
        ],
    }


def g4ar_radio_summary(settings: Any) -> dict[str, Any]:
    profile = settings.advanced_radio_profile
    profile_info = G4AR_RADIO_PROFILES[profile]
    active = settings.advanced_modem_mode in G4AR_LAB_MODES
    return {
        "profile": profile,
        "label": profile_info["label"],
        "description": profile_info["description"],
        "active": active,
        "supported_lte_bands": ["B2", "B4", "B5", "B12", "B25", "B48", "B66", "B71"],
        "supported_nr_bands": ["n25", "n41", "n48", "n66", "n71", "n77"],
        "available_profiles": [
            {
                "value": key,
                "label": value["label"],
                "description": value["description"],
            }
            for key, value in G4AR_RADIO_PROFILES.items()
        ],
        "notes": [
            "LTE/NSA selection is research intent, not proof that the stock gateway exposes a public command.",
            "Docker stores the selection for before/after comparisons but does not apply unsupported modem commands.",
        ],
    }


def g4ar_capability_status(
    mode: str,
    enabled: bool,
) -> dict[str, Any]:
    if mode not in G4AR_LAB_MODES:
        return {
            "supported": False,
            "status": "mode_required",
            "reason": "Select G4AR unlock / radio lab mode first.",
        }
    return capability_status(enabled)


def validate_flash_consent(payload: dict[str, Any]) -> list[str]:
    missing: list[str] = []
    if payload.get("consent_phrase") != G4AR_FLASH_CONSENT_PHRASE:
        missing.append("exact consent phrase")
    if not payload.get("backup_verified"):
        missing.append("stock backup verification")
    if not payload.get("recovery_verified"):
        missing.append("recovery path verification")
    if not payload.get("understands_brick_risk"):
        missing.append("brick-risk acknowledgement")
    if not _looks_like_sha256(payload.get("stock_backup_sha256")):
        missing.append("stock backup SHA-256")
    if not _looks_like_sha256(payload.get("firmware_sha256")):
        missing.append("custom firmware SHA-256")
    return missing


def _looks_like_sha256(value: Any) -> bool:
    text = str(value or "").strip().lower()
    return len(text) == 64 and all(character in "0123456789abcdef" for character in text)
