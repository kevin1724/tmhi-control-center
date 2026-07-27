# G4AR USB-C 2.5GbE Lab

The Arcadyan TMO-G4AR has a second USB-C connector described by T-Mobile as a
data/other-purpose port. The MediaTek T750 platform contains one USB 3
controller, and the G4AR FCC test setup connected a USB 3.2 Gen 1 Type-C flash
drive to the port. This makes a USB 3 to 2.5GbE experiment technically
reasonable, but it does not prove that stock G4AR firmware includes a USB
Ethernet driver or automatically adds that interface to the LAN bridge.

TMHI Control Center therefore treats this as an experimental, read-only-first
hardware lab. It does not label the port as working until a gateway-side probe
reports the controller, device, driver, interface, carrier, and negotiated
link speed.

## What Is Known

- The G4AR has one USB-C power input and one USB-C data/other-purpose port.
- MediaTek documents one USB 3 port on the T750 platform.
- The FCC test configuration attached a Silicon Power C31 Type-C USB flash
  drive to the G4AR USB port. Silicon Power identifies that model as USB 3.2
  Gen 1.
- The T750 platform can move multi-gigabit traffic internally, but the G4AR's
  two exposed yellow RJ45 ports are documented as Gigabit Ethernet.
- The stock browser UI exposes no documented USB network configuration.

The FCC flash-drive connection is strong evidence that the physical port can
operate as a USB host. It is not proof that every production firmware build
mounts storage, includes USB Ethernet modules, negotiates SuperSpeed, or can
offload NAT over a USB interface.

## Best First Adapter

Use a USB-C 5Gbps to 2.5GBASE-T adapter that explicitly lists the **ASIX
AX88279** chipset. AX88279 supports the generic CDC-NCM class in addition to
ASIX's driver. CDC-NCM gives an embedded Linux image a better chance of binding
the adapter when a vendor-specific Realtek `r8152` module is missing or old.

One documented example is Delock item `66046`. An equivalent adapter is fine
only when the seller identifies the AX88279 chipset; product shells and brand
names can remain the same while the internal chipset changes.

Also use:

- A Cat5e or Cat6 cable.
- A computer or switch with a real 2.5GbE port.
- The original G4AR power adapter connected to the USB-C power port.
- One normal G4AR RJ45 connection as a recovery path during testing.

Do not connect a powered USB-C hub or inject Power Delivery into the data port.
The FCC test proves a bus-powered flash drive was attached; it does not publish
the port's safe current budget or Power Delivery behavior.

## Stage 1: Stock Firmware Smoke Test

1. Leave the supplied power adapter connected to the G4AR power port.
2. Connect the AX88279 adapter to the separate USB-C data port.
3. Connect its RJ45 jack to a 2.5GbE switch or computer.
4. Check the adapter and switch link LEDs.
5. Check whether the connected computer receives a TMHI LAN address.

If DHCP works and the gateway remains reachable, stock firmware may already be
binding and bridging the adapter. Confirm the negotiated speed from the switch
or computer. Do not assume a lit LED means the gateway created a network
interface.

If there is no DHCP address, that is the expected result on locked firmware.
The next stage requires a verified shell or local agent running on the G4AR
itself. Docker running on another computer cannot inspect the gateway's USB bus.

## Stage 2: Read-Only Gateway Probe

On a rooted or custom-firmware G4AR, collect these values without changing the
network:

```sh
lsusb
lsusb -t
dmesg | tail -n 100
ip -br link
bridge link 2>/dev/null || brctl show
```

For a detected network interface, also collect:

```sh
ethtool usb0
ethtool -i usb0
```

Replace `usb0` with the interface actually reported by `ip -br link`. Useful
driver names include `cdc_ncm`, `cdc_ether`, `ax88179_178a`, and `r8152`.

Record the USB role, bus speed, chipset, driver, interface name, carrier state,
negotiated link speed, and current bridge members in private lab notes. Redact
serial numbers and MAC addresses before sharing command output.

## Stage 3: Isolated Link Test

Only continue when the command output confirms all of these:

- USB host
- USB 5 Gbps
- Ethernet NIC
- Driver
- Network interface

Give the USB interface a temporary, isolated subnet before touching `br-lan`:

```sh
USB_IF=usb0
ip link set "$USB_IF" up
ip addr add 192.168.250.1/24 dev "$USB_IF"
```

Set the directly connected test computer to `192.168.250.2/24`, then ping
`192.168.250.1`. Check `ethtool "$USB_IF"` again for `2500Mb/s`, full duplex,
and link detected. Remove the test address when finished:

```sh
ip addr del 192.168.250.1/24 dev "$USB_IF"
```

These commands are intentionally temporary. A reboot should discard them.

## Stage 4: Temporary LAN Bridge

Do this only with a working RJ45 recovery connection and console access. First
record the existing bridge and routes:

```sh
ip -d link show
ip route show
bridge link 2>/dev/null || brctl show
```

If the LAN bridge is confirmed as `br-lan`, a modern iproute2 system can add the
tested USB interface temporarily with:

```sh
ip link set usb0 master br-lan
ip link set usb0 up
```

Older BusyBox firmware may instead use:

```sh
brctl addif br-lan usb0
ip link set usb0 up
```

Do not persist this at boot until DHCP, IPv4, IPv6, DNS, firewall behavior,
gateway management, reboot recovery, and sustained throughput have all been
tested. To remove the temporary member:

```sh
ip link set usb0 nomaster
```

Or reboot the gateway if the change was kept in memory only.

## Performance Expectations

A successful 2.5GbE link removes the 1GbE ceiling between a Wi-Fi 6 client and a
2.5GbE LAN device or router. It does not guarantee 2.5Gbps internet service.
Cell conditions, the TMHI plan, tower load, modem throughput, NAT, and CPU or
offload support still apply.

The T750 has a hardware network acceleration engine, but a USB NIC may not be
part of the G4AR firmware's accelerated path. A working USB 2.5GbE link can
therefore perform worse than expected until the firmware's bridge, firewall,
and offload configuration are understood.

## Sources

- [T-Mobile G4AR specifications](https://www.t-mobile.com/support/home-internet/5g-gateway-g4ar)
- [MediaTek T750 platform specifications](https://www.mediatek.com/products/5g-broadband/mediatek-t750)
- [G4AR FCC test report showing the USB-C flash drive](https://fcc.report/FCC-ID/RAXTMOG4AR/6551584.pdf)
- [Silicon Power C31 USB 3.2 Gen 1 specifications](https://www.silicon-power.com/web/product-Mobile_C31)
- [ASIX AX88279 specifications](https://www.asix.com.tw/en/product/USBEthernet/Super-Speed_USB_Ethernet/AX88279)
- [Delock 66046 AX88279 adapter specifications](https://www.delock.com/produkt/66046/pdf.html?sprache=en)
