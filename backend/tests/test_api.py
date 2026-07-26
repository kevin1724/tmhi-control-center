import base64
import hashlib
import importlib
import sys
from datetime import datetime, timezone

from fastapi.testclient import TestClient


def load_main(monkeypatch, tmp_path):
    monkeypatch.setenv("WATCHDOG_ENABLED", "false")
    monkeypatch.setenv("DATABASE_PATH", str(tmp_path / "control-center.db"))
    monkeypatch.setenv("WATCHDOG_ENV_PATH", str(tmp_path / "control-center.env"))
    monkeypatch.setenv("FIRMWARE_BACKUP_DIR", str(tmp_path / "firmware-backups"))
    monkeypatch.delenv("GATEWAY_PASSWORD", raising=False)
    monkeypatch.delenv("GATEWAY_PASSWORD_FILE", raising=False)
    monkeypatch.delenv("OPENCELLID_API_KEY", raising=False)
    monkeypatch.delenv("OPENCELLID_API_KEY_FILE", raising=False)
    monkeypatch.setenv("PUBLIC_IP_LOCATION_ENABLED", "false")
    sys.modules.pop("tmhi_control_center.main", None)
    return importlib.import_module("tmhi_control_center.main")


def test_dashboard_is_served(monkeypatch, tmp_path) -> None:
    main = load_main(monkeypatch, tmp_path)

    with TestClient(main.app) as client:
        response = client.get("/")

    assert response.status_code == 200
    assert "Control Center" in response.text


def test_check_series_endpoint(monkeypatch, tmp_path) -> None:
    main = load_main(monkeypatch, tmp_path)

    class FakeWatchdog:
        async def stop(self) -> None:
            pass

        async def check_series(self, *, count: int, interval_seconds: float):
            return {
                "requested_count": count,
                "completed_count": count,
                "interval_seconds": interval_seconds,
                "results": [],
            }

    with TestClient(main.app) as client:
        main.watchdog = FakeWatchdog()
        response = client.post(
            "/api/check/series",
            json={"count": 2, "interval_seconds": 0},
        )

    assert response.status_code == 200
    assert response.json()["requested_count"] == 2


def test_gateway_overview_endpoint(monkeypatch, tmp_path) -> None:
    main = load_main(monkeypatch, tmp_path)

    class FakeGateway:
        async def overview(self):
            return {
                "observed_at": datetime.now(timezone.utc).isoformat(),
                "detection": {"reachable": True, "api_type": "unified"},
                "signal": {"score": 88, "quality": "Excellent", "metrics": []},
                "device": {"model": "TMOG4AR"},
                "connection": {"network_type": "5G"},
                "wifi": {},
                "sections": [],
            }

        async def close(self) -> None:
            pass

    with TestClient(main.app) as client:
        original_gateway = main.gateway
        main.gateway = FakeGateway()
        try:
            response = client.get("/api/gateway/overview")
            history_response = client.get("/api/gateway/telemetry/history?hours=6")
        finally:
            main.gateway = original_gateway

    assert response.status_code == 200
    assert response.json()["signal"]["score"] == 88
    assert response.json()["device"]["model"] == "TMOG4AR"
    assert history_response.status_code == 200
    assert history_response.json()["count"] == 1
    assert history_response.json()["points"][0]["signal_score"] == 88


def test_gateway_clients_endpoint(monkeypatch, tmp_path) -> None:
    main = load_main(monkeypatch, tmp_path)

    class FakeGateway:
        async def connected_devices(self, *, online_vendor_lookup: bool = False):
            return {
                "count": 1,
                "online_vendor_lookup": online_vendor_lookup,
                "devices": [
                    {
                        "hostname": "Kevin-iPad",
                        "mac_address": "AA:BB:CC:xx:xx:xx",
                        "identification": {"name": "Apple iPad"},
                    }
                ],
            }

        async def close(self) -> None:
            pass

    with TestClient(main.app) as client:
        original_gateway = main.gateway
        main.gateway = FakeGateway()
        try:
            response = client.get("/api/gateway/clients?online_lookup=true")
        finally:
            main.gateway = original_gateway

    assert response.status_code == 200
    assert response.json()["count"] == 1
    assert response.json()["online_vendor_lookup"] is True


def test_homelab_snapshot_includes_readiness_and_adapter_guide(
    monkeypatch,
    tmp_path,
) -> None:
    main = load_main(monkeypatch, tmp_path)

    class FakeWatchdog:
        async def stop(self) -> None:
            pass

        async def status_snapshot(self):
            return {
                "internet_online": True,
                "gateway_reachable": True,
                "dry_run": True,
                "watchdog_enabled": True,
            }

    class FakeGateway:
        async def overview(self):
            return {
                "observed_at": "2026-07-24T00:00:00+00:00",
                "detection": {"reachable": True, "api_type": "unified"},
                "signal": {
                    "score": 82,
                    "quality": "Good",
                    "metrics": [
                        {"key": "sinr", "label": "SINR", "score": 80, "value": "18 dB"},
                    ],
                },
                "device": {"model": "TMO-G4AR"},
                "connection": {
                    "network_type": "5G",
                    "band": "n41",
                    "cell_id": "1841925",
                },
                "wifi": {},
                "sections": [],
            }

        async def wifi_config(self):
            return {"ssid": "HomeLab", "radio_enabled": False}

        async def connected_devices(self, *, online_vendor_lookup: bool = False):
            return {
                "count": 1,
                "devices": [{"hostname": "nas", "ip_address": "192.168.12.20"}],
            }

        async def close(self) -> None:
            pass

    with TestClient(main.app) as client:
        original_gateway = main.gateway
        original_watchdog = main.watchdog
        main.gateway = FakeGateway()
        main.watchdog = FakeWatchdog()
        try:
            response = client.get("/api/homelab/snapshot")
        finally:
            main.gateway = original_gateway
            main.watchdog = original_watchdog

    assert response.status_code == 200
    payload = response.json()
    assert payload["insights"]["readiness"]["score"] > 50
    assert payload["insights"]["adapter_guide"]["examples"][0].startswith("http://")
    assert payload["config"]["gateway_password_configured"] is False
    assert payload["clients"]["count"] == 1


def test_gateway_map_endpoint_reports_tower_identity(monkeypatch, tmp_path) -> None:
    main = load_main(monkeypatch, tmp_path)

    class FakeGateway:
        async def overview(self):
            return {
                "observed_at": "2026-07-24T00:00:00+00:00",
                "detection": {"reachable": True, "api_type": "unified"},
                "signal": {"score": 78, "quality": "Good", "metrics": []},
                "device": {"model": "TMOG4AR"},
                "connection": {
                    "network_type": "5G",
                    "plmn": "310260",
                    "tac": "12345",
                    "cell_id": "987654",
                    "pci": 321,
                    "band": "n41",
                },
                "wifi": {},
                "sections": [],
            }

        async def close(self) -> None:
            pass

    with TestClient(main.app) as client:
        original_gateway = main.gateway
        main.gateway = FakeGateway()
        try:
            response = client.get("/api/gateway/map?include_nearby=false")
        finally:
            main.gateway = original_gateway

    assert response.status_code == 200
    payload = response.json()
    assert payload["map"]["library"] == "leaflet"
    assert payload["provider"]["configured"] is False
    assert payload["connected"]["identity"]["queryable"] is True
    assert payload["connected"]["identity"]["mcc"] == 310
    assert payload["connected"]["identity"]["mnc"] == 260
    assert payload["connected"]["identity"]["radio"] == "NR"
    assert payload["tower_lock"]["supported"] is False


def test_map_settings_update_saves_location_and_opencellid_key(
    monkeypatch,
    tmp_path,
) -> None:
    main = load_main(monkeypatch, tmp_path)
    env_path = tmp_path / "control-center.env"

    with TestClient(main.app) as client:
        response = client.post(
            "/api/map/settings",
            json={
                "latitude": 40.1,
                "longitude": -75.2,
                "radius_km": 0.75,
                "opencellid_api_key": "tower-key",
            },
        )
        saved_settings = env_path.read_text(encoding="utf-8")
        cleared = client.post(
            "/api/map/settings",
            json={"clear_opencellid_api_key": True},
        )

    assert response.status_code == 200
    payload = response.json()
    assert payload["map"]["latitude"] == 40.1
    assert payload["map"]["longitude"] == -75.2
    assert payload["map"]["radius_km"] == 0.75
    assert payload["map"]["opencellid_configured"] is True
    assert "MAP_LATITUDE=40.1\n" in saved_settings
    assert "MAP_LONGITUDE=-75.2\n" in saved_settings
    assert "MAP_RADIUS_KM=0.75\n" in saved_settings
    assert "OPENCELLID_API_KEY=tower-key\n" in saved_settings
    assert cleared.status_code == 200
    assert cleared.json()["map"]["opencellid_configured"] is False
    assert "OPENCELLID_API_KEY=\n" in env_path.read_text(encoding="utf-8")


def test_advanced_modem_lab_requires_acknowledgement_and_saves_settings(
    monkeypatch,
    tmp_path,
) -> None:
    main = load_main(monkeypatch, tmp_path)
    env_path = tmp_path / "control-center.env"

    with TestClient(main.app) as client:
        blocked = client.post(
            "/api/advanced-modem/settings",
            json={
                "mode": "openwrt_rooter",
                "control_url": "http://router.local:8080",
                "acknowledged": False,
            },
        )
        saved = client.post(
            "/api/advanced-modem/settings",
            json={
                "mode": "openwrt_rooter",
                "control_url": "http://router.local:8080",
                "acknowledged": True,
            },
        )
        saved_settings = env_path.read_text(encoding="utf-8")
        disabled = client.post(
            "/api/advanced-modem/settings",
            json={
                "mode": "disabled",
                "control_url": "",
                "acknowledged": False,
            },
        )

    assert blocked.status_code == 409
    assert saved.status_code == 200
    payload = saved.json()["advanced_modem"]
    assert payload["mode"] == "openwrt_rooter"
    assert payload["enabled"] is True
    assert payload["control_url_configured"] is True
    assert payload["capabilities"]["cell_lock"]["supported"] is True
    assert payload["capabilities"]["tx_power_override"]["supported"] is False
    assert "ADVANCED_MODEM_MODE=openwrt_rooter\n" in saved_settings
    assert "ADVANCED_MODEM_CONTROL_URL=http://router.local:8080\n" in saved_settings
    assert "ADVANCED_MODEM_ACKNOWLEDGED=true\n" in saved_settings
    assert disabled.json()["advanced_modem"]["enabled"] is False


def test_advanced_modem_lab_uses_docker_adapter_default(
    monkeypatch,
    tmp_path,
) -> None:
    main = load_main(monkeypatch, tmp_path)
    env_path = tmp_path / "control-center.env"

    with TestClient(main.app) as client:
        health = client.get("/health")
        saved = client.post(
            "/api/advanced-modem/settings",
            json={
                "mode": "g4ar_unlock_lab",
                "control_url": "",
                "acknowledged": True,
                "skip_stock_backup": True,
            },
        )

    assert health.status_code == 200
    assert health.json()["adapter"] == "tmhi-control-center-docker"
    assert health.json()["url"] == "http://127.0.0.1:8000"
    assert saved.status_code == 200
    lab = saved.json()["advanced_modem"]
    assert lab["control_url"] == "http://127.0.0.1:8000"
    assert lab["effective_control_url"] == "http://127.0.0.1:8000"
    assert lab["built_in_adapter_selected"] is True
    assert lab["skip_stock_backup"] is True
    assert lab["capabilities"]["stock_firmware_backup"]["status"] == (
        "hardware_bridge_required"
    )
    assert lab["g4ar_unlock_lab"]["adapter_ready"] is False
    assert lab["g4ar_unlock_lab"]["stock_backup_skipped"] is True
    saved_settings = env_path.read_text(encoding="utf-8")
    assert "ADVANCED_MODEM_CONTROL_URL=http://127.0.0.1:8000\n" in saved_settings
    assert "ADVANCED_SKIP_STOCK_BACKUP=true\n" in saved_settings


def test_g4ar_firmware_lab_flash_gate_requires_full_consent(
    monkeypatch,
    tmp_path,
) -> None:
    main = load_main(monkeypatch, tmp_path)

    valid_hash = "a" * 64
    firmware_hash = "b" * 64
    with TestClient(main.app) as client:
        saved = client.post(
            "/api/advanced-modem/settings",
            json={
                "mode": "g4ar_unlock_lab",
                "control_url": "http://127.0.0.1:8765",
                "acknowledged": True,
                "upload_profile": "prefer_upload",
                "radio_profile": "prefer_lte_anchor_nsa",
            },
        )
        status = client.get("/api/g4ar/firmware/status")
        blocked = client.post(
            "/api/g4ar/firmware/flash",
            json={
                "stock_backup_sha256": valid_hash,
                "firmware_sha256": firmware_hash,
                "consent_phrase": "wrong phrase",
                "backup_verified": True,
                "recovery_verified": True,
                "understands_brick_risk": True,
            },
        )
        fully_consented = client.post(
            "/api/g4ar/firmware/flash",
            json={
                "stock_backup_sha256": valid_hash,
                "firmware_sha256": firmware_hash,
                "consent_phrase": "I OWN THIS G4AR - BACKUP VERIFIED - OVERRIDE RISK ACCEPTED",
                "backup_verified": True,
                "recovery_verified": True,
                "understands_brick_risk": True,
            },
        )

    assert saved.status_code == 200
    lab = saved.json()["advanced_modem"]
    assert lab["mode"] == "g4ar_unlock_lab"
    assert lab["upload_priority"]["profile"] == "prefer_upload"
    assert lab["g4ar_radio"]["profile"] == "prefer_lte_anchor_nsa"
    assert lab["capabilities"]["custom_firmware_flash"]["status"] == (
        "consent_and_recovery_required"
    )
    assert status.status_code == 200
    assert status.json()["consent_phrase"] == (
        "I OWN THIS G4AR - BACKUP VERIFIED - OVERRIDE RISK ACCEPTED"
    )
    assert blocked.status_code == 409
    assert "exact consent phrase" in blocked.json()["detail"]
    assert fully_consented.status_code == 501
    assert "not implemented" in fully_consented.json()["detail"]


def test_g4ar_firmware_backup_requires_unlock_lab(
    monkeypatch,
    tmp_path,
) -> None:
    main = load_main(monkeypatch, tmp_path)

    with TestClient(main.app) as client:
        blocked = client.post(
            "/api/g4ar/firmware/backup",
            json={"reason": "test"},
        )

    assert blocked.status_code == 409
    assert "Select G4AR unlock" in blocked.json()["detail"]


def test_g4ar_firmware_backup_saves_adapter_artifacts(
    monkeypatch,
    tmp_path,
) -> None:
    main = load_main(monkeypatch, tmp_path)
    main.settings.firmware_backup_dir = str(tmp_path / "firmware-backups")
    backup_bytes = b"stock firmware image"
    backup_hash = hashlib.sha256(backup_bytes).hexdigest()
    calls: list[dict[str, object]] = []

    class FakeAdapterClient:
        def __init__(self, *args, **kwargs) -> None:
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, tb) -> None:
            return None

        async def post(self, url, json):
            calls.append({"url": url, "json": json})

            class Response:
                is_success = True
                reason_phrase = "OK"
                text = ""

                def json(self):
                    return {
                        "device": "Arcadyan TMO-G4AR",
                        "firmware_version": "1.00.12",
                        "hardware_revision": "lab-unit",
                        "artifacts": [
                            {
                                "name": "../stock-firmware.bin",
                                "content_base64": base64.b64encode(backup_bytes).decode(
                                    "ascii"
                                ),
                                "sha256": backup_hash,
                            }
                        ],
                    }

            return Response()

    monkeypatch.setattr(
        "tmhi_control_center.firmware_backup.httpx.AsyncClient",
        FakeAdapterClient,
    )

    with TestClient(main.app) as client:
        saved_settings = client.post(
            "/api/advanced-modem/settings",
            json={
                "mode": "g4ar_unlock_lab",
                "control_url": "http://127.0.0.1:8765",
                "acknowledged": True,
            },
        )
        created = client.post(
            "/api/g4ar/firmware/backup",
            json={"reason": "test"},
        )
        listed = client.get("/api/g4ar/firmware/backups")

    assert saved_settings.status_code == 200
    assert created.status_code == 200
    manifest = created.json()
    assert manifest["firmware_version"] == "1.00.12"
    assert manifest["artifact_count"] == 1
    assert manifest["artifacts"][0]["name"] == "stock-firmware.bin"
    assert manifest["artifacts"][0]["sha256"] == backup_hash
    assert calls[0]["url"] == "http://127.0.0.1:8765/g4ar/firmware/backup"
    assert calls[0]["json"]["reason"] == "test"

    artifact_path = tmp_path / "firmware-backups" / manifest["id"] / "stock-firmware.bin"
    assert artifact_path.read_bytes() == backup_bytes
    assert listed.status_code == 200
    assert listed.json()["count"] == 1
    assert listed.json()["backups"][0]["id"] == manifest["id"]


def test_gateway_map_limits_opencellid_bbox_radius(monkeypatch, tmp_path) -> None:
    main = load_main(monkeypatch, tmp_path)
    main.settings.opencellid_api_key = "tower-key"
    main.settings.opencellid_api_key_source = "saved"
    main.settings.map_latitude = 40.0
    main.settings.map_longitude = -75.0
    main.settings.map_radius_km = 8.0

    class FakeGateway:
        async def overview(self):
            return {
                "connection": {
                    "network_type": "5G",
                    "plmn": "310260",
                    "tac": "12345",
                    "cell_id": "987654",
                    "band": "n41",
                },
                "signal": {},
                "sections": [],
            }

        async def close(self) -> None:
            pass

    calls: list[dict[str, str]] = []

    class FakeOpenCellClient:
        def __init__(self, *args, **kwargs) -> None:
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, tb) -> None:
            return None

        async def get(self, url, params):
            calls.append({key: str(value) for key, value in params.items()})

            class Response:
                is_success = True
                status_code = 200

                def json(self):
                    if url.endswith("/cell/getInArea"):
                        return {"count": 0, "cells": []}
                    return {}

            return Response()

    monkeypatch.setattr("tmhi_control_center.towers.httpx.AsyncClient", FakeOpenCellClient)

    with TestClient(main.app) as client:
        original_gateway = main.gateway
        main.gateway = FakeGateway()
        try:
            response = client.get("/api/gateway/map?include_nearby=true")
        finally:
            main.gateway = original_gateway

    assert response.status_code == 200
    payload = response.json()
    assert payload["provider"]["requested_radius_km"] == 8.0
    assert payload["provider"]["search_limited"] is True
    assert payload["provider"]["search_radius_km"] == 0.85
    assert "limited" in payload["notices"][0].lower()
    area_call = next(call for call in calls if "BBOX" in call)
    lat_min, lon_min, lat_max, lon_max = [
        float(part) for part in area_call["BBOX"].split(",")
    ]
    assert lat_max - lat_min < 0.02
    assert lon_max - lon_min < 0.03


def test_gateway_map_estimates_connected_tower_from_nearby_cells(
    monkeypatch,
    tmp_path,
) -> None:
    main = load_main(monkeypatch, tmp_path)
    main.settings.opencellid_api_key = "tower-key"
    main.settings.opencellid_api_key_source = "saved"
    main.settings.map_latitude = 45.52345
    main.settings.map_longitude = -122.67621
    main.settings.map_radius_km = 0.8

    class FakeGateway:
        async def overview(self):
            return {
                "connection": {
                    "network_type": "registered",
                    "band": "n41",
                    "cell_id": "1841925",
                },
                "signal": {},
                "sections": [],
            }

        async def close(self) -> None:
            pass

    class FakeOpenCellClient:
        def __init__(self, *args, **kwargs) -> None:
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, tb) -> None:
            return None

        async def get(self, url, params):
            class Response:
                is_success = True
                status_code = 200

                def json(self):
                    assert url.endswith("/cell/getInArea")
                    return {
                        "count": 1,
                        "cells": [
                            {
                                "mcc": 310,
                                "mnc": 260,
                                "lac": 12345,
                                "cellid": 7654711319,
                                "radio": "NR",
                                "lat": 45.5301,
                                "lon": -122.6701,
                                "range": 1000,
                                "samples": 1,
                            }
                        ],
                    }

            return Response()

    monkeypatch.setattr("tmhi_control_center.towers.httpx.AsyncClient", FakeOpenCellClient)

    with TestClient(main.app) as client:
        original_gateway = main.gateway
        main.gateway = FakeGateway()
        try:
            response = client.get("/api/gateway/map?include_nearby=true")
        finally:
            main.gateway = original_gateway

    assert response.status_code == 200
    payload = response.json()
    connected_location = payload["connected"]["location"]
    assert connected_location["cell_id"] == 7654711319
    assert connected_location["connected"] is True
    assert connected_location["match_type"] == "single_radio_candidate"
    assert connected_location["match_confidence"] == "estimated"
    assert payload["nearby"][0]["connected"] is True
    assert "estimated" in payload["notices"][0].lower()


def test_gateway_map_uses_public_ip_location_when_no_saved_center(
    monkeypatch,
    tmp_path,
) -> None:
    main = load_main(monkeypatch, tmp_path)
    main.settings.public_ip_location_enabled = True

    class FakeGateway:
        async def overview(self):
            return {
                "connection": {
                    "network_type": "5G",
                    "band": "n41",
                    "cell_id": "1841925",
                },
                "signal": {},
                "sections": [],
            }

        async def close(self) -> None:
            pass

    class FakeLocation:
        def to_dict(self):
            return {
                "latitude": 40.123456,
                "longitude": -75.654321,
                "provider": "test-ip",
                "city": "Test City",
                "region": "PA",
                "country": "United States",
                "accuracy": "public_ip_city_estimate",
            }

    class FakeLocator:
        async def locate(self):
            return FakeLocation()

    with TestClient(main.app) as client:
        original_gateway = main.gateway
        original_locator = main.public_ip_locator
        main.gateway = FakeGateway()
        main.public_ip_locator = FakeLocator()
        try:
            response = client.get("/api/gateway/map?include_nearby=false")
        finally:
            main.gateway = original_gateway
            main.public_ip_locator = original_locator

    assert response.status_code == 200
    payload = response.json()
    assert payload["map"]["center"] == {
        "latitude": 40.123456,
        "longitude": -75.654321,
        "source": "public_ip",
    }
    assert payload["location"]["center_source"] == "public_ip"
    assert payload["location"]["auto_detected"]["provider"] == "test-ip"


def test_gateway_wifi_update_endpoint_records_event(monkeypatch, tmp_path) -> None:
    main = load_main(monkeypatch, tmp_path)

    class FakeGateway:
        async def update_wifi(self, *, ssid=None, radio_enabled=None):
            return {
                "accepted": True,
                "source": "/network/configuration/v2?set=ap",
                "changed": {
                    "ssid_fields": 2 if ssid else 0,
                    "radio_enabled": radio_enabled,
                },
                "wifi": {"ssid": ssid, "radio_enabled": radio_enabled},
            }

        async def close(self) -> None:
            pass

    with TestClient(main.app) as client:
        original_gateway = main.gateway
        main.gateway = FakeGateway()
        try:
            response = client.post(
                "/api/gateway/wifi",
                json={"ssid": "NewNet", "radio_enabled": False},
            )
            events = client.get("/api/events")
        finally:
            main.gateway = original_gateway

    assert response.status_code == 200
    assert response.json()["wifi"]["ssid"] == "NewNet"
    assert events.status_code == 200
    assert events.json()[0]["kind"] == "wifi_settings_updated"


def test_gateway_wifi_endpoint(monkeypatch, tmp_path) -> None:
    main = load_main(monkeypatch, tmp_path)

    class FakeGateway:
        async def wifi_config(self):
            return {
                "ssid": "KevinNet",
                "radio_enabled": True,
                "radios": [{"band": "5 GHz", "ssid": "KevinNet"}],
            }

        async def close(self) -> None:
            pass

    with TestClient(main.app) as client:
        original_gateway = main.gateway
        main.gateway = FakeGateway()
        try:
            response = client.get("/api/gateway/wifi")
        finally:
            main.gateway = original_gateway

    assert response.status_code == 200
    assert response.json()["ssid"] == "KevinNet"


def test_gateway_test_accepts_supplied_password(monkeypatch, tmp_path) -> None:
    main = load_main(monkeypatch, tmp_path)
    passwords: list[str] = []

    class FakeGatewayClient:
        def __init__(
            self,
            _base_url: str,
            _username: str,
            password: str,
            _timeout_seconds: float,
            _user_agent: str,
        ) -> None:
            passwords.append(password)

        async def is_reachable(self) -> bool:
            return True

        async def authenticate(self) -> str:
            return "token"

        async def close(self) -> None:
            pass

    monkeypatch.setattr(main, "UnifiedGatewayClient", FakeGatewayClient)

    with TestClient(main.app) as client:
        response = client.post(
            "/api/gateway/test",
            json={"gateway_password": "entered-password"},
        )

    assert response.status_code == 200
    assert response.json() == {
        "reachable": True,
        "authenticated": True,
        "used_supplied_password": True,
    }
    assert passwords == ["entered-password"]


def test_gateway_login_saves_authenticated_password(monkeypatch, tmp_path) -> None:
    main = load_main(monkeypatch, tmp_path)
    env_path = tmp_path / "control-center.env"
    passwords: list[str] = []

    class FakeGatewayClient:
        def __init__(
            self,
            _base_url: str,
            _username: str,
            password: str,
            _timeout_seconds: float,
            _user_agent: str,
        ) -> None:
            self.password = password
            passwords.append(password)

        async def is_reachable(self) -> bool:
            return True

        async def authenticate(self) -> str:
            return "token"

        async def close(self) -> None:
            pass

    monkeypatch.setattr(main, "UnifiedGatewayClient", FakeGatewayClient)

    with TestClient(main.app) as client:
        response = client.post(
            "/api/gateway/login",
            json={"gateway_password": "entered-password", "remember": True},
        )
        config = client.get("/api/config")

    assert response.status_code == 200
    assert response.json() == {
        "reachable": True,
        "authenticated": True,
        "saved": True,
        "gateway_password_configured": True,
        "gateway_password_source": "saved",
    }
    assert passwords == ["entered-password"]
    assert 'GATEWAY_PASSWORD=entered-password' in env_path.read_text(
        encoding="utf-8"
    )
    assert main.settings.gateway_password == "entered-password"
    assert main.settings.gateway_password_source == "saved"
    assert main.gateway._password == "entered-password"
    assert config.json()["gateway_password_configured"] is True
    assert config.json()["gateway_password_source"] == "saved"


def test_gateway_login_clear_removes_saved_password(monkeypatch, tmp_path) -> None:
    env_path = tmp_path / "control-center.env"
    env_path.write_text("GATEWAY_PASSWORD=saved-password\n", encoding="utf-8")
    main = load_main(monkeypatch, tmp_path)

    with TestClient(main.app) as client:
        response = client.delete("/api/gateway/login")

    assert response.status_code == 200
    assert response.json()["gateway_password_configured"] is False
    assert response.json()["gateway_password_source"] == "none"
    assert "GATEWAY_PASSWORD=\n" in env_path.read_text(encoding="utf-8")
    assert main.settings.gateway_password == ""
    assert main.gateway._password == ""


def test_settings_update_saves_dry_run_and_frequency(monkeypatch, tmp_path) -> None:
    main = load_main(monkeypatch, tmp_path)
    env_path = tmp_path / "control-center.env"
    main.settings.gateway_password = "saved-password"

    with TestClient(main.app) as client:
        response = client.post(
            "/api/settings",
            json={"dry_run": False, "tests_per_hour": 120},
        )

    assert response.status_code == 200
    payload = response.json()
    assert payload["dry_run"] is False
    assert payload["tests_per_hour"] == 120
    assert payload["check_interval_seconds"] == 30
    assert main.settings.dry_run is False
    assert main.settings.check_interval_seconds == 30
    saved_settings = env_path.read_text(encoding="utf-8")
    assert "DRY_RUN=false\n" in saved_settings
    assert "CHECK_INTERVAL_SECONDS=30\n" in saved_settings


def test_settings_update_requires_password_before_live_reboots(
    monkeypatch,
    tmp_path,
) -> None:
    main = load_main(monkeypatch, tmp_path)

    with TestClient(main.app) as client:
        response = client.post("/api/settings", json={"dry_run": False})

    assert response.status_code == 409
    assert main.settings.dry_run is True


def test_events_endpoint_returns_at_most_ten_events(monkeypatch, tmp_path) -> None:
    main = load_main(monkeypatch, tmp_path)

    with TestClient(main.app) as client:
        for index in range(12):
            client.post("/api/settings", json={"dry_run": True, "tests_per_hour": index + 1})
        response = client.get("/api/events?limit=500")

    assert response.status_code == 200
    assert len(response.json()) == 10
