package com.kevin1724.tmhicontrolcenter.core

data class AppSettings(
    val gatewayHost: String = "192.168.12.1",
    val gatewayPort: Int = 8080,
    val gatewayUsername: String = "admin",
    val gatewayPassword: String = "",
    val openCellIdKey: String = "",
    val mapLatitude: Double? = null,
    val mapLongitude: Double? = null,
    val mapRadiusKm: Double = 0.8,
    val advancedMode: AdvancedMode = AdvancedMode.Disabled,
    val adapterUrl: String = "",
    val radioProfile: RadioProfile = RadioProfile.Auto,
    val advancedAcknowledged: Boolean = false,
    val skipStockBackup: Boolean = false,
) {
    val gatewayBaseUrl: String
        get() = "http://$gatewayHost:$gatewayPort/TMI/v1"

    val passwordConfigured: Boolean
        get() = gatewayPassword.isNotBlank()
}

enum class AdvancedMode(val label: String) {
    Disabled("Disabled"),
    G4arUnlockLab("G4AR unlock / radio lab"),
}

enum class RadioProfile(val label: String, val description: String) {
    Auto("Auto", "Leave the gateway/modem in automatic mode."),
    PreferLteAnchorNsa(
        "Prefer LTE anchor / 5G NSA",
        "Adapter-facing intent for trying LTE anchor plus 5G NR NSA.",
    ),
    LteOnlyTest("LTE-only test", "Temporary diagnostic profile for measuring LTE by itself."),
    NrSa("5G Standalone", "Prefer NR SA where supported."),
    ScanOnly("Scan only", "Collect radio data without applying an override."),
}

data class GatewayOverview(
    val reachable: Boolean = false,
    val supported: Boolean = false,
    val apiType: String = "unknown",
    val device: Map<String, String> = emptyMap(),
    val connection: Map<String, String> = emptyMap(),
    val wifi: Map<String, String> = emptyMap(),
    val signal: SignalSummary = SignalSummary(),
    val sections: List<GatewaySection> = emptyList(),
    val rawJson: String = "",
    val error: String? = null,
)

data class SignalSummary(
    val score: Int? = null,
    val quality: String = "Unknown",
    val summary: String = "No signal metrics loaded yet.",
    val metrics: List<SignalMetric> = emptyList(),
)

data class SignalMetric(
    val key: String,
    val label: String,
    val value: String,
    val score: Int?,
)

data class GatewaySection(
    val title: String,
    val items: List<GatewayItem>,
)

data class GatewayItem(
    val label: String,
    val value: String,
    val source: String,
)

data class WifiConfig(
    val ssid: String = "",
    val radioEnabled: Boolean? = null,
    val broadcastEnabled: Boolean? = null,
    val source: String = "",
    val rawJson: String = "",
)

data class ConnectedDevice(
    val id: String,
    val hostname: String,
    val ipAddress: String,
    val macAddress: String,
    val interfaceName: String,
    val ssid: String,
    val band: String,
    val vendor: String,
    val bestGuess: String,
)

data class TowerIdentity(
    val mcc: Int? = null,
    val mnc: Int? = null,
    val lac: Int? = null,
    val cellId: Long? = null,
    val pci: Int? = null,
    val band: String = "",
    val radio: String = "",
    val networkType: String = "",
) {
    val queryable: Boolean
        get() = mcc != null && mnc != null && lac != null && cellId != null
}

data class MapCenter(
    val latitude: Double,
    val longitude: Double,
    val source: String,
)

data class Tower(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double?,
    val radio: String,
    val cellId: Long?,
    val lac: Int?,
    val averageSignal: Int?,
    val rangeMeters: Int?,
    val samples: Int?,
    val connected: Boolean = false,
)

data class TowerMapData(
    val center: MapCenter = MapCenter(39.8283, -98.5795, "default_us"),
    val identity: TowerIdentity = TowerIdentity(),
    val connectedTower: Tower? = null,
    val nearby: List<Tower> = emptyList(),
    val errors: List<String> = emptyList(),
)

data class FirmwareBackupManifest(
    val id: String,
    val firmwareVersion: String,
    val artifactCount: Int,
    val path: String,
)
