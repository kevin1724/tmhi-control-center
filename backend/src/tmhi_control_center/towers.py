from __future__ import annotations

import math
import re
from dataclasses import dataclass
from typing import Any

import httpx

from .models import utc_now


DEFAULT_MAP_CENTER = {"latitude": 39.8283, "longitude": -98.5795, "source": "default_us"}
OPENCELLID_BASE_URL = "https://opencellid.org"
OPENSTREETMAP_TILE_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
OPENCELLID_MAX_BBOX_AREA_SQ_M = 4_000_000
OPENCELLID_SAFE_RADIUS_KM = 0.85


class TowerLookupError(RuntimeError):
    """Cell tower provider returned an unusable response."""


@dataclass(frozen=True, slots=True)
class MapCenter:
    latitude: float
    longitude: float
    source: str

    def to_dict(self) -> dict[str, Any]:
        return {
            "latitude": self.latitude,
            "longitude": self.longitude,
            "source": self.source,
        }


async def build_tower_map_payload(
    overview: dict[str, Any] | None,
    *,
    settings: Any,
    latitude: float | None = None,
    longitude: float | None = None,
    radius_km: float | None = None,
    public_ip_location: dict[str, Any] | None = None,
    include_nearby: bool = False,
) -> dict[str, Any]:
    overview = overview or {}
    lookup_radius_km = radius_km or settings.map_radius_km
    provider_radius_km = min(lookup_radius_km, OPENCELLID_SAFE_RADIUS_KM)
    provider_limited = provider_radius_km < lookup_radius_km
    center = _map_center(
        overview,
        latitude=latitude,
        longitude=longitude,
        settings=settings,
        public_ip_location=public_ip_location,
    )
    identity = tower_identity_from_overview(overview)
    signal = overview.get("signal") if isinstance(overview.get("signal"), dict) else {}
    connection = (
        overview.get("connection") if isinstance(overview.get("connection"), dict) else {}
    )

    errors: list[str] = []
    notices: list[str] = []
    connected_location: dict[str, Any] | None = None
    nearby: list[dict[str, Any]] = []
    provider_configured = bool(settings.opencellid_api_key)

    if include_nearby and provider_limited:
        notices.append(
            f"OpenCellID search was limited to {provider_radius_km} km because its "
            "area endpoint rejects larger bounding boxes."
        )

    if include_nearby and provider_configured:
        async with httpx.AsyncClient(
            timeout=httpx.Timeout(12.0),
            follow_redirects=True,
            trust_env=False,
            headers={
                "Accept": "application/json",
                "User-Agent": "tmhi-control-center/0.1 (+https://github.com/kevin1724/tmhi-control-center)",
            },
        ) as client:
            if identity.get("queryable"):
                try:
                    connected_location = await _opencellid_connected_tower(
                        client,
                        settings.opencellid_api_key,
                        identity,
                        center,
                    )
                except TowerLookupError as exc:
                    errors.append(str(exc))

            try:
                nearby = await _opencellid_nearby_towers(
                    client,
                    settings.opencellid_api_key,
                    center,
                    provider_radius_km,
                    identity,
                )
            except TowerLookupError as exc:
                errors.append(str(exc))

    if connected_location is None and nearby:
        connected_location = _connected_tower_from_nearby(nearby, identity)
        if connected_location:
            notices.append(
                "Connected tower location was estimated from nearby OpenCellID results "
                "because the gateway did not expose a complete tower lookup identity."
            )
    elif include_nearby and not provider_configured:
        errors.append("OpenCellID API key is not configured")

    return {
        "observed_at": utc_now().isoformat(),
        "map": {
            "library": "leaflet",
            "tile_provider": "openstreetmap",
            "tile_url": OPENSTREETMAP_TILE_URL,
            "attribution": "OpenStreetMap contributors",
            "center": center.to_dict(),
            "radius_km": lookup_radius_km,
        },
        "provider": {
            "name": "OpenCellID",
            "configured": provider_configured,
            "api_key_source": settings.opencellid_api_key_source,
            "nearby_loaded": include_nearby and provider_configured and not errors,
            "requested_radius_km": lookup_radius_km,
            "search_radius_km": provider_radius_km,
            "search_limited": provider_limited,
            "max_bbox_area_sq_m": OPENCELLID_MAX_BBOX_AREA_SQ_M,
        },
        "home": _home_location(settings),
        "location": {
            "auto_detected": public_ip_location,
            "center_source": center.source,
        },
        "connected": {
            "identity": identity,
            "location": connected_location,
            "signal": signal,
            "connection": connection,
        },
        "nearby": nearby,
        "tower_lock": {
            "supported": False,
            "mode": "not_supported_on_stock_gateway",
            "message": (
                "Stock TMHI gateway APIs do not expose a safe tower-selection command. "
                "Use this map to compare likely towers, then retest after changing gateway "
                "placement or antenna direction."
            ),
            "safe_actions": [
                "Compare serving cell ID, PCI, band, and signal before and after moving the gateway.",
                "Use the strongest SINR/RSRP trend rather than distance alone.",
                "Retest connected clients and internet checks after each placement change.",
            ],
        },
        "errors": errors,
        "notices": notices,
    }


def tower_identity_from_overview(overview: dict[str, Any]) -> dict[str, Any]:
    plmn = _clean_text(_overview_value(overview, ("plmn", "plmnid", "operatorcode")))
    mcc = _int_or_none(_overview_value(overview, ("mcc", "mobilecountrycode")))
    mnc = _int_or_none(_overview_value(overview, ("mnc", "mobilenetworkcode")))
    if plmn and (mcc is None or mnc is None):
        plmn_parts = _plmn_parts(plmn)
        if plmn_parts:
            mcc = mcc or plmn_parts[0]
            mnc = mnc or plmn_parts[1]

    lac = _int_or_none(
        _overview_value(
            overview,
            (
                "tac",
                "trackingareacode",
                "trackingarea",
                "lac",
                "localareacode",
            ),
        )
    )
    cell_id = _int_or_none(
        _overview_value(
            overview,
            (
                "nci",
                "nrcellid",
                "nr_cell_id",
                "ecgi",
                "cgi",
                "cellid",
                "cell_id",
                "enbid",
                "gnbid",
            ),
        )
    )
    pci = _int_or_none(
        _overview_value(overview, ("pci", "physicalcellid", "physical_cell_id"))
    )
    band = _clean_text(
        _overview_value(overview, ("band", "primaryband", "nrband", "lteband"))
    )
    network_type = _clean_text(
        _overview_value(overview, ("networktype", "network_type", "rat"))
    )
    operator = _clean_text(
        _overview_value(overview, ("operator", "carrier", "plmnname", "plmn_name"))
    )
    radio = _radio_from_values(network_type, band)

    missing = [
        label
        for label, value in (
            ("MCC", mcc),
            ("MNC", mnc),
            ("TAC/LAC", lac),
            ("Cell ID", cell_id),
        )
        if value is None
    ]

    return {
        "mcc": mcc,
        "mnc": mnc,
        "lac": lac,
        "cell_id": cell_id,
        "pci": pci,
        "band": band,
        "radio": radio,
        "network_type": network_type,
        "operator": operator,
        "plmn": plmn,
        "queryable": not missing,
        "missing": missing,
    }


def _map_center(
    overview: dict[str, Any],
    *,
    latitude: float | None,
    longitude: float | None,
    settings: Any,
    public_ip_location: dict[str, Any] | None,
) -> MapCenter:
    if latitude is not None and longitude is not None:
        return MapCenter(latitude=latitude, longitude=longitude, source="request")
    if settings.map_latitude is not None and settings.map_longitude is not None:
        return MapCenter(
            latitude=settings.map_latitude,
            longitude=settings.map_longitude,
            source="saved_home",
        )

    gateway_latitude = _float_or_none(
        _overview_value(overview, ("latitude", "lat", "gpslatitude", "gps_latitude"))
    )
    gateway_longitude = _float_or_none(
        _overview_value(overview, ("longitude", "lon", "lng", "gpslongitude", "gps_longitude"))
    )
    if gateway_latitude is not None and gateway_longitude is not None:
        return MapCenter(
            latitude=gateway_latitude,
            longitude=gateway_longitude,
            source="gateway_telemetry",
        )

    if public_ip_location:
        public_ip_latitude = _float_or_none(public_ip_location.get("latitude"))
        public_ip_longitude = _float_or_none(public_ip_location.get("longitude"))
        if public_ip_latitude is not None and public_ip_longitude is not None:
            return MapCenter(
                latitude=public_ip_latitude,
                longitude=public_ip_longitude,
                source="public_ip",
            )

    return MapCenter(
        latitude=DEFAULT_MAP_CENTER["latitude"],
        longitude=DEFAULT_MAP_CENTER["longitude"],
        source=DEFAULT_MAP_CENTER["source"],
    )


def _home_location(settings: Any) -> dict[str, Any] | None:
    if settings.map_latitude is None or settings.map_longitude is None:
        return None
    return {
        "latitude": settings.map_latitude,
        "longitude": settings.map_longitude,
        "source": "saved_home",
    }


async def _opencellid_connected_tower(
    client: httpx.AsyncClient,
    api_key: str,
    identity: dict[str, Any],
    center: MapCenter,
) -> dict[str, Any] | None:
    params: dict[str, Any] = {
        "key": api_key,
        "mcc": identity["mcc"],
        "mnc": identity["mnc"],
        "lac": identity["lac"],
        "cellid": identity["cell_id"],
        "format": "json",
    }
    if identity.get("radio"):
        params["radio"] = identity["radio"]

    payload = await _opencellid_get(client, "/cell/get", params)
    if "lat" not in payload or "lon" not in payload:
        return None
    return _tower_from_opencellid_cell(payload, center=center, connected=True)


async def _opencellid_nearby_towers(
    client: httpx.AsyncClient,
    api_key: str,
    center: MapCenter,
    radius_km: float,
    identity: dict[str, Any],
) -> list[dict[str, Any]]:
    bbox = _bbox(center.latitude, center.longitude, radius_km)
    params: dict[str, Any] = {
        "key": api_key,
        "BBOX": ",".join(_format_coord(value) for value in bbox),
        "limit": 50,
        "format": "json",
    }
    if identity.get("mcc") is not None:
        params["mcc"] = identity["mcc"]
    if identity.get("mnc") is not None:
        params["mnc"] = identity["mnc"]
    if identity.get("radio"):
        params["radio"] = identity["radio"]

    payload = await _opencellid_get(client, "/cell/getInArea", params)
    cells = payload.get("cells")
    if not isinstance(cells, list):
        return []

    towers = [
        _tower_from_opencellid_cell(cell, center=center, connected=False)
        for cell in cells
        if isinstance(cell, dict)
    ]
    return [tower for tower in towers if tower is not None]


async def _opencellid_get(
    client: httpx.AsyncClient,
    path: str,
    params: dict[str, Any],
) -> dict[str, Any]:
    try:
        response = await client.get(f"{OPENCELLID_BASE_URL}{path}", params=params)
    except (httpx.HTTPError, OSError) as exc:
        raise TowerLookupError(
            f"OpenCellID lookup failed: {type(exc).__name__}: {exc}"
        ) from exc

    if not response.is_success:
        raise TowerLookupError(
            f"OpenCellID returned HTTP {response.status_code}"
        )

    try:
        payload = response.json()
    except ValueError as exc:
        raise TowerLookupError("OpenCellID returned invalid JSON") from exc

    if not isinstance(payload, dict):
        raise TowerLookupError("OpenCellID returned a non-object response")
    if "error" in payload:
        if _is_no_cells_found(payload.get("error")):
            return {}
        raise TowerLookupError(f"OpenCellID error: {payload.get('error')}")
    if "err" in payload and isinstance(payload["err"], dict):
        message = payload["err"].get("info") or payload["err"].get("code")
        if _is_no_cells_found(message):
            return {}
        raise TowerLookupError(f"OpenCellID error: {message}")
    return payload


def _tower_from_opencellid_cell(
    cell: dict[str, Any],
    *,
    center: MapCenter,
    connected: bool,
) -> dict[str, Any] | None:
    latitude = _float_or_none(cell.get("lat"))
    longitude = _float_or_none(cell.get("lon"))
    if latitude is None or longitude is None:
        return None

    tower = {
        "id": _tower_id(cell),
        "connected": connected,
        "latitude": latitude,
        "longitude": longitude,
        "distance_km": round(
            _distance_km(center.latitude, center.longitude, latitude, longitude),
            2,
        ),
        "mcc": _int_or_none(cell.get("mcc")),
        "mnc": _int_or_none(cell.get("mnc")),
        "lac": _int_or_none(cell.get("lac") or cell.get("tac")),
        "cell_id": _int_or_none(cell.get("cellid") or cell.get("cid") or cell.get("bid")),
        "radio": _clean_text(cell.get("radio")),
        "average_signal": _int_or_none(cell.get("averageSignalStrength")),
        "range_m": _int_or_none(cell.get("range")),
        "samples": _int_or_none(cell.get("samples")),
        "changeable": _bool_or_none(cell.get("changeable")),
        "source": "opencellid",
    }
    tower["label"] = _tower_label(tower)
    return tower


def _connected_tower_from_nearby(
    nearby: list[dict[str, Any]],
    identity: dict[str, Any],
) -> dict[str, Any] | None:
    candidates = [
        tower
        for tower in nearby
        if _float_or_none(tower.get("latitude")) is not None
        and _float_or_none(tower.get("longitude")) is not None
    ]
    if not candidates:
        return None

    identity_radio = _normalize_radio(identity.get("radio"))
    identity_cell_id = _int_or_none(identity.get("cell_id"))

    scored: list[tuple[int, dict[str, Any], str]] = []
    for tower in candidates:
        score = 0
        reasons: list[str] = []
        tower_radio = _normalize_radio(tower.get("radio"))
        tower_cell_id = _int_or_none(tower.get("cell_id"))

        if identity_radio and tower_radio == identity_radio:
            score += 35
            reasons.append("same radio")
        elif identity_radio and tower_radio and tower_radio != identity_radio:
            score -= 25

        if identity_cell_id is not None and tower_cell_id is not None:
            if identity_cell_id == tower_cell_id:
                score += 100
                reasons.append("same cell id")
            elif _is_related_cell_id(identity_cell_id, tower_cell_id, tower_radio):
                score += 65
                reasons.append("related cell id")

        for key in ("mcc", "mnc", "lac"):
            identity_value = _int_or_none(identity.get(key))
            tower_value = _int_or_none(tower.get(key))
            if identity_value is None or tower_value is None:
                continue
            if identity_value == tower_value:
                score += 12
                reasons.append(f"same {key}")
            else:
                score -= 20

        scored.append((score, tower, ", ".join(reasons) or "nearest available cell"))

    scored.sort(key=lambda item: (item[0], -float(item[1].get("distance_km") or 9999)), reverse=True)
    best_score, best_tower, reason = scored[0]

    same_radio_candidates = [
        tower
        for tower in candidates
        if not identity_radio or _normalize_radio(tower.get("radio")) == identity_radio
    ]
    if identity_cell_id is not None and best_score >= 65:
        match_type = "cell_id_match"
        confidence = "high" if best_score >= 100 else "medium"
    elif len(same_radio_candidates) == 1 and best_score >= 20:
        best_tower = same_radio_candidates[0]
        match_type = "single_radio_candidate"
        confidence = "estimated"
        reason = "only nearby cell matching the serving radio"
    elif len(candidates) == 1 and best_score >= 0:
        best_tower = candidates[0]
        match_type = "single_nearby_candidate"
        confidence = "estimated"
        reason = "only nearby cell returned by OpenCellID"
    else:
        return None

    connected = dict(best_tower)
    connected["connected"] = True
    connected["match_type"] = match_type
    connected["match_confidence"] = confidence
    connected["match_note"] = reason

    for tower in nearby:
        if tower.get("id") == best_tower.get("id"):
            tower["connected"] = True
            tower["match_type"] = match_type
            tower["match_confidence"] = confidence
            tower["match_note"] = reason
            break

    return connected


def _normalize_radio(value: Any) -> str | None:
    text = _clean_text(value)
    return text.upper() if text else None


def _is_related_cell_id(
    identity_cell_id: int,
    provider_cell_id: int,
    radio: str | None,
) -> bool:
    if identity_cell_id <= 0 or provider_cell_id <= 0:
        return False
    if identity_cell_id == provider_cell_id:
        return True

    if radio == "LTE":
        return provider_cell_id >> 8 == identity_cell_id
    if radio == "NR":
        return any(provider_cell_id >> shift == identity_cell_id for shift in (8, 10, 12, 14, 16))
    return False


def _tower_id(cell: dict[str, Any]) -> str:
    parts = [
        cell.get("mcc"),
        cell.get("mnc"),
        cell.get("lac") or cell.get("tac"),
        cell.get("cellid") or cell.get("cid") or cell.get("bid"),
        cell.get("radio"),
    ]
    return "-".join(str(part) for part in parts if part not in {None, ""}) or "tower"


def _tower_label(tower: dict[str, Any]) -> str:
    radio = tower.get("radio") or "Cell"
    cell_id = tower.get("cell_id") or "unknown"
    return f"{radio} cell {cell_id}"


def _bbox(latitude: float, longitude: float, radius_km: float) -> tuple[float, float, float, float]:
    lat_delta = radius_km / 110.574
    lon_scale = max(0.1, math.cos(math.radians(latitude)))
    lon_delta = radius_km / (111.320 * lon_scale)
    return (
        max(-90.0, latitude - lat_delta),
        max(-180.0, longitude - lon_delta),
        min(90.0, latitude + lat_delta),
        min(180.0, longitude + lon_delta),
    )


def _distance_km(
    latitude_a: float,
    longitude_a: float,
    latitude_b: float,
    longitude_b: float,
) -> float:
    earth_radius_km = 6371.0088
    lat_a = math.radians(latitude_a)
    lat_b = math.radians(latitude_b)
    delta_lat = math.radians(latitude_b - latitude_a)
    delta_lon = math.radians(longitude_b - longitude_a)
    value = (
        math.sin(delta_lat / 2) ** 2
        + math.cos(lat_a) * math.cos(lat_b) * math.sin(delta_lon / 2) ** 2
    )
    return earth_radius_km * 2 * math.atan2(math.sqrt(value), math.sqrt(1 - value))


def _overview_value(overview: dict[str, Any], candidates: tuple[str, ...]) -> Any:
    normalized_candidates = tuple(_normalize_key(candidate) for candidate in candidates)
    connection = overview.get("connection")
    if isinstance(connection, dict):
        for key, value in connection.items():
            if _normalize_key(key) in normalized_candidates and _has_value(value):
                return value

    for section in overview.get("sections", []):
        if not isinstance(section, dict):
            continue
        for item in section.get("items", []):
            if not isinstance(item, dict):
                continue
            source = _normalize_key(item.get("source", ""))
            label = _normalize_key(item.get("label", ""))
            if any(
                candidate == label
                or source.endswith(candidate)
                or f".{candidate}" in source
                for candidate in normalized_candidates
            ) and _has_value(item.get("value")):
                return item.get("value")
    return None


def _radio_from_values(network_type: str | None, band: str | None) -> str | None:
    text = f"{network_type or ''} {band or ''}".lower()
    if "nr" in text or "5g" in text or re.search(r"\bn\d+", text):
        return "NR"
    if "lte" in text or re.search(r"\bb\d+", text):
        return "LTE"
    if "umts" in text or "hspa" in text:
        return "UMTS"
    if "gsm" in text:
        return "GSM"
    return None


def _plmn_parts(value: str) -> tuple[int, int] | None:
    compact = re.sub(r"\D", "", value)
    if len(compact) < 5:
        return None
    return int(compact[:3]), int(compact[3:])


def _int_or_none(value: Any) -> int | None:
    if isinstance(value, bool) or value is None:
        return None
    if isinstance(value, int):
        return value
    if isinstance(value, float):
        return int(value)
    text = str(value).strip()
    if not text:
        return None
    hex_match = re.search(r"0x[0-9a-fA-F]+", text)
    if hex_match:
        return int(hex_match.group(0), 16)
    match = re.search(r"\d+", text)
    if match is None:
        return None
    return int(match.group(0))


def _float_or_none(value: Any) -> float | None:
    if isinstance(value, bool) or value is None:
        return None
    if isinstance(value, (int, float)):
        return float(value)
    text = str(value).strip()
    if not text:
        return None
    match = re.search(r"-?\d+(?:\.\d+)?", text)
    if match is None:
        return None
    return float(match.group(0))


def _bool_or_none(value: Any) -> bool | None:
    if isinstance(value, bool):
        return value
    if isinstance(value, (int, float)):
        return bool(value)
    if isinstance(value, str):
        normalized = value.strip().lower()
        if normalized in {"true", "1", "yes", "on"}:
            return True
        if normalized in {"false", "0", "no", "off"}:
            return False
    return None


def _clean_text(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def _has_value(value: Any) -> bool:
    return value is not None and (not isinstance(value, str) or bool(value.strip()))


def _normalize_key(value: Any) -> str:
    return re.sub(r"[^a-z0-9]", "", str(value).lower())


def _format_coord(value: float) -> str:
    return f"{value:.6f}"


def _is_no_cells_found(value: Any) -> bool:
    return "no cell" in str(value or "").lower()
