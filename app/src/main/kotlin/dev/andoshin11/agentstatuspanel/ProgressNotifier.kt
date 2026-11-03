package dev.andoshin11.agentstatuspanel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Shows one notification per session: a Live Update while it works, the widget's card once finished.
 * A Live Update cannot have a custom layout, so only its colors follow the widget.
 */
class ProgressNotifier(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun show(message: ProgressMessage.Update) {
        createChannels()
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val notification = if (message.finished) finished(message) else active(message)
        manager.notify(message.id, NOTIFICATION_ID, notification.build())
    }

    fun remove(id: String) = manager.cancel(id, NOTIFICATION_ID)

    private fun title(message: ProgressMessage.Update) = message.goal
        .ifEmpty { message.workspace }
        .ifEmpty { context.getString(R.string.agent_default) }

    private fun active(message: ProgressMessage.Update): NotificationCompat.Builder {
        val style = NotificationCompat.ProgressStyle()
            .setStyledByProgress(true)
            .setProgressSegments(
                progressSegments(message.total).map {
                    NotificationCompat.ProgressStyle.Segment(it).setColor(context.getColor(R.color.ms_bar))
                },
            )
            .setProgress(message.done)
        return base(message, CHANNEL_PROGRESS)
            .setStyle(style)
            .setContentText(message.step.ifEmpty { context.getString(R.string.progress_text, "${message.done}", "${message.total}") })
            .setSubText(message.workspace.ifEmpty { null })
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText("${message.done}/${message.total}")
            .setOnlyAlertOnce(true)
            .setSilent(true)
            // If the Mac mini goes away, nothing would ever end this. It outlasts the daemon's staleMinutes.
            .setTimeoutAfter(ACTIVE_TIMEOUT_MS)
            .setDeleteIntent(dismissIntent(message.id))
    }

    private fun finished(message: ProgressMessage.Update): NotificationCompat.Builder {
        val session = SessionBoard.apply(emptyList(), message).single()
        val renderer = WidgetRenderer(context)
        return base(message, CHANNEL_FINISHED)
            .setContentText(context.getString(R.string.finished_text, "${message.done}", "${message.total}"))
            .setSubText(message.workspace.ifEmpty { null })
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(renderer.notificationSummary(session))
            .setCustomBigContentView(renderer.notificationCard(session))
            .setAutoCancel(true)
    }

    private fun base(message: ProgressMessage.Update, channel: String) = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_notification)
        .setColor(context.getColor(R.color.ms_bar))
        .setContentTitle(title(message))
        .setContentIntent(openApp())

    private fun openApp() = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun dismissIntent(id: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        id.hashCode(),
        Intent(context, DismissReceiver::class.java).putExtra(DismissReceiver.EXTRA_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun createChannels() {
        val system = context.getSystemService(NotificationManager::class.java)
        system.createNotificationChannel(
            // A Live Update's channel must not be IMPORTANCE_MIN. Each update itself stays silent.
            NotificationChannel(CHANNEL_PROGRESS, context.getString(R.string.channel_progress), NotificationManager.IMPORTANCE_DEFAULT),
        )
        system.createNotificationChannel(
            NotificationChannel(CHANNEL_FINISHED, context.getString(R.string.channel_finished), NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    private companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_PROGRESS = "progress"
        const val CHANNEL_FINISHED = "finished"
        const val ACTIVE_TIMEOUT_MS = 3L * 60 * 60 * 1000
    }
}
