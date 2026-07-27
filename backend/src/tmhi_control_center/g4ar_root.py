from __future__ import annotations

from typing import Any


G4AR_ROOT_CONSENT_PHRASE = (
    "I OWN THIS G4AR - ROOT RESEARCH CAN PERMANENTLY BRICK IT"
)

G4AR_ROOT_WARNING = (
    "ROOT RESEARCH CAN PERMANENTLY BRICK THIS GATEWAY. Continue only with an "
    "Arcadyan TMO-G4AR that you own outright and can replace. Do not use this lab "
    "on leased, financed, carrier-owned, or service-critical hardware. Opening the "
    "case, attaching test equipment, bypassing verified boot, or writing storage can "
    "destroy calibration and identity data, void warranty, break service, and leave "
    "the unit without a working recovery path."
)

ROOT_READINESS_REQUIREMENTS = (
    ("owns_hardware", "confirmation that this exact G4AR is owned outright"),
    ("not_leased_or_financed", "confirmation that it is not leased, financed, or carrier-owned"),
    ("spare_noncritical_unit", "a spare, non-service-critical gateway that can be replaced"),
    ("hardware_revision_recorded", "exact model, board revision, and firmware version recorded"),
    ("uart_voltage_verified", "serial pad voltage and pin roles measured on this exact board"),
    ("read_only_boot_log_captured", "a read-only boot log captured without connecting adapter TX or VCC"),
    ("full_backup_verified", "complete partition, calibration, identity, and NVRAM backup verified"),
    ("offline_recovery_verified", "an offline recovery and restore path tested on this exact unit"),
    ("accepts_permanent_brick_risk", "permanent-brick and warranty/service risk acknowledgement"),
)


def g4ar_root_research_status() -> dict[str, Any]:
    return {
        "device": "Arcadyan TMO-G4AR",
        "status": "research_only_no_verified_root_chain",
        "verified_root_available": False,
        "one_click_root_available": False,
        "openwrt_image_available": False,
        "root_execution_enabled": False,
        "warning": G4AR_ROOT_WARNING,
        "consent_phrase": G4AR_ROOT_CONSENT_PHRASE,
        "current_finding": (
            "No public, reproducible G4AR root chain, bootloader unlock, or supported "
            "OpenWrt image has been verified. The safe next step is read-only hardware "
            "discovery and recovery research, not flashing."
        ),
        "verified_evidence": [
            (
                "The FCC filing for FCC ID RAXTMOG4AR provides G4AR internal-board "
                "photos that can be used to confirm board layout before probing."
            ),
            (
                "The stock G4AR /TMI/v1 local API exposes gateway administration data "
                "and selected controls, but API administrator access is not operating-system root."
            ),
            (
                "A related Arcadyan KVD21 teardown reports a MediaTek T75-family platform "
                "and read-only 1.8 V serial output; that is a research clue, not a verified "
                "G4AR pinout or root method."
            ),
        ],
        "not_verified": [
            "A G4AR UART pad order, writable console, bootloader prompt, or secure-boot bypass.",
            "A complete G4AR eMMC partition map and repeatable hardware restore procedure.",
            "A safe G4AR firmware downgrade package or signed custom firmware image.",
            "A G4AR-compatible OpenWrt/ROOTer build or one-click rooting utility.",
        ],
        "required_equipment": [
            "ESD-safe workspace, board photos, continuity-capable multimeter, and logic analyzer.",
            "Isolated 1.8 V-safe USB-UART interface; never attach a 3.3 V or 5 V UART directly.",
            "A separate Linux research host with encrypted local storage for redacted logs and backups.",
            "A tested, exact-device offline restore path before any write-capable connection.",
        ],
        "research_phases": [
            {
                "phase": 1,
                "title": "Prove ownership and identify the unit",
                "goal": "Record model, serial-redacted label, board revision, and firmware version.",
                "write_allowed": False,
            },
            {
                "phase": 2,
                "title": "Compare the board to the FCC filing",
                "goal": "Confirm that component and test-pad locations match this exact hardware revision.",
                "write_allowed": False,
            },
            {
                "phase": 3,
                "title": "Measure before connecting",
                "goal": "Identify ground, idle voltage, and likely transmit activity with test instruments only.",
                "write_allowed": False,
            },
            {
                "phase": 4,
                "title": "Capture receive-only boot output",
                "goal": "Connect adapter ground and RX only after voltage verification; never connect adapter VCC.",
                "write_allowed": False,
            },
            {
                "phase": 5,
                "title": "Document boot and storage evidence",
                "goal": "Identify boot stages, secure-boot state, storage type, and partition names from logs.",
                "write_allowed": False,
            },
            {
                "phase": 6,
                "title": "Build and test recovery",
                "goal": "Create complete readbacks and prove restoration on the same spare unit.",
                "write_allowed": False,
            },
            {
                "phase": 7,
                "title": "Evaluate a G4AR-specific root chain",
                "goal": "Proceed only after an exact-revision method and recovery path are independently reproduced.",
                "write_allowed": False,
            },
        ],
        "hard_stops": [
            "Do not apply KVD21 or other router pinouts to a G4AR without measuring this board.",
            "Do not connect USB-UART VCC, 3.3 V, or 5 V to an unknown G4AR test pad.",
            "Do not run generic U-Boot, fastboot, BROM, preloader, or partition-write commands copied from another model.",
            "Do not use firmware or scripts that send MAC, IMEI, serial, tokens, or credentials off the trusted LAN.",
            "Stop if a backup omits calibration, identity, NVRAM, partition-table, or restore metadata.",
        ],
        "sources": [
            {
                "label": "FCC G4AR internal photos",
                "url": "https://fccid.io/RAXTMOG4AR/Internal-Photos/Internal-Photos-1-rev-6551767",
            },
            {
                "label": "G4AR local API research",
                "url": "https://github.com/joaovorocha/tmobile-g4ar-local-api",
            },
            {
                "label": "Related KVD21 hardware research",
                "url": "https://github.com/chainofexecution/Arcadyan-KVD21",
            },
        ],
    }


def assess_g4ar_root_readiness(
    payload: dict[str, Any],
    *,
    lab_mode_active: bool,
    lab_acknowledged: bool,
) -> dict[str, Any]:
    missing = [
        description
        for key, description in ROOT_READINESS_REQUIREMENTS
        if not payload.get(key)
    ]
    if payload.get("consent_phrase") != G4AR_ROOT_CONSENT_PHRASE:
        missing.append("the exact owner/root-research consent phrase")
    if not lab_mode_active:
        missing.append("G4AR unlock / radio lab mode enabled")
    if not lab_acknowledged:
        missing.append("the owned-hardware lab warning acknowledged")

    read_only_ready = not missing
    return {
        "status": (
            "ready_for_read_only_hardware_research"
            if read_only_ready
            else "readiness_requirements_missing"
        ),
        "ready_for_read_only_research": read_only_ready,
        "ready_for_rooting": False,
        "root_execution_enabled": False,
        "missing": missing,
        "warning": G4AR_ROOT_WARNING,
        "next_action": (
            "Preserve and redact the read-only evidence. Rooting remains unavailable until "
            "a G4AR-specific chain and exact-device recovery path are independently verified."
            if read_only_ready
            else "Complete the missing ownership, measurement, backup, and recovery gates."
        ),
    }
