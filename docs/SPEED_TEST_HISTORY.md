# Speed Test History

TMHI Control Center can measure how download speed, upload speed, latency, and
jitter change over time without running a continuous background load.

## Schedule

Automatic tests are disabled by default. The dashboard offers:

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
| `Gentle` | 10 MiB | 2 MiB | about 12.6 MB |
| `Standard` | 25 MiB | 5 MiB | about 31.5 MB |
| `Accurate` | 100 MiB | 20 MiB | about 125.8 MB |

Latency probes, the download sample, and the upload sample run sequentially.
Only one speed test can run at a time. These bounded samples are intended for
trend comparison. Accurate uses a longer transfer to give fast connections more
time to reach steady throughput, but a single-connection result may still be
lower than a large multi-connection benchmark.

The schedule panel calculates the maximum daily and 30-day transfer before the
schedule is saved. For context, `Every 5 minutes` runs 288 tests per day:

| Profile | Maximum per day | Maximum per 30 days |
| --- | ---: | ---: |
| `Gentle` | about 3.62 GB | about 108.7 GB |
| `Standard` | about 9.06 GB | about 271.8 GB |
| `Accurate` | about 36.24 GB | about 1.09 TB |

The browser requires confirmation for high-frequency or high-volume choices.

## Storage and Privacy

Results are stored in `/data/control-center.db` for up to 730 days, with a hard
limit on returned chart points. The database records the measured rates, timing,
data volume, provider, trigger, and any failure message.

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
