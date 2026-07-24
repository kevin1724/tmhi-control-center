package com.kevin1724.tmhicontrolcenter

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin1724.tmhicontrolcenter.core.AdvancedMode
import com.kevin1724.tmhicontrolcenter.core.AppSettings
import com.kevin1724.tmhicontrolcenter.core.GatewayOverview
import com.kevin1724.tmhicontrolcenter.core.RadioProfile
import com.kevin1724.tmhicontrolcenter.core.TowerMapData

class MainActivity : ComponentActivity() {
    private val viewModel: TmhiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            TmhiTheme {
                TmhiApp(state = state, viewModel = viewModel)
            }
        }
    }
}

private enum class Screen(val label: String) {
    Dashboard("Dashboard"),
    Devices("Devices"),
    Map("Map"),
    Homelab("Homelab"),
    Diagnostics("Diagnostics"),
    Settings("Settings"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TmhiApp(state: AppUiState, viewModel: TmhiViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.Dashboard) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message, state.error) {
        state.message?.let { snackbarHostState.showSnackbar(it) }
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TMHI Control Center", fontWeight = FontWeight.Black)
                        Text(
                            state.overview?.device?.get("Model") ?: state.settings.gatewayHost,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (state.loading || state.actionBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .width(24.dp)
                                .height(24.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == screen,
                        onClick = { screen = item },
                        icon = { Text(item.label.take(1), fontWeight = FontWeight.Black) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (screen) {
                Screen.Dashboard -> DashboardScreen(state) { viewModel.refreshAll(includeNearbyTowers = false) }
                Screen.Devices -> DevicesScreen(
                    state = state,
                    onApplyWifi = viewModel::applyWifi,
                    onRefresh = { viewModel.refreshAll(includeNearbyTowers = false) },
                )
                Screen.Map -> MapScreen(
                    state = state,
                    onSaveSettings = viewModel::updateSettings,
                    onRefresh = viewModel::refreshTowers,
                )
                Screen.Homelab -> HomelabScreen(
                    state = state,
                    onRefresh = { viewModel.refreshAll(includeNearbyTowers = false) },
                    onCreateBackup = viewModel::createFirmwareBackup,
                )
                Screen.Diagnostics -> DiagnosticsScreen(
                    state = state,
                    onRefresh = { viewModel.refreshAll(includeNearbyTowers = false) },
                    onReboot = viewModel::requestReboot,
                )
                Screen.Settings -> SettingsScreen(
                    state = state,
                    onSave = viewModel::updateSettings,
                    onTestLogin = viewModel::testGatewayLogin,
                    onCreateBackup = viewModel::createFirmwareBackup,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardScreen(state: AppUiState, onRefresh: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroStatusCard(state.overview)
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Gateway", if (state.overview?.reachable == true) "Online" else "Offline")
                MetricCard("Signal", state.overview?.signal?.quality ?: "Unknown")
                MetricCard("Score", state.overview?.signal?.score?.let { "$it%" } ?: "--")
                MetricCard("Clients", state.clients.size.toString())
            }
        }
        item {
            SetupCoachCard(state, compact = true)
        }
        item {
            SectionCard("Connection") {
                DetailList(state.overview?.connection.orEmpty(), "No cellular details loaded yet.")
            }
        }
        item {
            SectionCard("Signal Metrics") {
                val metrics = state.overview?.signal?.metrics.orEmpty()
                if (metrics.isEmpty()) {
                    MutedText("No signal metrics found yet.")
                } else {
                    metrics.forEach { metric ->
                        DetailRow(metric.label, metric.value)
                    }
                }
            }
        }
        item {
            Button(onClick = onRefresh, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                Text("Refresh Gateway")
            }
        }
    }
}

@Composable
private fun DevicesScreen(
    state: AppUiState,
    onApplyWifi: (String, Boolean?) -> Unit,
    onRefresh: () -> Unit,
) {
    var ssid by remember(state.wifi?.ssid) { mutableStateOf(state.wifi?.ssid.orEmpty()) }
    var radioEnabled by remember(state.wifi?.radioEnabled) { mutableStateOf(state.wifi?.radioEnabled ?: true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard("Gateway Wi-Fi") {
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it.take(32) },
                    label = { Text("SSID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Gateway Wi-Fi radios", modifier = Modifier.weight(1f))
                    Switch(checked = radioEnabled, onCheckedChange = { radioEnabled = it })
                }
                Button(
                    onClick = { onApplyWifi(ssid, radioEnabled) },
                    enabled = state.settings.passwordConfigured && !state.actionBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Apply Wi-Fi")
                }
                DetailList(
                    mapOf(
                        "Source" to state.wifi?.source.orEmpty(),
                        "SSID" to state.wifi?.ssid.orEmpty(),
                        "Radio enabled" to (state.wifi?.radioEnabled?.let { if (it) "Yes" else "No" }.orEmpty()),
                    ),
                    "Save the gateway password to load Wi-Fi settings.",
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                    Text("Refresh")
                }
                OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                    Text("Reload Clients")
                }
            }
        }
        items(state.clients, key = { it.id }) { device ->
            SectionCard(device.hostname) {
                DetailRow("IP", device.ipAddress.ifBlank { "Unknown" })
                DetailRow("MAC", device.macAddress.ifBlank { "Unknown" })
                DetailRow("Connection", listOf(device.interfaceName, device.band, device.ssid).filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "Unknown" })
                DetailRow("Vendor", device.vendor.ifBlank { "Unknown" })
                DetailRow("Best guess", device.bestGuess)
            }
        }
    }
}

@Composable
private fun HomelabScreen(
    state: AppUiState,
    onRefresh: () -> Unit,
    onCreateBackup: () -> Unit,
) {
    val insights = buildAndroidInsights(state)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard("Homelab Control Room") {
                Text(
                    "${insights.readiness.score}% ${insights.readiness.label}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                )
                Text(insights.readiness.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                DetailRow("Next action", insights.readiness.nextAction)
                Button(onClick = onRefresh, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                    Text("Refresh Baseline")
                }
            }
        }
        item {
            SectionCard("Readiness Checklist") {
                insights.setupSteps.forEach { step -> StepRow(step) }
            }
        }
        item {
            SectionCard("Signal and Antenna Coach") {
                insights.signalTips.forEach { tip -> InsightRow(tip.title, tip.detail, tip.tone) }
            }
        }
        item {
            SectionCard("Homelab Playbook") {
                insights.playbook.forEach { card ->
                    Text(card.title, fontWeight = FontWeight.Black)
                    Text(card.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    card.actions.forEach { action -> Text("- $action") }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        item {
            SectionCard("Local Adapter URL") {
                val adapterReady = state.settings.advancedMode == AdvancedMode.G4arUnlockLab &&
                    state.settings.advancedAcknowledged &&
                    state.settings.adapterUrl.isNotBlank()
                DetailRow("Status", if (adapterReady) "Adapter configured" else "Setup needed")
                Text(
                    "This is not the stock gateway login page. It is a LAN-only HTTP service running on hardware you control, such as OpenWrt/ROOTer, a Raspberry Pi, a mini PC, or a Linux box attached to the modem lab hardware.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("How to get one", fontWeight = FontWeight.Black)
                adapterGuideSteps().forEachIndexed { index, step ->
                    Text("${index + 1}. $step")
                }
                Text("Examples", fontWeight = FontWeight.Black)
                listOf("http://router.local:8080", "http://192.168.1.2:8765", "http://rooter.lan:8080").forEach {
                    DetailRow("URL", it)
                }
                Text("Expected endpoints", fontWeight = FontWeight.Black)
                listOf(
                    "GET /health",
                    "POST /g4ar/firmware/backup",
                    "POST /modem/radio/profile",
                    "POST /modem/cell/scan",
                    "POST /modem/lock",
                ).forEach { Text(it) }
                InsightRow(
                    "Safety check",
                    "Keep the adapter LAN-only. Do not flash random images, and do not continue without a stock backup, hashes, and a tested restore path.",
                    "warn",
                )
            }
        }
        item {
            SectionCard("G4AR Backup Status") {
                if (state.backups.isEmpty()) {
                    MutedText("No local stock backups saved on this phone yet.")
                } else {
                    state.backups.forEach { backup ->
                        DetailRow(backup.id, "${backup.artifactCount} files - ${backup.firmwareVersion.ifBlank { "firmware unknown" }}")
                    }
                }
                OutlinedButton(
                    onClick = onCreateBackup,
                    enabled = state.settings.advancedMode == AdvancedMode.G4arUnlockLab &&
                        state.settings.advancedAcknowledged &&
                        state.settings.adapterUrl.isNotBlank() &&
                        !state.actionBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Create Stock Backup")
                }
            }
        }
    }
}

@Composable
private fun MapScreen(
    state: AppUiState,
    onSaveSettings: (AppSettings) -> Unit,
    onRefresh: () -> Unit,
) {
    var key by remember(state.settings.openCellIdKey) { mutableStateOf(state.settings.openCellIdKey) }
    var latitude by remember(state.settings.mapLatitude) { mutableStateOf(state.settings.mapLatitude?.toString().orEmpty()) }
    var longitude by remember(state.settings.mapLongitude) { mutableStateOf(state.settings.mapLongitude?.toString().orEmpty()) }
    var radius by remember(state.settings.mapRadiusKm) { mutableStateOf(state.settings.mapRadiusKm.toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard("Tower Map") {
                MapWebView(state.towerMap, Modifier.fillMaxWidth().height(280.dp))
                Spacer(Modifier.height(8.dp))
                DetailRow("Radio", state.towerMap.identity.radio.ifBlank { "Unknown" })
                DetailRow("Band", state.towerMap.identity.band.ifBlank { "Unknown" })
                DetailRow("Cell ID", state.towerMap.identity.cellId?.toString() ?: "Unknown")
                state.towerMap.connectedTower?.let { tower ->
                    DetailRow("Tower", tower.label)
                    DetailRow("Distance", tower.distanceKm?.let { "$it km" } ?: "Unknown")
                }
            }
        }
        item {
            SectionCard("Map Settings") {
                OutlinedTextField(key, { key = it }, label = { Text("OpenCellID API key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(latitude, { latitude = it }, label = { Text("Latitude") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(longitude, { longitude = it }, label = { Text("Longitude") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                OutlinedTextField(radius, { radius = it }, label = { Text("Radius km") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            onSaveSettings(state.settings.withMap(key, latitude, longitude, radius))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save Map")
                    }
                    OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f), enabled = !state.actionBusy) {
                        Text("Refresh Towers")
                    }
                }
                state.towerMap.errors.forEach { error ->
                    MutedText(error)
                }
            }
        }
        items(state.towerMap.nearby, key = { it.id }) { tower ->
            SectionCard(tower.label) {
                DetailRow("Radio", tower.radio.ifBlank { "Unknown" })
                DetailRow("Distance", tower.distanceKm?.let { "$it km" } ?: "Unknown")
                DetailRow("Signal", tower.averageSignal?.let { "$it dBm" } ?: "Unknown")
                DetailRow("Accuracy", tower.rangeMeters?.let { "$it m" } ?: "Unknown")
                DetailRow("Samples", tower.samples?.toString() ?: "Unknown")
            }
        }
    }
}

@Composable
private fun DiagnosticsScreen(
    state: AppUiState,
    onRefresh: () -> Unit,
    onReboot: () -> Unit,
) {
    var confirmReboot by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard("Manual Checks") {
                Text("The Android app only works while it is open. It does not run a watchdog or 24/7 background internet monitor.")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                        Text("Refresh")
                    }
                    OutlinedButton(
                        onClick = { confirmReboot = true },
                        enabled = state.settings.passwordConfigured,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Reboot")
                    }
                }
            }
        }
        item {
            SectionCard("Device") {
                DetailList(state.overview?.device.orEmpty(), "No device data loaded.")
            }
        }
        items(state.overview?.sections.orEmpty(), key = { it.title }) { section ->
            SectionCard(section.title) {
                section.items.take(20).forEach { item ->
                    DetailRow(item.label, item.value)
                }
            }
        }
    }

    if (confirmReboot) {
        AlertDialog(
            onDismissRequest = { confirmReboot = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReboot = false
                        onReboot()
                    },
                ) { Text("Reboot") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReboot = false }) { Text("Cancel") }
            },
            title = { Text("Reboot gateway?") },
            text = { Text("This sends a one-time reboot request to the gateway. The Android app will not monitor recovery in the background.") },
        )
    }
}

@Composable
private fun SettingsScreen(
    state: AppUiState,
    onSave: (AppSettings) -> Unit,
    onTestLogin: () -> Unit,
    onCreateBackup: () -> Unit,
) {
    var host by remember(state.settings.gatewayHost) { mutableStateOf(state.settings.gatewayHost) }
    var port by remember(state.settings.gatewayPort) { mutableStateOf(state.settings.gatewayPort.toString()) }
    var username by remember(state.settings.gatewayUsername) { mutableStateOf(state.settings.gatewayUsername) }
    var password by remember(state.settings.gatewayPassword) { mutableStateOf(state.settings.gatewayPassword) }
    var labEnabled by remember(state.settings.advancedMode) { mutableStateOf(state.settings.advancedMode == AdvancedMode.G4arUnlockLab) }
    var adapterUrl by remember(state.settings.adapterUrl) { mutableStateOf(state.settings.adapterUrl) }
    var radioProfile by remember(state.settings.radioProfile) { mutableStateOf(state.settings.radioProfile) }
    var acknowledged by remember(state.settings.advancedAcknowledged) { mutableStateOf(state.settings.advancedAcknowledged) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard("Gateway Login") {
                OutlinedTextField(host, { host = it }, label = { Text("Gateway host") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(port, { port = it }, label = { Text("Gateway API port") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    password,
                    { password = it },
                    label = { Text("Admin password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            onSave(state.settings.withGateway(host, port, username, password))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save Login")
                    }
                    OutlinedButton(onClick = onTestLogin, modifier = Modifier.weight(1f)) {
                        Text("Test")
                    }
                }
            }
        }
        item {
            SectionCard("G4AR Unlock / Radio Lab") {
                Text(
                    "For owner-controlled Arcadyan TMO-G4AR units only. Backup and recovery should be verified before any firmware work.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "The local adapter URL is not the stock gateway login URL. It is a LAN-only service on hardware you control that exposes safe backup, scan, and radio-profile endpoints for lab hardware.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable lab", modifier = Modifier.weight(1f))
                    Switch(checked = labEnabled, onCheckedChange = { labEnabled = it })
                }
                OutlinedTextField(adapterUrl, { adapterUrl = it }, label = { Text("Local adapter URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                DetailRow("Example", "http://router.local:8080")
                DetailRow("Health check", "GET /health")
                DetailRow("Backup endpoint", "POST /g4ar/firmware/backup")
                RadioProfilePicker(radioProfile, enabled = labEnabled) { radioProfile = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = acknowledged, onCheckedChange = { acknowledged = it && labEnabled }, enabled = labEnabled)
                    Text("I own this G4AR and accept the firmware, warranty, carrier-term, and RF compliance risk.")
                }
                Button(
                    onClick = {
                        onSave(state.settings.withAdvanced(labEnabled, adapterUrl, radioProfile, acknowledged))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save Lab Settings")
                }
                OutlinedButton(
                    onClick = onCreateBackup,
                    enabled = labEnabled && acknowledged && adapterUrl.isNotBlank() && !state.actionBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Create Stock Backup")
                }
                if (state.backups.isEmpty()) {
                    MutedText("No local stock backups saved on this phone yet.")
                } else {
                    state.backups.forEach { backup ->
                        DetailRow(backup.id, "${backup.artifactCount} files - ${backup.firmwareVersion.ifBlank { "firmware unknown" }}")
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioProfilePicker(
    selected: RadioProfile,
    enabled: Boolean,
    onSelected: (RadioProfile) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("G4AR radio profile", fontWeight = FontWeight.SemiBold)
        RadioProfile.entries.forEach { profile ->
            OutlinedButton(
                onClick = { onSelected(profile) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (profile == selected) "${profile.label} selected" else profile.label)
            }
        }
        Text(selected.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SetupCoachCard(state: AppUiState, compact: Boolean) {
    val insights = buildAndroidInsights(state)
    SectionCard("Setup Coach") {
        DetailRow("Readiness", "${insights.readiness.score}% ${insights.readiness.label}")
        Text(insights.readiness.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DetailRow("Next action", insights.readiness.nextAction)
        insights.setupSteps
            .let { if (compact) it.take(4) else it }
            .forEach { step -> StepRow(step) }
    }
}

@Composable
private fun StepRow(step: SetupStep) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TonePill(step.status.uppercase(), step.tone)
            Column(modifier = Modifier.weight(1f)) {
                Text(step.title, fontWeight = FontWeight.Black)
                Text(step.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(step.action, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InsightRow(title: String, detail: String, tone: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TonePill(tone.uppercase(), tone)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TonePill(label: String, tone: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = toneColor(tone).copy(alpha = 0.16f),
        contentColor = toneColor(tone),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun toneColor(tone: String): Color {
    return when (tone) {
        "good" -> Color(0xFF0F766E)
        "bad" -> Color(0xFFC2410C)
        "warn" -> Color(0xFFA16207)
        "info" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun HeroStatusCard(overview: GatewayOverview?) {
    SectionCard("Live Gateway") {
        val signal = overview?.signal
        Text(
            signal?.quality ?: "Waiting for telemetry",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
        )
        Text(
            signal?.summary ?: "Tap refresh after joining the gateway Wi-Fi network.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MetricCard(label: String, value: String) {
    Card(
        modifier = Modifier.width(160.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            content()
        }
    }
}

@Composable
private fun DetailList(items: Map<String, String>, emptyText: String) {
    val visible = items.filterValues { it.isNotBlank() }
    if (visible.isEmpty()) {
        MutedText(emptyText)
    } else {
        visible.forEach { (key, value) -> DetailRow(key, value) }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.42f))
        Text(value.ifBlank { "Unknown" }, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.58f))
    }
}

@Composable
private fun MutedText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun MapWebView(data: TowerMapData, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
            }
        },
        update = { view ->
            view.loadDataWithBaseURL(
                "https://tile.openstreetmap.org/",
                towerMapHtml(data),
                "text/html",
                "UTF-8",
                null,
            )
        },
    )
}

private data class AndroidInsights(
    val readiness: Readiness,
    val setupSteps: List<SetupStep>,
    val signalTips: List<SignalTip>,
    val playbook: List<PlaybookCard>,
)

private data class Readiness(
    val score: Int,
    val label: String,
    val summary: String,
    val nextAction: String,
)

private data class SetupStep(
    val title: String,
    val status: String,
    val tone: String,
    val detail: String,
    val action: String,
    val weight: Int,
)

private data class SignalTip(
    val title: String,
    val detail: String,
    val tone: String,
)

private data class PlaybookCard(
    val title: String,
    val tone: String,
    val summary: String,
    val actions: List<String>,
)

private fun buildAndroidInsights(state: AppUiState): AndroidInsights {
    val steps = androidSetupSteps(state)
    return AndroidInsights(
        readiness = androidReadiness(state, steps),
        setupSteps = steps,
        signalTips = androidSignalTips(state),
        playbook = androidPlaybookCards(state),
    )
}

private fun androidReadiness(state: AppUiState, steps: List<SetupStep>): Readiness {
    val totalWeight = steps.sumOf { it.weight }.coerceAtLeast(1)
    val earned = steps.filter { it.status == "done" || it.status == "optional" }.sumOf { it.weight }
    val score = ((earned.toDouble() / totalWeight.toDouble()) * 100.0).toInt()
    val signalScore = state.overview?.signal?.score
    val next = steps.firstOrNull { it.status != "done" && it.status != "optional" }
    val label = when {
        score >= 85 && (signalScore == null || signalScore >= 70) -> "Dialed in"
        score >= 65 -> "Operational"
        state.overview?.reachable == false -> "Needs recovery"
        else -> "Needs setup"
    }
    val summary = when {
        state.overview?.reachable == false -> "Gateway telemetry is offline. Verify Wi-Fi/LAN, host, port, and login before changing settings."
        signalScore != null && signalScore < 50 -> "Core setup is usable, but signal quality should be tuned before chasing firmware or tower changes."
        score >= 85 -> "The key setup pieces are in place. Use sweeps and snapshots to tune placement over time."
        score >= 65 -> "The control center is usable. Finish the remaining setup items to make troubleshooting easier."
        else -> "Start with gateway login, map center, and a baseline signal reading."
    }
    return Readiness(
        score = score,
        label = label,
        summary = summary,
        nextAction = next?.action ?: "Run a placement sweep and record the result.",
    )
}

private fun androidSetupSteps(state: AppUiState): List<SetupStep> {
    val signalScore = state.overview?.signal?.score
    val mapSaved = state.settings.mapLatitude != null && state.settings.mapLongitude != null
    val towerReady = state.settings.openCellIdKey.isNotBlank()
    val g4arEnabled = state.settings.advancedMode == AdvancedMode.G4arUnlockLab
    val steps = mutableListOf(
        setupStep(
            title = "Gateway login saved",
            done = state.settings.passwordConfigured,
            detail = "Save the admin password once so Wi-Fi, clients, reboot, and backup tools work without retyping it.",
            action = "Open Settings, save the gateway admin password, then press Test.",
            weight = 18,
        ),
        setupStep(
            title = "Gateway API reachable",
            done = state.overview?.reachable == true,
            detail = "The phone must be on the gateway LAN/Wi-Fi. Most stock gateways use 192.168.12.1 and port 8080.",
            action = "Join the gateway network and verify host and port in Settings.",
            weight = 16,
        ),
        setupStep(
            title = "Signal baseline captured",
            done = signalScore != null,
            detail = "A baseline lets you compare antenna direction, placement, bands, and tower changes.",
            action = "Refresh the dashboard and record RSRP, RSRQ, SINR, band, PCI, and cell ID.",
            weight = 14,
            warn = signalScore != null && signalScore < 50,
        ),
        setupStep(
            title = "Map center saved",
            done = mapSaved,
            detail = "A saved home location makes tower searches and serving-cell estimates much more useful.",
            action = "Open Map, paste coordinates, then save the map center.",
            weight = 12,
        ),
        setupStep(
            title = "Tower lookup ready",
            done = towerReady,
            detail = "OpenCellID is optional, but it unlocks nearby tower records and serving-cell map matches.",
            action = "Add an OpenCellID key in Map settings, then refresh towers.",
            weight = 10,
        ),
        setupStep(
            title = "LAN inventory loaded",
            done = state.clients.isNotEmpty(),
            detail = "Connected-device inventory helps catch unknown clients and identify which devices are stressing upload.",
            action = "Open Devices and reload clients after saving the gateway login.",
            weight = 9,
            warn = state.settings.passwordConfigured && state.clients.isEmpty(),
        ),
        setupStep(
            title = "Manual mode understood",
            done = true,
            detail = "The Android app only works while it is open. It does not run the Docker watchdog in the background.",
            action = "Use the Docker app for 24/7 monitoring and automatic recovery.",
            weight = 8,
        ),
    )
    if (g4arEnabled) {
        steps += setupStep(
            title = "G4AR stock backup saved",
            done = state.backups.isNotEmpty(),
            detail = "Owned G4AR lab work needs a local stock backup before any adapter-driven firmware research.",
            action = "Configure the local adapter URL, acknowledge the risk, then create a stock backup.",
            weight = 13,
            warn = state.settings.adapterUrl.isNotBlank() && state.backups.isEmpty(),
        )
    } else {
        steps += SetupStep(
            title = "G4AR lab disabled",
            status = "optional",
            tone = "muted",
            detail = "Advanced firmware/radio work is optional and should stay disabled on stock or leased hardware.",
            action = "Enable only for owner-controlled G4AR units with a recovery path.",
            weight = 6,
        )
    }
    return steps
}

private fun setupStep(
    title: String,
    done: Boolean,
    detail: String,
    action: String,
    weight: Int,
    warn: Boolean = false,
): SetupStep {
    return when {
        done && warn -> SetupStep(title, "warn", "warn", detail, action, weight)
        done -> SetupStep(title, "done", "good", detail, action, weight)
        else -> SetupStep(title, "todo", "warn", detail, action, weight)
    }
}

private fun androidSignalTips(state: AppUiState): List<SignalTip> {
    val metrics = state.overview?.signal?.metrics.orEmpty().associateBy { it.key.lowercase() }
    val sinr = metrics["sinr"]?.score
    val rsrp = metrics["rsrp"]?.score
    val rsrq = metrics["rsrq"]?.score
    val band = mapValue(state.overview?.connection.orEmpty(), "Band", "band").lowercase()
    val tips = mutableListOf<SignalTip>()

    if (sinr == null && rsrp == null && rsrq == null) {
        tips += SignalTip("Capture radio metrics", "Refresh after the gateway API responds. RSRP, RSRQ, SINR, band, PCI, and cell ID make every antenna move measurable.", "warn")
    }
    if (sinr != null && sinr < 70) {
        tips += SignalTip("Prioritize SINR before bars", "Rotate the gateway or directional antenna in small steps and keep the position that improves SINR without crushing RSRP.", if (sinr >= 45) "warn" else "bad")
    }
    if (rsrp != null && rsrp < 60) {
        tips += SignalTip("Improve received power", "Move the gateway higher, closer to an exterior wall/window, or aim the antenna at the best mapped cell.", if (rsrp >= 35) "warn" else "bad")
    }
    if (rsrq != null && rsrq < 55) {
        tips += SignalTip("Watch congestion and reflections", "Weak RSRQ often means noisy or loaded air. Compare another band/tower before assuming the closest site is best.", "warn")
    }
    if ("n41" in band) {
        tips += SignalTip("n41 detected", "n41 can be excellent for download. If upload or latency is weak, compare placement and LTE-anchor behavior on owned lab hardware.", "info")
    }
    if (state.towerMap.connectedTower != null) {
        tips += SignalTip("Serving tower is mapped", "Use the map line as an aiming baseline, then run a sweep after each antenna or placement change.", "good")
    }
    tips += SignalTip("Run repeatable sweeps", "Change one thing at a time, wait for the gateway to settle, then compare signal, ping, loss, and connected cell.", "info")
    return tips.take(6)
}

private fun androidPlaybookCards(state: AppUiState): List<PlaybookCard> {
    val signalScore = state.overview?.signal?.score
    return listOf(
        PlaybookCard(
            title = "Router offload mode",
            tone = if (state.wifi?.radioEnabled == false) "good" else "info",
            summary = if (state.wifi?.radioEnabled == false) {
                "Gateway Wi-Fi radios are off. Your own router can own Wi-Fi, DNS, VLANs, and SQM."
            } else {
                "Use Devices to turn gateway Wi-Fi off when an external router handles the LAN."
            },
            actions = listOf(
                "Put your router WAN behind the gateway LAN.",
                "Run DHCP, DNS, VLANs, and Wi-Fi from the router.",
                "Document double-NAT or port-forwarding limits for services.",
            ),
        ),
        PlaybookCard(
            title = "Upload and latency tuning",
            tone = if (signalScore != null && signalScore < 50) "warn" else "info",
            summary = "Use SQM/QoS on your own router to protect video calls, gaming, VPN, and remote access from upload bufferbloat.",
            actions = listOf(
                "Measure real upload at different times of day.",
                "Set SQM uplink slightly below stable upload speed.",
                "Retest ping under load after each change.",
            ),
        ),
        PlaybookCard(
            title = "Tower and antenna notebook",
            tone = if (state.towerMap.nearby.isNotEmpty()) "good" else "info",
            summary = "Track band, PCI, cell ID, SINR, RSRP, speed, and antenna direction so changes are repeatable.",
            actions = listOf(
                "Save the map center.",
                "Refresh nearby towers.",
                "Run sweeps after each antenna angle or gateway placement change.",
            ),
        ),
        PlaybookCard(
            title = "LAN inventory",
            tone = if (state.clients.isNotEmpty()) "good" else "warn",
            summary = "${state.clients.size} connected device${if (state.clients.size == 1) "" else "s"} loaded.",
            actions = listOf(
                "Reload clients after adding the gateway login.",
                "Rename important devices in your router/DNS notes.",
                "Watch for unknown clients before blaming the cellular link.",
            ),
        ),
        PlaybookCard(
            title = "Recovery discipline",
            tone = "info",
            summary = "Keep changes reversible: backup configs, record baselines, and avoid firmware work until recovery is proven.",
            actions = listOf(
                "Create a stock backup before G4AR lab work.",
                "Keep notes with antenna placement and tower IDs.",
                "Power the gateway and router from a UPS if possible.",
            ),
        ),
    )
}

private fun adapterGuideSteps(): List<String> {
    return listOf(
        "Choose the device that will physically reach the modem or gateway lab hardware.",
        "Install or build a trusted adapter service on that local device.",
        "Bind it to the LAN only and confirm GET /health works from the phone.",
        "Paste the base URL in Settings and create a stock backup before experiments.",
    )
}

private fun mapValue(values: Map<String, String>, vararg keys: String): String {
    for (key in keys) {
        val match = values.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
        if (match != null && match.value.isNotBlank()) {
            return match.value
        }
    }
    return ""
}

private fun towerMapHtml(data: TowerMapData): String {
    val markers = buildString {
        append("L.marker([${data.center.latitude}, ${data.center.longitude}], {title:'Map center'}).addTo(map).bindPopup('Map center');\n")
        data.connectedTower?.let { tower ->
            append("L.circleMarker([${tower.latitude}, ${tower.longitude}], {radius:9,color:'#e20074'}).addTo(map).bindPopup('${escapeJs(tower.label)}');\n")
        }
        data.nearby.forEach { tower ->
            append("L.circleMarker([${tower.latitude}, ${tower.longitude}], {radius:6,color:'#2563eb'}).addTo(map).bindPopup('${escapeJs(tower.label)}');\n")
        }
    }
    return """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1" />
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
          <style>html,body,#map{height:100%;margin:0;background:#101722}.leaflet-container{font-family:sans-serif}</style>
        </head>
        <body>
          <div id="map"></div>
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <script>
            const map = L.map('map', { zoomControl: true }).setView([${data.center.latitude}, ${data.center.longitude}], 13);
            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
              maxZoom: 19,
              attribution: '&copy; OpenStreetMap contributors'
            }).addTo(map);
            $markers
          </script>
        </body>
        </html>
    """.trimIndent()
}

private fun escapeJs(value: String): String {
    return value.replace("\\", "\\\\").replace("'", "\\'")
}

private fun screenPadding() = androidx.compose.foundation.layout.PaddingValues(16.dp)

@Composable
private fun TmhiTheme(content: @Composable () -> Unit) {
    val magenta = Color(0xFFE20074)
    val dark = darkColorScheme(
        primary = Color(0xFFFF6DB6),
        secondary = Color(0xFF7DD3FC),
        background = Color(0xFF0B1018),
        surface = Color(0xFF111827),
        surfaceVariant = Color(0xFF17202D),
        onPrimary = Color.White,
        onBackground = Color(0xFFE5E7EB),
        onSurface = Color(0xFFE5E7EB),
        onSurfaceVariant = Color(0xFFAAB6C5),
    )
    val light = lightColorScheme(
        primary = magenta,
        secondary = Color(0xFF2563EB),
        background = Color(0xFFF6F8FB),
        surface = Color.White,
        surfaceVariant = Color(0xFFEFF4FA),
        onPrimary = Color.White,
        onSurfaceVariant = Color(0xFF667085),
    )
    val colors = if (isSystemInDarkTheme()) dark else light
    MaterialTheme(colorScheme = colors, content = {
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    })
}
