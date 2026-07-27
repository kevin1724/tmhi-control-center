from __future__ import annotations

import re
from datetime import UTC, datetime
from typing import Any

import httpx

from .advanced_modem import BUILT_IN_DOCKER_ADAPTER_URL, G4AR_LAB_MODES


class UsbProbeError(RuntimeError):
    pass


G4AR_USB_PLATFORM = {
    "model": "Arcadyan TMO-G4AR",
    "data_port": "USB Type-C data port",
    "controller": "MediaTek T750 USB 3",
    "controller_speed_mbps": 5000,
    "ethernet_target_mbps": 2500,
    "stock_firmware_support": "Unverified",
}

RECOMMENDED_USB_NIC = {
    "chipset": "ASIX AX88279",
    "usb": "USB 3.2 Gen 1 Type-C",
    "ethernet": "2.5GBASE-T",
    "driver_preference": "CDC-NCM",
    "reason": (
        "CDC-NCM gives locked Linux firmware a better chance of recognizing the "
        "adapter without a vendor-specific kernel module."
    ),
}


def g4ar_usb_status(settings: Any, probe: dict[str, Any] | None = None) -> dict[str, Any]:
    mode_ready = settings.advanced_modem_mode in G4AR_LAB_MODES
    acknowledged = bool(settings.advanced_modem_acknowledged)
    control_url = str(settings.advanced_modem_control_url or "").rstrip("/")
    built_in = control_url == BUILT_IN_DOCKER_ADAPTER_URL
    hardware_adapter_ready = mode_ready and acknowledged and bool(control_url) and not built_in

    if probe:
        status = probe.get("status", "probe_complete")
        reason = probe.get("summary", "USB-C hardware probe completed.")
    elif not mode_ready:
        status = "lab_mode_required"
        reason = "Select G4AR unlock / radio lab mode before probing gateway hardware."
    elif not acknowledged:
        status = "acknowledgement_required"
        reason = "Acknowledge the owned-hardware research warning before probing."
    elif built_in or not control_url:
        status = "hardware_bridge_required"
        reason = (
            "Docker cannot inspect devices attached inside the gateway. A trusted adapter "
            "running on a rooted or custom-firmware G4AR must provide the read-only probe."
        )
    else:
        status = "ready_to_probe"
        reason = "The hardware adapter is configured and ready for a read-only USB probe."

    return {
        "status": status,
        "reason": reason,
        "platform": G4AR_USB_PLATFORM,
        "recommended_adapter": RECOMMENDED_USB_NIC,
        "mode_ready": mode_ready,
        "acknowledged": acknowledged,
        "hardware_adapter_ready": hardware_adapter_ready,
        "built_in_adapter_selected": built_in,
        "adapter_endpoint": "GET /g4ar/usb/probe",
        "probe": probe,
        "safety": [
            "Use the USB-C data port only; keep the supplied power adapter on the power port.",
            "Do not connect a powered hub or inject USB-C Power Delivery into the data port.",
            "Probe first. Do not make bridge changes until USB 3, driver, interface, and link speed are verified.",
            "Test bridge changes in memory first so a gateway reboot restores the previous network layout.",
        ],
    }


async def probe_g4ar_usb(settings: Any) -> dict[str, Any]:
    control_url = str(settings.advanced_modem_control_url or "").rstrip("/")
    if not control_url or control_url == BUILT_IN_DOCKER_ADAPTER_URL:
        return g4ar_usb_status(settings)

    url = f"{control_url}/g4ar/usb/probe"
    try:
        async with httpx.AsyncClient(timeout=httpx.Timeout(10.0)) as client:
            response = await client.get(url)
    except httpx.HTTPError as exc:
        raise UsbProbeError(f"USB hardware adapter could not be reached: {exc}") from exc

    if not response.is_success:
        raise UsbProbeError(
            f"USB hardware adapter returned HTTP {response.status_code} {response.reason_phrase}"
        )
    try:
        payload = response.json()
    except ValueError as exc:
        raise UsbProbeError("USB hardware adapter returned invalid JSON") from exc
    if not isinstance(payload, dict):
        raise UsbProbeError("USB hardware adapter response must be a JSON object")

    probe = normalize_usb_probe(payload, source=control_url)
    return g4ar_usb_status(settings, probe=probe)


def normalize_usb_probe(payload: dict[str, Any], *, source: str = "adapter") -> dict[str, Any]:
    port = _mapping(payload.get("port") or payload.get("controller"))
    raw_devices = payload.get("devices")
    devices = [
        _normalize_device(item)
        for item in (raw_devices if isinstance(raw_devices, list) else [])[:16]
        if isinstance(item, dict)
    ]
    devices = [device for device in devices if any(value is not None for value in device.values())]

    role = _text(port.get("role") or payload.get("role"), 24).lower() or "unknown"
    controller_speed = _speed_mbps(
        port.get("speed_mbps")
        or port.get("usb_speed_mbps")
        or payload.get("controller_speed_mbps")
    )
    vbus = _boolean(port.get("vbus") if "vbus" in port else payload.get("vbus"))
    bridge = _normalize_bridge(_mapping(payload.get("bridge")))

    ethernet_devices = [
        device
        for device in devices
        if device.get("interface")
        or device.get("driver") in {"cdc_ncm", "cdc_ether", "r8152", "ax88179_178a"}
        or "ethernet" in str(device.get("product") or "").lower()
        or "2.5g" in str(device.get("product") or "").lower()
        or "ax88279" in str(device.get("product") or "").lower()
        or "rtl8156" in str(device.get("product") or "").lower()
    ]
    best = max(
        ethernet_devices,
        key=lambda device: int(device.get("link_speed_mbps") or 0),
        default={},
    )

    usb_host = role == "host" or bool(devices)
    super_speed = (controller_speed or 0) >= 5000 or any(
        int(device.get("usb_speed_mbps") or 0) >= 5000 for device in devices
    )
    driver_bound = bool(best.get("driver"))
    interface_ready = bool(best.get("interface"))
    carrier = bool(best.get("carrier"))
    link_2500 = int(best.get("link_speed_mbps") or 0) >= 2500
    bridge_member = bool(bridge.get("usb_member"))

    checks = {
        "usb_host": usb_host,
        "super_speed_5gbps": super_speed,
        "ethernet_adapter": bool(ethernet_devices),
        "driver_bound": driver_bound,
        "network_interface": interface_ready,
        "carrier": carrier,
        "link_2500mbps": link_2500,
        "lan_bridge_member": bridge_member,
    }
    if link_2500 and bridge_member:
        status = "active_2_5gbe"
        summary = "USB-C Ethernet is linked at 2.5 Gbps and attached to the LAN bridge."
    elif link_2500:
        status = "link_ready"
        summary = "The adapter has a 2.5 Gbps link and is ready for an isolated network test."
    elif interface_ready:
        status = "interface_ready"
        summary = "The USB Ethernet interface exists; connect a 2.5GbE peer and verify carrier."
    elif ethernet_devices:
        status = "driver_needed"
        summary = "The USB Ethernet device is visible, but no usable network interface was reported."
    elif devices:
        status = "non_ethernet_usb_detected"
        summary = "USB host mode works, but no USB Ethernet adapter was detected."
    else:
        status = "no_usb_device"
        summary = "The adapter reported no device on the G4AR USB-C data port."

    return {
        "observed_at": _text(payload.get("observed_at"), 64)
        or datetime.now(UTC).isoformat(),
        "source": _text(source, 256),
        "status": status,
        "summary": summary,
        "port": {
            "role": role,
            "controller_speed_mbps": controller_speed,
            "vbus": vbus,
        },
        "devices": devices,
        "best_ethernet_device": best or None,
        "bridge": bridge or None,
        "checks": checks,
        "ready_for_isolated_test": all(
            (usb_host, super_speed, bool(ethernet_devices), driver_bound, interface_ready)
        ),
        "ready_for_lan_bridge": all(
            (
                usb_host,
                super_speed,
                bool(ethernet_devices),
                driver_bound,
                interface_ready,
                carrier,
                link_2500,
            )
        ),
    }


def _normalize_device(device: dict[str, Any]) -> dict[str, Any]:
    return {
        "vendor_id": _usb_id(device.get("vendor_id") or device.get("vid")),
        "product_id": _usb_id(device.get("product_id") or device.get("pid")),
        "manufacturer": _text(device.get("manufacturer"), 80) or None,
        "product": _text(device.get("product") or device.get("name"), 120) or None,
        "usb_speed_mbps": _speed_mbps(device.get("usb_speed_mbps") or device.get("usb_speed")),
        "driver": _identifier(device.get("driver")),
        "interface": _identifier(device.get("interface") or device.get("network_interface")),
        "carrier": _boolean(device.get("carrier") if "carrier" in device else device.get("link_up")),
        "link_speed_mbps": _speed_mbps(
            device.get("link_speed_mbps") or device.get("ethernet_speed_mbps")
        ),
        "duplex": _text(device.get("duplex"), 16).lower() or None,
    }


def _normalize_bridge(bridge: dict[str, Any]) -> dict[str, Any]:
    if not bridge:
        return {}
    members = bridge.get("members")
    raw_members = members if isinstance(members, list) else []
    safe_members = [
        member
        for member in (_identifier(value) for value in raw_members)
        if member
    ][:24]
    return {
        "name": _identifier(bridge.get("name")),
        "members": safe_members,
        "usb_member": _boolean(bridge.get("usb_member")),
    }


def _mapping(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def _text(value: Any, limit: int) -> str:
    if value is None:
        return ""
    return str(value).strip()[:limit]


def _identifier(value: Any) -> str | None:
    text = _text(value, 40)
    if not text or not re.fullmatch(r"[A-Za-z0-9_.:@-]+", text):
        return None
    return text


def _usb_id(value: Any) -> str | None:
    text = _text(value, 8).lower().removeprefix("0x")
    if not re.fullmatch(r"[0-9a-f]{4}", text):
        return None
    return text


def _speed_mbps(value: Any) -> int | None:
    if value is None or isinstance(value, bool):
        return None
    if isinstance(value, (int, float)):
        speed = int(value)
    else:
        match = re.search(r"\d+(?:\.\d+)?", str(value).replace(",", ""))
        if not match:
            return None
        number = float(match.group(0))
        lowered = str(value).lower()
        speed = int(number * 1000) if "gb" in lowered else int(number)
    return speed if 0 < speed <= 100_000 else None


def _boolean(value: Any) -> bool | None:
    if isinstance(value, bool):
        return value
    if isinstance(value, (int, float)):
        return bool(value)
    if isinstance(value, str):
        lowered = value.strip().lower()
        if lowered in {"true", "yes", "up", "on", "1", "present"}:
            return True
        if lowered in {"false", "no", "down", "off", "0", "absent"}:
            return False
    return None
