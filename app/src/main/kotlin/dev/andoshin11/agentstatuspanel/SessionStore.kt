package dev.andoshin11.agentstatuspanel

import android.content.Context
import androidx.core.content.edit
import org.json.JSONException

/** Keeps the widget's sessions in the app's private storage. */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("sessions", Context.MODE_PRIVATE)

    fun read(now: Long): List<WidgetSession> = synchronized(LOCK) { SessionBoard.fresh(load(), now) }

    fun apply(message: ProgressMessage, now: Long) = synchronized(LOCK) {
        val sessions = SessionBoard.fresh(SessionBoard.apply(load(), message), now)
        prefs.edit { putString(KEY, SessionBoard.toJson(sessions)) }
    }

    private fun load(): List<WidgetSession> = try {
        SessionBoard.fromJson(prefs.getString(KEY, null) ?: "[]")
    } catch (_: JSONException) {
        emptyList()
    }

    private companion object {
        val LOCK = Any()
        const val KEY = "sessions"
    }
}
