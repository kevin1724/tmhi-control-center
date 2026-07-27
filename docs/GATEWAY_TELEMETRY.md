# Gateway Telemetry

TMHI Control Center separates direct measurements, gateway state, and derived
labels so the dashboard does not present guesses as sensor data.

## Data Sources

The common Arcadyan, Sagemcom, and Sercomm TMI API provides two useful data
levels:

- `GET /TMI/v1/gateway?get=all` supplies the public device, time, connection,
  and basic LTE/5G signal blocks.
- `GET /TMI/v1/network/telemetry?get=cell` supplies authenticated advanced cell
  details on firmware versions that support it.

The advanced request is optional. If the login is missing or a firmware version
does not implement it, the public dashboard data continues to work.

## Measured Radio Fields

Each LTE and 5G block is normalized independently. A radio card appears only
when the gateway returned real data for that radio.

| Field | Meaning | History |
| --- | --- | --- |
| RSRP | Cellular reference-signal power in dBm | Yes |
| RSRQ | Cellular reference-signal quality in dB | Current sample |
| SINR | Signal clarity relative to interference and noise in dB | Yes |
| RSSI | Total received radio power in dBm | Current sample |
| Bars | Gateway-provided visual signal level | Current sample |
| CQI | Channel quality indicator reported by advanced cell telemetry | Current sample |
| Band | Active LTE or NR band | Stored with each sample |
| Bandwidth | Active channel bandwidth | Stored with each sample |
| Antenna | Gateway-reported internal or external antenna selection | Stored with each sample |
| PCI | Physical cell identity | Stored with each sample |
| EARFCN / NR-ARFCN | LTE or NR channel number | Stored with each sample |
| Cell ID | Sector/cell identity | Stored with each sample |
| eNBID / gNBID | LTE or NR base-station identity | Current sample |
| TAC / ECGI / PLMN | Carrier and tracking-area identity | Current sample |

## Gateway And Session Fields

The dashboard also normalizes model, manufacturer, firmware, hardware version,
update state, uptime, APN, registration, roaming, WAN addresses, IPv6 state,
MCC/MNC, and Wi-Fi/client totals when they are present.

Radio mode uses an explicit access-technology value when the firmware provides
one. Otherwise it reports the active radio combination as `4G LTE`, `5G NR`, or
`LTE + 5G NR`; it does not claim SA or NSA from signal blocks alone.

## Temperature

Temperature is never estimated. The dashboard accepts common temperature field
names and normalizes an actual reading to Celsius and Fahrenheit. If the
gateway response has no sensor value, the temperature tile and history chart
are omitted from the dashboard entirely.

The G4AR documentation lists an operating environment, but that ambient range
is not an internal sensor reading and is not used as dashboard telemetry.

## History And Retention

Every successful dashboard overview stores a compact snapshot in the same
SQLite database used by the app. The default 30-second web refresh builds the
history while the dashboard is open.

- Retention: 14 days.
- Available ranges: 1 hour, 6 hours, 24 hours, and 7 days.
- Maximum returned points: 2,000.
- Large ranges are evenly downsampled while preserving the first and latest
  samples.
- Unreachable-gateway responses are not written as zero-value measurements.

The stored snapshot excludes credentials, SIM identifiers, full MAC addresses,
and other private gateway fields.
