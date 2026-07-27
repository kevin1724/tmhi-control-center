package com.kevin1724.tmhicontrolcenter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kevin1724.tmhicontrolcenter.core.AdvancedMode
import com.kevin1724.tmhicontrolcenter.core.AppSettings
import com.kevin1724.tmhicontrolcenter.core.ConnectedDevice
import com.kevin1724.tmhicontrolcenter.core.GatewayClient
import com.kevin1724.tmhicontrolcenter.core.GatewayOverview
import com.kevin1724.tmhicontrolcenter.core.RadioProfile
import com.kevin1724.tmhicontrolcenter.core.SettingsStore
import com.kevin1724.tmhicontrolcenter.core.TowerClient
import com.kevin1724.tmhicontrolcenter.core.TowerMapData
import com.kevin1724.tmhicontrolcenter.core.WifiConfig
import com.kevin1724.tmhicontrolcenter.core.WifiBackupManifest
import com.kevin1724.tmhicontrolcenter.core.WifiBackupVault
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TmhiViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = SettingsStore(application)
    private val gatewayClient = GatewayClient()
    private val towerClient = TowerClient()
    private val wifiBackupVault = WifiBackupVault(application)

    private val _state = MutableStateFlow(
        AppUiState(
            settings = settingsStore.load(),
            wifiBackups = wifiBackupVault.list(),
        ),
    )
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        refreshAll(includeNearbyTowers = false)
    }

    fun updateSettings(settings: AppSettings) {
        settingsStore.save(settings)
        _state.update { it.copy(settings = settings, message = "Settings saved.") }
    }

    fun refreshAll(includeNearbyTowers: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val settings = _state.value.settings
            val overviewDeferred = async { gatewayClient.overview(settings) }
            val wifiDeferred = async {
                if (settings.passwordConfigured) runCatching { gatewayClient.wifiConfig(settings) }.getOrNull() else null
            }
            val clientsDeferred = async {
                if (settings.passwordConfigured) runCatching { gatewayClient.connectedDevices(settings) }.getOrDefault(emptyList()) else emptyList()
            }

            val overview = overviewDeferred.await()
            val wifi = wifiDeferred.await()
            val clients = clientsDeferred.await()
            val map = runCatching {
                towerClient.loadMap(settings, overview, includeNearby = includeNearbyTowers)
            }.getOrElse {
                TowerMapData(errors = listOf(it.message.orEmpty()))
            }

            _state.update {
                it.copy(
                    loading = false,
                    overview = overview,
                    wifi = wifi,
                    clients = clients,
                    towerMap = map,
                    wifiBackups = wifiBackupVault.list(),
                    error = overview.error,
                    message = if (overview.reachable) "Gateway refreshed." else null,
                )
            }
        }
    }

    fun testGatewayLogin() {
        viewModelScope.launch {
            _state.update { it.copy(actionBusy = true, error = null, message = null) }
            runCatching { gatewayClient.authenticate(_state.value.settings) }
                .onSuccess { _state.update { it.copy(actionBusy = false, message = "Gateway login works.") } }
                .onFailure { error -> _state.update { it.copy(actionBusy = false, error = error.message) } }
        }
    }

    fun applyWifi(ssid: String, radioEnabled: Boolean?) {
        viewModelScope.launch {
            _state.update { it.copy(actionBusy = true, error = null, message = null) }
            runCatching {
                gatewayClient.updateWifi(
                    settings = _state.value.settings,
                    ssid = ssid.trim().takeIf { it.isNotBlank() },
                    radioEnabled = radioEnabled,
                )
            }.onSuccess { wifi ->
                _state.update {
                    it.copy(
                        actionBusy = false,
                        wifi = wifi,
                        message = "Wi-Fi settings sent to gateway.",
                    )
                }
                refreshAll(includeNearbyTowers = false)
            }.onFailure { error ->
                _state.update { it.copy(actionBusy = false, error = error.message) }
            }
        }
    }

    fun refreshTowers() {
        viewModelScope.launch {
            _state.update { it.copy(actionBusy = true, error = null, message = null) }
            val map = runCatching {
                towerClient.loadMap(
                    settings = _state.value.settings,
                    overview = _state.value.overview,
                    includeNearby = true,
                )
            }
            map.onSuccess { data ->
                _state.update {
                    it.copy(
                        actionBusy = false,
                        towerMap = data,
                        message = "Tower map refreshed.",
                        error = data.errors.firstOrNull(),
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(actionBusy = false, error = error.message) }
            }
        }
    }

    fun createWifiBackup() {
        viewModelScope.launch {
            _state.update { it.copy(actionBusy = true, error = null, message = null) }
            runCatching {
                val wifi = gatewayClient.wifiConfig(_state.value.settings)
                wifiBackupVault.create(wifi, _state.value.overview)
            }
                .onSuccess { backup ->
                    _state.update {
                        it.copy(
                            actionBusy = false,
                            wifiBackups = wifiBackupVault.list(),
                            message = "Encrypted Wi-Fi backup saved: ${backup.id}",
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(actionBusy = false, error = error.message) }
                }
        }
    }

    fun restoreWifiBackup(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(actionBusy = true, error = null, message = null) }
            runCatching {
                val backup = wifiBackupVault.load(id)
                val backupModel = backup.manifest.gatewayModel.trim()
                val currentModel = _state.value.overview?.device?.get("Model").orEmpty().trim()
                if (backupModel.isNotBlank() && currentModel.isNotBlank() &&
                    !backupModel.equals(currentModel, ignoreCase = true)
                ) {
                    error("This backup was created for $backupModel, not $currentModel.")
                }
                gatewayClient.restoreWifi(_state.value.settings, backup)
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        actionBusy = false,
                        wifi = result.wifi,
                        message = "Restored ${result.ssidsRestored} Wi-Fi name(s) and ${result.passwordsRestored} password(s).",
                    )
                }
                refreshAll(includeNearbyTowers = false)
            }.onFailure { error ->
                _state.update { it.copy(actionBusy = false, error = error.message) }
            }
        }
    }

    fun deleteWifiBackup(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(actionBusy = true, error = null, message = null) }
            runCatching { wifiBackupVault.delete(id) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            actionBusy = false,
                            wifiBackups = wifiBackupVault.list(),
                            message = "Wi-Fi backup deleted.",
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(actionBusy = false, error = error.message) }
                }
        }
    }

    fun requestReboot() {
        viewModelScope.launch {
            _state.update { it.copy(actionBusy = true, error = null, message = null) }
            runCatching { gatewayClient.reboot(_state.value.settings) }
                .onSuccess { message -> _state.update { it.copy(actionBusy = false, message = message) } }
                .onFailure { error -> _state.update { it.copy(actionBusy = false, error = error.message) } }
        }
    }
}

data class AppUiState(
    val settings: AppSettings = AppSettings(),
    val overview: GatewayOverview? = null,
    val wifi: WifiConfig? = null,
    val clients: List<ConnectedDevice> = emptyList(),
    val towerMap: TowerMapData = TowerMapData(),
    val wifiBackups: List<WifiBackupManifest> = emptyList(),
    val loading: Boolean = false,
    val actionBusy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

fun AppSettings.withGateway(
    host: String,
    port: String,
    username: String,
    password: String,
): AppSettings {
    return copy(
        gatewayHost = host.trim().ifBlank { "192.168.12.1" },
        gatewayPort = port.toIntOrNull()?.coerceIn(1, 65535) ?: 8080,
        gatewayUsername = username.trim().ifBlank { "admin" },
        gatewayPassword = password,
    )
}

fun AppSettings.withMap(
    openCellIdKey: String,
    latitude: String,
    longitude: String,
    radius: String,
): AppSettings {
    return copy(
        openCellIdKey = openCellIdKey.trim(),
        mapLatitude = latitude.toDoubleOrNull(),
        mapLongitude = longitude.toDoubleOrNull(),
        mapRadiusKm = radius.toDoubleOrNull()?.coerceIn(0.25, 100.0) ?: 0.8,
    )
}

fun AppSettings.withAdvanced(
    enabled: Boolean,
    radioProfile: RadioProfile,
    acknowledged: Boolean,
): AppSettings {
    return copy(
        advancedMode = if (enabled) AdvancedMode.G4arUnlockLab else AdvancedMode.Disabled,
        radioProfile = radioProfile,
        advancedAcknowledged = enabled && acknowledged,
    )
}
