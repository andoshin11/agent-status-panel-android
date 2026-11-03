package dev.andoshin11.agentstatuspanel

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context

/** The system also asks for an update every 30 minutes, which clears a session gone stale. */
class AgentWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        update(context, manager, ids)
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            update(context, manager, manager.getAppWidgetIds(ComponentName(context, AgentWidgetProvider::class.java)))
        }

        private fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val sessions = SessionStore(context).read(System.currentTimeMillis())
            manager.updateAppWidget(ids, WidgetRenderer(context).render(sessions))
        }
    }
}
