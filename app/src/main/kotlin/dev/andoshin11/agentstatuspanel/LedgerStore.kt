package dev.andoshin11.agentstatuspanel

import android.content.Context
import androidx.core.content.edit

/** Keeps the ledger and the FCM token in the app's private storage. */
class LedgerStore(context: Context) {
    private val prefs = context.getSharedPreferences("ledger", Context.MODE_PRIVATE)
    private val tokenPrefs = context.getSharedPreferences("token", Context.MODE_PRIVATE)

    fun <T> update(now: Long, change: (SessionLedger) -> T): T = synchronized(LOCK) {
        val entries = prefs.all
            .filterKeys { it.startsWith(AT_PREFIX) }
            .mapNotNull { (key, value) -> (value as? Long)?.let { key.removePrefix(AT_PREFIX) to it } }
            .toMap()
        val ledger = SessionLedger(entries)
        val result = change(ledger)
        ledger.prune(now - RETENTION_MS)
        prefs.edit {
            clear()
            ledger.entries.forEach { (id, at) -> putLong(AT_PREFIX + id, at) }
        }
        result
    }

    fun saveToken(token: String) = tokenPrefs.edit { putString(TOKEN, token) }

    private companion object {
        val LOCK = Any()
        const val AT_PREFIX = "at:"
        const val TOKEN = "fcm_token"
        const val RETENTION_MS = 7L * 24 * 60 * 60 * 1000
    }
}
