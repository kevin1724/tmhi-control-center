# Low-Impact Speed History

TMHI Control Center can measure how download speed, upload speed, latency, and
jitter change over time without running a continuous background load.

## Schedule

Automatic tests are disabled by default. The dashboard offers:

- `Once a day`
- `Once a week`
- `Once a month`

Each scheduled run moves to the next local daypart: night, morning, afternoon,
then evening. This builds a useful time-of-day comparison with only one test per
selected interval. Missed runs are not replayed after a container restart.

The browser's current UTC offset is saved when the schedule is changed. Update
the schedule after a daylight-saving-time change if the displayed local run
time is off by an hour.

## Data Budgets

| Profile | Download | Upload | Maximum per run |
| --- | ---: | ---: | ---: |
| `Gentle` | 10 MiB | 2 MiB | about 12.6 MB |
| `Standard` | 25 MiB | 5 MiB | about 31.5 MB |

Latency probes, the download sample, and the upload sample run sequentially.
Only one speed test can run at a time. These bounded samples are intended for
trend comparison and may report lower peak throughput than a large,
multi-connection benchmark.

## Storage and Privacy

Results are stored in `/data/control-center.db` for up to 730 days, with a hard
limit on returned chart points. The database records the measured rates, timing,
data volume, provider, trigger, and any failure message.

The measurement requests use Cloudflare's public speed-test download and upload
endpoints. Test traffic and measurement metadata are sent to Cloudflare. Review
Cloudflare's official [speedtest repository](https://github.com/cloudflare/speedtest)
and [measurement page](https://speed.cloudflare.com/) before enabling a schedule.

## Operational Notes

- Start with `Gentle` and `Once a week` on a busy or data-sensitive connection.
- Use `Run Test Now` to verify the provider is reachable before scheduling.
- Automatic failures are retained so gaps and outages remain visible.
- Turn the schedule off before bandwidth-sensitive events or troubleshooting.
- This feature measures the Docker host's internet path, which normally passes
  through the TMHI gateway. It does not measure Wi-Fi speed between a client and
  the gateway.
