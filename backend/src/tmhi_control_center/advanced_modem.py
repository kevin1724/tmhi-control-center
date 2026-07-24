from __future__ import annotations

from typing import Any


ADVANCED_MODEM_MODES = {
    "disabled": {
        "label": "Disabled",
        "description": "Use stock TMHI gateway APIs only.",
        "adapter_required": False,
    },
    "openwrt_rooter": {
        "label": "OpenWrt / ROOTer",
        "description": "For user-owned routers that expose modem controls through OpenWrt or ROOTer.",
        "adapter_required": True,
    },
    "modemmanager": {
        "label": "ModemManager",
        "description": "For Linux hosts exposing owned modems through ModemManager/QMI/MBIM tooling.",
        "adapter_required": True,
    },
    "custom_adapter": {
        "label": "Custom local adapter",
        "description": "For a local adapter service that safely wraps supported vendor modem commands.",
        "adapter_required": True,
    },
    "g4ar_unlock_lab": {
        "label": "G4AR unlock / radio lab",
        "description": (
            "For owner-controlled Arcadyan TMO-G4AR units where a local adapter can "
            "research backups, recovery, firmware overrides, and LTE/NSA radio profiles."
        ),
        "adapter_required": True,
    },
    "g4ar_firmware_lab": {
        "label": "G4AR firmware lab (legacy)",
        "description": (
            "Legacy saved value for G4AR unlock / radio lab."
        ),
        "adapter_required": True,
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
            "Adapter-facing research profile for trying LTE anchor plus 5G NR NSA "
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
    mode = settings.advanced_modem_mode
    mode_info = ADVANCED_MODEM_MODES.get(mode, ADVANCED_MODEM_MODES["disabled"])
    enabled = mode != "disabled" and settings.advanced_modem_acknowledged
    adapter_configured = bool(settings.advanced_modem_control_url)
    effective_control_url = settings.advanced_modem_control_url or (
        BUILT_IN_DOCKER_ADAPTER_URL if mode != "disabled" else ""
    )
    built_in_adapter_selected = effective_control_url.rstrip("/") == BUILT_IN_DOCKER_ADAPTER_URL

    return {
        "mode": mode,
        "label": mode_info["label"],
        "description": mode_info["description"],
        "enabled": enabled,
        "acknowledged": settings.advanced_modem_acknowledged,
        "control_url": settings.advanced_modem_control_url,
        "effective_control_url": effective_control_url,
        "default_control_url": BUILT_IN_DOCKER_ADAPTER_URL,
        "built_in_adapter_selected": built_in_adapter_selected,
        "control_url_configured": adapter_configured or bool(effective_control_url),
        "requires_adapter": mode_info["adapter_required"],
        "available_modes": [
            {
                "value": key,
                "label": value["label"],
                "description": value["description"],
            }
            for key, value in ADVANCED_MODEM_MODES.items()
        ],
        "capabilities": {
            "cell_lock": capability_status(
                enabled,
                adapter_configured,
                built_in_adapter_selected=built_in_adapter_selected,
            ),
            "band_lock": capability_status(
                enabled,
                adapter_configured,
                built_in_adapter_selected=built_in_adapter_selected,
            ),
            "cell_scan": capability_status(
                enabled,
                adapter_configured,
                built_in_adapter_selected=built_in_adapter_selected,
            ),
            "lte_anchor_override": g4ar_capability_status(
                settings.advanced_modem_mode,
                enabled,
                adapter_configured,
                built_in_adapter_selected=built_in_adapter_selected,
            ),
            "radio_mode_override": g4ar_capability_status(
                settings.advanced_modem_mode,
                enabled,
                adapter_configured,
                built_in_adapter_selected=built_in_adapter_selected,
            ),
            "upload_priority_qos": capability_status(
                enabled,
                adapter_configured,
                built_in_adapter_selected=built_in_adapter_selected,
            ),
            "stock_firmware_backup": firmware_capability_status(
                settings.advanced_modem_mode,
                enabled,
                adapter_configured,
                built_in_adapter_selected=built_in_adapter_selected,
            ),
            "custom_firmware_flash": firmware_flash_status(
                settings.advanced_modem_mode,
                enabled,
                adapter_configured,
                built_in_adapter_selected=built_in_adapter_selected,
            ),
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
                "A local adapter must apply the profile on supported router firmware.",
                "The Docker default adapter URL is only a built-in coordinator unless hardware-specific bridge tooling is installed.",
            ],
        },
        "g4ar_radio": g4ar_radio_summary(settings),
        "g4ar_unlock_lab": g4ar_firmware_lab_status(settings),
        "g4ar_firmware_lab": g4ar_firmware_lab_status(settings),
        "upload_optimization": [
            "Aim directional antennas using SINR and RSRP trends, not bars alone.",
            "Compare nearby cells on the tower map before applying any lock.",
            "Use SQM/QoS to keep upload queues short when download traffic is heavy.",
            "On owned G4AR lab units, compare Auto, LTE anchor/5G NSA, LTE-only, and NR SA profiles.",
            "Prefer supported band/cell locks from the modem vendor or router firmware.",
            "Use the built-in Docker adapter URL for health checks and defaults; use a hardware bridge for real modem commands.",
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
    adapter_configured: bool,
    *,
    built_in_adapter_selected: bool = False,
) -> dict[str, Any]:
    if not enabled:
        return {
            "supported": False,
            "status": "disabled",
            "reason": "Enable the unlock/radio lab and acknowledge the risk warning first.",
        }
    if not adapter_configured:
        return {
            "supported": False,
            "status": "adapter_required",
            "reason": "Configure a local adapter URL before sending modem control requests.",
        }
    if built_in_adapter_selected:
        return {
            "supported": False,
            "status": "hardware_bridge_required",
            "reason": (
                "The built-in Docker adapter is reachable for health checks and "
                "defaults, but real modem commands require trusted hardware bridge tooling."
            ),
        }
    return {
        "supported": True,
        "status": "adapter_ready",
        "reason": "Adapter mode is configured. Command support still depends on the modem.",
    }


def firmware_capability_status(
    mode: str,
    enabled: bool,
    adapter_configured: bool,
    *,
    built_in_adapter_selected: bool = False,
) -> dict[str, Any]:
    if mode not in G4AR_LAB_MODES:
        return {
            "supported": False,
            "status": "mode_required",
            "reason": "Select G4AR unlock / radio lab mode first.",
        }
    return capability_status(
        enabled,
        adapter_configured,
        built_in_adapter_selected=built_in_adapter_selected,
    )


def firmware_flash_status(
    mode: str,
    enabled: bool,
    adapter_configured: bool,
    *,
    built_in_adapter_selected: bool = False,
) -> dict[str, Any]:
    if mode not in G4AR_LAB_MODES:
        return {
            "supported": False,
            "status": "mode_required",
            "reason": "Select G4AR unlock / radio lab mode first.",
        }
    if not enabled:
        return capability_status(
            enabled,
            adapter_configured,
            built_in_adapter_selected=built_in_adapter_selected,
        )
    if not adapter_configured:
        return capability_status(
            enabled,
            adapter_configured,
            built_in_adapter_selected=built_in_adapter_selected,
        )
    if built_in_adapter_selected:
        return capability_status(
            enabled,
            adapter_configured,
            built_in_adapter_selected=built_in_adapter_selected,
        )
    return {
        "supported": False,
        "status": "consent_and_recovery_required",
        "reason": (
            "Flashing stays locked until stock backup hash, recovery verification, "
            "firmware hash, and typed consent are provided."
        ),
    }


def g4ar_firmware_lab_status(settings: Any) -> dict[str, Any]:
    active = settings.advanced_modem_mode in G4AR_LAB_MODES
    built_in_adapter_selected = (
        settings.advanced_modem_control_url.rstrip("/") == BUILT_IN_DOCKER_ADAPTER_URL
    )
    adapter_ready = active and settings.advanced_modem_acknowledged and bool(
        settings.advanced_modem_control_url
    ) and not built_in_adapter_selected
    effective_control_url = settings.advanced_modem_control_url or (
        BUILT_IN_DOCKER_ADAPTER_URL if active else ""
    )
    return {
        "device": "Arcadyan TMO-G4AR",
        "active": active,
        "adapter_ready": adapter_ready,
        "built_in_adapter_selected": built_in_adapter_selected,
        "effective_control_url": effective_control_url,
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
            "Create a complete local stock firmware backup.",
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
            "stock-firmware.bin",
            "partition-table.txt",
            "calibration-and-identity-backup.tar",
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
            "LTE/NSA override is an adapter-facing research profile, not proof that the stock gateway exposes a public command.",
            "The app stores intent and validates safety gates; a local adapter must implement device-specific commands.",
        ],
    }


def g4ar_capability_status(
    mode: str,
    enabled: bool,
    adapter_configured: bool,
    *,
    built_in_adapter_selected: bool = False,
) -> dict[str, Any]:
    if mode not in G4AR_LAB_MODES:
        return {
            "supported": False,
            "status": "mode_required",
            "reason": "Select G4AR unlock / radio lab mode first.",
        }
    return capability_status(
        enabled,
        adapter_configured,
        built_in_adapter_selected=built_in_adapter_selected,
    )


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
