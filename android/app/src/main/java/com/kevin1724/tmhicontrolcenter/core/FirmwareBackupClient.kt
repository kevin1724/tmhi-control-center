package com.kevin1724.tmhicontrolcenter.core

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class FirmwareBackupClient(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
) {
    suspend fun createBackup(settings: AppSettings): FirmwareBackupManifest {
        if (settings.advancedMode != AdvancedMode.G4arUnlockLab) {
            throw FirmwareBackupException("Enable G4AR unlock / radio lab first.")
        }
        if (!settings.advancedAcknowledged) {
            throw FirmwareBackupException("Acknowledge the G4AR firmware risk warning first.")
        }
        val adapterUrl = settings.adapterUrl.trim().trimEnd('/')
        if (adapterUrl.isBlank()) {
            throw FirmwareBackupException("Configure a local adapter URL first.")
        }

        val payload = JSONObject()
            .put("device", "Arcadyan TMO-G4AR")
            .put("reason", "android_ui_request")
            .put("radio_profile", settings.radioProfile.name)
            .put("requested_at", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .put(
                "expected_artifacts",
                JSONArray(
                    listOf(
                        "stock-firmware.bin",
                        "partition-table.txt",
                        "calibration-and-identity-backup.tar",
                        "restore-notes.md",
                        "SHA256SUMS",
                    ),
                ),
            )

        val responseJson = postBackup("$adapterUrl/g4ar/firmware/backup", payload)
        return saveBackup(responseJson)
    }

    fun listBackups(): List<FirmwareBackupManifest> {
        val root = backupRoot()
        if (!root.exists()) return emptyList()
        return root.listFiles().orEmpty().mapNotNull { dir ->
            val manifest = File(dir, "backup-manifest.json")
            if (!manifest.exists()) return@mapNotNull null
            runCatching {
                val json = JSONObject(manifest.readText())
                FirmwareBackupManifest(
                    id = json.optString("id", dir.name),
                    firmwareVersion = json.optString("firmware_version"),
                    artifactCount = json.optInt("artifact_count", 0),
                    path = dir.absolutePath,
                )
            }.getOrNull()
        }.sortedByDescending { it.id }
    }

    private suspend fun postBackup(url: String, payload: JSONObject): JSONObject {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "tmhi-control-center-android/0.1")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw FirmwareBackupException("Adapter returned HTTP ${response.code}: ${body.take(160)}")
                    }
                    JSONObject(body)
                }
            } catch (exc: Exception) {
                if (exc is FirmwareBackupException) throw exc
                throw FirmwareBackupException("Backup request failed: ${exc.message.orEmpty()}")
            }
        }
    }

    private suspend fun saveBackup(adapterPayload: JSONObject): FirmwareBackupManifest {
        return withContext(Dispatchers.IO) {
            val id = "g4ar-" + OffsetDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            val dir = File(backupRoot(), id)
            if (!dir.mkdirs()) {
                throw FirmwareBackupException("Could not create backup folder.")
            }

            val savedArtifacts = JSONArray()
            val artifacts = adapterPayload.optJSONArray("artifacts") ?: JSONArray()
            for (index in 0 until artifacts.length()) {
                val artifact = artifacts.optJSONObject(index) ?: continue
                savedArtifacts.put(saveArtifact(dir, artifact, index + 1))
            }

            val manifest = JSONObject()
                .put("id", id)
                .put("created_at", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .put("device", adapterPayload.optString("device", "Arcadyan TMO-G4AR"))
                .put("firmware_version", adapterPayload.optString("firmware_version"))
                .put("hardware_revision", adapterPayload.optString("hardware_revision"))
                .put("status", "saved")
                .put("artifact_count", savedArtifacts.length())
                .put("artifacts", savedArtifacts)
                .put("notes", adapterPayload.optJSONArray("notes") ?: JSONArray())

            File(dir, "backup-manifest.json").writeText(manifest.toString(2))

            FirmwareBackupManifest(
                id = id,
                firmwareVersion = manifest.optString("firmware_version"),
                artifactCount = savedArtifacts.length(),
                path = dir.absolutePath,
            )
        }
    }

    private fun saveArtifact(dir: File, artifact: JSONObject, index: Int): JSONObject {
        val name = safeName(artifact.optString("name"), index)
        val encoded = artifact.optString("content_base64")
        val summary = JSONObject()
            .put("name", name)
            .put("saved", false)
            .put("declared_sha256", artifact.optString("sha256"))
        if (encoded.isBlank()) {
            return summary.put("note", "Adapter did not include inline content.")
        }

        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        val actualHash = sha256(bytes)
        val declaredHash = artifact.optString("sha256").lowercase()
        if (declaredHash.isNotBlank() && declaredHash != actualHash) {
            throw FirmwareBackupException("Artifact $name SHA-256 does not match.")
        }
        val file = File(dir, name)
        file.writeBytes(bytes)
        return summary
            .put("saved", true)
            .put("sha256", actualHash)
            .put("size_bytes", bytes.size)
            .put("path", file.absolutePath)
    }

    private fun backupRoot(): File {
        return File(context.filesDir, "g4ar-firmware-backups").also { it.mkdirs() }
    }

    private fun safeName(raw: String, index: Int): String {
        val base = raw.replace("\\", "/").substringAfterLast("/").trim()
        val cleaned = base.replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('.', '_')
        return cleaned.ifBlank { "artifact-$index.bin" }.take(120)
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}

class FirmwareBackupException(message: String) : IOException(message)
