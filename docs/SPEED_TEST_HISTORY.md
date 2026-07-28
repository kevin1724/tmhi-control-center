# Speed Test History

TMHI Control Center can measure how download speed, upload speed, latency, and
jitter change over time without running a continuous background load.

## Schedule

Automatic tests are disabled by default. Open `Settings` and use the
`Speed Test History` section to choose:

- `Every 5 minutes`
- `Every 10 minutes`
- `Every 15 minutes`
- `Every 30 minutes`
- `Every hour`
- `Once a day`
- `Once a week`
- `Once a month`

Interval schedules start from the time they are saved. Daily, weekly, and
monthly runs move through local night, morning, afternoon, and evening slots.
Only one test can run at a time, and missed runs are not replayed after a
container restart.

The browser's current UTC offset is saved when the schedule is changed. Update
the schedule after a daylight-saving-time change if the displayed local run
time is off by an hour.

## Data Budgets

| Profile | Download | Upload | Maximum per run |
| --- | ---: | ---: | ---: |
| `Gentle` | 10 MB | 2 MB | 12 MB |
| `Standard` | 25 MB | 5 MB | 30 MB |
| `Accurate` | 100 MB | 25 MB | 125 MB |
| `Extended` | 250 MB | 50 MB | 300 MB |
| `Maximum` | 800 MB | 200 MB | 1 GB |

Latency probes, the download sample, and the upload sample run sequentially.
Only one speed test can run at a time. These bounded samples are intended for
trend comparison. Transfers are split into 25 MB provider requests so a large
profile does not depend on one oversized request. Upload bodies are streamed in
small chunks instead of being allocated as one large in-memory payload.

Accurate and larger profiles give fast connections more time to reach steady
throughput, but a sequential single-connection result may still be lower than a
large multi-connection benchmark.

The Settings panel calculates the maximum daily and 30-day transfer before the
schedule is saved. For context, `Every 5 minutes` runs 288 tests per day:

| Profile | Maximum per day | Maximum per 30 days |
| --- | ---: | ---: |
| `Gentle` | 3.46 GB | 103.68 GB |
| `Standard` | 8.64 GB | 259.20 GB |
| `Accurate` | 36.00 GB | 1.08 TB |
| `Extended` | 86.40 GB | 2.59 TB |
| `Maximum` | 288.00 GB | 8.64 TB |

The browser requires confirmation for high-frequency or high-volume choices.

## Storage and Privacy

Results are stored in `/data/control-center.db`. The dashboard lets each user
choose 30 days, 90 days, 6 months, 1 year, or 2 years of retention. The `All`
chart range displays the complete retained period with a hard limit on returned
chart points. Reducing retention permanently deletes records older than the new
selection as soon as the settings are saved.

The database records the measured rates, timing, data volume, provider, trigger,
and any failure message. `SPEEDTEST_RETENTION_DAYS` can also be set directly to
any value from 30 through 730.

The measurement requests use Cloudflare's public speed-test download and upload
endpoints. Test traffic and measurement metadata are sent to Cloudflare. Review
Cloudflare's official [speedtest repository](https://github.com/cloudflare/speedtest)
and [measurement page](https://speed.cloudflare.com/) before enabling a schedule.

## Operational Notes

- Start with `Gentle` and an hourly or daily schedule on a busy connection.
- Use the 24-hour chart to inspect all-day variation from interval schedules.
- Use `Run Test Now` to verify the provider is reachable before scheduling.
- Automatic failures are retained so gaps and outages remain visible.
- Turn the schedule off before bandwidth-sensitive events or troubleshooting.
- This feature measures the Docker host's internet path, which normally passes
  through the TMHI gateway. It does not measure Wi-Fi speed between a client and
  the gateway.
