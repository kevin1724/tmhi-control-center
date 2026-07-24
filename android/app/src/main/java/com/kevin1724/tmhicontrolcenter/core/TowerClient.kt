package com.kevin1724.tmhicontrolcenter.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

class TowerClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
) {
    suspend fun loadMap(
        settings: AppSettings,
        overview: GatewayOverview?,
        includeNearby: Boolean,
    ): TowerMapData {
        val center = mapCenter(settings)
        val identity = towerIdentityFromOverview(overview)
        val errors = mutableListOf<String>()
        var connected: Tower? = null
        var nearby = emptyList<Tower>()

        if (includeNearby && settings.openCellIdKey.isBlank()) {
            errors += "OpenCellID key is not configured."
        }

        if (includeNearby && settings.openCellIdKey.isNotBlank()) {
            if (identity.queryable) {
                runCatching {
                    connected = connectedTower(settings.openCellIdKey, identity, center)
                }.onFailure { errors += it.message.orEmpty() }
            }
            runCatching {
                nearby = nearbyTowers(settings.openCellIdKey, identity, center, settings.mapRadiusKm)
            }.onFailure { errors += it.message.orEmpty() }

            if (connected == null && nearby.isNotEmpty()) {
                connected = estimateConnectedTower(identity, nearby)
            }
        }

        return TowerMapData(
            center = center,
            identity = identity,
            connectedTower = connected,
            nearby = nearby,
            errors = errors,
        )
    }

    private suspend fun connectedTower(
        key: String,
        identity: TowerIdentity,
        center: MapCenter,
    ): Tower? {
        val params = mapOf(
            "key" to key,
            "mcc" to identity.mcc,
            "mnc" to identity.mnc,
            "lac" to identity.lac,
            "cellid" to identity.cellId,
            "radio" to identity.radio.ifBlank { null },
            "format" to "json",
        )
        val payload = getOpenCellId("/cell/get", params)
        if (!payload.has("lat") || !payload.has("lon")) return null
        return towerFromCell(payload, center, connected = true)
    }

    private suspend fun nearbyTowers(
        key: String,
        identity: TowerIdentity,
        center: MapCenter,
        radiusKm: Double,
    ): List<Tower> {
        val limitedRadius = radiusKm.coerceAtMost(0.85)
        val params = mapOf(
            "key" to key,
            "BBOX" to bbox(center.latitude, center.longitude, limitedRadius).joinToString(",") {
                "%.6f".format(it)
            },
            "limit" to 50,
            "format" to "json",
            "mcc" to identity.mcc,
            "mnc" to identity.mnc,
            "radio" to identity.radio.ifBlank { null },
        )
        val payload = getOpenCellId("/cell/getInArea", params)
        val cells = payload.optJSONArray("cells") ?: return emptyList()
        return (0 until cells.length()).mapNotNull { index ->
            cells.optJSONObject(index)?.let { towerFromCell(it, center, connected = false) }
        }
    }

    private suspend fun getOpenCellId(path: String, params: Map<String, Any?>): JSONObject {
        return withContext(Dispatchers.IO) {
            val url = "https://opencellid.org$path?${query(params)}"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "tmhi-control-center-android/0.1")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw TowerException("OpenCellID returned HTTP ${response.code}.")
                    }
                    val json = JSONObject(body)
                    val error = json.optString("error").ifBlank {
                        json.optJSONObject("err")?.optString("info").orEmpty()
                    }
                    if (error.isNotBlank() && !"no cells found".equals(error, ignoreCase = true)) {
                        throw TowerException("OpenCellID error: $error")
                    }
                    json
                }
            } catch (exc: Exception) {
                if (exc is TowerException) throw exc
                throw TowerException("OpenCellID lookup failed: ${exc.message.orEmpty()}")
            }
        }
    }

    private fun towerIdentityFromOverview(overview: GatewayOverview?): TowerIdentity {
        val connection = overview?.connection.orEmpty()
        val plmn = connection["PLMN"].orEmpty()
        val plmnMcc = plmn.take(3).toIntOrNull()
        val plmnMnc = plmn.drop(3).takeIf { it.isNotBlank() }?.toIntOrNull()
        val band = connection["Band"].orEmpty()
        val network = connection["Network"].orEmpty()
        return TowerIdentity(
            mcc = connection["MCC"]?.toIntOrNull() ?: plmnMcc,
            mnc = connection["MNC"]?.toIntOrNull() ?: plmnMnc,
            lac = connection["TAC"]?.toIntOrNull() ?: connection["LAC"]?.toIntOrNull(),
            cellId = connection["Cell ID"]?.toLongOrNull(),
            pci = connection["PCI"]?.toIntOrNull(),
            band = band,
            radio = radioFromValues(network, band),
            networkType = network,
        )
    }

    private fun mapCenter(settings: AppSettings): MapCenter {
        val latitude = settings.mapLatitude
        val longitude = settings.mapLongitude
        return if (latitude != null && longitude != null) {
            MapCenter(latitude, longitude, "saved_home")
        } else {
            MapCenter(39.8283, -98.5795, "default_us")
        }
    }

    private fun towerFromCell(cell: JSONObject, center: MapCenter, connected: Boolean): Tower? {
        val latitude = cell.optDoubleOrNull("lat") ?: return null
        val longitude = cell.optDoubleOrNull("lon") ?: return null
        val cellId = cell.optLongOrNull("cellid") ?: cell.optLongOrNull("cid") ?: cell.optLongOrNull("bid")
        val radio = cell.optString("radio")
        val id = listOf(radio, cell.optIntOrNull("mcc"), cell.optIntOrNull("mnc"), cellId)
            .filterNotNull()
            .joinToString("-")
            .ifBlank { "tower-${latitude}-${longitude}" }
        return Tower(
            id = id,
            label = "${radio.ifBlank { "Cell" }} ${cellId ?: "unknown"}",
            latitude = latitude,
            longitude = longitude,
            distanceKm = round(distanceKm(center.latitude, center.longitude, latitude, longitude) * 100.0) / 100.0,
            radio = radio,
            cellId = cellId,
            lac = cell.optIntOrNull("lac") ?: cell.optIntOrNull("tac"),
            averageSignal = cell.optIntOrNull("averageSignalStrength"),
            rangeMeters = cell.optIntOrNull("range"),
            samples = cell.optIntOrNull("samples"),
            connected = connected,
        )
    }

    private fun estimateConnectedTower(identity: TowerIdentity, nearby: List<Tower>): Tower? {
        val identityRadio = identity.radio.lowercase()
        val identityCell = identity.cellId
        return nearby.maxByOrNull { tower ->
            var score = 0
            if (identityRadio.isNotBlank() && tower.radio.lowercase() == identityRadio) score += 35
            if (identityCell != null && tower.cellId == identityCell) score += 100
            if (identity.lac != null && tower.lac == identity.lac) score += 12
            score - ((tower.distanceKm ?: 99.0) * 2).toInt()
        }?.copy(connected = true)
    }

    private fun radioFromValues(network: String, band: String): String {
        val text = "$network $band".lowercase()
        return when {
            text.contains("nr") || text.contains("5g") || text.contains("n41") || text.contains("n71") -> "NR"
            text.contains("lte") || text.contains("4g") || Regex("\\bb\\d+").containsMatchIn(text) -> "LTE"
            else -> ""
        }
    }

    private fun bbox(latitude: Double, longitude: Double, radiusKm: Double): List<Double> {
        val latDelta = radiusKm / 111.0
        val lonDelta = radiusKm / (111.0 * cos(Math.toRadians(latitude))).coerceAtLeast(0.01)
        return listOf(latitude - latDelta, longitude - lonDelta, latitude + latDelta, longitude + lonDelta)
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        return earthKm * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}

class TowerException(message: String) : IOException(message)

private fun JSONObject.optDoubleOrNull(key: String): Double? {
    return if (has(key) && !isNull(key)) optDouble(key) else null
}

private fun JSONObject.optIntOrNull(key: String): Int? {
    return if (has(key) && !isNull(key)) optInt(key) else null
}

private fun JSONObject.optLongOrNull(key: String): Long? {
    return if (has(key) && !isNull(key)) optLong(key) else null
}
