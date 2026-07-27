from __future__ import annotations

from types import SimpleNamespace

from tmhi_control_center.usb_lab import g4ar_usb_status, normalize_usb_probe


def test_usb_probe_normalizes_ready_2_5gbe_adapter() -> None:
    payload = {
        "observed_at": "2026-07-27T00:00:00Z",
        "port": {"role": "host", "speed_mbps": "5 Gbps", "vbus": True},
        "devices": [
            {
                "vid": "0x0b95",
                "pid": "2790",
                "manufacturer": "ASIX",
                "product": "AX88279 USB 2.5G Ethernet",
                "usb_speed": "5 Gbps",
                "driver": "cdc_ncm",
                "network_interface": "usb0",
                "link_up": True,
                "ethernet_speed_mbps": 2500,
                "duplex": "full",
                "mac": "should-not-be-returned",
            }
        ],
        "bridge": {
            "name": "br-lan",
            "members": ["lan1", "lan2", "usb0"],
            "usb_member": True,
        },
    }

    probe = normalize_usb_probe(payload, source="http://router.local:8765")

    assert probe["status"] == "active_2_5gbe"
    assert probe["ready_for_isolated_test"] is True
    assert probe["ready_for_lan_bridge"] is True
    assert probe["port"]["controller_speed_mbps"] == 5000
    assert probe["best_ethernet_device"]["link_speed_mbps"] == 2500
    assert probe["best_ethernet_device"]["driver"] == "cdc_ncm"
    assert "mac" not in probe["best_ethernet_device"]


def test_usb_probe_reports_visible_device_without_driver() -> None:
    probe = normalize_usb_probe(
        {
            "role": "host",
            "controller_speed_mbps": 5000,
            "devices": [
                {
                    "vendor_id": "0bda",
                    "product_id": "8156",
                    "product": "AX88279",
                    "usb_speed_mbps": 5000,
                }
            ],
        }
    )

    assert probe["status"] == "driver_needed"
    assert probe["checks"]["ethernet_adapter"] is True
    assert probe["checks"]["driver_bound"] is False
    assert probe["ready_for_isolated_test"] is False


def test_usb_status_explains_docker_hardware_boundary() -> None:
    settings = SimpleNamespace(
        advanced_modem_mode="g4ar_unlock_lab",
        advanced_modem_acknowledged=True,
        advanced_modem_control_url="http://127.0.0.1:8000",
    )

    status = g4ar_usb_status(settings)

    assert status["status"] == "hardware_bridge_required"
    assert status["hardware_adapter_ready"] is False
    assert status["built_in_adapter_selected"] is True
    assert status["platform"]["controller_speed_mbps"] == 5000
    assert status["recommended_adapter"]["chipset"] == "ASIX AX88279"
