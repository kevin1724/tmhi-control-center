from tmhi_control_center.g4ar_root import (
    G4AR_ROOT_CONSENT_PHRASE,
    assess_g4ar_root_readiness,
    g4ar_root_research_status,
)


def _complete_payload() -> dict[str, object]:
    return {
        "owns_hardware": True,
        "not_leased_or_financed": True,
        "spare_noncritical_unit": True,
        "hardware_revision_recorded": True,
        "uart_voltage_verified": True,
        "read_only_boot_log_captured": True,
        "full_backup_verified": True,
        "offline_recovery_verified": True,
        "accepts_permanent_brick_risk": True,
        "consent_phrase": G4AR_ROOT_CONSENT_PHRASE,
    }


def test_root_status_never_claims_a_verified_chain() -> None:
    status = g4ar_root_research_status()

    assert status["status"] == "research_only_no_verified_root_chain"
    assert status["verified_root_available"] is False
    assert status["one_click_root_available"] is False
    assert status["openwrt_image_available"] is False
    assert status["root_execution_enabled"] is False
    assert all(phase["write_allowed"] is False for phase in status["research_phases"])


def test_complete_readiness_still_does_not_enable_root_execution() -> None:
    result = assess_g4ar_root_readiness(
        _complete_payload(),
        lab_mode_active=True,
        lab_acknowledged=True,
    )

    assert result["ready_for_read_only_research"] is True
    assert result["ready_for_rooting"] is False
    assert result["root_execution_enabled"] is False
    assert result["missing"] == []


def test_readiness_requires_mode_acknowledgement_and_exact_consent() -> None:
    payload = _complete_payload()
    payload["consent_phrase"] = "I own it"

    result = assess_g4ar_root_readiness(
        payload,
        lab_mode_active=False,
        lab_acknowledged=False,
    )

    assert result["ready_for_read_only_research"] is False
    assert result["root_execution_enabled"] is False
    assert "the exact owner/root-research consent phrase" in result["missing"]
    assert "G4AR unlock / radio lab mode enabled" in result["missing"]
    assert "the owned-hardware lab warning acknowledged" in result["missing"]
