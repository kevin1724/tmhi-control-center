package com.kevin1724.tmhicontrolcenter.core

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("tmhi_control_center", Context.MODE_PRIVATE)

    fun load(): AppSettings {
        return AppSettings(
            gatewayHost = prefs.getString(KEY_GATEWAY_HOST, "192.168.12.1").orEmpty(),
            gatewayPort = prefs.getInt(KEY_GATEWAY_PORT, 8080),
            gatewayUsername = prefs.getString(KEY_GATEWAY_USERNAME, "admin").orEmpty(),
            gatewayPassword = prefs.getString(KEY_GATEWAY_PASSWORD, "").orEmpty(),
            openCellIdKey = prefs.getString(KEY_OPENCELLID_KEY, "").orEmpty(),
            mapLatitude = nullableDouble(KEY_MAP_LATITUDE),
            mapLongitude = nullableDouble(KEY_MAP_LONGITUDE),
            mapRadiusKm = double(KEY_MAP_RADIUS_KM, 0.8),
            advancedMode = enumValue(KEY_ADVANCED_MODE, AdvancedMode.Disabled),
            adapterUrl = prefs.getString(KEY_ADAPTER_URL, "").orEmpty(),
            radioProfile = enumValue(KEY_RADIO_PROFILE, RadioProfile.Auto),
            advancedAcknowledged = prefs.getBoolean(KEY_ADVANCED_ACKNOWLEDGED, false),
        )
    }

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_GATEWAY_HOST, settings.gatewayHost.trim())
            .putInt(KEY_GATEWAY_PORT, settings.gatewayPort)
            .putString(KEY_GATEWAY_USERNAME, settings.gatewayUsername.trim())
            .putString(KEY_GATEWAY_PASSWORD, settings.gatewayPassword)
            .putString(KEY_OPENCELLID_KEY, settings.openCellIdKey.trim())
            .putDoubleOrRemove(KEY_MAP_LATITUDE, settings.mapLatitude)
            .putDoubleOrRemove(KEY_MAP_LONGITUDE, settings.mapLongitude)
            .putString(KEY_MAP_RADIUS_KM, settings.mapRadiusKm.toString())
            .putString(KEY_ADVANCED_MODE, settings.advancedMode.name)
            .putString(KEY_ADAPTER_URL, settings.adapterUrl.trim())
            .putString(KEY_RADIO_PROFILE, settings.radioProfile.name)
            .putBoolean(KEY_ADVANCED_ACKNOWLEDGED, settings.advancedAcknowledged)
            .apply()
    }

    private fun nullableDouble(key: String): Double? {
        val value = prefs.getString(key, null) ?: return null
        return value.toDoubleOrNull()
    }

    private fun double(key: String, default: Double): Double {
        return prefs.getString(key, null)?.toDoubleOrNull() ?: default
    }

    private inline fun <reified T : Enum<T>> enumValue(key: String, default: T): T {
        val raw = prefs.getString(key, null) ?: return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }

    private fun android.content.SharedPreferences.Editor.putDoubleOrRemove(
        key: String,
        value: Double?,
    ): android.content.SharedPreferences.Editor {
        return if (value == null) {
            remove(key)
        } else {
            putString(key, value.toString())
        }
    }

    private companion object {
        const val KEY_GATEWAY_HOST = "gateway_host"
        const val KEY_GATEWAY_PORT = "gateway_port"
        const val KEY_GATEWAY_USERNAME = "gateway_username"
        const val KEY_GATEWAY_PASSWORD = "gateway_password"
        const val KEY_OPENCELLID_KEY = "opencellid_key"
        const val KEY_MAP_LATITUDE = "map_latitude"
        const val KEY_MAP_LONGITUDE = "map_longitude"
        const val KEY_MAP_RADIUS_KM = "map_radius_km"
        const val KEY_ADVANCED_MODE = "advanced_mode"
        const val KEY_ADAPTER_URL = "adapter_url"
        const val KEY_RADIO_PROFILE = "radio_profile"
        const val KEY_ADVANCED_ACKNOWLEDGED = "advanced_acknowledged"
    }
}
