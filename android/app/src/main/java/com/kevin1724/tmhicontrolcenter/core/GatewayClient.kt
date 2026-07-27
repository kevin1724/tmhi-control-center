package com.kevin1724.tmhicontrolcenter.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class GatewayClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
) {
    suspend fun overview(settings: AppSettings): GatewayOverview {
        val errors = mutableListOf<String>()
        for (baseUrl in candidateBaseUrls(settings)) {
            for (path in INFO_PATHS) {
                val result = getJsonOrError("$baseUrl$path")
                if (result.json != null) {
                    return buildOverview(result.json)
                }
                errors += "${baseUrl}${path}: ${result.error}"
            }
        }
        return GatewayOverview(
            reachable = false,
            error = errors.take(3).joinToString("; ").ifBlank { "Gateway was not reachable." },
        )
    }

    suspend fun authenticate(settings: AppSettings): String {
        if (settings.gatewayPassword.isBlank()) {
            throw GatewayException("Gateway password is not configured.")
        }

        val payload = JSONObject()
            .put("username", settings.gatewayUsername)
            .put("password", settings.gatewayPassword)
        val errors = mutableListOf<String>()
        for (baseUrl in candidateBaseUrls(settings)) {
            val response = postJsonOrError("$baseUrl/auth/login", payload, token = null)
            val json = response.json
            if (json != null) {
                val token = json.optJSONObject("auth")?.optString("token").orEmpty()
                if (token.isNotBlank()) {
                    return token
                }
                val message = json.optJSONObject("result")?.optString("message").orEmpty()
                errors += message.ifBlank { "Login response did not include a token." }
            } else {
                errors += response.error.orEmpty()
            }
        }
        throw GatewayException(errors.take(3).joinToString("; ").ifBlank { "Gateway login failed." })
    }

    suspend fun wifiConfig(settings: AppSettings): WifiConfig {
        val token = authenticate(settings)
        val payload = fetchWifiPayload(settings, token)
        val path = payload.path
        val json = payload.json
        return buildWifiConfig(path, json)
    }

    suspend fun updateWifi(
        settings: AppSettings,
        ssid: String?,
        radioEnabled: Boolean?,
    ): WifiConfig {
        if (ssid == null && radioEnabled == null) {
            throw GatewayException("No Wi-Fi changes were requested.")
        }
        val token = authenticate(settings)
        val wifiPayload = fetchWifiPayload(settings, token)
        val getPath = wifiPayload.path
        val updated = JSONObject(wifiPayload.json.toString())

        if (ssid != null) {
            val changed = setWifiSsid(updated, ssid)
            if (changed == 0) {
                throw GatewayException("No writable SSID fields were found.")
            }
        }
        if (radioEnabled != null) {
            val changed = setWifiRadioEnabled(updated, radioEnabled) +
                setWifiSsidEnabled(updated, radioEnabled) +
                setWifiBroadcastEnabled(updated, radioEnabled)
            if (changed == 0) {
                throw GatewayException("No writable Wi-Fi radio fields were found.")
            }
        }

        val setPath = WIFI_SET_PATHS[getPath] ?: getPath.replace("get=ap", "set=ap")
        val response = postJsonOrError("${wifiPayload.baseUrl}$setPath", updated, token = token)
        if (response.json == null && response.error != null) {
            throw GatewayException(response.error)
        }
        return buildWifiConfig(getPath, updated)
    }

    suspend fun restoreWifi(
        settings: AppSettings,
        backup: WifiBackup,
    ): WifiRestoreResult {
        if (backup.networks.isEmpty()) {
            throw GatewayException("This Wi-Fi backup does not contain any network profiles.")
        }
        val token = authenticate(settings)
        val wifiPayload = fetchWifiPayload(settings, token)
        val updated = JSONObject(wifiPayload.json.toString())
        val currentProfiles = wifiNetworkProfiles(updated)
        val claimedPaths = mutableSetOf<List<String>>()
        var ssidsRestored = 0
        var passwordsRestored = 0

        backup.networks.forEachIndexed { index, saved ->
            val targetProfile = currentProfiles.firstOrNull {
                it.sourcePath == saved.sourcePath && it.sourcePath !in claimedPaths
            } ?: currentProfiles.firstOrNull {
                saved.band.isNotBlank() && it.band.equals(saved.band, ignoreCase = true) &&
                    it.sourcePath !in claimedPaths
            } ?: currentProfiles.firstOrNull {
                it.ssid == saved.ssid && it.sourcePath !in claimedPaths
            } ?: currentProfiles.getOrNull(index)?.takeIf { it.sourcePath !in claimedPaths }
                ?: return@forEachIndexed

            val target = objectAtPath(updated, targetProfile.sourcePath)
                ?: return@forEachIndexed
            claimedPaths += targetProfile.sourcePath
            if (setDirectStringKeys(target, WIFI_SSID_CANDIDATES, saved.ssid) > 0) {
                ssidsRestored += 1
            }
            val savedPassword = saved.password
            if (!savedPassword.isNullOrBlank() &&
                setStringKeys(target, WIFI_PASSWORD_CANDIDATES, savedPassword) > 0
            ) {
                passwordsRestored += 1
            }
        }

        if (ssidsRestored == 0 && passwordsRestored == 0) {
            throw GatewayException("No matching writable Wi-Fi fields were found on this gateway.")
        }
        val setPath = WIFI_SET_PATHS[wifiPayload.path]
            ?: wifiPayload.path.replace("get=ap", "set=ap")
        val response = postJsonOrError(
            "${wifiPayload.baseUrl}$setPath",
            updated,
            token = token,
        )
        if (response.error != null) throw GatewayException(response.error)
        return WifiRestoreResult(
            wifi = buildWifiConfig(wifiPayload.path, updated),
            ssidsRestored = ssidsRestored,
            passwordsRestored = passwordsRestored,
        )
    }

    suspend fun connectedDevices(settings: AppSettings): List<ConnectedDevice> {
        val token = authenticate(settings)
        val errors = mutableListOf<String>()
        for (baseUrl in candidateBaseUrls(settings)) {
            for (path in CLIENT_PATHS) {
                val response = getJsonOrError("$baseUrl$path", token = token)
                val json = response.json
                if (json != null) {
                    return clientDevicesFromPayload(json)
                }
                errors += response.error.orEmpty()
            }
        }
        throw GatewayException(
            errors.take(3).joinToString("; ").ifBlank { "Connected-device telemetry was not reachable." },
        )
    }

    suspend fun reboot(settings: AppSettings): String {
        val token = authenticate(settings)
        val baseUrl = candidateBaseUrls(settings).first()
        val response = postJsonOrError("$baseUrl/gateway/reset?set=reboot", JSONObject(), token = token)
        if (response.error != null && response.statusCode !in 200..299) {
            throw GatewayException(response.error)
        }
        return "Gateway reboot request sent."
    }

    private suspend fun fetchWifiPayload(settings: AppSettings, token: String): WifiPayload {
        val errors = mutableListOf<String>()
        for (baseUrl in candidateBaseUrls(settings)) {
            for (path in WIFI_CONFIG_PATHS) {
                val response = getJsonOrError("$baseUrl$path", token = token)
                val json = response.json
                if (json != null) {
                    return WifiPayload(baseUrl, path, json)
                }
                errors += response.error.orEmpty()
            }
        }
        throw GatewayException(errors.take(3).joinToString("; ").ifBlank { "Wi-Fi config was not reachable." })
    }

    private suspend fun getJsonOrError(url: String, token: String? = null): JsonResult {
        return withContext(Dispatchers.IO) {
            try {
                val builder = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                if (!token.isNullOrBlank()) {
                    builder.header("Authorization", "Bearer $token")
                }
                client.newCall(builder.build()).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext JsonResult(
                            statusCode = response.code,
                            error = "HTTP ${response.code}",
                        )
                    }
                    JsonResult(statusCode = response.code, json = JSONObject(body))
                }
            } catch (exc: Exception) {
                JsonResult(error = "${exc::class.java.simpleName}: ${exc.message.orEmpty()}")
            }
        }
    }

    private suspend fun postJsonOrError(
        url: String,
        payload: JSONObject,
        token: String?,
    ): JsonResult {
        return withContext(Dispatchers.IO) {
            try {
                val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
                val builder = Request.Builder()
                    .url(url)
                    .post(body)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", USER_AGENT)
                if (!token.isNullOrBlank()) {
                    builder.header("Authorization", "Bearer $token")
                }
                client.newCall(builder.build()).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext JsonResult(
                            statusCode = response.code,
                            error = "HTTP ${response.code}: ${responseBody.take(160)}",
                        )
                    }
                    val json = responseBody.takeIf { it.isNotBlank() }?.let { JSONObject(it) }
                    JsonResult(statusCode = response.code, json = json)
                }
            } catch (exc: Exception) {
                JsonResult(error = "${exc::class.java.simpleName}: ${exc.message.orEmpty()}")
            }
        }
    }

    private fun buildOverview(payload: JSONObject): GatewayOverview {
        val redacted = redactSensitive(payload) as JSONObject
        val leaves = flattenLeaves(redacted)
        return GatewayOverview(
            reachable = true,
            supported = true,
            apiType = "unified",
            device = fieldSummary(leaves, DEVICE_FIELDS),
            connection = fieldSummary(leaves, CONNECTION_FIELDS),
            wifi = fieldSummary(leaves, WIFI_FIELDS),
            signal = signalSummary(leaves),
            sections = sectionsFromPayload(redacted),
            rawJson = redacted.toString(2),
        )
    }

    private fun buildWifiConfig(source: String, payload: JSONObject): WifiConfig {
        val networks = wifiNetworkProfiles(payload)
        val redacted = redactSensitive(payload) as JSONObject
        val leaves = flattenLeaves(redacted)
        val ssid = findValue(leaves, WIFI_SSID_CANDIDATES).orEmpty()
        val radioEnabled = findBoolean(redacted, WIFI_RADIO_CANDIDATES)
        val broadcastEnabled = findBoolean(redacted, WIFI_BROADCAST_CANDIDATES)
        return WifiConfig(
            ssid = ssid,
            radioEnabled = radioEnabled,
            broadcastEnabled = broadcastEnabled,
            source = source,
            networks = networks,
            rawJson = redacted.toString(2),
        )
    }

    private fun wifiNetworkProfiles(
        value: Any?,
        path: List<String> = emptyList(),
    ): List<WifiNetworkProfile> {
        val profiles = mutableListOf<WifiNetworkProfile>()
        when (value) {
            is JSONObject -> {
                val keys = value.keys().asSequence().toList()
                val ssidKey = keys.firstOrNull {
                    normalizeKey(it) in WIFI_SSID_CANDIDATES && normalizeKey(it) != "bssid"
                }
                if (ssidKey != null) {
                    val leaves = flattenLeaves(value)
                    val ssid = formatScalar(value.opt(ssidKey))
                    if (ssid.isNotBlank()) {
                        val password = findValue(leaves, WIFI_PASSWORD_CANDIDATES)
                            ?.takeIf(::isUsableWifiPassword)
                        profiles += WifiNetworkProfile(
                            sourcePath = path,
                            ssid = ssid,
                            password = password,
                            band = findValue(leaves, WIFI_BAND_CANDIDATES).orEmpty(),
                            enabled = findBoolean(value, WIFI_SSID_ENABLED_CANDIDATES),
                            broadcastEnabled = findBoolean(
                                value,
                                WIFI_BROADCAST_CANDIDATES,
                            ),
                        )
                    }
                }
                keys.forEach { key ->
                    profiles += wifiNetworkProfiles(value.opt(key), path + key)
                }
            }
            is JSONArray -> (0 until value.length()).forEach { index ->
                profiles += wifiNetworkProfiles(value.opt(index), path + "#$index")
            }
        }
        return profiles.distinctBy { it.sourcePath to it.ssid }
    }

    private fun objectAtPath(root: JSONObject, path: List<String>): JSONObject? {
        var current: Any? = root
        for (part in path) {
            current = when {
                part.startsWith("#") && current is JSONArray -> {
                    current.opt(part.removePrefix("#").toIntOrNull() ?: return null)
                }
                current is JSONObject -> current.opt(part)
                else -> return null
            }
        }
        return current as? JSONObject
    }

    private fun setStringKeys(value: Any?, keysToSet: Set<String>, text: String): Int {
        var count = 0
        when (value) {
            is JSONObject -> value.keys().forEach { key ->
                if (normalizeKey(key) in keysToSet) {
                    value.put(key, text)
                    count += 1
                } else {
                    count += setStringKeys(value.opt(key), keysToSet, text)
                }
            }
            is JSONArray -> (0 until value.length()).forEach { index ->
                count += setStringKeys(value.opt(index), keysToSet, text)
            }
        }
        return count
    }

    private fun setDirectStringKeys(
        value: JSONObject,
        keysToSet: Set<String>,
        text: String,
    ): Int {
        var count = 0
        value.keys().forEach { key ->
            if (normalizeKey(key) in keysToSet) {
                value.put(key, text)
                count += 1
            }
        }
        return count
    }

    private fun isUsableWifiPassword(value: String): Boolean {
        val text = value.trim()
        return text.isNotBlank() &&
            text.lowercase() !in setOf("null", "[redacted]", "redacted", "hidden") &&
            text.any { it != '*' && it != '\u2022' }
    }

    private fun clientDevicesFromPayload(payload: JSONObject): List<ConnectedDevice> {
        val arrays = clientListCandidates(payload)
        val seen = mutableSetOf<String>()
        return arrays.flatMap { it.items.mapNotNull(::clientFromObject) }
            .filter { device ->
                val key = device.macAddress.ifBlank { device.ipAddress.ifBlank { device.hostname.ifBlank { device.id } } }
                seen.add(key)
            }
    }

    private fun clientFromObject(value: JSONObject): ConnectedDevice? {
        val leaves = flattenLeaves(value)
        val mac = findValue(leaves, CLIENT_FIELDS["mac_address"].orEmpty()).orEmpty()
        val ip = findValue(leaves, CLIENT_FIELDS["ip_address"].orEmpty()).orEmpty()
        val hostname = findValue(leaves, CLIENT_FIELDS["hostname"].orEmpty()).orEmpty()
        val interfaceName = findValue(leaves, CLIENT_FIELDS["interface"].orEmpty()).orEmpty()
        if (mac.isBlank() && ip.isBlank() && hostname.isBlank()) {
            return null
        }
        val vendor = findValue(leaves, CLIENT_FIELDS["vendor"].orEmpty()).orEmpty()
        val model = findValue(leaves, CLIENT_FIELDS["model"].orEmpty()).orEmpty()
        val os = findValue(leaves, CLIENT_FIELDS["os"].orEmpty()).orEmpty()
        val ssid = findValue(leaves, CLIENT_FIELDS["ssid"].orEmpty()).orEmpty()
        val band = findValue(leaves, CLIENT_FIELDS["band"].orEmpty()).orEmpty()
        val guess = bestGuess(hostname, vendor, model, os)
        return ConnectedDevice(
            id = listOf(mac, ip, hostname).firstOrNull { it.isNotBlank() } ?: value.hashCode().toString(),
            hostname = hostname.ifBlank { "Unknown device" },
            ipAddress = ip,
            macAddress = mac,
            interfaceName = interfaceName,
            ssid = ssid,
            band = band,
            vendor = vendor,
            bestGuess = guess,
        )
    }

    private fun bestGuess(hostname: String, vendor: String, model: String, os: String): String {
        val text = "$hostname $vendor $model $os".lowercase()
        return when {
            "ipad" in text -> "Apple iPad"
            "iphone" in text -> "Apple iPhone"
            "macbook" in text || "mac" in text && "apple" in text -> "Apple Mac"
            "android" in text && model.isNotBlank() -> model
            model.isNotBlank() && vendor.isNotBlank() -> "$vendor $model"
            model.isNotBlank() -> model
            vendor.isNotBlank() -> vendor
            os.isNotBlank() -> os
            else -> "Unknown"
        }
    }

    private fun clientListCandidates(value: Any?, path: List<String> = emptyList()): List<ClientArray> {
        val candidates = mutableListOf<ClientArray>()
        when (value) {
            is JSONArray -> {
                val items = (0 until value.length()).mapNotNull { value.opt(it) }
                val score = items.filterIsInstance<JSONObject>().sumOf(::clientScore)
                val pathText = normalizeKey(path.joinToString("."))
                if (score > 0 && listOf("client", "device", "host", "lan").any { it in pathText }) {
                    candidates += ClientArray(score, items.filterIsInstance<JSONObject>())
                }
                items.forEachIndexed { index, child ->
                    candidates += clientListCandidates(child, path + (index + 1).toString())
                }
            }
            is JSONObject -> {
                value.keys().forEach { key ->
                    candidates += clientListCandidates(value.opt(key), path + key)
                }
            }
        }
        return candidates.sortedByDescending { it.score }
    }

    private fun clientScore(value: JSONObject): Int {
        val leaves = flattenLeaves(value)
        var score = 0
        for (leaf in leaves) {
            if (formatScalar(leaf.value).isBlank()) continue
            val key = normalizeKey(leaf.path.lastOrNull().orEmpty())
            score += when {
                key in CLIENT_FIELDS["mac_address"].orEmpty() -> 5
                key in CLIENT_FIELDS["ip_address"].orEmpty() -> 3
                key in CLIENT_FIELDS["hostname"].orEmpty() -> 2
                key in CLIENT_FIELDS["interface"].orEmpty() -> 1
                else -> 0
            }
        }
        return score
    }

    private fun sectionsFromPayload(payload: JSONObject): List<GatewaySection> {
        return payload.keys().asSequence().mapNotNull { key ->
            val child = payload.opt(key)
            val items = flattenLeaves(child).take(60).mapNotNull { leaf ->
                val value = formatScalar(leaf.value)
                if (value.isBlank()) {
                    null
                } else {
                    GatewayItem(
                        label = humanizePath(leaf.path),
                        value = value,
                        source = listOf(key, *leaf.path.toTypedArray()).joinToString("."),
                    )
                }
            }
            if (items.isEmpty()) null else GatewaySection(humanizeKey(key), items)
        }.toList()
    }

    private fun fieldSummary(
        leaves: List<Leaf>,
        fields: List<FieldSpec>,
    ): Map<String, String> {
        return fields.mapNotNull { field ->
            val value = findValue(leaves, field.candidates)
            if (value.isNullOrBlank()) null else field.label to value
        }.toMap()
    }

    private fun signalSummary(leaves: List<Leaf>): SignalSummary {
        val metrics = SIGNAL_FIELDS.mapNotNull { field ->
            val leaf = leaves.firstOrNull { normalizeKey(it.path.lastOrNull().orEmpty()) in field.candidates }
            val value = leaf?.value ?: return@mapNotNull null
            val number = numberOrNull(value)
            val score = metricScore(field.key, number)
            SignalMetric(
                key = field.key,
                label = field.label,
                value = formatMetric(value, number, field.unit),
                score = score,
            )
        }
        val scores = metrics.mapNotNull { it.score }
        val score = if (scores.isEmpty()) null else scores.average().roundToInt()
        val quality = qualityFromScore(score)
        val summary = if (score == null) {
            "No signal metrics were found in the gateway response."
        } else {
            "Signal looks $quality across ${metrics.take(3).joinToString(", ") { it.label }}."
        }
        return SignalSummary(score = score, quality = quality, summary = summary, metrics = metrics)
    }

    private fun setWifiSsid(value: Any?, ssid: String): Int {
        var count = 0
        when (value) {
            is JSONObject -> value.keys().forEach { key ->
                if (normalizeKey(key) in WIFI_SSID_CANDIDATES && normalizeKey(key) != "bssid") {
                    value.put(key, ssid)
                    count += 1
                } else {
                    count += setWifiSsid(value.opt(key), ssid)
                }
            }
            is JSONArray -> (0 until value.length()).forEach { index ->
                count += setWifiSsid(value.opt(index), ssid)
            }
        }
        return count
    }

    private fun setWifiRadioEnabled(value: Any?, enabled: Boolean): Int {
        val specificKeys = WIFI_RADIO_CANDIDATES - "enabled"
        return setBooleanKeys(value, specificKeys, enabled)
    }

    private fun setWifiBroadcastEnabled(value: Any?, enabled: Boolean): Int {
        return setBooleanKeys(value, WIFI_BROADCAST_CANDIDATES, enabled)
    }

    private fun setWifiSsidEnabled(value: Any?, enabled: Boolean): Int {
        var count = 0
        when (value) {
            is JSONObject -> {
                val keys = value.keys().asSequence().toList()
                val hasSsid = keys.any { normalizeKey(it) in WIFI_SSID_CANDIDATES }
                keys.forEach { key ->
                    if (hasSsid && normalizeKey(key) == "enabled") {
                        value.put(key, enabled)
                        count += 1
                    } else {
                        count += setWifiSsidEnabled(value.opt(key), enabled)
                    }
                }
            }
            is JSONArray -> (0 until value.length()).forEach { count += setWifiSsidEnabled(value.opt(it), enabled) }
        }
        return count
    }

    private fun setBooleanKeys(value: Any?, keysToSet: Set<String>, enabled: Boolean): Int {
        var count = 0
        when (value) {
            is JSONObject -> value.keys().forEach { key ->
                if (normalizeKey(key) in keysToSet) {
                    value.put(key, enabled)
                    count += 1
                } else {
                    count += setBooleanKeys(value.opt(key), keysToSet, enabled)
                }
            }
            is JSONArray -> (0 until value.length()).forEach { count += setBooleanKeys(value.opt(it), keysToSet, enabled) }
        }
        return count
    }

    private fun findBoolean(value: Any?, candidates: Set<String>): Boolean? {
        when (value) {
            is JSONObject -> {
                value.keys().forEach { key ->
                    val child = value.opt(key)
                    if (normalizeKey(key) in candidates) {
                        return boolOrNull(child)
                    }
                    findBoolean(child, candidates)?.let { return it }
                }
            }
            is JSONArray -> (0 until value.length()).forEach { index ->
                findBoolean(value.opt(index), candidates)?.let { return it }
            }
        }
        return null
    }

    private fun findValue(leaves: List<Leaf>, candidates: Set<String>): String? {
        return leaves.firstOrNull { normalizeKey(it.path.lastOrNull().orEmpty()) in candidates }
            ?.value
            ?.let(::formatScalar)
            ?.takeIf { it.isNotBlank() }
    }

    private fun flattenLeaves(value: Any?, path: List<String> = emptyList()): List<Leaf> {
        return when (value) {
            is JSONObject -> value.keys().asSequence().flatMap { key ->
                flattenLeaves(value.opt(key), path + key).asSequence()
            }.toList()
            is JSONArray -> {
                val values = (0 until value.length()).map { value.opt(it) }
                if (values.all { it !is JSONObject && it !is JSONArray }) {
                    listOf(Leaf(path, values.take(12).joinToString(", ") { formatScalar(it) }))
                } else {
                    values.take(12).flatMapIndexed { index, child ->
                        flattenLeaves(child, path + (index + 1).toString())
                    }
                }
            }
            JSONObject.NULL, null -> emptyList()
            else -> if (path.isEmpty()) emptyList() else listOf(Leaf(path, value))
        }
    }

    private fun redactSensitive(value: Any?, key: String = ""): Any? {
        if (isSensitiveKey(key)) return "[redacted]"
        return when (value) {
            is JSONObject -> JSONObject().also { copy ->
                value.keys().forEach { childKey ->
                    copy.put(childKey, redactSensitive(value.opt(childKey), childKey))
                }
            }
            is JSONArray -> JSONArray().also { copy ->
                (0 until value.length()).forEach { index ->
                    copy.put(redactSensitive(value.opt(index), key))
                }
            }
            else -> value
        }
    }

    private fun candidateBaseUrls(settings: AppSettings): List<String> {
        val host = settings.gatewayHost.trim().removePrefix("http://").removePrefix("https://").substringBefore("/")
        val ports = listOf(settings.gatewayPort, 80, 8080).distinct()
        return ports.map { port ->
            val portPart = if (port == 80) "" else ":$port"
            "http://$host$portPart/TMI/v1"
        }.distinct()
    }

    private fun metricScore(kind: String, value: Double?): Int? {
        if (value == null) return null
        return when (kind) {
            "sinr" -> thresholdScore(value, 20.0 to 100, 13.0 to 80, 5.0 to 55, 0.0 to 35, -100.0)
            "rsrp" -> thresholdScore(value, -80.0 to 100, -90.0 to 80, -100.0 to 60, -110.0 to 35, -200.0)
            "rsrq" -> thresholdScore(value, -10.0 to 90, -15.0 to 65, -20.0 to 40, -30.0 to 20, -100.0)
            "rssi" -> thresholdScore(value, -65.0 to 100, -75.0 to 80, -85.0 to 60, -95.0 to 35, -200.0)
            "bars" -> if (value <= 5) ((value / 5.0) * 100).roundToInt().coerceIn(0, 100) else value.roundToInt().coerceIn(0, 100)
            else -> null
        }
    }

    private fun thresholdScore(
        value: Double,
        excellent: Pair<Double, Int>,
        good: Pair<Double, Int>,
        fair: Pair<Double, Int>,
        weak: Pair<Double, Int>,
        floor: Double,
    ): Int {
        for ((threshold, score) in listOf(excellent, good, fair, weak)) {
            if (value >= threshold) return score
        }
        return if (value <= floor) 0 else 10
    }

    private data class JsonResult(
        val statusCode: Int = 0,
        val json: JSONObject? = null,
        val error: String? = null,
    )

    private data class WifiPayload(val baseUrl: String, val path: String, val json: JSONObject)
    private data class Leaf(val path: List<String>, val value: Any?)
    private data class FieldSpec(val key: String, val label: String, val candidates: Set<String>)
    private data class SignalSpec(val key: String, val label: String, val unit: String, val candidates: Set<String>)
    private data class ClientArray(val score: Int, val items: List<JSONObject>)

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
        const val USER_AGENT = "homeisp/android/2.12.1"

        val INFO_PATHS = listOf("/gateway/?get=all", "/gateway?get=all")
        val CLIENT_PATHS = listOf(
            "/network/telemetry/?get=clients",
            "/network/telemetry?get=clients",
            "/network/telemetry/?get=all",
            "/network/telemetry?get=all",
        )
        val WIFI_CONFIG_PATHS = listOf(
            "/network/configuration/v2?get=ap",
            "/network/configuration?get=ap",
        )
        val WIFI_SET_PATHS = mapOf(
            "/network/configuration/v2?get=ap" to "/network/configuration/v2?set=ap",
            "/network/configuration?get=ap" to "/network/configuration?set=ap",
        )
        val SENSITIVE_KEY_FRAGMENTS = setOf(
            "password", "passphrase", "wpakey", "presharedkey", "psk", "token",
            "secret", "credential", "cookie", "authorization", "auth", "imei",
            "imsi", "iccid", "msisdn", "serial", "pin", "puk",
        )
        val SIGNAL_FIELDS = listOf(
            SignalSpec("rsrp", "RSRP", "dBm", setOf("rsrp", "nrrsrp", "ltersrp", "5grsrp")),
            SignalSpec("rsrq", "RSRQ", "dB", setOf("rsrq", "nrrsrq", "ltersrq", "5grsrq")),
            SignalSpec("sinr", "SINR", "dB", setOf("sinr", "snr", "nrsinr", "ltesinr", "5gsinr")),
            SignalSpec("rssi", "RSSI", "dBm", setOf("rssi", "nrrssi", "lterssi", "5grssi")),
            SignalSpec("bars", "Bars", "", setOf("bars", "signalbars", "signalbar", "signalstrength")),
        )
        val DEVICE_FIELDS = listOf(
            FieldSpec("manufacturer", "Manufacturer", setOf("manufacturer", "vendor", "brand")),
            FieldSpec("model", "Model", setOf("model", "productclass", "productclass", "sku")),
            FieldSpec("name", "Name", setOf("friendlyname", "devicename", "name")),
            FieldSpec("firmware", "Firmware", setOf("firmware", "firmwareversion", "softwareversion")),
            FieldSpec("hardware", "Hardware", setOf("hardware", "hardwareversion", "hwversion")),
        )
        val CONNECTION_FIELDS = listOf(
            FieldSpec("state", "Connection", setOf("connectionstatus", "wanstatus", "state")),
            FieldSpec("network_type", "Network", setOf("networktype", "rat", "accesstechnology")),
            FieldSpec("operator", "Operator", setOf("operator", "carrier", "plmnname")),
            FieldSpec("plmn", "PLMN", setOf("plmn", "plmnid", "operatorcode")),
            FieldSpec("mcc", "MCC", setOf("mcc", "mobilecountrycode")),
            FieldSpec("mnc", "MNC", setOf("mnc", "mobilenetworkcode")),
            FieldSpec("band", "Band", setOf("primaryband", "nrband", "lteband", "band")),
            FieldSpec("pci", "PCI", setOf("pci", "physicalcellid")),
            FieldSpec("tac", "TAC", setOf("tac", "trackingareacode", "trackingarea")),
            FieldSpec("lac", "LAC", setOf("lac", "localareacode")),
            FieldSpec("cell_id", "Cell ID", setOf("nci", "nrcellid", "cellid", "enbid", "gnbid")),
            FieldSpec("wan_ipv4", "WAN IPv4", setOf("wanip", "ipv4address")),
            FieldSpec("wan_ipv6", "WAN IPv6", setOf("ipv6address", "wanipv6")),
            FieldSpec("uptime", "Uptime", setOf("uptime", "systemuptime")),
        )
        val WIFI_FIELDS = listOf(
            FieldSpec("ssid", "SSID", setOf("ssid", "primaryssid")),
            FieldSpec("ssid_2g", "2.4 GHz SSID", setOf("ssid2g", "2gssid", "24ghzssid")),
            FieldSpec("ssid_5g", "5 GHz SSID", setOf("ssid5g", "5gssid", "5ghzssid")),
            FieldSpec("clients", "Connected clients", setOf("clients", "connectedclients", "connecteddevices")),
        )
        val WIFI_RADIO_CANDIDATES = setOf("isradioenabled", "radioenabled", "radioenable", "wifienabled", "wirelessenabled", "enabled")
        val WIFI_BROADCAST_CANDIDATES = setOf("isbroadcastenabled", "broadcastenabled", "ssidbroadcast", "broadcastssid")
        val WIFI_SSID_CANDIDATES = setOf("ssid", "ssidname", "networkname", "apname")
        val WIFI_SSID_ENABLED_CANDIDATES = setOf("enabled", "ssidenabled", "isactive")
        val WIFI_PASSWORD_CANDIDATES = setOf(
            "password", "passphrase", "wpakey", "wpapassphrase", "presharedkey",
            "psk", "securitykey", "wifipassword",
        )
        val WIFI_BAND_CANDIDATES = setOf(
            "band", "frequency", "frequencyband", "radio", "radioband",
        )
        val CLIENT_FIELDS = mapOf(
            "mac_address" to setOf("mac", "macaddress", "macaddr", "clientmac", "hwaddr", "physicaladdress"),
            "ip_address" to setOf("ip", "ipaddress", "ipv4", "ipv4address", "clientip", "hostip"),
            "hostname" to setOf("hostname", "host", "hostname", "name", "clientname", "devicename", "friendlyname"),
            "interface" to setOf("interface", "connectiontype", "medium", "type", "ifname"),
            "ssid" to setOf("ssid", "networkname", "apname"),
            "band" to setOf("band", "radio", "frequency", "freq"),
            "vendor" to setOf("vendor", "manufacturer", "maker", "brand"),
            "model" to setOf("model", "modelname", "devicemodel", "product", "productname"),
            "os" to setOf("os", "operatingsystem", "platform"),
        )
    }
}

class GatewayException(message: String) : IOException(message)

fun normalizeKey(value: String): String = value.lowercase().replace(Regex("[^a-z0-9]"), "")

fun humanizeKey(value: String): String {
    val spaced = value
        .replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ")
        .replace("_", " ")
        .replace("-", " ")
        .replace(".", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (spaced.isBlank()) return "Value"
    val upper = setOf("apn", "api", "dns", "ip", "ipv4", "ipv6", "lte", "mcc", "mnc", "nr", "pci", "rsrp", "rsrq", "rssi", "sinr", "ssid", "tac", "wan")
    return spaced.split(" ").joinToString(" ") { part ->
        if (part.lowercase() in upper) part.uppercase() else part.replaceFirstChar { it.uppercase() }
    }
}

fun humanizePath(path: List<String>): String {
    return path.filterNot { it.toIntOrNull() != null }
        .takeLast(3)
        .joinToString(" / ") { humanizeKey(it) }
        .ifBlank { "Value" }
}

fun formatScalar(value: Any?): String {
    if (value == null || value == JSONObject.NULL) return ""
    return when (value) {
        is Boolean -> if (value) "Yes" else "No"
        is Number -> {
            val double = value.toDouble()
            if (double % 1.0 == 0.0) double.toLong().toString() else "%.1f".format(double)
        }
        else -> value.toString()
    }
}

fun numberOrNull(value: Any?): Double? {
    if (value is Number && value !is Boolean) return value.toDouble()
    val text = formatScalar(value)
    return Regex("-?\\d+(?:\\.\\d+)?").find(text)?.value?.toDoubleOrNull()
}

fun boolOrNull(value: Any?): Boolean? {
    return when (value) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> when (value.trim().lowercase()) {
            "1", "true", "yes", "on", "enabled" -> true
            "0", "false", "no", "off", "disabled" -> false
            else -> null
        }
        else -> null
    }
}

fun qualityFromScore(score: Int?): String {
    return when {
        score == null -> "Unknown"
        score >= 85 -> "Excellent"
        score >= 70 -> "Good"
        score >= 50 -> "Fair"
        score >= 30 -> "Weak"
        else -> "Poor"
    }
}

fun formatMetric(value: Any?, number: Double?, unit: String): String {
    val original = formatScalar(value)
    if (number == null || unit.isBlank() || original.contains(unit, ignoreCase = true)) {
        return original
    }
    val formatted = if (number % 1.0 == 0.0) number.toLong().toString() else "%.1f".format(number)
    return "$formatted $unit"
}

fun query(params: Map<String, Any?>): String {
    return params.entries
        .filter { it.value != null && it.value.toString().isNotBlank() }
        .joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value.toString(), "UTF-8")}"
        }
}

fun isSensitiveKey(key: String): Boolean {
    val normalized = normalizeKey(key)
    return listOf(
        "password", "passphrase", "wpakey", "presharedkey", "psk", "token",
        "secret", "credential", "cookie", "authorization", "auth", "imei",
        "imsi", "iccid", "msisdn", "serial", "pin", "puk",
    ).any { it in normalized }
}
