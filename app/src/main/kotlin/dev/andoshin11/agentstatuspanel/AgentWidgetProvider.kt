package dev.andoshin11.agentstatuspanel

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle

/** The system also asks for an update every 30 minutes, which clears a session gone stale. */
class AgentWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        update(context, manager, id)
    }

    companion object {
        private const val DEFAULT_HEIGHT_DP = 240

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, AgentWidgetProvider::class.java))
                .forEach { update(context, manager, it) }
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val sessions = SessionStore(context).read(System.currentTimeMillis())
            manager.updateAppWidget(id, WidgetRenderer(context).render(sessions, heightOf(context, manager, id)))
        }

        /** The system gives a range: the tall end applies in portrait, the short end in landscape. */
        private fun heightOf(context: Context, manager: AppWidgetManager, id: Int): Int {
            val options = manager.getAppWidgetOptions(id)
            val portrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            val key = if (portrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
            return options.getInt(key).takeIf { it > 0 } ?: DEFAULT_HEIGHT_DP
        }
    }
}
