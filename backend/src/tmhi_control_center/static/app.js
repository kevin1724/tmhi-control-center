const THEME_STORAGE_KEY = "tmhi-control-center-theme";
const VIEW_STORAGE_KEY = "tmhi-control-center-view";
const DEFAULT_VIEW = "dashboard";
const DEFAULT_MAP_CENTER = { latitude: 39.8283, longitude: -98.5795 };
const DEFAULT_MAP_RADIUS_KM = 0.8;
const MAP_TILE_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
const BUILT_IN_DOCKER_ADAPTER_URL = "http://127.0.0.1:8000";

const state = {
  config: null,
  status: null,
  overview: null,
  wifi: null,
  clients: null,
  events: [],
  firmwareBackups: null,
  usbLab: null,
  mapData: null,
  telemetryHistory: null,
  telemetryHours: 6,
  activeView: DEFAULT_VIEW,
  theme: "light",
  refreshing: false,
  mapBusy: false,
  actionBusy: false,
  gatewayLoginBusy: false,
  snapshotBusy: false,
  seriesRunning: false,
  seriesAbort: false,
  maps: {
    preview: null,
    main: null,
    previewLayer: null,
    mainLayer: null,
  },
};

const ids = [
  "actionMessage",
  "advancedCellTag",
  "adapterEndpointList",
  "adapterExampleList",
  "adapterGuideSummary",
  "adapterGuideTag",
  "adapterHowToList",
  "adapterSafetyList",
  "advancedModemAcknowledge",
  "advancedModemControlUrl",
  "advancedModemMode",
  "advancedModemStatus",
  "advancedRadioProfile",
  "advancedUploadProfile",
  "checkButton",
  "clearOpenCellIdButton",
  "clientCountTag",
  "clientTableBody",
  "connectedDetail",
  "connectedMetric",
  "connectionDetails",
  "darkModeToggle",
  "dashboardMapPreview",
  "dashboardNextAction",
  "dashboardReadinessTag",
  "dashboardSetupList",
  "deviceDetails",
  "downloadSnapshotButton",
  "dryRunMetric",
  "dryRunToggle",
  "errorBanner",
  "eventsList",
  "forceReboot",
  "forgetGatewayButton",
  "firmwareBackupButton",
  "firmwareBackupList",
  "firmwareBackupSha256",
  "firmwareBackupStatus",
  "firmwareBackupVerified",
  "firmwareConsentPhrase",
  "firmwareFlashButton",
  "firmwareLabStatus",
  "firmwareRecoveryVerified",
  "firmwareSha256",
  "firmwareUnderstandsRisk",
  "gatewayButton",
  "gatewayDetail",
  "gatewayIdentity",
  "gatewayLoginState",
  "gatewayMetric",
  "gatewayPassword",
  "gatewayReachTag",
  "gatewaySections",
  "gatewaySubtitle",
  "homelabPlaybook",
  "homelabReadinessTag",
  "homelabScore",
  "homelabSetupList",
  "homelabSignalCoach",
  "homelabSummary",
  "internetDetail",
  "internetMetric",
  "lastCheckMetric",
  "lastOnlineMetric",
  "lastRefresh",
  "lookupClientsButton",
  "mapLatitude",
  "mapLongitude",
  "mapPreviewDetails",
  "mapPreviewTag",
  "mapProviderTag",
  "mapRadius",
  "mapRefreshButton",
  "mapStatusMessage",
  "mapTowerLockTag",
  "nearbyTowerCountTag",
  "nearbyTowerTableBody",
  "openCellIdKey",
  "probeDetail",
  "probeMetric",
  "probeTableBody",
  "radioCards",
  "radioModeDetail",
  "radioModeMetric",
  "rebootButton",
  "rebootMetric",
  "refreshButton",
  "refreshClientsButton",
  "rememberPassword",
  "saveAdvancedModemButton",
  "saveGatewayButton",
  "saveMapCenterButton",
  "saveOpenCellIdButton",
  "saveSettingsButton",
  "saveWifiButton",
  "seriesCount",
  "seriesInterval",
  "seriesStartButton",
  "seriesStopButton",
  "seriesTableBody",
  "signalMeter",
  "signalMeterLabel",
  "signalMeterValue",
  "signalMetrics",
  "signalQuality",
  "signalScore",
  "signalStateTag",
  "signalSummary",
  "sinrTrendChart",
  "skipStockBackupReminder",
  "rsrpTrendChart",
  "telemetryFreshness",
  "telemetryHistoryTag",
  "temperatureDetail",
  "temperatureMetric",
  "temperatureTrendChart",
  "temperatureTrendPanel",
  "testFrequency",
  "themeModeLabel",
  "towerMap",
  "towerIdentityDetails",
  "uptimeDetail",
  "uptimeMetric",
  "usbDeviceList",
  "usbLabSummary",
  "usbLabTag",
  "usbPlatformDetails",
  "usbProbeButton",
  "usbProbeChecks",
  "useBrowserLocationButton",
  "watchdogMetric",
  "watchdogPhaseTag",
  "wifiDetails",
  "wifiRadioToggle",
  "wifiSsid",
];

const els = {};

const detailLabels = {
  api: "API",
  apn: "APN",
  band: "Band",
  broadcast_enabled: "SSID broadcast",
  built_in_adapter: "Docker adapter",
  controller: "USB controller",
  data_port: "Gateway port",
  cell_id: "Cell ID",
  channel_2g: "2.4 GHz channel",
  channel_5g: "5 GHz channel",
  clients: "Connected clients",
  band_lock: "Band lock",
  cell_lock: "Tower lock",
  cell_scan: "Cell scan",
  custom_firmware_flash: "Custom flash",
  firmware: "Firmware",
  hardware: "Hardware",
  effective_adapter: "Adapter URL",
  ethernet_target: "Ethernet target",
  lac: "TAC/LAC",
  manufacturer: "Manufacturer",
  mcc: "MCC",
  mnc: "MNC",
  model: "Model",
  name: "Name",
  network_type: "Network",
  mode: "Radio mode",
  operator: "Operator",
  pci: "PCI",
  plmn: "PLMN",
  preferred_chipset: "Preferred chipset",
  preferred_driver: "Preferred driver",
  radio: "Radio",
  radio_enabled: "Gateway Wi-Fi radios",
  radio_profile: "G4AR radio profile",
  radio_mode_override: "Radio override",
  registration: "Registration",
  roaming: "Roaming",
  lte_anchor_override: "LTE anchor / NSA",
  stock_firmware_backup: "Stock backup",
  stock_backup_skipped: "Backup reminder",
  stock_support: "Stock firmware",
  ssid: "SSID",
  ssid_2g: "2.4 GHz SSID",
  ssid_5g: "5 GHz SSID",
  state: "Connection",
  support: "Support",
  tower_accuracy: "Tower accuracy",
  tower_distance: "Tower distance",
  tower_match: "Tower match",
  tower_match_note: "Match note",
  tx_power: "Transmit power",
  usb_ceiling: "USB ceiling",
  usb_ethernet_bridge: "USB Ethernet bridge",
  usb_hardware_probe: "USB hardware probe",
  upload_priority_qos: "Upload priority",
  uptime: "Uptime",
  wan_ipv4: "WAN IPv4",
  wan_ipv6: "WAN IPv6",
};

document.addEventListener("DOMContentLoaded", () => {
  for (const id of ids) {
    els[id] = document.getElementById(id);
  }

  initializeTheme();
  bindControls();
  activateView(window.localStorage.getItem(VIEW_STORAGE_KEY) || DEFAULT_VIEW, {
    refreshMap: false,
  });
  selectTab("probes");
  refreshAll();
  window.setInterval(() => refreshAll({ quiet: true }), 30000);
});

function bindControls() {
  els.refreshButton.addEventListener("click", () => refreshAll());
  els.checkButton.addEventListener("click", runCheck);
  els.gatewayButton.addEventListener("click", runGatewayTest);
  els.saveGatewayButton.addEventListener("click", saveGatewayLogin);
  els.forgetGatewayButton.addEventListener("click", clearGatewayLogin);
  els.saveWifiButton.addEventListener("click", saveWifiSettings);
  els.mapRefreshButton.addEventListener("click", () => refreshTowerMap({ includeNearby: true }));
  els.useBrowserLocationButton.addEventListener("click", useBrowserLocation);
  els.saveMapCenterButton.addEventListener("click", saveMapCenter);
  els.saveOpenCellIdButton.addEventListener("click", saveOpenCellIdKey);
  els.clearOpenCellIdButton.addEventListener("click", clearOpenCellIdKey);
  els.refreshClientsButton.addEventListener("click", () => refreshClients(false));
  els.lookupClientsButton.addEventListener("click", () => refreshClients(true));
  els.saveAdvancedModemButton.addEventListener("click", saveAdvancedModemSettings);
  els.usbProbeButton.addEventListener("click", probeG4ARUsbPort);
  els.firmwareBackupButton.addEventListener("click", createG4ARFirmwareBackup);
  els.firmwareFlashButton.addEventListener("click", armG4ARFlashGate);
  els.downloadSnapshotButton.addEventListener("click", downloadSnapshot);
  els.saveSettingsButton.addEventListener("click", saveSettings);
  els.rebootButton.addEventListener("click", requestReboot);
  els.seriesStartButton.addEventListener("click", startSweep);
  els.seriesStopButton.addEventListener("click", stopSweep);
  els.gatewayPassword.addEventListener("input", updateControlState);
  els.wifiSsid.addEventListener("input", updateControlState);
  els.openCellIdKey.addEventListener("input", updateControlState);
  els.mapLatitude.addEventListener("input", updateControlState);
  els.mapLongitude.addEventListener("input", updateControlState);
  els.mapRadius.addEventListener("input", updateControlState);
  els.advancedModemMode.addEventListener("change", () => {
    ensureAdvancedAdapterDefault();
    updateControlState();
  });
  els.advancedModemControlUrl.addEventListener("input", updateControlState);
  els.advancedModemAcknowledge.addEventListener("change", updateControlState);
  els.advancedUploadProfile.addEventListener("change", updateControlState);
  els.advancedRadioProfile.addEventListener("change", updateControlState);
  els.skipStockBackupReminder.addEventListener("change", updateControlState);
  els.firmwareBackupSha256.addEventListener("input", updateControlState);
  els.firmwareSha256.addEventListener("input", updateControlState);
  els.firmwareConsentPhrase.addEventListener("input", updateControlState);
  els.firmwareBackupVerified.addEventListener("change", updateControlState);
  els.firmwareRecoveryVerified.addEventListener("change", updateControlState);
  els.firmwareUnderstandsRisk.addEventListener("change", updateControlState);
  els.darkModeToggle.addEventListener("change", () => {
    setTheme(els.darkModeToggle.checked ? "dark" : "light", { persist: true });
  });

  document.querySelectorAll("[data-view]").forEach((button) => {
    button.addEventListener("click", () => activateView(button.dataset.view));
    button.addEventListener("keydown", (event) => {
      if (event.key !== "Enter" && event.key !== " ") {
        return;
      }
      event.preventDefault();
      activateView(button.dataset.view);
    });
  });
  document.querySelectorAll(".tab").forEach((button) => {
    button.addEventListener("click", () => selectTab(button.dataset.tab));
  });
  document.querySelectorAll("[data-telemetry-hours]").forEach((button) => {
    button.addEventListener("click", () => {
      const hours = Number(button.dataset.telemetryHours);
      if (Number.isFinite(hours)) {
        refreshTelemetryHistory(hours);
      }
    });
  });
}

function initializeTheme() {
  const storedTheme = window.localStorage.getItem(THEME_STORAGE_KEY);
  const prefersDark = window.matchMedia?.("(prefers-color-scheme: dark)").matches;
  setTheme(storedTheme || (prefersDark ? "dark" : "light"), { persist: false });
}

function setTheme(theme, { persist = false } = {}) {
  state.theme = theme === "dark" ? "dark" : "light";
  document.documentElement.dataset.theme = state.theme;
  document.documentElement.style.colorScheme = state.theme;
  if (els.darkModeToggle) {
    els.darkModeToggle.checked = state.theme === "dark";
  }
  if (els.themeModeLabel) {
    setText(els.themeModeLabel, state.theme === "dark" ? "Dark mode" : "Light mode");
  }
  if (persist) {
    window.localStorage.setItem(THEME_STORAGE_KEY, state.theme);
  }
}

function selectView(name) {
  const requestedView = String(name || DEFAULT_VIEW);
  const availableViews = new Set(
    Array.from(document.querySelectorAll("[data-view-panel]")).map((panel) => panel.dataset.viewPanel)
  );
  state.activeView = availableViews.has(requestedView) ? requestedView : DEFAULT_VIEW;
  document.querySelectorAll(".nav-item[data-view]").forEach((button) => {
    button.classList.toggle("is-active", button.dataset.view === state.activeView);
  });
  document.querySelectorAll("[data-view-panel]").forEach((panel) => {
    panel.classList.toggle("is-active", panel.dataset.viewPanel === state.activeView);
  });
  window.localStorage.setItem(VIEW_STORAGE_KEY, state.activeView);
}

function activateView(name, { refreshMap = true } = {}) {
  selectView(name);
  if (state.activeView === "map") {
    scheduleMainMapRender();
    if (refreshMap) {
      window.setTimeout(() => {
        refreshTowerMap({ includeNearby: true, quiet: true });
      }, 80);
    }
  }
}

async function refreshAll({ quiet = false } = {}) {
  if (state.refreshing) {
    return;
  }
  state.refreshing = true;
  updateControlState();
  if (!quiet) {
    hideError();
  }

  const results = await Promise.allSettled([
    api("/api/config"),
    api("/api/status"),
    api("/api/gateway/overview"),
    api("/api/gateway/wifi"),
    api("/api/gateway/clients"),
    api("/api/gateway/map?include_nearby=false"),
    api("/api/events?limit=10"),
    api("/api/g4ar/firmware/backups"),
    api(`/api/gateway/telemetry/history?hours=${state.telemetryHours}`),
    api("/api/g4ar/usb/status"),
  ]);

  const errors = [];
  if (results[0].status === "fulfilled") {
    state.config = results[0].value;
  } else {
    errors.push(`Config: ${results[0].reason.message}`);
  }
  if (results[1].status === "fulfilled") {
    state.status = results[1].value;
  } else {
    errors.push(`Status: ${results[1].reason.message}`);
  }
  if (results[2].status === "fulfilled") {
    state.overview = results[2].value;
  } else {
    errors.push(`Gateway: ${results[2].reason.message}`);
  }
  if (results[3].status === "fulfilled") {
    state.wifi = results[3].value;
  } else {
    errors.push(`Wi-Fi: ${results[3].reason.message}`);
  }
  if (results[4].status === "fulfilled") {
    state.clients = results[4].value;
  } else {
    errors.push(`Clients: ${results[4].reason.message}`);
  }
  if (results[5].status === "fulfilled") {
    state.mapData = results[5].value;
  } else {
    errors.push(`Map: ${results[5].reason.message}`);
  }
  if (results[6].status === "fulfilled") {
    state.events = results[6].value;
  } else {
    errors.push(`Events: ${results[6].reason.message}`);
  }
  if (results[7].status === "fulfilled") {
    state.firmwareBackups = results[7].value;
  } else {
    errors.push(`Backups: ${results[7].reason.message}`);
  }
  if (results[8].status === "fulfilled") {
    state.telemetryHistory = results[8].value;
  } else {
    errors.push(`History: ${results[8].reason.message}`);
  }
  if (results[9].status === "fulfilled") {
    const previousProbe = state.usbLab?.probe || null;
    state.usbLab = {
      ...results[9].value,
      probe: previousProbe,
    };
  } else {
    errors.push(`USB lab: ${results[9].reason.message}`);
  }

  renderAll();
  setText(els.lastRefresh, `Updated ${formatTime(new Date())}`);
  if (errors.length) {
    showError(errors.join(" | "));
  }
  state.refreshing = false;
  updateControlState();
}

async function refreshTelemetryHistory(hours) {
  state.telemetryHours = Math.max(1, Math.min(336, Number(hours) || 6));
  renderTelemetryTrends();
  try {
    state.telemetryHistory = await api(
      `/api/gateway/telemetry/history?hours=${state.telemetryHours}`
    );
    renderTelemetryTrends();
  } catch (error) {
    showError(`Telemetry history: ${error.message}`);
  }
}

async function downloadSnapshot() {
  if (state.snapshotBusy) {
    return;
  }

  state.snapshotBusy = true;
  updateControlState();
  setActionMessage("Building redacted homelab snapshot.", "");
  hideError();

  try {
    const snapshot = await api("/api/homelab/snapshot?include_nearby=false");
    const serialized = JSON.stringify(snapshot, null, 2);
    const blob = new Blob([serialized], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `tmhi-control-center-snapshot-${new Date()
      .toISOString()
      .replace(/[:.]/g, "-")}.json`;
    document.body.append(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
    setActionMessage("Snapshot downloaded.", "success");
  } catch (error) {
    setActionMessage(error.message, "error");
  } finally {
    state.snapshotBusy = false;
    updateControlState();
  }
}

async function api(path, options = {}) {
  const init = {
    method: options.method || "GET",
    headers: {},
  };
  if (options.body !== undefined) {
    init.headers["Content-Type"] = "application/json";
    init.body = JSON.stringify(options.body);
  }

  const response = await fetch(path, init);
  const text = await response.text();
  let payload = null;
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      payload = text;
    }
  }

  if (!response.ok) {
    throw new Error(readError(payload, response.status));
  }
  return payload;
}

function readError(payload, status) {
  if (payload && typeof payload === "object" && payload.detail) {
    return payload.detail;
  }
  if (typeof payload === "string" && payload.trim()) {
    return payload.trim();
  }
  return `HTTP ${status}`;
}

function renderAll() {
  renderHeader();
  renderOverviewMetrics();
  renderRadioStack();
  renderTelemetryTrends();
  renderSignal();
  renderDetails();
  renderControls();
  renderClients();
  renderHomelabInsights();
  renderTowerMapSummary();
  renderProbes();
  renderEvents();
  renderGatewaySections();
}

function renderHeader() {
  const detection = state.overview?.detection || {};
  const device = state.overview?.device || {};
  const connection = state.overview?.connection || {};
  const title = compactJoin([device.manufacturer, device.model]) || device.name;
  const network = compactJoin([connection.network_type, connection.band], " / ");

  setText(els.gatewayIdentity, title || "Gateway dashboard");
  setText(
    els.gatewaySubtitle,
    compactJoin([device.name, network, formatDate(state.overview?.observed_at)], " - ") ||
      "Waiting for gateway telemetry"
  );

  if (detection.reachable === true) {
    setTag(
      els.gatewayReachTag,
      detection.supported === false ? "Detected, limited" : "Gateway online",
      detection.supported === false ? "warn" : "good"
    );
  } else if (detection.reachable === false) {
    setTag(els.gatewayReachTag, "Gateway offline", "bad");
  } else {
    setTag(els.gatewayReachTag, "Detecting", "muted");
  }
}

function renderOverviewMetrics() {
  const status = state.status || {};
  const overview = state.overview || {};
  const detection = overview.detection || {};
  const signal = overview.signal || {};
  const connection = overview.connection || {};
  const system = overview.system || {};

  setText(els.internetMetric, booleanLabel(status.internet_online, "Online", "Offline"));
  setTone(els.internetMetric, toneFromBoolean(status.internet_online));
  setText(els.internetDetail, status.last_error || phaseText(status.phase) || "No check yet");

  const gatewayLabel =
    detection.model ||
    status.gateway_model ||
    detection.name ||
    status.gateway_name ||
    booleanLabel(detection.reachable ?? status.gateway_reachable, "Reachable", "Not found");
  setText(els.gatewayMetric, gatewayLabel);
  setTone(els.gatewayMetric, toneFromBoolean(detection.reachable ?? status.gateway_reachable));
  setText(els.gatewayDetail, detection.api_type || status.gateway_api_type || "Local API");

  const score = signal.score;
  setText(els.signalScore, Number.isFinite(score) ? `${score}%` : "--");
  setTone(els.signalScore, toneFromQuality(signal.quality));
  setText(els.signalQuality, signal.quality || "Unknown");

  const mode = connection.mode || connection.network_type || "Unknown";
  setText(els.radioModeMetric, mode);
  setTone(els.radioModeMetric, mode === "Unknown" ? "muted" : "info");
  setText(
    els.radioModeDetail,
    compactJoin([connection.operator, connection.band]) || "Cellular link"
  );

  const temperature = system.temperature;
  setText(els.temperatureMetric, temperature?.display || "--");
  setTone(els.temperatureMetric, thermalTone(temperature?.celsius));
  setText(
    els.temperatureDetail,
    temperature ? `${formatValue(temperature.fahrenheit)} F` : "Not exposed by firmware"
  );

  setText(els.uptimeMetric, system.uptime || connection.uptime || "--");
  setTone(els.uptimeMetric, system.uptime || connection.uptime ? "good" : "muted");
  setText(els.uptimeDetail, system.update_state ? `Update ${system.update_state}` : "Gateway runtime");

  const clientCount = state.clients?.count ?? state.clients?.devices?.length ?? 0;
  setText(els.connectedMetric, String(clientCount));
  setTone(els.connectedMetric, clientCount ? "info" : "muted");
  setText(els.connectedDetail, clientCount === 1 ? "Connected client" : "Connected clients");

  const probes = `${status.successful_probes || 0} / ${status.total_probes || 0}`;
  setText(els.probeMetric, probes);
  setTone(
    els.probeMetric,
    status.total_probes && status.successful_probes >= status.total_probes ? "good" : "warn"
  );
  setText(els.probeDetail, `${state.config?.minimum_successful_probes || 0} required`);

  setText(els.dryRunMetric, status.dry_run ? "Dry run" : "Live");
  setTone(els.dryRunMetric, status.dry_run ? "warn" : "good");
  setText(els.watchdogMetric, status.watchdog_enabled ? "Watchdog enabled" : "Watchdog off");

  setText(
    els.lastCheckMetric,
    status.last_check_at ? formatTime(new Date(status.last_check_at)) : "Never"
  );
  setText(els.lastOnlineMetric, status.last_online_at ? `Online ${formatDate(status.last_online_at)}` : "Online history");

  setText(els.rebootMetric, String(status.reboot_count_24h || 0));
  setTone(els.rebootMetric, status.reboot_count_24h ? "warn" : "muted");

  setTag(els.watchdogPhaseTag, phaseText(status.phase) || "Initializing", toneFromPhase(status.phase));
}

function renderHomelabInsights() {
  const insights = buildUiInsights();
  const readiness = insights.readiness;
  const readinessLabel = `${readiness.score}% ${readiness.label}`;
  const readinessTone = toneFromReadiness(readiness);

  setTag(els.dashboardReadinessTag, readinessLabel, readinessTone);
  setTag(els.homelabReadinessTag, readinessLabel, readinessTone);
  setText(els.dashboardNextAction, readiness.next_best_action);
  setText(els.homelabScore, `${readiness.score}%`);
  setText(els.homelabSummary, readiness.summary);

  renderChecklist(els.dashboardSetupList, insights.setup_steps.slice(0, 4), { compact: true });
  renderChecklist(els.homelabSetupList, insights.setup_steps, { compact: false });
  renderInsightList(els.homelabSignalCoach, insights.signal_coach);
  renderPlaybook(els.homelabPlaybook, insights.homelab_cards);

  const guide = insights.adapter_guide;
  setTag(
    els.adapterGuideTag,
    guide.status === "ready" ? "Adapter configured" : "Setup needed",
    guide.status === "ready" ? "good" : "warn"
  );
  setText(els.adapterGuideSummary, guide.summary);
  renderOrderedList(els.adapterHowToList, guide.how_to_get_one);
  renderCodeChips(els.adapterExampleList, guide.examples);
  renderCodeChips(els.adapterEndpointList, guide.required_endpoints);
  renderInsightList(
    els.adapterSafetyList,
    guide.safety.map((detail) => ({
      title: "Safety check",
      detail,
      tone: "warn",
    }))
  );
}

function buildUiInsights() {
  const setupSteps = buildSetupSteps();
  return {
    readiness: buildReadiness(setupSteps),
    setup_steps: setupSteps,
    signal_coach: buildSignalCoach(),
    homelab_cards: buildHomelabCards(),
    adapter_guide: buildAdapterGuide(),
  };
}

function buildReadiness(setupSteps) {
  const totalWeight = setupSteps.reduce((sum, step) => sum + step.weight, 0) || 1;
  const earned = setupSteps
    .filter((step) => step.status === "done" || step.status === "optional")
    .reduce((sum, step) => sum + step.weight, 0);
  const score = Math.round((earned / totalWeight) * 100);
  const next = setupSteps.find((step) => step.status !== "done" && step.status !== "optional");
  const signalScore = numericScore(state.overview?.signal?.score);
  const online = state.status?.internet_online;
  let label = "Needs setup";
  if (score >= 85 && (!Number.isFinite(signalScore) || signalScore >= 70)) {
    label = "Dialed in";
  } else if (score >= 65) {
    label = "Operational";
  } else if (online === false) {
    label = "Needs recovery";
  }

  let summary = "Start with gateway login, map center, and a baseline signal reading.";
  if (online === false) {
    summary = "Internet probes are failing. Check gateway reachability, then run a manual check before rebooting.";
  } else if (Number.isFinite(signalScore) && signalScore < 50) {
    summary = "Core setup is usable, but signal quality should be tuned before chasing firmware or tower changes.";
  } else if (score >= 85) {
    summary = "The key setup pieces are in place. Use sweeps and snapshots to tune placement over time.";
  } else if (score >= 65) {
    summary = "The control center is usable. Finish the remaining setup items to make troubleshooting easier.";
  }

  return {
    score,
    label,
    summary,
    next_best_action: next ? next.action : "Run a placement sweep and save the snapshot.",
  };
}

function buildSetupSteps() {
  const advanced = state.config?.advanced_modem || {};
  const mapConfig = state.config?.map || {};
  const detection = state.overview?.detection || {};
  const provider = state.mapData?.provider || {};
  const center = state.mapData?.map?.center || {};
  const clients = state.clients?.devices || [];
  const clientCount = state.clients?.count ?? clients.length;
  const backupCount = Array.isArray(state.firmwareBackups?.backups)
    ? state.firmwareBackups.backups.length
    : 0;
  const signalScore = numericScore(state.overview?.signal?.score);
  const g4arEnabled = isG4ARLabMode(advanced.mode);
  const skipStockBackup = Boolean(advanced.skip_stock_backup);

  const steps = [
    setupStep(
      "gateway-login",
      "Gateway login saved",
      Boolean(state.config?.gateway_password_configured),
      "Save the admin password once so Wi-Fi, clients, reboot, and backup tools work without retyping it.",
      "Open Settings, save the gateway admin password, then press Test.",
      18
    ),
    setupStep(
      "gateway-api",
      "Gateway API reachable",
      detection.reachable === true || state.status?.gateway_reachable === true,
      "The app needs the local gateway API, usually 192.168.12.1 on port 8080.",
      "Join the gateway LAN/Wi-Fi and verify the gateway host and port in Settings.",
      16
    ),
    setupStep(
      "signal-baseline",
      "Signal baseline captured",
      Number.isFinite(signalScore),
      "A baseline lets you compare antenna direction, placement, bands, and tower changes.",
      "Refresh the dashboard and record RSRP, RSRQ, SINR, band, PCI, and cell ID.",
      14,
      { warn: Number.isFinite(signalScore) && signalScore < 50 }
    ),
    setupStep(
      "map-center",
      "Map center saved",
      mapConfig.latitude !== null && mapConfig.latitude !== undefined && mapConfig.longitude !== null && mapConfig.longitude !== undefined,
      "A saved home location makes tower searches and serving-cell estimates much more useful.",
      "Open Map, use browser location or paste coordinates, then save the map center.",
      12,
      { warn: center.source === "public_ip" }
    ),
    setupStep(
      "tower-provider",
      "Tower lookup ready",
      Boolean(provider.configured || mapConfig.opencellid_configured),
      "OpenCellID is optional, but it unlocks nearby tower records and serving-cell map matches.",
      "Add an OpenCellID key in Settings, then refresh towers on the Map page.",
      10
    ),
    setupStep(
      "lan-inventory",
      "LAN inventory loaded",
      clientCount > 0,
      "Connected-device inventory helps catch unknown clients and identify which devices are stressing upload.",
      "Open Devices and run Reverse Lookup after saving the gateway login.",
      9,
      { warn: Boolean(state.config?.gateway_password_configured) && clientCount === 0 }
    ),
    setupStep(
      "watchdog-policy",
      "Watchdog policy reviewed",
      Boolean(state.status?.dry_run) || Boolean(state.config?.gateway_password_configured),
      "Dry Run keeps reboot automation safe until the gateway login and recovery behavior have been tested.",
      "Keep Dry Run on until manual checks and reboot recovery look predictable.",
      8
    ),
  ];

  if (g4arEnabled && backupCount === 0 && skipStockBackup) {
    steps.push({
      id: "g4ar-backup",
      title: "G4AR stock backup skipped for now",
      status: "skipped",
      tone: "warn",
      detail:
        "The setup reminder is suppressed, but firmware override stays locked until backup, recovery, and hashes are verified.",
      action: "Create a stock backup later before any firmware or radio-profile experiment.",
      weight: 13,
    });
  } else if (g4arEnabled) {
    steps.push(
      setupStep(
        "g4ar-backup",
        "G4AR stock backup saved",
        backupCount > 0,
        "Owned G4AR lab work needs a local stock backup before any adapter-driven firmware research.",
        "Configure the local adapter URL, acknowledge the risk, then create a stock backup.",
        13,
        { warn: Boolean(advanced.control_url_configured) && backupCount === 0 }
      )
    );
  } else {
    steps.push({
      id: "g4ar-lab",
      title: "G4AR lab disabled",
      status: "optional",
      tone: "muted",
      detail: "Advanced firmware/radio work is optional and should stay disabled on stock or leased hardware.",
      action: "Enable only for owner-controlled G4AR units with a recovery path.",
      weight: 6,
    });
  }

  return steps;
}

function setupStep(id, title, done, detail, action, weight, { warn = false } = {}) {
  let status = "todo";
  let tone = "warn";
  if (done && warn) {
    status = "warn";
    tone = "warn";
  } else if (done) {
    status = "done";
    tone = "good";
  }
  return { id, title, status, tone, detail, action, weight };
}

function buildSignalCoach() {
  const signal = state.overview?.signal || {};
  const connection = state.overview?.connection || {};
  const metrics = Array.isArray(signal.metrics) ? signal.metrics : [];
  const byKey = Object.fromEntries(metrics.map((metric) => [String(metric.key || "").toLowerCase(), metric]));
  const tips = [];
  const sinr = numericScore(byKey.sinr?.score);
  const rsrp = numericScore(byKey.rsrp?.score);
  const rsrq = numericScore(byKey.rsrq?.score);
  const band = String(connection.band || "").toLowerCase();

  if (!Number.isFinite(sinr) && !Number.isFinite(rsrp) && !Number.isFinite(rsrq)) {
    tips.push(tip("Capture radio metrics", "Refresh after the gateway API responds. RSRP, RSRQ, SINR, band, PCI, and cell ID make every antenna move measurable.", "warn"));
  }
  if (Number.isFinite(sinr) && sinr < 70) {
    tips.push(tip("Prioritize SINR before bars", "Rotate the gateway or directional antenna in small steps and keep the position that improves SINR without crushing RSRP.", sinr >= 45 ? "warn" : "bad"));
  }
  if (Number.isFinite(rsrp) && rsrp < 60) {
    tips.push(tip("Improve received power", "Move the gateway higher, closer to an exterior wall/window, or aim the antenna at the best mapped cell.", rsrp >= 35 ? "warn" : "bad"));
  }
  if (Number.isFinite(rsrq) && rsrq < 55) {
    tips.push(tip("Watch congestion and reflections", "Weak RSRQ often means noisy or loaded air. Compare another band/tower before assuming the closest site is best.", "warn"));
  }
  if (band.includes("n41")) {
    tips.push(tip("n41 detected", "n41 can be excellent for download. If upload or latency is weak, compare placement and LTE-anchor behavior on owned lab hardware.", "info"));
  }
  if (state.mapData?.connected?.location) {
    tips.push(tip("Serving tower is mapped", "Use the map line as an aiming baseline, then run a sweep after each antenna or placement change.", "good"));
  }
  tips.push(tip("Run repeatable sweeps", "Change one thing at a time, wait for the gateway to settle, then compare signal, ping, loss, and connected cell.", "info"));
  return tips.slice(0, 6);
}

function buildHomelabCards() {
  const clients = state.clients?.devices || [];
  const clientCount = state.clients?.count ?? clients.length;
  const signalScore = numericScore(state.overview?.signal?.score);
  const radioEnabled = state.wifi?.radio_enabled;
  const provider = state.mapData?.provider || {};
  return [
    {
      title: "Router offload mode",
      tone: radioEnabled === false ? "good" : "info",
      summary:
        radioEnabled === false
          ? "Gateway Wi-Fi radios are off. Your own router can own Wi-Fi, DNS, VLANs, and SQM."
          : "Use Devices to turn gateway Wi-Fi off when an external router handles the LAN.",
      actions: [
        "Put your router WAN behind the gateway LAN.",
        "Run DHCP, DNS, VLANs, and Wi-Fi from the router.",
        "Document double-NAT or port-forwarding limits for services.",
      ],
    },
    {
      title: "Upload and latency tuning",
      tone: Number.isFinite(signalScore) && signalScore < 50 ? "warn" : "info",
      summary: "Use SQM/QoS on your own router to protect video calls, gaming, VPN, and remote access from upload bufferbloat.",
      actions: [
        "Measure real upload at different times of day.",
        "Set SQM uplink slightly below stable upload speed.",
        "Retest ping under load after each change.",
      ],
    },
    {
      title: "Tower and antenna notebook",
      tone: provider.nearby_loaded ? "good" : "info",
      summary: "Track band, PCI, cell ID, SINR, RSRP, speed, and antenna direction so changes are repeatable.",
      actions: [
        "Save the map center.",
        "Refresh nearby towers.",
        "Run sweeps after each antenna angle or gateway placement change.",
      ],
    },
    {
      title: "LAN inventory",
      tone: clientCount ? "good" : "warn",
      summary: `${clientCount} connected device${clientCount === 1 ? "" : "s"} loaded.`,
      actions: [
        "Run reverse lookup after adding the gateway login.",
        "Rename important devices in your router/DNS notes.",
        "Watch for unknown clients before blaming the cellular link.",
      ],
    },
    {
      title: "Recovery discipline",
      tone: state.status?.dry_run ? "warn" : "good",
      summary: "Keep changes reversible: backup configs, export snapshots, and avoid live reboot automation until recovery is proven.",
      actions: [
        "Download a snapshot before lab changes.",
        "Keep Dry Run on during first setup.",
        "Power the gateway and router from a UPS if possible.",
      ],
    },
    {
      title: "Event context",
      tone: "info",
      summary: `${state.events.length} recent event${state.events.length === 1 ? "" : "s"} in the local log.`,
      actions: [
        "Compare outages with weather, load, tower changes, and router logs.",
        "Keep snapshots with antenna placement notes.",
      ],
    },
  ];
}

function buildAdapterGuide() {
  const advanced = state.config?.advanced_modem || {};
  const adapterReady = Boolean(advanced.enabled && advanced.control_url_configured);
  const defaultUrl = advanced.default_control_url || BUILT_IN_DOCKER_ADAPTER_URL;
  return {
    status: adapterReady ? "ready" : "setup_needed",
    summary:
      "Docker can use its built-in local adapter URL automatically. Real firmware backup, cell scan, tower lock, or radio-profile changes still need a hardware-specific bridge.",
    how_to_get_one: [
      "Leave the field blank to use the built-in Docker adapter URL.",
      "For real modem commands, choose the device that will physically reach the modem or gateway lab hardware.",
      "Install or build a trusted adapter service on that local device.",
      "Bind it to the LAN only, confirm its health endpoint, then paste its base URL only if it is different from the Docker default.",
      "Create a stock backup before any firmware or radio-profile experiment.",
    ],
    examples: [
      defaultUrl,
      "http://router.local:8080",
      "http://192.168.1.2:8765",
      "http://rooter.lan:8080",
    ],
    required_endpoints: [
      "GET /health",
      "POST /g4ar/firmware/backup",
      "POST /modem/radio/profile",
      "POST /modem/cell/scan",
      "POST /modem/lock",
    ],
    safety: [
      "Do not expose the adapter to the public internet.",
      "Do not paste random firmware URLs into the adapter.",
      "Do not continue without a stock backup, SHA-256 hashes, and a tested recovery path.",
      "Transmit-power override is intentionally unsupported.",
    ],
  };
}

function renderChecklist(container, steps, { compact }) {
  replaceChildren(container);
  if (!steps.length) {
    container.append(emptyNode("No setup items are available yet."));
    return;
  }
  for (const step of steps) {
    const item = document.createElement("article");
    item.className = `check-item check-item--${step.tone || "muted"}`;

    const status = document.createElement("span");
    status.className = "check-status";
    status.textContent = step.status === "todo" ? "Next" : humanize(step.status);

    const body = document.createElement("div");
    const title = document.createElement("strong");
    title.textContent = step.title;
    const detail = document.createElement("p");
    detail.textContent = step.detail;
    body.append(title, detail);
    if (!compact || step.status !== "done") {
      const action = document.createElement("small");
      action.textContent = step.action;
      body.append(action);
    }

    item.append(status, body);
    container.append(item);
  }
}

function renderInsightList(container, insights) {
  replaceChildren(container);
  if (!insights.length) {
    container.append(emptyNode("No recommendations yet."));
    return;
  }
  for (const insight of insights) {
    const item = document.createElement("article");
    item.className = `insight-card insight-card--${insight.tone || "info"}`;
    const title = document.createElement("strong");
    title.textContent = insight.title;
    const detail = document.createElement("p");
    detail.textContent = insight.detail;
    item.append(title, detail);
    container.append(item);
  }
}

function renderPlaybook(container, cards) {
  replaceChildren(container);
  if (!cards.length) {
    container.append(emptyNode("No playbook items yet."));
    return;
  }
  for (const card of cards) {
    const item = document.createElement("article");
    item.className = `playbook-card playbook-card--${card.tone || "info"}`;
    const title = document.createElement("strong");
    title.textContent = card.title;
    const summary = document.createElement("p");
    summary.textContent = card.summary;
    const list = document.createElement("ul");
    for (const action of card.actions || []) {
      const row = document.createElement("li");
      row.textContent = action;
      list.append(row);
    }
    item.append(title, summary, list);
    container.append(item);
  }
}

function renderOrderedList(container, values) {
  replaceChildren(container);
  for (const value of values || []) {
    const item = document.createElement("li");
    item.textContent = value;
    container.append(item);
  }
}

function renderCodeChips(container, values) {
  replaceChildren(container);
  for (const value of values || []) {
    const chip = document.createElement("code");
    chip.textContent = value;
    container.append(chip);
  }
}

function tip(title, detail, tone) {
  return { title, detail, tone };
}

function toneFromReadiness(readiness) {
  if (readiness.label === "Dialed in") {
    return "good";
  }
  if (readiness.label === "Operational") {
    return "info";
  }
  if (readiness.label === "Needs recovery") {
    return "bad";
  }
  return "warn";
}

function renderRadioStack() {
  const radios = Array.isArray(state.overview?.radios) ? state.overview.radios : [];
  const advanced = Boolean(state.overview?.telemetry?.advanced_cell_available);
  setTag(
    els.advancedCellTag,
    advanced ? "Advanced cell data" : "Basic telemetry",
    advanced ? "good" : "muted"
  );
  setText(
    els.telemetryFreshness,
    state.overview?.observed_at ? `Sampled ${formatDate(state.overview.observed_at)}` : "Waiting for data"
  );

  replaceChildren(els.radioCards);
  if (!radios.length) {
    els.radioCards.append(emptyNode("No LTE or 5G radio blocks were returned by this firmware."));
    return;
  }

  for (const radio of radios) {
    const card = document.createElement("article");
    card.className = `radio-status-card radio-status-card--${radio.key || "unknown"}`;

    const header = document.createElement("div");
    header.className = "radio-card-header";
    const titleGroup = document.createElement("div");
    const eyebrow = document.createElement("span");
    eyebrow.textContent = radio.active === false ? "Not registered" : "Active radio";
    const title = document.createElement("h3");
    title.textContent = radio.label || humanize(radio.key);
    titleGroup.append(eyebrow, title);
    const quality = document.createElement("span");
    quality.className = `tag tag--${toneFromQuality(radio.quality)}`;
    quality.textContent = Number.isFinite(radio.score)
      ? `${radio.score}% ${radio.quality || ""}`.trim()
      : radio.active === false
        ? "Idle"
        : "Connected";
    header.append(titleGroup, quality);

    const metricGrid = document.createElement("div");
    metricGrid.className = "radio-metric-grid";
    const metrics = Array.isArray(radio.metrics) ? radio.metrics : [];
    for (const metric of metrics) {
      const metricNode = document.createElement("div");
      metricNode.className = `radio-metric radio-metric--${toneFromQuality(metric.rating)}`;
      const label = document.createElement("span");
      label.textContent = metric.label || humanize(metric.key);
      const value = document.createElement("strong");
      value.textContent = metric.display || formatValue(metric.value);
      metricNode.append(label, value);
      metricGrid.append(metricNode);
    }

    const facts = document.createElement("dl");
    facts.className = "radio-fact-grid";
    const cell = radio.cell && typeof radio.cell === "object" ? radio.cell : {};
    const factEntries = [
      ["Band", cell.band],
      ["Bandwidth", cell.bandwidth],
      ["Antenna", radio.antenna],
      ["PCI", cell.pci],
      [cell.arfcn_label || "Channel", cell.arfcn],
      ["Cell ID", cell.cell_id],
      [cell.node_label || "Node ID", cell.node_id],
      ["TAC", cell.tac],
    ].filter(([, value]) => hasDisplayValue(value));
    for (const [labelText, valueText] of factEntries) {
      const item = document.createElement("div");
      const term = document.createElement("dt");
      term.textContent = labelText;
      const value = document.createElement("dd");
      value.textContent = formatValue(valueText);
      item.append(term, value);
      facts.append(item);
    }

    if (!metrics.length) {
      metricGrid.append(emptyNode("Signal measurements are not exposed for this radio."));
    }
    card.append(header, metricGrid);
    if (factEntries.length) {
      card.append(facts);
    }
    els.radioCards.append(card);
  }
}

function renderTelemetryTrends() {
  const points = telemetryPointsWithCurrent();
  const storedCount = Number(state.telemetryHistory?.count || 0);
  setTag(
    els.telemetryHistoryTag,
    storedCount ? `${storedCount} samples / ${formatHistoryRange(state.telemetryHours)}` : "Collecting",
    storedCount >= 2 ? "good" : "muted"
  );
  document.querySelectorAll("[data-telemetry-hours]").forEach((button) => {
    button.classList.toggle("is-active", Number(button.dataset.telemetryHours) === state.telemetryHours);
  });

  renderLineChart(
    els.rsrpTrendChart,
    points,
    [
      { key: "lte", label: "4G LTE", className: "chart-series--lte", read: (point) => point.radios?.lte?.metrics?.rsrp },
      { key: "nr", label: "5G NR", className: "chart-series--nr", read: (point) => point.radios?.nr?.metrics?.rsrp },
    ],
    { unit: "dBm", decimals: 0, emptyText: "RSRP history will appear after two gateway samples." }
  );
  renderLineChart(
    els.sinrTrendChart,
    points,
    [
      { key: "lte", label: "4G LTE", className: "chart-series--lte", read: (point) => point.radios?.lte?.metrics?.sinr },
      { key: "nr", label: "5G NR", className: "chart-series--nr", read: (point) => point.radios?.nr?.metrics?.sinr },
    ],
    { unit: "dB", decimals: 0, emptyText: "SINR history will appear when the gateway exposes it." }
  );
  renderLineChart(
    els.temperatureTrendChart,
    points,
    [
      { key: "temperature", label: "Gateway", className: "chart-series--thermal", read: (point) => point.system?.temperature_c },
    ],
    { unit: "C", decimals: 1, emptyText: "This firmware does not expose an internal temperature sensor." }
  );
  els.temperatureTrendPanel.classList.toggle(
    "is-unavailable",
    !points.some((point) => isFiniteReading(point.system?.temperature_c))
  );
}

function telemetryPointsWithCurrent() {
  const stored = Array.isArray(state.telemetryHistory?.points)
    ? state.telemetryHistory.points.slice()
    : [];
  const current = currentTelemetryPoint();
  if (current) {
    const currentTime = new Date(current.observed_at).getTime();
    const lastTime = stored.length ? new Date(stored[stored.length - 1].observed_at).getTime() : NaN;
    if (!Number.isFinite(lastTime) || Math.abs(currentTime - lastTime) > 1000) {
      stored.push(current);
    }
  }
  return stored
    .filter((point) => Number.isFinite(new Date(point.observed_at).getTime()))
    .sort((left, right) => new Date(left.observed_at) - new Date(right.observed_at));
}

function currentTelemetryPoint() {
  const overview = state.overview;
  if (!overview?.observed_at || overview?.detection?.reachable !== true) {
    return null;
  }
  const radios = {};
  for (const radio of Array.isArray(overview.radios) ? overview.radios : []) {
    const metrics = {};
    for (const metric of Array.isArray(radio.metrics) ? radio.metrics : []) {
      const value = Number(metric.value);
      if (Number.isFinite(value)) {
        metrics[metric.key] = value;
      }
    }
    radios[radio.key] = { metrics };
  }
  return {
    observed_at: overview.observed_at,
    signal_score: overview.signal?.score,
    radios,
    system: {
      temperature_c: overview.system?.temperature?.celsius,
      uptime_seconds: overview.system?.uptime_seconds,
    },
  };
}

function renderLineChart(container, points, seriesDefinitions, options) {
  replaceChildren(container);
  const series = seriesDefinitions
    .map((definition) => ({
      ...definition,
      values: points
        .map((point) => {
          const rawValue = definition.read(point);
          return {
            time: new Date(point.observed_at).getTime(),
            value: isFiniteReading(rawValue) ? Number(rawValue) : NaN,
          };
        })
        .filter((item) => Number.isFinite(item.time) && Number.isFinite(item.value)),
    }))
    .filter((definition) => definition.values.length);
  if (!series.length) {
    container.append(emptyNode(options.emptyText));
    return;
  }

  const legend = document.createElement("div");
  legend.className = "chart-legend";
  for (const definition of series) {
    const item = document.createElement("span");
    item.className = definition.className;
    const marker = document.createElement("i");
    const latest = definition.values[definition.values.length - 1].value;
    const textNode = document.createElement("span");
    textNode.textContent = `${definition.label} ${formatChartNumber(latest, options.decimals)} ${options.unit}`;
    item.append(marker, textNode);
    legend.append(item);
  }

  const width = 720;
  const height = 230;
  const margin = { top: 14, right: 18, bottom: 36, left: 54 };
  const plotWidth = width - margin.left - margin.right;
  const plotHeight = height - margin.top - margin.bottom;
  const allValues = series.flatMap((definition) => definition.values.map((item) => item.value));
  const allTimes = series.flatMap((definition) => definition.values.map((item) => item.time));
  let minValue = Math.min(...allValues);
  let maxValue = Math.max(...allValues);
  const valuePadding = Math.max((maxValue - minValue) * 0.12, options.decimals ? 1.5 : 3);
  minValue -= valuePadding;
  maxValue += valuePadding;
  let minTime = Math.min(...allTimes);
  let maxTime = Math.max(...allTimes);
  if (minTime === maxTime) {
    minTime -= 30000;
    maxTime += 30000;
  }
  const x = (time) => margin.left + ((time - minTime) / (maxTime - minTime)) * plotWidth;
  const y = (value) => margin.top + ((maxValue - value) / (maxValue - minValue)) * plotHeight;

  const svg = svgElement("svg", {
    viewBox: `0 0 ${width} ${height}`,
    role: "img",
    "aria-label": `${series.map((item) => item.label).join(" and ")} ${options.unit} history`,
  });
  svg.classList.add("line-chart");
  const title = svgElement("title");
  title.textContent = `${series.map((item) => item.label).join(" and ")} history`;
  svg.append(title);

  for (let index = 0; index <= 4; index += 1) {
    const ratio = index / 4;
    const gridY = margin.top + ratio * plotHeight;
    svg.append(svgElement("line", { x1: margin.left, y1: gridY, x2: width - margin.right, y2: gridY, class: "chart-grid-line" }));
    const label = svgElement("text", { x: margin.left - 9, y: gridY + 4, class: "chart-axis-label", "text-anchor": "end" });
    label.textContent = formatChartNumber(maxValue - ratio * (maxValue - minValue), options.decimals);
    svg.append(label);
  }

  const timeLabels = [minTime, minTime + (maxTime - minTime) / 2, maxTime];
  timeLabels.forEach((time, index) => {
    const label = svgElement("text", {
      x: x(time),
      y: height - 10,
      class: "chart-axis-label",
      "text-anchor": index === 0 ? "start" : index === 2 ? "end" : "middle",
    });
    label.textContent = formatChartTime(time, state.telemetryHours);
    svg.append(label);
  });

  for (const definition of series) {
    const path = definition.values
      .map((item, index) => `${index ? "L" : "M"} ${x(item.time).toFixed(2)} ${y(item.value).toFixed(2)}`)
      .join(" ");
    const line = svgElement("path", { d: path, class: `chart-series ${definition.className}` });
    svg.append(line);
    const latest = definition.values[definition.values.length - 1];
    const marker = svgElement("circle", {
      cx: x(latest.time),
      cy: y(latest.value),
      r: 4.5,
      class: `chart-latest ${definition.className}`,
    });
    const markerTitle = svgElement("title");
    markerTitle.textContent = `${definition.label}: ${formatChartNumber(latest.value, options.decimals)} ${options.unit}`;
    marker.append(markerTitle);
    svg.append(marker);
  }

  container.append(legend, svg);
}

function svgElement(name, attributes = {}) {
  const element = document.createElementNS("http://www.w3.org/2000/svg", name);
  for (const [key, value] of Object.entries(attributes)) {
    element.setAttribute(key, String(value));
  }
  return element;
}

function formatChartNumber(value, decimals = 0) {
  return Number(value).toFixed(decimals);
}

function formatChartTime(timestamp, hours) {
  const date = new Date(timestamp);
  if (hours > 24) {
    return date.toLocaleDateString([], { month: "short", day: "numeric" });
  }
  return date.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
}

function formatHistoryRange(hours) {
  return hours === 168 ? "7 days" : `${hours} hour${hours === 1 ? "" : "s"}`;
}

function thermalTone(value) {
  return Number.isFinite(Number(value)) ? "info" : "muted";
}

function hasDisplayValue(value) {
  return value !== null && value !== undefined && String(value).trim() !== "";
}

function isFiniteReading(value) {
  return value !== null && value !== undefined && value !== "" && Number.isFinite(Number(value));
}

function renderSignal() {
  const signal = state.overview?.signal || {};
  const score = Number.isFinite(signal.score) ? signal.score : 0;
  const quality = signal.quality || "Unknown";

  els.signalMeter.style.setProperty("--score", String(Math.max(0, Math.min(100, score))));
  els.signalMeter.dataset.rating = toneFromQuality(quality);
  setText(els.signalMeterValue, Number.isFinite(signal.score) ? `${signal.score}%` : "--");
  setText(els.signalMeterLabel, quality);
  setText(els.signalSummary, signal.summary || "Gateway signal metrics are not available.");
  setTag(els.signalStateTag, quality, toneFromQuality(quality));

  replaceChildren(els.signalMetrics);
  const metrics = Array.isArray(signal.metrics) ? signal.metrics : [];
  if (!metrics.length) {
    els.signalMetrics.append(emptyNode("No signal metrics found yet."));
    return;
  }

  for (const metric of metrics) {
    const item = document.createElement("article");
    item.className = `mini-metric mini-metric--${toneFromQuality(metric.rating)}`;

    const label = document.createElement("span");
    label.textContent = metric.label || humanize(metric.key);

    const value = document.createElement("strong");
    value.textContent = metric.display || formatValue(metric.value);

    const source = document.createElement("small");
    source.textContent = metric.source || "";

    item.append(label, value, source);
    els.signalMetrics.append(item);
  }
}

function renderDetails() {
  renderDetailList(els.connectionDetails, state.overview?.connection, "No cellular session data yet.");
  renderDetailList(els.deviceDetails, state.overview?.device, "No device data yet.");
  renderDetailList(els.wifiDetails, wifiDetailData(), wifiEmptyText());
}

function wifiEmptyText() {
  if (!state.config?.gateway_password_configured) {
    return "Save the gateway login to load Wi-Fi settings.";
  }
  return "No Wi-Fi data returned by the gateway yet.";
}

function wifiDetailData() {
  const overviewWifi = state.overview?.wifi || {};
  const wifi = state.wifi || {};
  return {
    ...overviewWifi,
    ssid: wifi.ssid || overviewWifi.ssid,
    radio_enabled: typeof wifi.radio_enabled === "boolean" ? wifi.radio_enabled : undefined,
    broadcast_enabled:
      typeof wifi.broadcast_enabled === "boolean" ? wifi.broadcast_enabled : undefined,
    clients: state.clients?.count ?? overviewWifi.clients,
  };
}

function renderDetailList(container, data, emptyText) {
  replaceChildren(container);
  const entries = Object.entries(data || {}).filter(([, value]) => hasValue(value));
  if (!entries.length) {
    container.append(emptyNode(emptyText));
    return;
  }

  for (const [key, value] of entries) {
    const row = document.createElement("div");
    row.className = "detail-row";

    const dt = document.createElement("dt");
    dt.textContent = detailLabels[key] || humanize(key);

    const dd = document.createElement("dd");
    dd.textContent = formatValue(value);

    row.append(dt, dd);
    container.append(row);
  }
}

function renderControls() {
  if (state.config) {
    els.dryRunToggle.checked = Boolean(state.config.dry_run);
    els.testFrequency.value = String(state.config.tests_per_hour || 180);
  }
  renderWifiControls();
  renderAdvancedModemControls();

  const source = state.config?.gateway_password_source || "none";
  const configured = Boolean(state.config?.gateway_password_configured);
  if (state.gatewayLoginBusy) {
    setTag(els.gatewayLoginState, "Working", "warn");
  } else if (!configured) {
    setTag(els.gatewayLoginState, "Login needed", "warn");
  } else if (source === "saved") {
    setTag(els.gatewayLoginState, "Saved", "good");
  } else if (source === "environment") {
    setTag(els.gatewayLoginState, "Environment", "info");
  } else if (source === "runtime") {
    setTag(els.gatewayLoginState, "Session", "info");
  } else {
    setTag(els.gatewayLoginState, "Configured", "good");
  }

  updateControlState();
}

function renderAdvancedModemControls() {
  const lab = state.config?.advanced_modem || {};
  if (els.advancedModemMode && document.activeElement !== els.advancedModemMode) {
    els.advancedModemMode.value = lab.mode || "disabled";
  }
  if (els.advancedModemControlUrl && document.activeElement !== els.advancedModemControlUrl) {
    els.advancedModemControlUrl.placeholder = `Auto: ${lab.default_control_url || BUILT_IN_DOCKER_ADAPTER_URL}`;
    els.advancedModemControlUrl.value =
      lab.control_url ||
      (lab.mode && lab.mode !== "disabled"
        ? lab.default_control_url || BUILT_IN_DOCKER_ADAPTER_URL
        : "");
  }
  if (els.advancedModemAcknowledge && document.activeElement !== els.advancedModemAcknowledge) {
    els.advancedModemAcknowledge.checked = Boolean(lab.acknowledged);
  }
  if (els.skipStockBackupReminder && document.activeElement !== els.skipStockBackupReminder) {
    els.skipStockBackupReminder.checked = Boolean(lab.skip_stock_backup);
  }
  if (els.advancedUploadProfile && document.activeElement !== els.advancedUploadProfile) {
    els.advancedUploadProfile.value = lab.upload_priority?.profile || "balanced";
  }
  if (els.advancedRadioProfile && document.activeElement !== els.advancedRadioProfile) {
    els.advancedRadioProfile.value = lab.g4ar_radio?.profile || "auto";
  }

  if (!els.advancedModemStatus) {
    return;
  }

  const capabilities = lab.capabilities || {};
  const firmwareLab = lab.g4ar_unlock_lab || lab.g4ar_firmware_lab || {};
  const g4arRadio = lab.g4ar_radio || {};
  renderDetailList(
    els.advancedModemStatus,
    {
      mode: lab.label || "Disabled",
      effective_adapter:
        lab.effective_control_url ||
        (lab.mode && lab.mode !== "disabled" ? BUILT_IN_DOCKER_ADAPTER_URL : ""),
      built_in_adapter: lab.built_in_adapter_selected,
      upload_profile: lab.upload_priority?.label || "Balanced",
      radio_profile: g4arRadio.label || "Auto",
      cell_lock: capabilityText(capabilities.cell_lock),
      band_lock: capabilityText(capabilities.band_lock),
      cell_scan: capabilityText(capabilities.cell_scan),
      lte_anchor_override: capabilityText(capabilities.lte_anchor_override),
      radio_mode_override: capabilityText(capabilities.radio_mode_override),
      usb_hardware_probe: capabilityText(capabilities.usb_hardware_probe),
      usb_ethernet_bridge: capabilityText(capabilities.usb_ethernet_bridge),
      upload_priority_qos: capabilityText(capabilities.upload_priority_qos),
      stock_firmware_backup: capabilityText(capabilities.stock_firmware_backup),
      custom_firmware_flash: capabilityText(capabilities.custom_firmware_flash),
      tx_power: capabilityText(capabilities.tx_power_override),
    },
    "Advanced modem capabilities load after settings refresh."
  );
  renderDetailList(
    els.firmwareLabStatus,
    {
      device: firmwareLab.device || "Arcadyan TMO-G4AR",
      lab_status: firmwareLab.flash_status || "Select G4AR unlock / radio lab mode",
      radio_goal: g4arRadio.label || "Auto",
      adapter_ready: firmwareLab.adapter_ready,
      stock_backup_skipped: firmwareLab.stock_backup_skipped,
      required_consent: firmwareLab.consent_phrase,
    },
    "G4AR unlock / radio lab status loads after settings refresh."
  );
  renderFirmwareBackupList();
  renderUsbLab();
}

function renderUsbLab() {
  if (!els.usbLabTag) {
    return;
  }

  const lab = state.usbLab || {};
  const probe = lab.probe || null;
  const platform = lab.platform || {};
  const adapter = lab.recommended_adapter || {};
  const displayStatus = probe?.status || lab.status || "not_probed";
  const summary = probe?.summary || lab.reason || "USB-C hardware status loads after refresh.";
  const tone =
    displayStatus === "active_2_5gbe" || displayStatus === "link_ready"
      ? "good"
      : displayStatus === "interface_ready" || displayStatus === "ready_to_probe"
        ? "info"
        : displayStatus === "hardware_bridge_required" || displayStatus === "driver_needed"
          ? "warn"
          : "muted";

  setTag(els.usbLabTag, humanize(displayStatus), tone);
  setText(els.usbLabSummary, summary);
  renderDetailList(
    els.usbPlatformDetails,
    {
      controller: platform.controller || "MediaTek T750 USB 3",
      data_port: platform.data_port || "USB Type-C data port",
      usb_ceiling: platform.controller_speed_mbps
        ? `${formatNetworkSpeed(platform.controller_speed_mbps)} USB`
        : "Unknown",
      ethernet_target: platform.ethernet_target_mbps
        ? formatNetworkSpeed(platform.ethernet_target_mbps)
        : "2.5 Gbps",
      preferred_chipset: adapter.chipset || "ASIX AX88279",
      preferred_driver: adapter.driver_preference || "CDC-NCM",
      stock_support: platform.stock_firmware_support || "Unverified",
    },
    "G4AR USB platform details unavailable."
  );

  replaceChildren(els.usbProbeChecks);
  const checks = probe?.checks || {};
  const checkOrder = [
    ["usb_host", "USB host"],
    ["super_speed_5gbps", "USB 5 Gbps"],
    ["ethernet_adapter", "Ethernet NIC"],
    ["driver_bound", "Driver"],
    ["network_interface", "Interface"],
    ["carrier", "Carrier"],
    ["link_2500mbps", "2.5G link"],
    ["lan_bridge_member", "LAN bridge"],
  ];
  if (!probe) {
    els.usbProbeChecks.append(emptyNode("Run a probe through a gateway-side hardware adapter."));
  } else {
    for (const [key, label] of checkOrder) {
      const item = document.createElement("div");
      item.className = `usb-check ${checks[key] ? "is-pass" : "is-pending"}`;
      const indicator = document.createElement("span");
      indicator.className = "usb-check-indicator";
      indicator.setAttribute("aria-hidden", "true");
      const text = document.createElement("span");
      text.textContent = label;
      const value = document.createElement("strong");
      value.textContent = checks[key] ? "Ready" : "Waiting";
      item.append(indicator, text, value);
      els.usbProbeChecks.append(item);
    }
  }

  replaceChildren(els.usbDeviceList);
  const devices = Array.isArray(probe?.devices) ? probe.devices : [];
  if (!devices.length) {
    els.usbDeviceList.append(emptyNode("No USB Ethernet device reported yet."));
  } else {
    for (const device of devices) {
      const item = document.createElement("article");
      item.className = "usb-device";
      const identity = document.createElement("div");
      const title = document.createElement("strong");
      title.textContent = device.product || device.manufacturer || "USB device";
      const ids = document.createElement("small");
      ids.textContent = compactJoin(
        [
          device.vendor_id && device.product_id
            ? `${device.vendor_id}:${device.product_id}`
            : "",
          device.usb_speed_mbps && `USB ${formatNetworkSpeed(device.usb_speed_mbps)}`,
        ],
        " - "
      );
      identity.append(title, ids);

      const link = document.createElement("strong");
      link.className = "usb-device-link";
      link.textContent = device.link_speed_mbps
        ? formatNetworkSpeed(device.link_speed_mbps)
        : device.carrier
          ? "Link up"
          : "No carrier";

      const metadata = document.createElement("p");
      metadata.textContent = compactJoin(
        [
          device.driver && `Driver ${device.driver}`,
          device.interface && `Interface ${device.interface}`,
          device.duplex && humanize(device.duplex),
        ],
        " - "
      ) || "Waiting for driver and interface details.";
      item.append(identity, link, metadata);
      els.usbDeviceList.append(item);
    }
  }
}

function renderFirmwareBackupList() {
  if (!els.firmwareBackupList) {
    return;
  }

  const backups = Array.isArray(state.firmwareBackups?.backups)
    ? state.firmwareBackups.backups
    : [];
  replaceChildren(els.firmwareBackupList);
  const backupStatus = state.firmwareBackups?.backup_dir
    ? `Backup folder: ${state.firmwareBackups.backup_dir}`
    : "Backup history loads after refresh.";
  const skipMessage = labSkipsStockBackup()
    ? "Reminder skipped for now. Override gate still requires verified backup and recovery."
    : "";
  setText(els.firmwareBackupStatus, compactJoin([backupStatus, skipMessage], " "));

  if (!backups.length) {
    els.firmwareBackupList.append(emptyNode("No local G4AR stock backups saved yet."));
    return;
  }

  for (const backup of backups.slice(0, 5)) {
    const item = document.createElement("article");
    item.className = "backup-item";

    const identity = document.createElement("div");
    const title = document.createElement("strong");
    title.textContent = backup.id || "G4AR backup";
    const subtitle = document.createElement("small");
    subtitle.textContent = compactJoin(
      [formatDate(backup.created_at), backup.firmware_version && `Firmware ${backup.firmware_version}`],
      " - "
    );
    identity.append(title, subtitle);

    const count = document.createElement("span");
    count.textContent = `${backup.artifact_count || 0} ${backup.artifact_count === 1 ? "file" : "files"}`;

    const artifacts = document.createElement("p");
    artifacts.className = "backup-artifacts";
    const artifactNames = Array.isArray(backup.artifacts)
      ? backup.artifacts.map((artifact) => artifact.name).filter(Boolean)
      : [];
    artifacts.textContent = artifactNames.length
      ? artifactNames.join(", ")
      : "Manifest saved; adapter did not return inline artifacts.";

    item.append(identity, count, artifacts);
    els.firmwareBackupList.append(item);
  }
}

function capabilityText(capability) {
  if (!capability) {
    return "Unknown";
  }
  if (capability.supported) {
    return "Adapter ready";
  }
  return capability.status ? `${humanize(capability.status)} - ${capability.reason || "Unavailable"}` : "Unavailable";
}

function isG4ARLabMode(mode) {
  return mode === "g4ar_unlock_lab" || mode === "g4ar_firmware_lab";
}

function isAdvancedMode(mode) {
  return mode && mode !== "disabled";
}

function labSkipsStockBackup() {
  return Boolean(state.config?.advanced_modem?.skip_stock_backup);
}

function effectiveAdvancedControlUrl() {
  const mode = els.advancedModemMode?.value || "disabled";
  const value = els.advancedModemControlUrl?.value.trim() || "";
  if (value) {
    return value;
  }
  return isAdvancedMode(mode) ? BUILT_IN_DOCKER_ADAPTER_URL : "";
}

function ensureAdvancedAdapterDefault() {
  if (!els.advancedModemControlUrl) {
    return;
  }
  const mode = els.advancedModemMode?.value || "disabled";
  if (isAdvancedMode(mode) && !els.advancedModemControlUrl.value.trim()) {
    els.advancedModemControlUrl.value = BUILT_IN_DOCKER_ADAPTER_URL;
  }
}

function renderWifiControls() {
  const currentSsid = state.wifi?.ssid || state.overview?.wifi?.ssid || "";
  if (document.activeElement !== els.wifiSsid) {
    els.wifiSsid.value = currentSsid;
  }
  if (typeof state.wifi?.radio_enabled === "boolean") {
    els.wifiRadioToggle.checked = state.wifi.radio_enabled;
  } else {
    els.wifiRadioToggle.checked = true;
  }
}

function renderClients() {
  const devices = state.clients?.devices || [];
  const count = state.clients?.count ?? devices.length;
  setTag(els.clientCountTag, `${count} ${count === 1 ? "device" : "devices"}`, count ? "info" : "muted");
  replaceChildren(els.clientTableBody);

  if (!devices.length) {
    addEmptyTableRow(els.clientTableBody, 6, clientsEmptyText());
    return;
  }

  for (const device of devices) {
    const identification = device.identification || {};
    const row = document.createElement("tr");
    row.append(
      tableCell(device.hostname || identification.name || "Unknown device"),
      tableCell(device.ip_address || "Unknown"),
      tableCell(device.mac_address || "Hidden"),
      tableCell(compactJoin([device.interface, device.band || device.ssid], " / ") || "Unknown"),
      tableCell(device.vendor || "Unknown"),
      tableCell(deviceGuessNode(identification))
    );
    els.clientTableBody.append(row);
  }
}

function clientsEmptyText() {
  if (!state.config?.gateway_password_configured) {
    return "Save the gateway login to load connected devices.";
  }
  if (state.clients === null) {
    return "Connected-device data is not available yet.";
  }
  return "The gateway reported no connected devices.";
}

function deviceGuessNode(identification) {
  const wrapper = document.createElement("div");
  wrapper.className = "device-guess";
  const name = document.createElement("strong");
  name.textContent = identification.name || "Unknown device";
  const confidence = document.createElement("small");
  const value = Number(identification.confidence || 0);
  confidence.textContent = `${Math.round(value * 100)}% confidence`;
  wrapper.append(name, confidence);
  return wrapper;
}

function renderTowerMapSummary() {
  const data = state.mapData || {};
  const identity = data.connected?.identity || {};
  const provider = data.provider || {};
  const hasIdentity = Boolean(identity.cell_id || identity.pci || identity.band);
  const providerReady = Boolean(provider.configured);
  const nearbyCount = Array.isArray(data.nearby) ? data.nearby.length : 0;

  setTag(
    els.mapPreviewTag,
    hasIdentity ? "Cell detected" : "No tower ID",
    hasIdentity ? "info" : "muted"
  );
  setTag(
    els.mapProviderTag,
    providerReady
      ? nearbyCount
        ? "OpenCellID active"
        : "OpenCellID ready"
      : "Add OpenCellID key",
    providerReady ? "good" : "warn"
  );
  const advancedModem = state.config?.advanced_modem || {};
  if (advancedModem.enabled && advancedModem.control_url_configured) {
    setTag(els.mapTowerLockTag, "Advanced lock adapter", "info");
  } else if (advancedModem.mode && advancedModem.mode !== "disabled") {
    setTag(els.mapTowerLockTag, "Adapter setup needed", "warn");
  } else {
    setTag(els.mapTowerLockTag, "Tower lock unsupported", "warn");
  }
  setTag(
    els.nearbyTowerCountTag,
    `${nearbyCount} ${nearbyCount === 1 ? "tower" : "towers"}`,
    nearbyCount ? "info" : "muted"
  );

  renderMapInputs(data);
  renderTowerIdentityDetails(data);
  renderNearbyTowers(data);
  renderMapStatus(data);
  renderMaps(data);
}

function renderMapInputs(data) {
  const mapConfig = state.config?.map || {};
  const center = data.map?.center || {};
  const editableLatitude =
    mapConfig.latitude ?? (center.source !== "default_us" ? center.latitude : "");
  const editableLongitude =
    mapConfig.longitude ?? (center.source !== "default_us" ? center.longitude : "");
  const radius = mapConfig.radius_km ?? data.map?.radius_km ?? DEFAULT_MAP_RADIUS_KM;

  if (document.activeElement !== els.mapLatitude) {
    els.mapLatitude.value = hasValue(editableLatitude) ? String(editableLatitude) : "";
  }
  if (document.activeElement !== els.mapLongitude) {
    els.mapLongitude.value = hasValue(editableLongitude) ? String(editableLongitude) : "";
  }
  if (document.activeElement !== els.mapRadius) {
    els.mapRadius.value = String(radius);
  }
}

function renderTowerIdentityDetails(data) {
  const identity = data.connected?.identity || {};
  const location = data.connected?.location || null;
  const details = {
    operator: identity.operator,
    radio: identity.radio,
    network_type: identity.network_type,
    band: identity.band,
    pci: identity.pci,
    cell_id: identity.cell_id,
    lac: identity.lac,
    mcc: identity.mcc,
    mnc: identity.mnc,
    tower_distance:
      location && Number.isFinite(location.distance_km)
        ? `${location.distance_km} km`
        : undefined,
    tower_accuracy:
      location && location.range_m ? `${location.range_m} m` : undefined,
    tower_match: location?.match_confidence,
    tower_match_note: location?.match_note,
  };
  renderDetailList(
    els.towerIdentityDetails,
    details,
    "No serving-cell identity has been reported by the gateway yet."
  );
  renderDetailList(
    els.mapPreviewDetails,
    {
      radio: identity.radio,
      band: identity.band,
      pci: identity.pci,
      cell_id: identity.cell_id,
    },
    "Open Map to configure a center and tower data source."
  );
}

function renderNearbyTowers(data) {
  replaceChildren(els.nearbyTowerTableBody);
  const towers = data.nearby || [];
  if (!towers.length) {
    let message = "Save an OpenCellID API key in Settings to load nearby tower records.";
    if (data.provider?.nearby_loaded) {
      message = "OpenCellID found no nearby tower records around this map center.";
    } else if (data.provider?.configured) {
      message = "No nearby tower records loaded yet. Refresh Towers to query OpenCellID.";
    }
    addEmptyTableRow(els.nearbyTowerTableBody, 6, message);
    return;
  }

  for (const tower of towers) {
    const row = document.createElement("tr");
    row.append(
      tableCell(tower.label || "Cell tower"),
      tableCell(tower.radio || "Unknown"),
      tableCell(formatDistance(tower.distance_km)),
      tableCell(formatSignal(tower.average_signal)),
      tableCell(tower.range_m ? `${tower.range_m} m` : "Unknown"),
      tableCell(tower.samples ?? "Unknown")
    );
    els.nearbyTowerTableBody.append(row);
  }
}

function renderMapStatus(data) {
  const errors = data.errors || [];
  const notices = data.notices || [];
  if (errors.length) {
    setMapStatus(errors.join(" | "), "error");
    return;
  }
  if (notices.length) {
    setMapStatus(notices.join(" "), "warn");
    return;
  }
  if (!data.provider?.configured) {
    setMapStatus("OpenCellID key is optional, but needed for real tower locations.", "");
    return;
  }
  if (state.mapBusy) {
    setMapStatus("Refreshing tower map.", "");
    return;
  }
  if (data.location?.center_source === "public_ip" && data.location?.auto_detected) {
    const location = data.location.auto_detected;
    const place = compactJoin([location.city, location.region, location.country], ", ");
    setMapStatus(
      place
        ? `Map centered from public IP estimate near ${place}.`
        : "Map centered from public IP estimate.",
      "info"
    );
    return;
  }
  setMapStatus(data.provider?.nearby_loaded ? "Tower map refreshed." : "", "success");
}

function renderMaps(data) {
  renderLeafletMap("preview", els.dashboardMapPreview, data, { preview: true });
  if (isMapViewVisible()) {
    renderLeafletMap("main", els.towerMap, data, { preview: false });
  }
}

function renderLeafletMap(key, container, data, { preview }) {
  if (!container) {
    return;
  }
  if (!window.L) {
    container.textContent = "Map library is loading.";
    return;
  }
  if (!preview && !isElementRenderable(container)) {
    scheduleMainMapRender(120);
    return;
  }

  const map = ensureLeafletMap(key, container, { preview });
  if (!map) {
    return;
  }

  const layerKey = key === "preview" ? "previewLayer" : "mainLayer";
  if (state.maps[layerKey]) {
    state.maps[layerKey].remove();
  }

  const layer = window.L.layerGroup().addTo(map);
  state.maps[layerKey] = layer;

  const center = mapCenter(data);
  const points = [];
  const home = data.home;
  const connectedTower = data.connected?.location;

  if (home) {
    const position = [home.latitude, home.longitude];
    points.push(position);
    window.L.circleMarker(position, {
      radius: preview ? 7 : 8,
      color: "#2563eb",
      weight: 2,
      fillColor: "#2563eb",
      fillOpacity: 0.78,
    })
      .bindPopup("Saved gateway location")
      .addTo(layer);
  }

  if (connectedTower) {
    const position = [connectedTower.latitude, connectedTower.longitude];
    points.push(position);
    window.L.circleMarker(position, {
      radius: preview ? 9 : 10,
      color: "#e20074",
      weight: 3,
      fillColor: "#e20074",
      fillOpacity: 0.82,
    })
      .bindPopup(towerPopup(connectedTower, "Serving tower"))
      .bindTooltip("Serving tower", {
        direction: "top",
        offset: [0, -8],
        permanent: !preview,
        className: "tower-tooltip tower-tooltip--connected",
      })
      .addTo(layer);

    if (connectedTower.range_m && !preview) {
      window.L.circle(position, {
        radius: connectedTower.range_m,
        color: "#e20074",
        fillColor: "#e20074",
        fillOpacity: 0.08,
        weight: 1,
      }).addTo(layer);
    }
  }

  if (!preview) {
    for (const tower of data.nearby || []) {
      if (!Number.isFinite(tower.latitude) || !Number.isFinite(tower.longitude)) {
        continue;
      }
      const position = [tower.latitude, tower.longitude];
      const isConnected = connectedTower && tower.id === connectedTower.id;
      if (isConnected) {
        continue;
      }
      points.push(position);
      window.L.circleMarker(position, {
        radius: isConnected ? 10 : 7,
        color: isConnected ? "#e20074" : "#0f766e",
        weight: isConnected ? 3 : 2,
        fillColor: isConnected ? "#e20074" : "#0f766e",
        fillOpacity: isConnected ? 0.82 : 0.66,
      })
        .bindPopup(towerPopup(tower, isConnected ? "Connected tower" : "Nearby tower"))
        .addTo(layer);
    }
  }

  if (home && connectedTower && !preview) {
    window.L.polyline(
      [
        [home.latitude, home.longitude],
        [connectedTower.latitude, connectedTower.longitude],
      ],
      {
        color: "#e20074",
        weight: 2,
        dashArray: "6 6",
      }
    ).addTo(layer);
  }

  if (!points.length) {
    points.push([center.latitude, center.longitude]);
    window.L.circleMarker(points[0], {
      radius: preview ? 7 : 8,
      color: "#64748b",
      weight: 2,
      fillColor: "#64748b",
      fillOpacity: 0.72,
    })
      .bindPopup("Map center")
      .addTo(layer);
  }

  if (points.length > 1 && !preview) {
    map.fitBounds(window.L.latLngBounds(points), { padding: [28, 28], maxZoom: 14 });
  } else {
    map.setView(points[0], center.source === "default_us" ? 4 : preview ? 11 : 12);
  }
  window.setTimeout(() => map.invalidateSize(), 0);
}

function scheduleMainMapRender(delay = 0) {
  window.setTimeout(() => {
    window.requestAnimationFrame(() => {
      if (!isMapViewVisible()) {
        return;
      }
      renderLeafletMap("main", els.towerMap, mapRenderData(), { preview: false });
      invalidateMaps();
      window.setTimeout(() => {
        renderLeafletMap("main", els.towerMap, mapRenderData(), { preview: false });
        invalidateMaps();
      }, 180);
    });
  }, delay);
}

function mapRenderData() {
  if (state.mapData) {
    return state.mapData;
  }
  const latitude = Number.parseFloat(els.mapLatitude?.value || "");
  const longitude = Number.parseFloat(els.mapLongitude?.value || "");
  const hasInputCenter = Number.isFinite(latitude) && Number.isFinite(longitude);
  const center = hasInputCenter
    ? { latitude, longitude, source: "form" }
    : { ...DEFAULT_MAP_CENTER, source: "default_us" };
  return {
    map: {
      center,
      radius_km: DEFAULT_MAP_RADIUS_KM,
    },
    home: hasInputCenter ? { latitude, longitude, source: "form" } : null,
    connected: { identity: {}, location: null },
    nearby: [],
    provider: {},
    errors: [],
    notices: [],
  };
}

function ensureLeafletMap(key, container, { preview }) {
  if (state.maps[key]) {
    return state.maps[key];
  }

  try {
    const map = window.L.map(container, {
      attributionControl: true,
      zoomControl: !preview,
      dragging: !preview,
      scrollWheelZoom: !preview,
      doubleClickZoom: !preview,
      boxZoom: !preview,
      keyboard: !preview,
      tap: !preview,
    });
    window.L.tileLayer(MAP_TILE_URL, {
      maxZoom: 19,
      attribution: "&copy; OpenStreetMap contributors",
    }).addTo(map);
    if (!preview) {
      window.L.control.scale({ imperial: true, metric: true }).addTo(map);
    }
    state.maps[key] = map;
    return map;
  } catch (error) {
    container.textContent = `Map could not initialize: ${error.message}`;
    return null;
  }
}

function isMapViewVisible() {
  const panel = document.querySelector('[data-view-panel="map"]');
  return Boolean(panel?.classList.contains("is-active") && isElementRenderable(els.towerMap));
}

function isElementRenderable(element) {
  if (!element) {
    return false;
  }
  const box = element.getBoundingClientRect();
  return box.width > 0 && box.height > 0;
}

function mapCenter(data) {
  const center = data?.map?.center || DEFAULT_MAP_CENTER;
  return {
    latitude: Number.isFinite(center.latitude) ? center.latitude : DEFAULT_MAP_CENTER.latitude,
    longitude: Number.isFinite(center.longitude)
      ? center.longitude
      : DEFAULT_MAP_CENTER.longitude,
    source: center.source || "default_us",
  };
}

function invalidateMaps() {
  for (const map of [state.maps.preview, state.maps.main]) {
    if (map) {
      window.setTimeout(() => map.invalidateSize(), 0);
    }
  }
}

function towerPopup(tower, title) {
  return `
    <strong>${escapeHtml(title)}</strong><br>
    ${escapeHtml(tower.label || "Cell tower")}<br>
    Distance: ${escapeHtml(formatDistance(tower.distance_km))}<br>
    Signal: ${escapeHtml(formatSignal(tower.average_signal))}<br>
    Accuracy: ${escapeHtml(tower.range_m ? `${tower.range_m} m` : "Unknown")}
  `;
}

async function refreshTowerMap({ includeNearby = false, quiet = false, force = false } = {}) {
  if (state.mapBusy && !force) {
    return;
  }
  const ownsBusyState = !state.mapBusy;
  if (ownsBusyState) {
    state.mapBusy = true;
    updateControlState();
  }
  if (!quiet) {
    setMapStatus(includeNearby ? "Refreshing tower data." : "Refreshing map.", "");
  }

  const params = new URLSearchParams({
    include_nearby: includeNearby ? "true" : "false",
  });
  const latitude = Number.parseFloat(els.mapLatitude.value);
  const longitude = Number.parseFloat(els.mapLongitude.value);
  const radiusKm = Number.parseFloat(els.mapRadius.value);
  if (Number.isFinite(latitude) && Number.isFinite(longitude)) {
    params.set("latitude", String(latitude));
    params.set("longitude", String(longitude));
  }
  if (Number.isFinite(radiusKm)) {
    params.set("radius_km", String(radiusKm));
  }

  try {
    state.mapData = await api(`/api/gateway/map?${params.toString()}`);
    renderTowerMapSummary();
  } catch (error) {
    setMapStatus(error.message, "error");
  } finally {
    if (ownsBusyState) {
      state.mapBusy = false;
      updateControlState();
    }
    renderTowerMapSummary();
  }
}

async function useBrowserLocation() {
  if (!navigator.geolocation) {
    setMapStatus("Browser location is not available in this browser.", "error");
    return;
  }

  state.mapBusy = true;
  updateControlState();
  setMapStatus("Requesting browser location.", "");

  try {
    const position = await new Promise((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 60000,
      });
    });
    els.mapLatitude.value = position.coords.latitude.toFixed(6);
    els.mapLongitude.value = position.coords.longitude.toFixed(6);
    await saveMapCenter({ quiet: true });
    setMapStatus("Browser location saved as the map center.", "success");
  } catch (error) {
    setMapStatus(error.message || "Browser location was not allowed.", "error");
  } finally {
    state.mapBusy = false;
    updateControlState();
  }
}

async function saveMapCenter({ quiet = false } = {}) {
  const latitude = Number.parseFloat(els.mapLatitude.value);
  const longitude = Number.parseFloat(els.mapLongitude.value);
  const radiusKm = Number.parseFloat(els.mapRadius.value);

  if (!Number.isFinite(latitude) || latitude < -90 || latitude > 90) {
    setMapStatus("Latitude must be between -90 and 90.", "error");
    els.mapLatitude.focus();
    return;
  }
  if (!Number.isFinite(longitude) || longitude < -180 || longitude > 180) {
    setMapStatus("Longitude must be between -180 and 180.", "error");
    els.mapLongitude.focus();
    return;
  }
  if (!Number.isFinite(radiusKm) || radiusKm < 0.25 || radiusKm > 100) {
    setMapStatus("Radius must be between 0.25 and 100 km.", "error");
    els.mapRadius.focus();
    return;
  }

  await runMapAction(quiet ? "" : "Saving map center.", async () => {
    state.config = await api("/api/map/settings", {
      method: "POST",
      body: {
        latitude,
        longitude,
        radius_km: radiusKm,
      },
    });
    await refreshTowerMap({ includeNearby: true, quiet: true, force: true });
    return "Map center saved.";
  });
}

async function saveOpenCellIdKey() {
  const key = els.openCellIdKey.value.trim();
  if (!key) {
    setActionMessage("Enter an OpenCellID API key first.", "error");
    els.openCellIdKey.focus();
    return;
  }

  await runAction("Saving OpenCellID key.", async () => {
    state.config = await api("/api/map/settings", {
      method: "POST",
      body: { opencellid_api_key: key },
    });
    els.openCellIdKey.value = "";
    await refreshTowerMap({ includeNearby: true, quiet: true });
    return "OpenCellID key saved.";
  });
}

async function clearOpenCellIdKey() {
  if (!window.confirm("Clear the saved OpenCellID API key?")) {
    return;
  }

  await runAction("Clearing OpenCellID key.", async () => {
    state.config = await api("/api/map/settings", {
      method: "POST",
      body: { clear_opencellid_api_key: true },
    });
    els.openCellIdKey.value = "";
    await refreshTowerMap({ includeNearby: false, quiet: true });
    return "OpenCellID key cleared.";
  });
}

async function runMapAction(workingMessage, action) {
  state.mapBusy = true;
  updateControlState();
  if (workingMessage) {
    setMapStatus(workingMessage, "");
  }
  try {
    const message = await action();
    if (message) {
      setMapStatus(message, "success");
    }
  } catch (error) {
    setMapStatus(error.message, "error");
  } finally {
    state.mapBusy = false;
    updateControlState();
  }
}

function setMapStatus(message, tone) {
  setText(els.mapStatusMessage, message || "");
  els.mapStatusMessage.className = "message";
  if (tone) {
    els.mapStatusMessage.classList.add(`message--${tone}`);
  }
}

function renderProbes() {
  replaceChildren(els.probeTableBody);
  const probes = state.status?.last_probe_results || [];
  if (!probes.length) {
    addEmptyTableRow(els.probeTableBody, 4, "No probe results yet.");
    return;
  }

  for (const probe of probes) {
    const row = document.createElement("tr");
    row.append(
      tableCell(cleanUrl(probe.url)),
      tableCell(statusBadge(probe.success, probe.status_code)),
      tableCell(formatLatency(probe.latency_ms)),
      tableCell(probe.error || probe.status_code || "OK")
    );
    els.probeTableBody.append(row);
  }
}

function renderEvents() {
  replaceChildren(els.eventsList);
  if (!state.events.length) {
    els.eventsList.append(emptyNode("No events recorded yet."));
    return;
  }

  for (const event of state.events) {
    const item = document.createElement("article");
    item.className = "event-item";

    const header = document.createElement("div");
    const kind = document.createElement("strong");
    kind.textContent = humanize(event.kind || "event");
    const time = document.createElement("span");
    time.textContent = formatDate(event.timestamp) || "";
    header.append(kind, time);

    const message = document.createElement("p");
    message.textContent = event.message || "";

    item.append(header, message);
    els.eventsList.append(item);
  }
}

function renderGatewaySections() {
  replaceChildren(els.gatewaySections);
  const sections = state.overview?.sections || [];
  if (!sections.length) {
    els.gatewaySections.append(emptyNode("No gateway data sections yet."));
    return;
  }

  for (const section of sections) {
    const details = document.createElement("details");
    details.className = "gateway-data";

    const summary = document.createElement("summary");
    const title = document.createElement("strong");
    title.textContent = section.title || humanize(section.key);
    const count = document.createElement("span");
    count.textContent = `${section.items?.length || 0} fields`;
    summary.append(title, count);
    details.append(summary);

    const list = document.createElement("dl");
    list.className = "gateway-data-list";
    for (const item of section.items || []) {
      const row = document.createElement("div");
      row.className = "detail-row";
      const dt = document.createElement("dt");
      dt.textContent = item.label || "Value";
      const dd = document.createElement("dd");
      dd.textContent = item.value || "";
      row.append(dt, dd);
      list.append(row);
    }
    details.append(list);
    els.gatewaySections.append(details);
  }
}

async function runCheck() {
  await runAction("Running connectivity check.", async () => {
    state.status = await api("/api/check", { method: "POST" });
    await refreshGatewayAndEvents();
    renderAll();
    return "Connectivity check complete.";
  });
}

async function runGatewayTest() {
  const password = els.gatewayPassword.value;
  const configured = Boolean(state.config?.gateway_password_configured);
  if (!configured && !password) {
    setActionMessage("Enter the gateway admin password or save a login first.", "error");
    els.gatewayPassword.focus();
    return;
  }

  await runAction("Testing gateway login.", async () => {
    const result = await api("/api/gateway/test", {
      method: "POST",
      body: password ? { gateway_password: password } : {},
    });
    await refreshGatewayAndEvents();
    if (result.authenticated) {
      return "Gateway is reachable and the login worked.";
    }
    if (result.reachable) {
      return result.message || "Gateway is reachable, but login was not verified.";
    }
    return "Gateway local API was not reachable.";
  });
}

async function saveGatewayLogin() {
  const password = els.gatewayPassword.value;
  if (!password) {
    setActionMessage("Enter the gateway admin password first.", "error");
    els.gatewayPassword.focus();
    return;
  }

  state.gatewayLoginBusy = true;
  await runAction("Saving gateway login.", async () => {
    const result = await api("/api/gateway/login", {
      method: "POST",
      body: {
        gateway_password: password,
        remember: els.rememberPassword.checked,
      },
    });
    state.config = {
      ...(state.config || {}),
      gateway_password_configured: result.gateway_password_configured,
      gateway_password_source: result.gateway_password_source,
      gateway_login_saved: result.gateway_password_source === "saved",
    };
    els.gatewayPassword.value = "";
    await refreshGatewayAndEvents();
    return result.saved ? "Gateway login saved." : "Gateway login active for this session.";
  });
  state.gatewayLoginBusy = false;
  renderControls();
}

async function clearGatewayLogin() {
  if (!window.confirm("Forget the saved gateway login?")) {
    return;
  }

  state.gatewayLoginBusy = true;
  await runAction("Clearing gateway login.", async () => {
    const result = await api("/api/gateway/login", { method: "DELETE" });
    state.config = {
      ...(state.config || {}),
      gateway_password_configured: result.gateway_password_configured,
      gateway_password_source: result.gateway_password_source,
      gateway_login_saved: false,
    };
    els.gatewayPassword.value = "";
    return result.gateway_password_source === "environment"
      ? "Saved login cleared. Environment password remains active."
      : "Gateway login cleared.";
  });
  state.gatewayLoginBusy = false;
  renderControls();
}

async function saveWifiSettings() {
  const ssid = els.wifiSsid.value.trim();
  const radioEnabled = els.wifiRadioToggle.checked;

  if (!state.config?.gateway_password_configured) {
    activateView("settings", { refreshMap: false });
    setActionMessage("Save the gateway admin password in Settings before changing Wi-Fi settings.", "error");
    els.gatewayPassword.focus();
    return;
  }
  if (ssid.length < 1 || ssid.length > 32) {
    setActionMessage("SSID must be between 1 and 32 characters.", "error");
    els.wifiSsid.focus();
    return;
  }
  if (!radioEnabled && !window.confirm("Turn off the gateway Wi-Fi radios?")) {
    return;
  }

  await runAction("Applying Wi-Fi settings.", async () => {
    const result = await api("/api/gateway/wifi", {
      method: "POST",
      body: {
        ssid,
        radio_enabled: radioEnabled,
      },
    });
    state.wifi = result.wifi;
    await refreshGatewayAndEvents();
    renderAll();
    return radioEnabled
      ? "Wi-Fi settings applied."
      : "Gateway Wi-Fi radios disabled.";
  });
}

async function refreshClients(onlineLookup) {
  await runAction(
    onlineLookup ? "Running reverse vendor lookup." : "Refreshing connected devices.",
    async () => {
      state.clients = await api(`/api/gateway/clients?online_lookup=${onlineLookup ? "true" : "false"}`);
      renderDetails();
      renderClients();
      return onlineLookup
        ? "Connected devices refreshed with vendor lookup."
        : "Connected devices refreshed.";
    }
  );
}

async function saveAdvancedModemSettings() {
  const mode = els.advancedModemMode.value;
  const acknowledged = els.advancedModemAcknowledge.checked;
  const controlUrl = effectiveAdvancedControlUrl();

  if (mode !== "disabled" && !acknowledged) {
    setActionMessage("Acknowledge the custom firmware and RF compliance warning first.", "error");
    els.advancedModemAcknowledge.focus();
    return;
  }

  await runAction("Saving advanced modem lab settings.", async () => {
    state.config = await api("/api/advanced-modem/settings", {
      method: "POST",
      body: {
        mode,
        control_url: controlUrl,
        acknowledged,
        upload_profile: els.advancedUploadProfile.value,
        radio_profile: els.advancedRadioProfile.value,
        skip_stock_backup: els.skipStockBackupReminder.checked,
      },
    });
    state.usbLab = await api("/api/g4ar/usb/status");
    renderAll();
    return mode === "disabled"
      ? "G4AR unlock lab disabled."
      : "Unlock / radio lab settings saved.";
  });
}

async function probeG4ARUsbPort() {
  if (!isG4ARLabMode(els.advancedModemMode.value)) {
    setActionMessage("Select G4AR unlock / radio lab mode before probing the USB-C port.", "error");
    els.advancedModemMode.focus();
    return;
  }
  if (!els.advancedModemAcknowledge.checked) {
    setActionMessage("Acknowledge the owned-hardware research warning first.", "error");
    els.advancedModemAcknowledge.focus();
    return;
  }

  await runAction("Probing the G4AR USB-C data port.", async () => {
    state.usbLab = await api("/api/g4ar/usb/probe", { method: "POST" });
    renderUsbLab();
    const probe = state.usbLab?.probe;
    if (!probe) {
      return state.usbLab?.reason || "A gateway-side hardware adapter is required.";
    }
    return probe.summary || "USB-C hardware probe completed.";
  });
}

async function armG4ARFlashGate() {
  if (!isG4ARLabMode(els.advancedModemMode.value)) {
    setActionMessage("Select G4AR unlock / radio lab mode before arming the flash gate.", "error");
    els.advancedModemMode.focus();
    return;
  }

  await runAction("Validating G4AR override consent gate.", async () => {
    await api("/api/g4ar/firmware/flash", {
      method: "POST",
      body: {
        stock_backup_sha256: els.firmwareBackupSha256.value.trim(),
        firmware_sha256: els.firmwareSha256.value.trim(),
        consent_phrase: els.firmwareConsentPhrase.value.trim(),
        backup_verified: els.firmwareBackupVerified.checked,
        recovery_verified: els.firmwareRecoveryVerified.checked,
        understands_brick_risk: els.firmwareUnderstandsRisk.checked,
      },
    });
    return "G4AR override gate validated.";
  });
}

async function createG4ARFirmwareBackup() {
  if (!isG4ARLabMode(els.advancedModemMode.value)) {
    setActionMessage("Select G4AR unlock / radio lab mode before creating a stock backup.", "error");
    els.advancedModemMode.focus();
    return;
  }
  if (!els.advancedModemAcknowledge.checked) {
    setActionMessage("Acknowledge the custom firmware and RF compliance warning first.", "error");
    els.advancedModemAcknowledge.focus();
    return;
  }
  if (!els.advancedModemControlUrl.value.trim()) {
    ensureAdvancedAdapterDefault();
  }

  await runAction("Creating G4AR stock firmware backup.", async () => {
    const manifest = await api("/api/g4ar/firmware/backup", {
      method: "POST",
      body: { reason: "ui_request" },
    });
    state.firmwareBackups = await api("/api/g4ar/firmware/backups");

    const stockArtifact = Array.isArray(manifest.artifacts)
      ? manifest.artifacts.find((artifact) => artifact.saved && artifact.sha256)
      : null;
    if (stockArtifact?.sha256 && document.activeElement !== els.firmwareBackupSha256) {
      els.firmwareBackupSha256.value = stockArtifact.sha256;
      els.firmwareBackupVerified.checked = true;
    }

    renderAdvancedModemControls();
    return `Stock backup saved: ${manifest.id}`;
  });
}

async function saveSettings() {
  const testsPerHour = Number.parseInt(els.testFrequency.value, 10);
  if (!Number.isFinite(testsPerHour) || testsPerHour < 1 || testsPerHour > 720) {
    setActionMessage("Checks per hour must be between 1 and 720.", "error");
    els.testFrequency.focus();
    return;
  }

  if (!els.dryRunToggle.checked && !state.config?.gateway_password_configured) {
    activateView("settings", { refreshMap: false });
    setActionMessage("Save the gateway admin password before turning Dry Run off.", "error");
    els.gatewayPassword.focus();
    return;
  }

  await runAction("Saving settings.", async () => {
    state.config = await api("/api/settings", {
      method: "POST",
      body: {
        dry_run: els.dryRunToggle.checked,
        tests_per_hour: testsPerHour,
      },
    });
    renderAll();
    return "Settings saved.";
  });
}

async function requestReboot() {
  const force = els.forceReboot.checked;
  const confirmed = window.confirm(
    force
      ? "Force a gateway reboot request now?"
      : "Request a gateway reboot if limits allow it?"
  );
  if (!confirmed) {
    return;
  }

  await runAction("Requesting gateway reboot.", async () => {
    state.status = await api("/api/reboot", {
      method: "POST",
      body: { force },
    });
    await refreshGatewayAndEvents();
    renderAll();
    return state.status?.dry_run
      ? "Dry run recorded. No live reboot was sent."
      : "Gateway reboot request sent.";
  });
}

async function startSweep() {
  if (state.seriesRunning) {
    return;
  }

  const count = clamp(Number.parseInt(els.seriesCount.value, 10) || 1, 1, 30);
  const intervalSeconds = clamp(Number.parseFloat(els.seriesInterval.value) || 0, 0, 300);
  els.seriesCount.value = String(count);
  els.seriesInterval.value = String(intervalSeconds);
  replaceChildren(els.seriesTableBody);

  state.seriesRunning = true;
  state.seriesAbort = false;
  updateControlState();
  setActionMessage("Diagnostic sweep running.", "");

  try {
    for (let index = 1; index <= count; index += 1) {
      if (state.seriesAbort) {
        break;
      }
      state.status = await api("/api/check", { method: "POST" });
      addSeriesRow(index, state.status);
      await refreshGatewayAndEvents();
      renderAll();
      if (index < count && intervalSeconds > 0 && !state.seriesAbort) {
        await sleep(intervalSeconds * 1000);
      }
    }
    setActionMessage(state.seriesAbort ? "Diagnostic sweep stopped." : "Diagnostic sweep complete.", "success");
  } catch (error) {
    setActionMessage(error.message, "error");
  } finally {
    state.seriesRunning = false;
    state.seriesAbort = false;
    updateControlState();
  }
}

function stopSweep() {
  state.seriesAbort = true;
  updateControlState();
}

function addSeriesRow(index, status) {
  const row = document.createElement("tr");
  row.append(
    tableCell(String(index)),
    tableCell(formatDate(status.last_check_at) || formatTime(new Date())),
    tableCell(statusBadge(status.internet_online)),
    tableCell(`${status.successful_probes || 0} / ${status.total_probes || 0}`),
    tableCell(phaseText(status.phase) || "Unknown")
  );
  els.seriesTableBody.prepend(row);
}

async function refreshGatewayAndEvents() {
  const [overviewResult, wifiResult, clientsResult, mapResult, eventsResult] = await Promise.allSettled([
    api("/api/gateway/overview"),
    api("/api/gateway/wifi"),
    api("/api/gateway/clients"),
    api("/api/gateway/map?include_nearby=false"),
    api("/api/events?limit=10"),
  ]);
  if (overviewResult.status === "fulfilled") {
    state.overview = overviewResult.value;
  }
  if (wifiResult.status === "fulfilled") {
    state.wifi = wifiResult.value;
  }
  if (clientsResult.status === "fulfilled") {
    state.clients = clientsResult.value;
  }
  if (mapResult.status === "fulfilled") {
    state.mapData = mapResult.value;
  }
  if (eventsResult.status === "fulfilled") {
    state.events = eventsResult.value;
  }
  try {
    state.telemetryHistory = await api(
      `/api/gateway/telemetry/history?hours=${state.telemetryHours}`
    );
  } catch {
    // A diagnostic sweep can continue even if chart history is temporarily unavailable.
  }
}

async function runAction(workingMessage, action) {
  state.actionBusy = true;
  updateControlState();
  setActionMessage(workingMessage, "");
  hideError();
  try {
    const message = await action();
    setActionMessage(message, "success");
  } catch (error) {
    setActionMessage(error.message, "error");
  } finally {
    state.actionBusy = false;
    updateControlState();
  }
}

function updateControlState() {
  const busy =
    state.refreshing ||
    state.actionBusy ||
    state.mapBusy ||
    state.gatewayLoginBusy ||
    state.snapshotBusy ||
    state.seriesRunning;
  const hasPassword = Boolean(els.gatewayPassword.value);
  const configured = Boolean(state.config?.gateway_password_configured);
  const openCellIdConfigured = Boolean(state.config?.map?.opencellid_configured);
  const advancedMode = els.advancedModemMode?.value || "disabled";
  const advancedEnabled = advancedMode !== "disabled";
  const g4arLabEnabled = isG4ARLabMode(advancedMode);
  const g4arBackupReady =
    g4arLabEnabled &&
    els.advancedModemAcknowledge.checked &&
    Boolean(els.advancedModemControlUrl.value.trim());
  const firmwareConsentPhrase =
    state.config?.advanced_modem?.g4ar_unlock_lab?.consent_phrase ||
    state.config?.advanced_modem?.g4ar_firmware_lab?.consent_phrase ||
    "I OWN THIS G4AR - BACKUP VERIFIED - OVERRIDE RISK ACCEPTED";
  const hasFirmwareGateInputs =
    els.firmwareBackupSha256.value.trim().length === 64 &&
    els.firmwareSha256.value.trim().length === 64 &&
    els.firmwareConsentPhrase.value.trim() === firmwareConsentPhrase &&
    els.firmwareBackupVerified.checked &&
    els.firmwareRecoveryVerified.checked &&
    els.firmwareUnderstandsRisk.checked;

  els.refreshButton.disabled = state.refreshing;
  els.refreshClientsButton.disabled = busy || !configured;
  els.lookupClientsButton.disabled = busy || !configured;
  els.checkButton.disabled = busy;
  els.gatewayButton.disabled = busy || (!configured && !hasPassword);
  els.saveGatewayButton.disabled = busy || !hasPassword;
  els.forgetGatewayButton.disabled = busy || !configured;
  els.saveWifiButton.disabled = busy || !configured || !els.wifiSsid.value.trim();
  els.saveSettingsButton.disabled = busy;
  els.downloadSnapshotButton.disabled = busy;
  els.saveAdvancedModemButton.disabled =
    busy || (advancedEnabled && !els.advancedModemAcknowledge.checked);
  els.rebootButton.disabled = busy || (!configured && !state.config?.dry_run);
  els.gatewayPassword.disabled = busy;
  els.rememberPassword.disabled = busy;
  els.wifiSsid.disabled = busy || !configured;
  els.wifiRadioToggle.disabled = busy || !configured;
  els.dryRunToggle.disabled = busy;
  els.testFrequency.disabled = busy;
  els.advancedModemMode.disabled = busy;
  els.advancedModemControlUrl.disabled = busy || !advancedEnabled;
  els.advancedModemAcknowledge.disabled = busy || !advancedEnabled;
  els.advancedUploadProfile.disabled = busy || !advancedEnabled;
  els.advancedRadioProfile.disabled = busy || !g4arLabEnabled;
  els.skipStockBackupReminder.disabled = busy || !g4arLabEnabled;
  els.usbProbeButton.disabled =
    busy || !g4arLabEnabled || !els.advancedModemAcknowledge.checked;
  els.firmwareBackupButton.disabled = busy || !g4arBackupReady;
  els.firmwareBackupSha256.disabled = busy || !g4arLabEnabled;
  els.firmwareSha256.disabled = busy || !g4arLabEnabled;
  els.firmwareConsentPhrase.disabled = busy || !g4arLabEnabled;
  els.firmwareBackupVerified.disabled = busy || !g4arLabEnabled;
  els.firmwareRecoveryVerified.disabled = busy || !g4arLabEnabled;
  els.firmwareUnderstandsRisk.disabled = busy || !g4arLabEnabled;
  els.firmwareFlashButton.disabled = busy || !g4arLabEnabled || !hasFirmwareGateInputs;
  els.forceReboot.disabled = busy;
  els.seriesStartButton.disabled = busy;
  els.seriesStopButton.disabled = !state.seriesRunning;
  els.seriesCount.disabled = busy;
  els.seriesInterval.disabled = busy;
  els.mapRefreshButton.disabled = busy;
  els.useBrowserLocationButton.disabled = busy;
  els.saveMapCenterButton.disabled =
    busy ||
    !els.mapLatitude.value.trim() ||
    !els.mapLongitude.value.trim() ||
    !els.mapRadius.value.trim();
  els.saveOpenCellIdButton.disabled = busy || !els.openCellIdKey.value.trim();
  els.clearOpenCellIdButton.disabled = busy || !openCellIdConfigured;
  els.mapLatitude.disabled = busy;
  els.mapLongitude.disabled = busy;
  els.mapRadius.disabled = busy;
  els.openCellIdKey.disabled = busy;
  els.darkModeToggle.disabled = false;
}

function selectTab(name) {
  document.querySelectorAll(".tab").forEach((button) => {
    button.classList.toggle("is-active", button.dataset.tab === name);
  });
  document.querySelectorAll(".tab-panel").forEach((panel) => {
    panel.classList.toggle("is-active", panel.id === `tab-${name}`);
  });
}

function setActionMessage(message, tone) {
  setText(els.actionMessage, message || "");
  els.actionMessage.className = "message";
  if (tone) {
    els.actionMessage.classList.add(`message--${tone}`);
  }
}

function showError(message) {
  setText(els.errorBanner, message);
  els.errorBanner.classList.remove("alert--hidden");
}

function hideError() {
  els.errorBanner.classList.add("alert--hidden");
  setText(els.errorBanner, "");
}

function setTag(element, label, tone = "muted") {
  setText(element, label);
  element.className = `tag tag--${tone}`;
}

function setTone(element, tone) {
  element.classList.remove("tone-good", "tone-bad", "tone-warn", "tone-info", "tone-muted");
  element.classList.add(`tone-${tone || "muted"}`);
}

function toneFromBoolean(value) {
  if (value === true) {
    return "good";
  }
  if (value === false) {
    return "bad";
  }
  return "muted";
}

function toneFromQuality(value) {
  const quality = String(value || "").toLowerCase();
  if (quality === "excellent" || quality === "good") {
    return "good";
  }
  if (quality === "fair") {
    return "warn";
  }
  if (quality === "weak" || quality === "poor") {
    return "bad";
  }
  return "muted";
}

function toneFromPhase(phase) {
  if (!phase) {
    return "muted";
  }
  if (phase === "online") {
    return "good";
  }
  if (phase.includes("grace") || phase.includes("cooldown") || phase.includes("dry_run")) {
    return "warn";
  }
  if (phase.includes("error") || phase.includes("failed") || phase.includes("outage")) {
    return "bad";
  }
  return "info";
}

function booleanLabel(value, truthy, falsy, unknown = "Unknown") {
  if (value === true) {
    return truthy;
  }
  if (value === false) {
    return falsy;
  }
  return unknown;
}

function phaseText(value) {
  return value ? humanize(value) : "";
}

function statusBadge(success, statusCode) {
  const span = document.createElement("span");
  span.className = `inline-status inline-status--${toneFromBoolean(success)}`;
  if (success === true) {
    span.textContent = statusCode ? `OK ${statusCode}` : "OK";
  } else if (success === false) {
    span.textContent = statusCode ? `Failed ${statusCode}` : "Failed";
  } else {
    span.textContent = "Unknown";
  }
  return span;
}

function tableCell(value) {
  const cell = document.createElement("td");
  if (value instanceof Node) {
    cell.append(value);
  } else {
    cell.textContent = formatValue(value);
  }
  return cell;
}

function addEmptyTableRow(body, columns, message) {
  const row = document.createElement("tr");
  const cell = document.createElement("td");
  cell.colSpan = columns;
  cell.append(emptyNode(message));
  row.append(cell);
  body.append(row);
}

function emptyNode(message) {
  const node = document.createElement("p");
  node.className = "empty-state";
  node.textContent = message;
  return node;
}

function replaceChildren(element) {
  while (element.firstChild) {
    element.removeChild(element.firstChild);
  }
}

function setText(element, value) {
  element.textContent = value;
}

function compactJoin(values, separator = " ") {
  return values.filter((value) => hasValue(value)).join(separator);
}

function hasValue(value) {
  if (value === null || value === undefined) {
    return false;
  }
  if (typeof value === "string") {
    return Boolean(value.trim());
  }
  return true;
}

function numericScore(value) {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string" && value.trim()) {
    const number = Number.parseFloat(value.replace("%", ""));
    return Number.isFinite(number) ? number : NaN;
  }
  return NaN;
}

function formatValue(value) {
  if (value === null || value === undefined) {
    return "Unknown";
  }
  if (typeof value === "boolean") {
    return value ? "Yes" : "No";
  }
  return String(value);
}

function formatDate(value) {
  if (!value) {
    return "";
  }
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  return date.toLocaleString([], {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

function formatTime(value) {
  const date = value instanceof Date ? value : new Date(value);
  return date.toLocaleTimeString([], {
    hour: "numeric",
    minute: "2-digit",
    second: "2-digit",
  });
}

function formatLatency(value) {
  if (!Number.isFinite(value)) {
    return "Unknown";
  }
  if (value < 1000) {
    return `${Math.round(value)} ms`;
  }
  return `${(value / 1000).toFixed(2)} s`;
}

function formatNetworkSpeed(value) {
  const speed = Number(value);
  if (!Number.isFinite(speed) || speed <= 0) {
    return "Unknown";
  }
  if (speed >= 1000) {
    const gbps = speed / 1000;
    return `${Number.isInteger(gbps) ? gbps.toFixed(0) : gbps.toFixed(1)} Gbps`;
  }
  return `${Math.round(speed)} Mbps`;
}

function formatDistance(value) {
  if (!Number.isFinite(value)) {
    return "Unknown";
  }
  if (value < 1) {
    return `${Math.round(value * 1000)} m`;
  }
  return `${value.toFixed(value >= 10 ? 1 : 2)} km`;
}

function formatSignal(value) {
  if (!Number.isFinite(value)) {
    return "Unknown";
  }
  return `${Math.round(value)} dBm`;
}

function cleanUrl(url) {
  try {
    const parsed = new URL(url);
    return parsed.host;
  } catch {
    return url || "Probe";
  }
}

function humanize(value) {
  return String(value || "")
    .replace(/[_-]+/g, " ")
    .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
    .replace(/\s+/g, " ")
    .trim()
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

function sleep(ms) {
  return new Promise((resolve) => {
    window.setTimeout(resolve, ms);
  });
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
