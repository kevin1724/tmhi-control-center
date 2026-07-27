from __future__ import annotations

from datetime import datetime, timezone

import httpx
import pytest

from tmhi_control_center.speedtest import (
    LowImpactSpeedTest,
    next_initial_slot,
    next_scheduled_slot,
)


@pytest.mark.asyncio
async def test_gentle_speed_test_uses_bounded_sequential_samples() -> None:
    uploaded = 0

    async def handler(request: httpx.Request) -> httpx.Response:
        nonlocal uploaded
        if request.url.path == "/__down":
            byte_count = int(request.url.params.get("bytes", "0"))
            return httpx.Response(200, content=b"x" * byte_count)
        if request.url.path == "/__up":
            uploaded = len(await request.aread())
            return httpx.Response(200, content=b"ok")
        return httpx.Response(404)

    client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    runner = LowImpactSpeedTest(client)
    result = await runner.run("gentle")
    await client.aclose()

    assert result["success"] is True
    assert result["bytes_downloaded"] == 10 * 1024 * 1024
    assert result["bytes_uploaded"] == 2 * 1024 * 1024
    assert uploaded == 2 * 1024 * 1024
    assert result["download_mbps"] > 0
    assert result["upload_mbps"] > 0
    assert result["latency_ms"] >= 0


def test_rotating_schedule_moves_through_local_dayparts() -> None:
    now = datetime(2026, 7, 26, 16, 0, tzinfo=timezone.utc)
    first_run, first_slot = next_initial_slot(now, -7 * 60)
    second_run, second_slot = next_scheduled_slot(
        first_run,
        "daily",
        first_slot,
        -7 * 60,
    )

    assert first_run == datetime(2026, 7, 26, 21, 0, tzinfo=timezone.utc)
    assert first_slot == 2
    assert second_run == datetime(2026, 7, 28, 3, 0, tzinfo=timezone.utc)
    assert second_slot == 3


def test_monthly_schedule_handles_short_months() -> None:
    completed = datetime(2026, 1, 31, 10, 0, tzinfo=timezone.utc)
    next_run, next_slot = next_scheduled_slot(completed, "monthly", 0, 0)

    assert next_run == datetime(2026, 2, 28, 8, 0, tzinfo=timezone.utc)
    assert next_slot == 1
