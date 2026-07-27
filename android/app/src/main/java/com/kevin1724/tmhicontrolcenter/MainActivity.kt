package com.kevin1724.tmhicontrolcenter

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin1724.tmhicontrolcenter.core.AdvancedMode
import com.kevin1724.tmhicontrolcenter.core.AppSettings
import com.kevin1724.tmhicontrolcenter.core.ConnectedDevice
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

private enum class Screen(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Devices("Devices", Icons.Default.Devices),
    Map("Map", Icons.Default.Map),
    Lab("Lab", Icons.Default.Science),
    Settings("Profile", Icons.Default.Person),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TmhiApp(state: AppUiState, viewModel: TmhiViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message, state.error) {
        state.message?.let { snackbarHostState.showSnackbar(it) }
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 12.dp, end = 6.dp)
                            .size(38.dp),
                    )
                },
                title = {
                    Column {
                        Text(
                            screen.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            state.overview?.device?.get("Model") ?: "TMHI Control Center",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    if (state.loading || state.actionBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .width(24.dp)
                                .height(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(
                            onClick = { viewModel.refreshAll(includeNearbyTowers = false) },
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh gateway")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = if (isSystemInDarkTheme()) Color(0xFF0A0F17) else Color(0xFF111722),
                tonalElevation = 0.dp,
            ) {
                Screen.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == screen,
                        onClick = { screen = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFF472B6),
                            selectedTextColor = Color(0xFFF8FAFC),
                            indicatorColor = Color(0xFF1A2230),
                            unselectedIconColor = Color(0xFFA8B3C4),
                            unselectedTextColor = Color(0xFFA8B3C4),
                        ),
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
                Screen.Home -> DashboardScreen(state) { viewModel.refreshAll(includeNearbyTowers = false) }
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
                Screen.Lab -> HomelabScreen(
                    state = state,
                    onRefresh = { viewModel.refreshAll(includeNearbyTowers = false) },
                    onCreateBackup = viewModel::createWifiBackup,
                    onRestoreBackup = viewModel::restoreWifiBackup,
                    onDeleteBackup = viewModel::deleteWifiBackup,
                )
                Screen.Settings -> SettingsScreen(
                    state = state,
                    onSave = viewModel::updateSettings,
                    onTestLogin = viewModel::testGatewayLogin,
                    onRefresh = { viewModel.refreshAll(includeNearbyTowers = false) },
                    onReboot = viewModel::requestReboot,
                )
            }
        }
    }
}

@Composable
private fun DashboardScreen(state: AppUiState, onRefresh: () -> Unit) {
    val insights = buildAndroidInsights(state)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroStatusCard(state.overview)
        }
        item {
            MetricGrid(
                listOf(
                    "Gateway" to if (state.overview?.reachable == true) "Online" else "Offline",
                    "Signal" to (state.overview?.signal?.quality ?: "Unknown"),
                    "Score" to (state.overview?.signal?.score?.let { "$it%" } ?: "--"),
                    "Clients" to state.clients.size.toString(),
                ),
            )
        }
        item {
            SectionCard("Next Best Action", eyebrow = "SETUP COACH") {
                Text(
                    insights.readiness.nextAction,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                DetailRow("Readiness", "${insights.readiness.score}% ${insights.readiness.label}")
                MutedText("The full setup plan is available in Lab.")
            }
        }
        item {
            ExpandableSectionCard(
                title = "Connection",
                eyebrow = "CELLULAR",
                summary = compactConnectionSummary(state.overview),
            ) {
                DetailList(state.overview?.connection.orEmpty(), "No cellular details loaded yet.")
            }
        }
        item {
            ExpandableSectionCard(
                title = "Signal Metrics",
                eyebrow = "RADIO",
                summary = state.overview?.signal?.metrics?.take(2)?.joinToString(" / ") { "${it.label} ${it.value}" }
                    ?: "No radio measurements loaded",
            ) {
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
            OutlinedButton(onClick = onRefresh, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
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
    var editingWifi by rememberSaveable { mutableStateOf(false) }
    var expandedDeviceId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard("Gateway Wi-Fi", eyebrow = "NETWORK") {
                DetailRow("SSID", state.wifi?.ssid.orEmpty())
                DetailRow(
                    "Radios",
                    state.wifi?.radioEnabled?.let { if (it) "Enabled" else "Disabled" } ?: "Not reported",
                )
                TextButton(onClick = { editingWifi = !editingWifi }) {
                    Text(if (editingWifi) "Close controls" else "Edit Wi-Fi")
                    Icon(
                        if (editingWifi) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                    )
                }
                if (editingWifi) {
                    HorizontalDivider()
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
                        onClick = {
                            editingWifi = false
                            onApplyWifi(ssid, radioEnabled)
                        },
                        enabled = state.settings.passwordConfigured && !state.actionBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Apply Wi-Fi")
                    }
                }
            }
        }
        item {
            val networks = state.wifi?.networks.orEmpty()
            ExpandableSectionCard(
                title = "Wi-Fi Networks",
                eyebrow = "RECOVERY",
                summary = if (networks.isEmpty()) "No profiles loaded" else "${networks.size} profile(s) / ${state.wifi?.passwordCount ?: 0} restorable password(s)",
            ) {
                if (networks.isEmpty()) {
                    MutedText("No Wi-Fi profiles are exposed by this gateway yet.")
                } else {
                    networks.forEachIndexed { index, network ->
                        if (index > 0) HorizontalDivider()
                        Text(network.ssid, fontWeight = FontWeight.Black)
                        DetailRow("Band", network.band.ifBlank { "Not reported" })
                        DetailRow(
                            "Recovery",
                            if (network.password.isNullOrBlank()) {
                                "Name only; gateway hides the password"
                            } else {
                                "Name and password available for encrypted backup"
                            },
                        )
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Eyebrow("LAN DEVICES")
                    Text("Connected Devices", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
                IconButton(onClick = onRefresh, enabled = !state.loading) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload connected devices")
                }
            }
        }
        if (state.clients.isEmpty()) {
            item {
                EmptySurface("No connected devices reported yet.")
            }
        }
        items(state.clients, key = { it.id }) { device ->
            DeviceSurface(
                device = device,
                expanded = expandedDeviceId == device.id,
                onClick = {
                    expandedDeviceId = if (expandedDeviceId == device.id) null else device.id
                },
            )
        }
    }
}

@Composable
private fun HomelabScreen(
    state: AppUiState,
    onRefresh: () -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: (String) -> Unit,
    onDeleteBackup: (String) -> Unit,
) {
    val insights = buildAndroidInsights(state)
    var restoreId by remember { mutableStateOf<String?>(null) }
    var deleteId by remember { mutableStateOf<String?>(null) }

    restoreId?.let { id ->
        AlertDialog(
            onDismissRequest = { restoreId = null },
            title = { Text("Restore Wi-Fi configuration?") },
            text = {
                Text(
                    "This replaces matching Wi-Fi names and any restorable passwords. " +
                        "The phone may disconnect while the gateway applies the change.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        restoreId = null
                        onRestoreBackup(id)
                    },
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { restoreId = null }) { Text("Cancel") }
            },
        )
    }
    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("Delete encrypted backup?") },
            text = { Text("This removes the selected Wi-Fi backup from this phone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteId = null
                        onDeleteBackup(id)
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteId = null }) { Text("Cancel") }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard("Homelab Control Room", eyebrow = "READINESS") {
                Text(
                    "${insights.readiness.score}% ${insights.readiness.label}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(insights.readiness.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                DetailRow("Next action", insights.readiness.nextAction)
                OutlinedButton(onClick = onRefresh, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh Baseline")
                }
            }
        }
        item {
            ExpandableSectionCard(
                title = "Setup Plan",
                eyebrow = "CHECKLIST",
                summary = "${insights.setupSteps.count { it.status == "done" }} of ${insights.setupSteps.size} steps complete",
            ) {
                insights.setupSteps.forEachIndexed { index, step ->
                    if (index > 0) HorizontalDivider()
                    StepRow(step)
                }
            }
        }
        item {
            SectionCard("Encrypted Wi-Fi Vault", eyebrow = "RECOVERY") {
                Text(
                    "Backs up every Wi-Fi name and any real, unmasked password exposed by " +
                        "the authenticated gateway API.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DetailRow("Current profiles", state.wifi?.networks?.size?.toString() ?: "Not loaded")
                DetailRow("Passwords available", state.wifi?.passwordCount?.toString() ?: "Not loaded")
                Button(
                    onClick = onCreateBackup,
                    enabled = state.settings.passwordConfigured && !state.actionBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Create Encrypted Backup")
                }
                Text(
                    "Protected by Android Keystore and stored in this app's private storage. " +
                        "Clearing app data or uninstalling removes the vault.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.wifiBackups.isEmpty()) {
                    MutedText("No Wi-Fi recovery backups saved on this phone.")
                } else {
                    state.wifiBackups.forEach { backup ->
                        HorizontalDivider()
                        Text(backup.id, fontWeight = FontWeight.Bold)
                        Text(
                            "${backup.networkCount} network(s) / ${backup.passwordCount} password(s) / " +
                                backup.createdAt.replace("T", " ").take(16),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { restoreId = backup.id },
                                enabled = state.settings.passwordConfigured && !state.actionBusy,
                                modifier = Modifier.weight(1f),
                            ) { Text("Restore") }
                            TextButton(
                                onClick = { deleteId = backup.id },
                                enabled = !state.actionBusy,
                            ) { Text("Delete") }
                        }
                    }
                }
            }
        }
        item {
            ExpandableSectionCard(
                title = "Signal and Antenna Coach",
                eyebrow = "TUNING",
                summary = insights.signalTips.firstOrNull()?.title ?: "No recommendations yet",
            ) {
                insights.signalTips.forEach { tip -> InsightRow(tip.title, tip.detail, tip.tone) }
            }
        }
        item {
            ExpandableSectionCard(
                title = "Home Network Playbook",
                eyebrow = "HOMELAB",
                summary = "${insights.playbook.size} practical network guides",
            ) {
                insights.playbook.forEachIndexed { index, card ->
                    if (index > 0) HorizontalDivider()
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TonePill(card.tone.uppercase(), card.tone)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(card.title, fontWeight = FontWeight.Black)
                            Text(card.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            card.actions.forEach { action ->
                                Text("- $action", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
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
            SectionCard("Serving Cell", eyebrow = "TOWER MAP") {
                MapWebView(state.towerMap, Modifier.fillMaxWidth().height(320.dp))
                DetailRow("Radio", state.towerMap.identity.radio.ifBlank { "Unknown" })
                DetailRow("Band", state.towerMap.identity.band.ifBlank { "Unknown" })
                state.towerMap.connectedTower?.let { tower ->
                    DetailRow("Tower", tower.label)
                    DetailRow("Distance", tower.distanceKm?.let { "$it km" } ?: "Unknown")
                }
            }
        }
        item {
            ExpandableSectionCard(
                title = "Map Settings",
                eyebrow = "LOCATION",
                summary = if (state.settings.mapLatitude == null) {
                    "Add a home location to center tower searches"
                } else {
                    "${state.settings.mapLatitude}, ${state.settings.mapLongitude} / ${state.settings.mapRadiusKm} km"
                },
            ) {
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
        if (state.towerMap.nearby.isNotEmpty()) {
            item {
                ExpandableSectionCard(
                    title = "Nearby Towers",
                    eyebrow = "OPENCELLID",
                    summary = "${state.towerMap.nearby.size} result(s) near the saved center",
                ) {
                    state.towerMap.nearby.forEachIndexed { index, tower ->
                        if (index > 0) HorizontalDivider()
                        Text(tower.label, fontWeight = FontWeight.Bold)
                        Text(
                            listOfNotNull(
                                tower.radio.takeIf { it.isNotBlank() },
                                tower.distanceKm?.let { "$it km" },
                                tower.averageSignal?.let { "$it dBm" },
                            ).joinToString(" / ").ifBlank { "No additional tower details" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: AppUiState,
    onSave: (AppSettings) -> Unit,
    onTestLogin: () -> Unit,
    onRefresh: () -> Unit,
    onReboot: () -> Unit,
) {
    var host by remember(state.settings.gatewayHost) { mutableStateOf(state.settings.gatewayHost) }
    var port by remember(state.settings.gatewayPort) { mutableStateOf(state.settings.gatewayPort.toString()) }
    var username by remember(state.settings.gatewayUsername) { mutableStateOf(state.settings.gatewayUsername) }
    var password by remember(state.settings.gatewayPassword) { mutableStateOf(state.settings.gatewayPassword) }
    var labEnabled by remember(state.settings.advancedMode) { mutableStateOf(state.settings.advancedMode == AdvancedMode.G4arUnlockLab) }
    var radioProfile by remember(state.settings.radioProfile) { mutableStateOf(state.settings.radioProfile) }
    var acknowledged by remember(state.settings.advancedAcknowledged) { mutableStateOf(state.settings.advancedAcknowledged) }
    var confirmReboot by remember { mutableStateOf(false) }
    var editingLogin by rememberSaveable { mutableStateOf(!state.settings.passwordConfigured) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard("Gateway Login", eyebrow = "CONNECTION") {
                DetailRow("Address", "${state.settings.gatewayHost}:${state.settings.gatewayPort}")
                DetailRow("Status", if (state.overview?.reachable == true) "Gateway reachable" else "Not connected")
                TextButton(onClick = { editingLogin = !editingLogin }) {
                    Text(if (editingLogin) "Close login setup" else "Edit login")
                    Icon(
                        if (editingLogin) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                    )
                }
                if (editingLogin) {
                    HorizontalDivider()
                    MutedText("The admin password is encrypted with Android Keystore on this phone.")
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
                                editingLogin = false
                                onSave(state.settings.withGateway(host, port, username, password))
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("Save") }
                        OutlinedButton(onClick = onTestLogin, modifier = Modifier.weight(1f)) {
                            Text("Test")
                        }
                    }
                }
            }
        }
        item {
            ExpandableSectionCard(
                title = "G4AR Owner Lab",
                eyebrow = "ADVANCED",
                summary = if (labEnabled) "Enabled / ${radioProfile.label}" else "Disabled for stock and leased hardware",
            ) {
                Text(
                    "For owner-controlled Arcadyan TMO-G4AR units only. This records experiment intent and safety acknowledgement; the stock gateway API does not provide rooting, flashing, tower-lock, or radio-profile commands.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Warning: unofficial firmware work can permanently disable the gateway, erase calibration data, violate carrier terms, or void any remaining warranty. Keep this disabled on leased hardware.",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable lab", modifier = Modifier.weight(1f))
                    Switch(
                        checked = labEnabled,
                        onCheckedChange = {
                            labEnabled = it
                            if (!it) {
                                acknowledged = false
                            }
                        },
                    )
                }
                RadioProfilePicker(radioProfile, enabled = labEnabled) { radioProfile = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = acknowledged, onCheckedChange = { acknowledged = it && labEnabled }, enabled = labEnabled)
                    Text("I own this G4AR and accept the firmware, warranty, carrier-term, and recovery risk.")
                }
                Button(
                    onClick = {
                        onSave(
                            state.settings.withAdvanced(
                                labEnabled,
                                radioProfile,
                                acknowledged,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save Lab Settings")
                }
                MutedText("The encrypted Wi-Fi recovery vault is in Lab. It backs up settings exposed by the authenticated API; it is not a raw firmware image.")
            }
        }
        item {
            SectionCard("Manual Diagnostics", eyebrow = "GATEWAY TOOLS") {
                Text(
                    "This app runs only while it is open. Use the Docker service for scheduled checks and 24/7 monitoring.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DetailRow("Gateway", if (state.overview?.reachable == true) "Online" else "Offline")
                DetailRow("API", state.overview?.apiType ?: "Not loaded")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onRefresh,
                        enabled = !state.loading && !state.actionBusy,
                        modifier = Modifier.weight(1f),
                    ) { Text("Refresh") }
                    OutlinedButton(
                        onClick = { confirmReboot = true },
                        enabled = state.settings.passwordConfigured && !state.actionBusy,
                        modifier = Modifier.weight(1f),
                    ) { Text("Reboot") }
                }
            }
        }
        item {
            ExpandableSectionCard(
                title = "Local Security",
                eyebrow = "PRIVACY",
                summary = "Keystore encrypted / no background service",
            ) {
                DetailRow("Login", "Android Keystore encrypted")
                DetailRow("Wi-Fi vault", "Encrypted app-private storage")
                DetailRow("Background service", "Disabled")
                MutedText("Uninstalling the app or clearing its data permanently removes local credentials and Wi-Fi backups.")
            }
        }
    }

    if (confirmReboot) {
        AlertDialog(
            onDismissRequest = { confirmReboot = false },
            title = { Text("Reboot gateway?") },
            text = { Text("Internet access will be interrupted while the gateway restarts. The app will not monitor recovery in the background.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmReboot = false
                        onReboot()
                    },
                ) { Text("Reboot") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReboot = false }) { Text("Cancel") }
            },
        )
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = profile == selected,
                    onClick = { onSelected(profile) },
                    enabled = enabled,
                )
                Text(profile.label)
            }
        }
        Text(selected.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    SectionCard("Live Gateway", eyebrow = "OVERVIEW") {
        val signal = overview?.signal
        Text(
            signal?.quality ?: "Waiting for telemetry",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
        )
        Text(
            signal?.summary ?: "Tap refresh after joining the gateway Wi-Fi network.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MetricGrid(values: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.chunked(2).forEach { rowValues ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowValues.forEach { (label, value) ->
                    MetricCard(label, value, Modifier.weight(1f))
                }
                if (rowValues.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.heightIn(min = 96.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    eyebrow: String? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            eyebrow?.let { Eyebrow(it) }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            content()
        }
    }
}

@Composable
private fun ExpandableSectionCard(
    title: String,
    eyebrow: String,
    summary: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Eyebrow(eyebrow)
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) 3 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.sp,
    )
}

@Composable
private fun EmptySurface(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeviceSurface(device: ConnectedDevice, expanded: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        device.hostname,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        device.bestGuess,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (device.band.isNotBlank()) TonePill(device.band, "info")
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DetailRow("IP", device.ipAddress.ifBlank { "Unknown" })
                DetailRow("MAC", device.macAddress.ifBlank { "Unknown" })
                DetailRow(
                    "Connection",
                    listOf(device.interfaceName, device.band, device.ssid)
                        .filter { it.isNotBlank() }
                        .joinToString(" / ")
                        .ifBlank { "Unknown" },
                )
                DetailRow("Vendor", device.vendor.ifBlank { "Unknown" })
            }
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank { "Unknown" }, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MutedText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    steps += setupStep(
        title = "Wi-Fi recovery backup saved",
        done = state.wifiBackups.isNotEmpty(),
        detail = "The encrypted vault can restore Wi-Fi names and any real credentials the gateway exposes.",
        action = "Open Lab and create a phone-local encrypted Wi-Fi backup.",
        weight = 13,
        warn = state.settings.passwordConfigured && state.wifiBackups.isEmpty(),
    )
    if (g4arEnabled) {
        steps += setupStep(
            title = "G4AR owner risk acknowledged",
            done = state.settings.advancedAcknowledged,
            detail = "The owner lab records intent only; stock firmware exposes no supported root, flash, tower-lock, or radio-profile API.",
            action = "Review and acknowledge the owner-only warning in Profile.",
            weight = 7,
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
                "Create an encrypted Wi-Fi recovery backup before resetting the gateway.",
                "Keep notes with antenna placement and tower IDs.",
                "Power the gateway and router from a UPS if possible.",
            ),
        ),
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

private fun compactConnectionSummary(overview: GatewayOverview?): String {
    val connection = overview?.connection.orEmpty()
    return listOf(
        mapValue(connection, "Connection", "State"),
        mapValue(connection, "Network", "Network type"),
        mapValue(connection, "Band"),
    )
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" / ")
        .ifBlank { "No cellular connection details loaded" }
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
    val dark = darkColorScheme(
        primary = Color(0xFFF472B6),
        secondary = Color(0xFF7AA7FF),
        tertiary = Color(0xFF5EEAD4),
        error = Color(0xFFFF9B7A),
        background = Color(0xFF0D1118),
        surface = Color(0xFF151B25),
        surfaceVariant = Color(0xFF1C2431),
        onPrimary = Color.White,
        onBackground = Color(0xFFEDF2F7),
        onSurface = Color(0xFFEDF2F7),
        onSurfaceVariant = Color(0xFF9AA7BA),
        outline = Color(0xFF46546A),
        outlineVariant = Color(0xFF2D3747),
    )
    val light = lightColorScheme(
        primary = Color(0xFFE20074),
        secondary = Color(0xFF2563EB),
        tertiary = Color(0xFF0F766E),
        error = Color(0xFFC2410C),
        background = Color(0xFFF3F5F8),
        surface = Color.White,
        surfaceVariant = Color(0xFFF7F9FC),
        onPrimary = Color.White,
        onBackground = Color(0xFF141922),
        onSurface = Color(0xFF141922),
        onSurfaceVariant = Color(0xFF667385),
        outline = Color(0xFFBAC5D4),
        outlineVariant = Color(0xFFDCE3ED),
    )
    val colors = if (isSystemInDarkTheme()) dark else light
    MaterialTheme(colorScheme = colors, content = {
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    })
}
