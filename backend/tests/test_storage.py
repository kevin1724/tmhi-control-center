from datetime import datetime, timedelta, timezone

import pytest

from tmhi_control_center.storage import EventStore


@pytest.mark.asyncio
async def test_telemetry_history_compacts_and_downsamples_gateway_data(tmp_path) -> None:
    store = EventStore(str(tmp_path / "control-center.db"))
    await store.initialize()
    now = datetime.now(timezone.utc)

    for index in range(25):
        observed_at = now - timedelta(minutes=24 - index)
        recorded = await store.record_telemetry(
            {
                "observed_at": observed_at.isoformat(),
                "detection": {"reachable": True},
                "signal": {"score": 70 + index},
                "connection": {"mode": "LTE + 5G NR", "band": "n41"},
                "system": {
                    "temperature": {"celsius": 40 + (index / 10)},
                    "uptime_seconds": 1000 + index,
                },
                "radios": [
                    {
                        "key": "nr",
                        "active": True,
                        "score": 80,
                        "quality": "Good",
                        "antenna": "External",
                        "cell": {"band": "n41", "pci": "321"},
                        "metrics": [
                            {"key": "rsrp", "value": -90 + index},
                            {"key": "sinr", "value": 10 + index},
                        ],
                    }
                ],
            }
        )
        assert recorded is True

    history = await store.telemetry_history(hours=1, limit=20)

    assert history["count"] == 20
    assert history["points"][-1]["signal_score"] == 94
    assert history["points"][-1]["radios"]["nr"]["metrics"]["rsrp"] == -66.0
    assert "nr.rsrp" in history["series"]
    assert "nr.sinr" in history["series"]
    assert "temperature_c" in history["series"]


@pytest.mark.asyncio
async def test_telemetry_history_ignores_unreachable_gateway(tmp_path) -> None:
    store = EventStore(str(tmp_path / "control-center.db"))
    await store.initialize()

    recorded = await store.record_telemetry(
        {
            "observed_at": datetime.now(timezone.utc).isoformat(),
            "detection": {"reachable": False},
            "signal": {"score": 0},
        }
    )

    assert recorded is False
    assert (await store.telemetry_history())["count"] == 0


async def test_speed_test_history_and_schedule_are_persisted(tmp_path) -> None:
    store = EventStore(str(tmp_path / "control-center.db"))
    await store.initialize()
    observed_at = datetime(2026, 7, 26, 15, 0, tzinfo=timezone.utc)
    await store.record_speed_test(
        {
            "observed_at": observed_at.isoformat(),
            "profile": "gentle",
            "provider": "cloudflare",
            "success": True,
            "download_mbps": 184.2,
            "upload_mbps": 22.8,
            "latency_ms": 31.4,
            "jitter_ms": 2.2,
            "bytes_downloaded": 10 * 1024 * 1024,
            "bytes_uploaded": 2 * 1024 * 1024,
            "duration_seconds": 6.5,
            "error": None,
        },
        trigger="scheduled",
        daypart="morning",
    )
    next_run = datetime(2026, 7, 27, 21, 0, tzinfo=timezone.utc)
    await store.set_speed_test_schedule(next_run, 2)

    history = await store.speed_test_history(days=730)
    schedule = await store.get_speed_test_schedule()

    assert history["count"] == 1
    assert history["averages"]["download_mbps"] == 184.2
    assert history["dayparts"][1]["label"] == "Morning"
    assert history["dayparts"][1]["count"] == 1
    assert schedule["next_run_at"] == next_run
    assert schedule["slot_index"] == 2


@pytest.mark.asyncio
async def test_speed_test_retention_prunes_older_records_immediately(tmp_path) -> None:
    store = EventStore(
        str(tmp_path / "control-center.db"),
        speed_test_retention_days=730,
    )
    await store.initialize()
    now = datetime.now(timezone.utc)

    for observed_at in (now - timedelta(days=60), now - timedelta(days=1)):
        await store.record_speed_test(
            {
                "observed_at": observed_at.isoformat(),
                "profile": "gentle",
                "provider": "cloudflare",
                "success": True,
                "download_mbps": 100.0,
                "upload_mbps": 20.0,
                "latency_ms": 30.0,
                "jitter_ms": 2.0,
                "bytes_downloaded": 10 * 1024 * 1024,
                "bytes_uploaded": 2 * 1024 * 1024,
                "duration_seconds": 5.0,
                "error": None,
            },
            trigger="scheduled",
            daypart="morning",
        )

    deleted_count = await store.set_speed_test_retention_days(30)
    history = await store.speed_test_history(days=730)

    assert deleted_count == 1
    assert history["retention_days"] == 30
    assert history["range_days"] == 30
    assert history["count"] == 1
