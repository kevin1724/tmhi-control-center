package com.kevin1724.tmhicontrolcenter.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class WifiBackupVault(context: Context) {
    private val root = File(context.filesDir, "wifi-recovery-vault").also { it.mkdirs() }
    private val secretBox = SecretBox("tmhi-control-center-wifi-vault-v1")

    suspend fun create(wifi: WifiConfig, overview: GatewayOverview?): WifiBackupManifest {
        if (wifi.networks.isEmpty()) {
            throw WifiBackupException("The gateway did not expose any Wi-Fi network profiles.")
        }
        return withContext(Dispatchers.IO) {
            val createdAt = Instant.now()
            val id = "wifi-${ID_FORMAT.format(createdAt)}"
            val manifest = WifiBackupManifest(
                id = id,
                createdAt = createdAt.toString(),
                gatewayModel = overview?.device?.get("Model").orEmpty(),
                firmwareVersion = overview?.device?.get("Firmware").orEmpty(),
                networkCount = wifi.networks.size,
                passwordCount = wifi.passwordCount,
            )
            val payload = backupToJson(WifiBackup(manifest, wifi.networks)).toString(2)
            File(root, "$id.tmhivault").writeText(secretBox.encrypt(payload))
            manifest
        }
    }

    fun list(): List<WifiBackupManifest> {
        return root.listFiles { file -> file.isFile && file.extension == "tmhivault" }
            .orEmpty()
            .mapNotNull { file -> runCatching { read(file).manifest }.getOrNull() }
            .sortedByDescending { it.createdAt }
    }

    suspend fun load(id: String): WifiBackup = withContext(Dispatchers.IO) {
        if (!ID_PATTERN.matches(id)) throw WifiBackupException("Invalid Wi-Fi backup ID.")
        val file = File(root, "$id.tmhivault")
        if (!file.isFile) throw WifiBackupException("Wi-Fi backup was not found.")
        read(file)
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        if (!ID_PATTERN.matches(id)) throw WifiBackupException("Invalid Wi-Fi backup ID.")
        val file = File(root, "$id.tmhivault")
        if (file.exists() && !file.delete()) {
            throw WifiBackupException("Wi-Fi backup could not be deleted.")
        }
    }

    private fun read(file: File): WifiBackup {
        val encrypted = file.readText()
        val payload = secretBox.decrypt(encrypted)
            ?: throw WifiBackupException("This backup cannot be decrypted on this phone.")
        return backupFromJson(JSONObject(payload))
    }

    private fun backupToJson(backup: WifiBackup): JSONObject {
        val manifest = backup.manifest
        return JSONObject()
            .put("version", 1)
            .put("id", manifest.id)
            .put("created_at", manifest.createdAt)
            .put("gateway_model", manifest.gatewayModel)
            .put("firmware_version", manifest.firmwareVersion)
            .put("network_count", manifest.networkCount)
            .put("password_count", manifest.passwordCount)
            .put(
                "networks",
                JSONArray().apply {
                    backup.networks.forEach { network ->
                        put(
                            JSONObject()
                                .put("source_path", JSONArray(network.sourcePath))
                                .put("ssid", network.ssid)
                                .put("password", network.password ?: JSONObject.NULL)
                                .put("band", network.band)
                                .put("enabled", network.enabled ?: JSONObject.NULL)
                                .put(
                                    "broadcast_enabled",
                                    network.broadcastEnabled ?: JSONObject.NULL,
                                ),
                        )
                    }
                },
            )
    }

    private fun backupFromJson(json: JSONObject): WifiBackup {
        if (json.optInt("version") != 1) throw WifiBackupException("Unsupported backup version.")
        val manifest = WifiBackupManifest(
            id = json.getString("id"),
            createdAt = json.getString("created_at"),
            gatewayModel = json.optString("gateway_model"),
            firmwareVersion = json.optString("firmware_version"),
            networkCount = json.optInt("network_count"),
            passwordCount = json.optInt("password_count"),
        )
        val networksJson = json.optJSONArray("networks") ?: JSONArray()
        val networks = (0 until networksJson.length()).mapNotNull { index ->
            val value = networksJson.optJSONObject(index) ?: return@mapNotNull null
            val pathJson = value.optJSONArray("source_path") ?: JSONArray()
            WifiNetworkProfile(
                sourcePath = (0 until pathJson.length()).map { pathJson.optString(it) },
                ssid = value.optString("ssid"),
                password = if (value.has("password") && !value.isNull("password")) {
                    value.optString("password").takeIf { it.isNotBlank() }
                } else {
                    null
                },
                band = value.optString("band"),
                enabled = value.optBooleanOrNull("enabled"),
                broadcastEnabled = value.optBooleanOrNull("broadcast_enabled"),
            )
        }
        return WifiBackup(manifest, networks)
    }

    private companion object {
        val ID_FORMAT: DateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(ZoneOffset.UTC)
        val ID_PATTERN = Regex("wifi-\\d{8}-\\d{6}-\\d{3}")
    }
}

class WifiBackupException(message: String) : IOException(message)

private fun JSONObject.optBooleanOrNull(key: String): Boolean? {
    if (!has(key) || isNull(key)) return null
    return optBoolean(key)
}
