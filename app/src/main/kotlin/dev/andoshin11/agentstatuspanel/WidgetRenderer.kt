package dev.andoshin11.agentstatuspanel

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.RemoteViews

/** Draws the widget with the design of the Milestone panel in its dark theme. */
class WidgetRenderer(private val context: Context) {
    private val packageName = context.packageName
    private fun color(id: Int) = context.getColor(id)

    fun render(sessions: List<WidgetSession>, heightDp: Int): RemoteViews {
        val views = RemoteViews(packageName, R.layout.widget_root)
        views.setOnClickPendingIntent(R.id.widget_root, openApp())
        val arrangement = SessionBoard.arrange(sessions)
        views.setViewVisibility(R.id.empty, if (arrangement == null) View.VISIBLE else View.GONE)
        val content = if (arrangement == null) View.GONE else View.VISIBLE
        views.setViewVisibility(R.id.header, content)
        views.setViewVisibility(R.id.card, content)
        views.setViewVisibility(R.id.others, View.GONE)
        if (arrangement != null) showContent(views, arrangement, heightDp)
        return views
    }

    private fun showContent(views: RemoteViews, arrangement: Arrangement, heightDp: Int) {
        val main = arrangement.main
        val fit = fitWidget(heightDp, main.tasks, arrangement.others.size)

        views.setTextViewText(R.id.workspace, name(main))
        val label = label(main)
        views.setTextViewText(R.id.agent, label?.first.orEmpty())
        label?.let { views.setTextColor(R.id.agent, it.second) }

        views.setViewVisibility(R.id.goal, if (main.goal.isEmpty()) View.GONE else View.VISIBLE)
        views.setTextViewText(R.id.goal, main.goal)
        views.setTextViewText(R.id.count, count(main))
        views.setProgressBar(R.id.bar, main.total.coerceAtLeast(1), main.done, false)

        views.removeAllViews(R.id.tasks)
        main.tasks.drop(fit.taskStart).take(fit.taskCount).forEach { views.addView(R.id.tasks, task(it)) }

        views.removeAllViews(R.id.rows)
        arrangement.others.take(fit.otherCount).forEach { views.addView(R.id.rows, row(it)) }
        views.setTextViewText(R.id.others_title, context.getString(R.string.widget_others, arrangement.others.size))
        views.setViewVisibility(R.id.others, if (fit.otherCount > 0) View.VISIBLE else View.GONE)
    }

    private fun name(session: WidgetSession) =
        session.workspace.ifEmpty { context.getString(R.string.widget_session_default) }

    private fun count(session: WidgetSession) =
        context.getString(R.string.widget_count, session.done, session.total)

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

    private fun row(session: WidgetSession): RemoteViews {
        val views = RemoteViews(packageName, R.layout.widget_row)
        views.setViewVisibility(R.id.check, if (session.finished) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.dot, if (session.finished) View.GONE else View.VISIBLE)
        views.setImageViewResource(R.id.dot, if (session.running) R.drawable.widget_dot_running else R.drawable.widget_dot)

        val text = SpannableStringBuilder(name(session))
        if (session.goal.isNotEmpty()) {
            val start = text.length
            text.append("  ").append(session.goal)
            text.setSpan(ForegroundColorSpan(color(R.color.ms_muted)), start, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(AbsoluteSizeSpan(11, true), start, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        views.setTextViewText(R.id.name, text)

        val working = session.running
        views.setViewVisibility(R.id.label, if (working) View.VISIBLE else View.GONE)
        if (working) {
            views.setTextViewText(R.id.label, context.getString(R.string.widget_working))
            views.setTextColor(R.id.label, color(R.color.ms_working))
        }
        views.setTextViewText(R.id.of, count(session))
        views.setProgressBar(R.id.bar, session.total.coerceAtLeast(1), session.done, false)
        return views
    }

    private fun openApp() = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )
}
