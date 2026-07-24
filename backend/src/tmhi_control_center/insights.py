from __future__ import annotations

from typing import Any

from .advanced_modem import BUILT_IN_DOCKER_ADAPTER_URL

ADAPTER_EXAMPLES = (
    BUILT_IN_DOCKER_ADAPTER_URL,
    "http://router.local:8080",
    "http://192.168.1.2:8765",
    "http://rooter.lan:8080",
)

ADAPTER_REQUIRED_ENDPOINTS = (
    "GET /health",
    "POST /g4ar/firmware/backup",
    "POST /modem/radio/profile",
    "POST /modem/cell/scan",
    "POST /modem/lock",
)


def build_homelab_insights(
    *,
    config: dict[str, Any],
    status: dict[str, Any],
    overview: dict[str, Any],
    wifi: dict[str, Any],
    clients: dict[str, Any],
    map_data: dict[str, Any],
    events: list[dict[str, Any]],
    firmware_backups: dict[str, Any],
) -> dict[str, Any]:
    setup_steps = _setup_steps(config, status, overview, clients, map_data, firmware_backups)
    signal_coach = _signal_coach(overview, map_data)
    homelab_cards = _homelab_cards(config, status, overview, wifi, clients, map_data, events)
    readiness = _readiness(setup_steps, overview, status)
    return {
        "readiness": readiness,
        "setup_steps": setup_steps,
        "signal_coach": signal_coach,
        "homelab_cards": homelab_cards,
        "adapter_guide": _adapter_guide(config),
    }


def _readiness(
    setup_steps: list[dict[str, Any]],
    overview: dict[str, Any],
    status: dict[str, Any],
) -> dict[str, Any]:
    total_weight = sum(step["weight"] for step in setup_steps) or 1
    earned = sum(
        step["weight"]
        for step in setup_steps
        if step["status"] in {"done", "optional"}
    )
    score = round((earned / total_weight) * 100)
    next_action = next(
        (
            step
            for step in setup_steps
            if step["status"] not in {"done", "optional"}
        ),
        None,
    )
    signal_score = _signal_score(overview)
    online = status.get("internet_online")
    if score >= 85 and (signal_score is None or signal_score >= 70):
        label = "Dialed in"
    elif score >= 65:
        label = "Operational"
    elif online is False:
        label = "Needs recovery"
    else:
        label = "Needs setup"
    return {
        "score": score,
        "label": label,
        "summary": _readiness_summary(score, signal_score, online),
        "next_best_action": next_action["action"] if next_action else "Run a placement sweep and save the snapshot.",
    }


def _readiness_summary(score: int, signal_score: int | None, online: Any) -> str:
    if online is False:
        return "Internet probes are failing. Check gateway reachability, then run a manual check before rebooting."
    if signal_score is not None and signal_score < 50:
        return "Core setup is usable, but signal quality should be tuned before chasing firmware or tower changes."
    if score >= 85:
        return "The key setup pieces are in place. Use sweeps and snapshots to tune placement over time."
    if score >= 65:
        return "The control center is usable. Finish the remaining setup items to make troubleshooting easier."
    return "Start with gateway login, map center, and a baseline signal reading."


def _setup_steps(
    config: dict[str, Any],
    status: dict[str, Any],
    overview: dict[str, Any],
    clients: dict[str, Any],
    map_data: dict[str, Any],
    firmware_backups: dict[str, Any],
) -> list[dict[str, Any]]:
    advanced = _dict(config.get("advanced_modem"))
    map_config = _dict(config.get("map"))
    detection = _dict(overview.get("detection"))
    provider = _dict(map_data.get("provider"))
    center = _dict(_dict(map_data.get("map")).get("center"))
    client_count = _int(clients.get("count"), len(clients.get("devices") or []))
    backup_count = len(firmware_backups.get("backups") or [])
    signal_score = _signal_score(overview)
    g4ar_enabled = advanced.get("mode") in {"g4ar_unlock_lab", "g4ar_firmware_lab"}
    skip_stock_backup = bool(advanced.get("skip_stock_backup"))

    steps = [
        _step(
            "gateway-login",
            "Gateway login saved",
            bool(config.get("gateway_password_configured")),
            "Save the admin password once so Wi-Fi, clients, reboot, and backup tools work without retyping it.",
            "Open Settings, save the gateway admin password, then press Test.",
            18,
        ),
        _step(
            "gateway-api",
            "Gateway API reachable",
            detection.get("reachable") is True or status.get("gateway_reachable") is True,
            "The app needs the local gateway API, usually 192.168.12.1 on port 8080.",
            "Join the gateway LAN/Wi-Fi and verify the gateway host and port in Settings.",
            16,
        ),
        _step(
            "signal-baseline",
            "Signal baseline captured",
            signal_score is not None,
            "A baseline lets you compare antenna direction, placement, bands, and tower changes.",
            "Refresh the dashboard and record RSRP, RSRQ, SINR, band, PCI, and cell ID.",
            14,
            warn=signal_score is not None and signal_score < 50,
        ),
        _step(
            "map-center",
            "Map center saved",
            map_config.get("latitude") is not None and map_config.get("longitude") is not None,
            "A saved home location makes tower searches and serving-cell estimates much more useful.",
            "Open Map, use browser location or paste coordinates, then save the map center.",
            12,
            warn=center.get("source") == "public_ip",
        ),
        _step(
            "tower-provider",
            "Tower lookup ready",
            bool(provider.get("configured") or map_config.get("opencellid_configured")),
            "OpenCellID is optional, but it unlocks nearby tower records and serving-cell map matches.",
            "Add an OpenCellID key in Settings, then refresh towers on the Map page.",
            10,
        ),
        _step(
            "lan-inventory",
            "LAN inventory loaded",
            client_count > 0,
            "Connected-device inventory helps catch unknown clients and identify which devices are stressing upload.",
            "Open Devices and run Reverse Lookup after saving the gateway login.",
            9,
            warn=bool(config.get("gateway_password_configured")) and client_count == 0,
        ),
        _step(
            "watchdog-policy",
            "Watchdog policy reviewed",
            bool(status.get("dry_run")) or bool(config.get("gateway_password_configured")),
            "Dry Run keeps reboot automation safe until the gateway login and recovery behavior have been tested.",
            "Keep Dry Run on until manual checks and reboot recovery look predictable.",
            8,
        ),
    ]

    if g4ar_enabled and backup_count == 0 and skip_stock_backup:
        steps.append(
            {
                "id": "g4ar-backup",
                "title": "G4AR stock backup skipped for now",
                "status": "skipped",
                "tone": "warn",
                "detail": (
                    "The setup reminder is suppressed, but firmware override stays "
                    "locked until backup, recovery, and hashes are verified."
                ),
                "action": "Create a stock backup later before any firmware or radio-profile experiment.",
                "weight": 13,
            }
        )
    elif g4ar_enabled:
        steps.append(
            _step(
                "g4ar-backup",
                "G4AR stock backup saved",
                backup_count > 0,
                "Owned G4AR lab work needs a local stock backup before any adapter-driven firmware research.",
                "Configure the local adapter URL, acknowledge the risk, then create a stock backup.",
                13,
                warn=bool(advanced.get("control_url_configured")) and backup_count == 0,
            )
        )
    else:
        steps.append(
            {
                "id": "g4ar-lab",
                "title": "G4AR lab disabled",
                "status": "optional",
                "tone": "muted",
                "detail": "Advanced firmware/radio work is optional and should stay disabled on stock or leased hardware.",
                "action": "Enable only for owner-controlled G4AR units with a recovery path.",
                "weight": 6,
            }
        )
    return steps


def _step(
    step_id: str,
    title: str,
    done: bool,
    detail: str,
    action: str,
    weight: int,
    *,
    warn: bool = False,
) -> dict[str, Any]:
    if done and warn:
        status = "warn"
        tone = "warn"
    elif done:
        status = "done"
        tone = "good"
    else:
        status = "todo"
        tone = "warn"
    return {
        "id": step_id,
        "title": title,
        "status": status,
        "tone": tone,
        "detail": detail,
        "action": action,
        "weight": weight,
    }


def _signal_coach(overview: dict[str, Any], map_data: dict[str, Any]) -> list[dict[str, Any]]:
    signal = _dict(overview.get("signal"))
    connection = _dict(overview.get("connection"))
    metrics = signal.get("metrics") if isinstance(signal.get("metrics"), list) else []
    by_key = {
        str(metric.get("key", "")).lower(): metric
        for metric in metrics
        if isinstance(metric, dict)
    }
    tips: list[dict[str, Any]] = []
    sinr = _metric_score(by_key.get("sinr"))
    rsrp = _metric_score(by_key.get("rsrp"))
    rsrq = _metric_score(by_key.get("rsrq"))
    band = str(connection.get("band") or "").lower()

    if sinr is None and rsrp is None and rsrq is None:
        tips.append(
            _tip(
                "Capture radio metrics",
                "Refresh after the gateway API responds. RSRP, RSRQ, SINR, band, PCI, and cell ID make every antenna move measurable.",
                "warn",
            )
        )
    if sinr is not None and sinr < 70:
        tips.append(
            _tip(
                "Prioritize SINR before bars",
                "Rotate the gateway or directional antenna in small steps and keep the position that improves SINR without crushing RSRP.",
                "warn" if sinr >= 45 else "bad",
            )
        )
    if rsrp is not None and rsrp < 60:
        tips.append(
            _tip(
                "Improve received power",
                "Move the gateway higher, closer to an exterior wall/window, or aim the antenna at the best mapped cell.",
                "warn" if rsrp >= 35 else "bad",
            )
        )
    if rsrq is not None and rsrq < 55:
        tips.append(
            _tip(
                "Watch congestion and reflections",
                "Weak RSRQ often means noisy or loaded air. Compare another band/tower before assuming the closest site is best.",
                "warn",
            )
        )
    if "n41" in band:
        tips.append(
            _tip(
                "n41 detected",
                "n41 can be excellent for download. If upload or latency is weak, compare placement and LTE-anchor behavior on owned lab hardware.",
                "info",
            )
        )
    if _dict(_dict(map_data.get("connected")).get("location")):
        tips.append(
            _tip(
                "Serving tower is mapped",
                "Use the map line as an aiming baseline, then run a sweep after each antenna or placement change.",
                "good",
            )
        )
    tips.append(
        _tip(
            "Run repeatable sweeps",
            "Change one thing at a time, wait for the gateway to settle, then compare signal, ping, loss, and connected cell.",
            "info",
        )
    )
    return tips[:6]


def _homelab_cards(
    config: dict[str, Any],
    status: dict[str, Any],
    overview: dict[str, Any],
    wifi: dict[str, Any],
    clients: dict[str, Any],
    map_data: dict[str, Any],
    events: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    radio_enabled = wifi.get("radio_enabled")
    client_count = _int(clients.get("count"), len(clients.get("devices") or []))
    signal_score = _signal_score(overview)
    map_provider = _dict(map_data.get("provider"))
    return [
        {
            "title": "Router offload mode",
            "tone": "good" if radio_enabled is False else "info",
            "summary": (
                "Gateway Wi-Fi radios are off. Your own router can own Wi-Fi, DNS, VLANs, and SQM."
                if radio_enabled is False
                else "Use Devices to turn gateway Wi-Fi off when an external router handles the LAN."
            ),
            "actions": [
                "Put your router WAN behind the gateway LAN.",
                "Run DHCP, DNS, VLANs, and Wi-Fi from the router.",
                "Document double-NAT or port-forwarding limits for services.",
            ],
        },
        {
            "title": "Upload and latency tuning",
            "tone": "warn" if signal_score is not None and signal_score < 50 else "info",
            "summary": "Use SQM/QoS on your own router to protect video calls, gaming, VPN, and remote access from upload bufferbloat.",
            "actions": [
                "Measure real upload at different times of day.",
                "Set SQM uplink slightly below stable upload speed.",
                "Retest ping under load after each change.",
            ],
        },
        {
            "title": "Tower and antenna notebook",
            "tone": "good" if map_provider.get("nearby_loaded") else "info",
            "summary": "Track band, PCI, cell ID, SINR, RSRP, speed, and antenna direction so changes are repeatable.",
            "actions": [
                "Save the map center.",
                "Refresh nearby towers.",
                "Run sweeps after each antenna angle or gateway placement change.",
            ],
        },
        {
            "title": "LAN inventory",
            "tone": "good" if client_count else "warn",
            "summary": f"{client_count} connected device{'' if client_count == 1 else 's'} loaded.",
            "actions": [
                "Run reverse lookup after adding the gateway login.",
                "Rename important devices in your router/DNS notes.",
                "Watch for unknown clients before blaming the cellular link.",
            ],
        },
        {
            "title": "Recovery discipline",
            "tone": "warn" if status.get("dry_run") else "good",
            "summary": "Keep changes reversible: backup configs, export snapshots, and avoid live reboot automation until recovery is proven.",
            "actions": [
                "Download a snapshot before lab changes.",
                "Keep Dry Run on during first setup.",
                "Power the gateway and router from a UPS if possible.",
            ],
        },
        {
            "title": "Event context",
            "tone": "info",
            "summary": f"{len(events)} recent event{'' if len(events) == 1 else 's'} in the local log.",
            "actions": [
                "Compare outages with weather, load, tower changes, and router logs.",
                "Keep snapshots with antenna placement notes.",
            ],
        },
    ]


def _adapter_guide(config: dict[str, Any]) -> dict[str, Any]:
    advanced = _dict(config.get("advanced_modem"))
    adapter_ready = bool(advanced.get("enabled") and advanced.get("control_url_configured"))
    return {
        "status": "ready" if adapter_ready else "setup_needed",
        "title": "Local adapter URL",
        "summary": (
            "Docker can use its built-in local adapter URL automatically. Real firmware backup, "
            "cell scan, tower lock, or radio-profile changes still need a hardware-specific bridge."
        ),
        "what_it_does": [
            "Defaults to the Docker app itself for adapter health checks and simple setup.",
            "Runs on an OpenWrt/ROOTer router, Raspberry Pi, mini PC, or Linux host on your LAN.",
            "Talks to an owned modem/gateway through supported local tools such as QMI, MBIM, AT, serial, or vendor tooling.",
            "Exposes narrow HTTP endpoints that TMHI Control Center can call for backups, scans, and profile intent.",
            "Keeps risky device-specific commands outside the main app until they can be tested and audited.",
        ],
        "how_to_get_one": [
            "Leave the field blank to use the built-in Docker adapter URL.",
            "For real modem commands, choose the device that will physically reach the modem or gateway lab hardware.",
            "Install or build a trusted adapter service on that local device.",
            "Bind it to the LAN only, confirm its health endpoint, then paste its base URL only if it is different from the Docker default.",
            "Create a stock backup before any firmware or radio-profile experiment.",
        ],
        "examples": list(ADAPTER_EXAMPLES),
        "required_endpoints": list(ADAPTER_REQUIRED_ENDPOINTS),
        "safety": [
            "Do not expose the adapter to the public internet.",
            "Do not paste random firmware URLs into the adapter.",
            "Do not continue without a stock backup, SHA-256 hashes, and a tested recovery path.",
            "Transmit-power override is intentionally unsupported.",
        ],
    }


def _tip(title: str, detail: str, tone: str) -> dict[str, str]:
    return {"title": title, "detail": detail, "tone": tone}


def _signal_score(overview: dict[str, Any]) -> int | None:
    score = _dict(overview.get("signal")).get("score")
    return _int_or_none(score)


def _metric_score(metric: dict[str, Any] | None) -> int | None:
    if not metric:
        return None
    return _int_or_none(metric.get("score"))


def _dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def _int(value: Any, default: int = 0) -> int:
    result = _int_or_none(value)
    return default if result is None else result


def _int_or_none(value: Any) -> int | None:
    if isinstance(value, bool) or value is None:
        return None
    if isinstance(value, int):
        return value
    if isinstance(value, float):
        return round(value)
    text = str(value).strip()
    if not text:
        return None
    try:
        return round(float(text.rstrip("%")))
    except ValueError:
        return None
