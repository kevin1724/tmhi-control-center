from __future__ import annotations

import asyncio
from datetime import datetime, timezone

import pytest

from tmhi_control_center.storage import EventStore
from tmhi_control_center.telemetry import GatewayTelemetryCollector


@pytest.mark.asyncio
async def test_background_collector_records_without_browser_requests(tmp_path) -> None:
    store = EventStore(str(tmp_path / "telemetry.db"))
    await store.initialize()
    collected_twice = asyncio.Event()
    calls = 0

    async def overview_provider():
        nonlocal calls
        calls += 1
        if calls >= 2:
            collected_twice.set()
        return {
            "observed_at": datetime.now(timezone.utc).isoformat(),
            "detection": {"reachable": True},
            "signal": {
                "score": 80 + calls,
                "metrics": [{"key": "rsrp", "value": -90 + calls}],
            },
            "radios": [],
            "system": {},
        }

    collector = GatewayTelemetryCollector(
        overview_provider,
        store,
        interval_seconds=0.02,
    )
    task = asyncio.create_task(collector.run())
    try:
        await asyncio.wait_for(collected_twice.wait(), timeout=2)
    finally:
        await collector.stop()
        await task

    history = await store.telemetry_history(hours=1)
    assert calls >= 2
    assert history["count"] >= 2
    assert collector.status()["last_collected_at"] is not None


@pytest.mark.asyncio
async def test_collector_reuses_a_fresh_snapshot(tmp_path) -> None:
    store = EventStore(str(tmp_path / "telemetry.db"))
    await store.initialize()
    calls = 0

    async def overview_provider():
        nonlocal calls
        calls += 1
        return {
            "observed_at": datetime.now(timezone.utc).isoformat(),
            "detection": {"reachable": True},
            "signal": {"score": 90, "metrics": []},
            "radios": [],
            "system": {},
        }

    collector = GatewayTelemetryCollector(overview_provider, store)
    first = await collector.collect_once()
    second = await collector.collect_once(max_age_seconds=60)

    assert second is first
    assert calls == 1
    assert (await store.telemetry_history(hours=1))["count"] == 1
