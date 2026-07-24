from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta
from typing import Any

import httpx

from .models import utc_now


class PublicIpLocationError(RuntimeError):
    """Public IP geolocation providers were not able to estimate a location."""


@dataclass(frozen=True, slots=True)
class PublicIpLocation:
    latitude: float
    longitude: float
    provider: str
    ip: str | None = None
    city: str | None = None
    region: str | None = None
    country: str | None = None
    isp: str | None = None
    timezone: str | None = None

    def to_dict(self) -> dict[str, Any]:
        return {
            "latitude": self.latitude,
            "longitude": self.longitude,
            "provider": self.provider,
            "ip": self.ip,
            "city": self.city,
            "region": self.region,
            "country": self.country,
            "isp": self.isp,
            "timezone": self.timezone,
            "accuracy": "public_ip_city_estimate",
        }


class PublicIpLocator:
    def __init__(self, *, ttl_seconds: int = 21_600) -> None:
        self._ttl = timedelta(seconds=ttl_seconds)
        self._cached_at: datetime | None = None
        self._cached_location: PublicIpLocation | None = None

    async def locate(self, *, refresh: bool = False) -> PublicIpLocation:
        now = utc_now()
        if (
            not refresh
            and self._cached_location is not None
            and self._cached_at is not None
            and now - self._cached_at < self._ttl
        ):
            return self._cached_location

        errors: list[str] = []
        async with httpx.AsyncClient(
            timeout=httpx.Timeout(8.0),
            follow_redirects=True,
            trust_env=False,
            headers={
                "Accept": "application/json",
                "User-Agent": "tmhi-control-center/0.1",
            },
        ) as client:
            for provider in (_lookup_ipapi, _lookup_ipwhois):
                try:
                    location = await provider(client)
                except PublicIpLocationError as exc:
                    errors.append(str(exc))
                    continue
                self._cached_at = now
                self._cached_location = location
                return location

        raise PublicIpLocationError(
            "; ".join(errors) if errors else "No public IP geolocation provider responded"
        )


async def _lookup_ipapi(client: httpx.AsyncClient) -> PublicIpLocation:
    payload = await _get_json(client, "https://ipapi.co/json/", "ipapi.co")
    if payload.get("error"):
        raise PublicIpLocationError(
            f"ipapi.co: {payload.get('reason') or payload.get('message') or 'lookup failed'}"
        )
    latitude = _float_or_none(payload.get("latitude"))
    longitude = _float_or_none(payload.get("longitude"))
    if latitude is None or longitude is None:
        raise PublicIpLocationError("ipapi.co: no latitude/longitude returned")
    return PublicIpLocation(
        latitude=latitude,
        longitude=longitude,
        provider="ipapi.co",
        ip=_text_or_none(payload.get("ip")),
        city=_text_or_none(payload.get("city")),
        region=_text_or_none(payload.get("region")),
        country=_text_or_none(payload.get("country_name") or payload.get("country")),
        isp=_text_or_none(payload.get("org")),
        timezone=_text_or_none(payload.get("timezone")),
    )


async def _lookup_ipwhois(client: httpx.AsyncClient) -> PublicIpLocation:
    payload = await _get_json(client, "https://ipwho.is/", "ipwho.is")
    if payload.get("success") is False:
        raise PublicIpLocationError(
            f"ipwho.is: {payload.get('message') or 'lookup failed'}"
        )
    latitude = _float_or_none(payload.get("latitude"))
    longitude = _float_or_none(payload.get("longitude"))
    if latitude is None or longitude is None:
        raise PublicIpLocationError("ipwho.is: no latitude/longitude returned")
    connection = payload.get("connection") if isinstance(payload.get("connection"), dict) else {}
    timezone = payload.get("timezone") if isinstance(payload.get("timezone"), dict) else {}
    return PublicIpLocation(
        latitude=latitude,
        longitude=longitude,
        provider="ipwho.is",
        ip=_text_or_none(payload.get("ip")),
        city=_text_or_none(payload.get("city")),
        region=_text_or_none(payload.get("region")),
        country=_text_or_none(payload.get("country")),
        isp=_text_or_none(connection.get("isp") or connection.get("org")),
        timezone=_text_or_none(timezone.get("id")),
    )


async def _get_json(
    client: httpx.AsyncClient,
    url: str,
    provider_name: str,
) -> dict[str, Any]:
    try:
        response = await client.get(url)
    except (httpx.HTTPError, OSError) as exc:
        raise PublicIpLocationError(
            f"{provider_name}: {type(exc).__name__}: {exc}"
        ) from exc
    if not response.is_success:
        raise PublicIpLocationError(
            f"{provider_name}: HTTP {response.status_code}"
        )
    try:
        payload = response.json()
    except ValueError as exc:
        raise PublicIpLocationError(f"{provider_name}: invalid JSON") from exc
    if not isinstance(payload, dict):
        raise PublicIpLocationError(f"{provider_name}: non-object response")
    return payload


def _float_or_none(value: Any) -> float | None:
    if isinstance(value, bool) or value is None:
        return None
    if isinstance(value, (int, float)):
        return float(value)
    text = str(value).strip()
    if not text:
        return None
    try:
        return float(text)
    except ValueError:
        return None


def _text_or_none(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None
