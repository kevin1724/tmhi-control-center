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
