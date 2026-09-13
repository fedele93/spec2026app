package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import java.util.UUID

/**
 * Preferenze locali: identificativo del dispositivo, URL del server, token organizzatore
 * e stato della sincronizzazione. I valori di default vengono dal file .env (BuildConfig).
 */
class AppPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("neuroparty_prefs", Context.MODE_PRIVATE)

    /** Stringa casuale che identifica questo telefono verso il server (header X-Client-Id). */
    val clientId: String
        get() = prefs.getString(KEY_CLIENT_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_CLIENT_ID, it).apply()
        }

    /** URL base del backend (es. https://festa.tuodominio.it). Vuoto = modalità locale/demo. */
    var apiBaseUrl: String
        get() = prefs.getString(KEY_API_URL, null) ?: BuildConfig.API_BASE_URL
        set(value) = prefs.edit().putString(KEY_API_URL, value.trim().trimEnd('/')).apply()

    /** Token organizzatore (header X-Admin-Token). */
    var adminToken: String
        get() = prefs.getString(KEY_ADMIN_TOKEN, null) ?: BuildConfig.ADMIN_TOKEN
        set(value) = prefs.edit().putString(KEY_ADMIN_TOKEN, value.trim()).apply()

    /** Ultima versione dei dati scaricata dal server (per il polling). */
    var dataVersion: Long
        get() = prefs.getLong(KEY_DATA_VERSION, -1L)
        set(value) = prefs.edit().putLong(KEY_DATA_VERSION, value).apply()

    /** Timestamp (ms) dell'ultima notifica già mostrata come notifica di sistema. */
    var lastSeenNotificationTs: Long
        get() = prefs.getLong(KEY_LAST_SEEN_NOTIFICATION, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SEEN_NOTIFICATION, value).apply()

    val serverConfigured: Boolean
        get() = apiBaseUrl.isNotBlank()

    fun resetSyncState() {
        prefs.edit().remove(KEY_DATA_VERSION).remove(KEY_LAST_SEEN_NOTIFICATION).apply()
    }

    companion object {
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_API_URL = "api_base_url"
        private const val KEY_ADMIN_TOKEN = "admin_token"
        private const val KEY_DATA_VERSION = "data_version"
        private const val KEY_LAST_SEEN_NOTIFICATION = "last_seen_notification_ts"
    }
}
