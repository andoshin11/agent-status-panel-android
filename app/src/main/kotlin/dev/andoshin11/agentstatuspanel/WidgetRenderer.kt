package dev.andoshin11.agentstatuspanel

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.RemoteViews

/**
 * Draws the widget with the design of the Milestone panel in its dark theme: every session as an
 * open card with its tasks, in a list that scrolls and fills the widget.
 */
class WidgetRenderer(private val context: Context) {
    private val packageName = context.packageName
    private fun color(id: Int) = context.getColor(id)

    fun render(sessions: List<WidgetSession>): RemoteViews {
        val views = RemoteViews(packageName, R.layout.widget_root)
        val items = RemoteViews.RemoteCollectionItems.Builder()
            .setHasStableIds(true)
            .setViewTypeCount(1)
        SessionBoard.ordered(sessions).forEach { session ->
            val item = session(session).apply { setOnClickFillInIntent(R.id.session, Intent()) }
            items.addItem(session.id.hashCode().toLong(), item)
        }
        views.setRemoteAdapter(R.id.list, items.build())
        views.setViewVisibility(R.id.empty, if (sessions.isEmpty()) View.VISIBLE else View.GONE)
        // Items of a list take their click from this template. It stays mutable for their fill-in.
        views.setPendingIntentTemplate(R.id.list, openApp(PendingIntent.FLAG_MUTABLE))
        views.setOnClickPendingIntent(R.id.empty, openApp(PendingIntent.FLAG_IMMUTABLE))
        return views
    }

    private fun session(session: WidgetSession): RemoteViews {
        val views = RemoteViews(packageName, R.layout.widget_session)
        views.setTextViewText(R.id.workspace, session.workspace.ifEmpty { context.getString(R.string.widget_session_default) })
        val label = label(session)
        views.setTextViewText(R.id.agent, label?.first.orEmpty())
        label?.let { views.setTextColor(R.id.agent, it.second) }

        views.setViewVisibility(R.id.goal, if (session.goal.isEmpty()) View.GONE else View.VISIBLE)
        views.setTextViewText(R.id.goal, session.goal)
        views.setTextViewText(R.id.count, context.getString(R.string.widget_count, session.done, session.total))
        views.setProgressBar(R.id.bar, session.total.coerceAtLeast(1), session.done, false)

        views.removeAllViews(R.id.tasks)
        session.tasks.forEach { views.addView(R.id.tasks, task(it)) }
        return views
    }

    private fun label(session: WidgetSession): Pair<String, Int>? = when {
        session.finished -> context.getString(R.string.widget_finished) to color(R.color.ms_done)
        session.running -> context.getString(R.string.widget_working) to color(R.color.ms_working)
        else -> null
    }

    private fun task(task: TaskRow): RemoteViews {
        val views = RemoteViews(packageName, R.layout.widget_task)
        val (mark, markColor) = when (task.status) {
            TaskStatus.COMPLETED -> R.string.mark_completed to R.color.ms_done
            TaskStatus.IN_PROGRESS -> R.string.mark_in_progress to R.color.ms_todo
            TaskStatus.PENDING -> R.string.mark_pending to R.color.ms_muted
        }
        views.setTextViewText(R.id.mark, context.getString(mark))
        views.setTextColor(R.id.mark, color(markColor))

        val subject = SpannableString(task.subject)
        when (task.status) {
            TaskStatus.COMPLETED -> {
                subject.setSpan(StrikethroughSpan(), 0, subject.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                views.setTextColor(R.id.subject, color(R.color.ms_muted))
            }
            TaskStatus.IN_PROGRESS -> {
                subject.setSpan(StyleSpan(Typeface.BOLD), 0, subject.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                views.setTextColor(R.id.subject, color(R.color.ms_foreground))
            }
            TaskStatus.PENDING -> views.setTextColor(R.id.subject, color(R.color.ms_foreground))
        }
        views.setTextViewText(R.id.subject, subject)
        return views
    }

    private fun openApp(mutability: Int) = PendingIntent.getActivity(
        context,
        mutability,
        Intent(context, MainActivity::class.java),
        mutability or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
