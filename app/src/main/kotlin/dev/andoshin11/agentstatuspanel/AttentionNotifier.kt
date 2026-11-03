package dev.andoshin11.agentstatuspanel

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** The only notifications of the app: one per workspace whose agent is waiting for the user. */
class AttentionNotifier(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun show(message: ProgressMessage.Attention) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_attention), NotificationManager.IMPORTANCE_HIGH),
        )
        val title = message.workspace.ifEmpty { context.getString(R.string.agent_default) }
        val text = context.getString(reason(message.reason))
        val card = RemoteViews(context.packageName, R.layout.notification_attention).apply {
            setTextViewText(R.id.workspace, title)
            setTextViewText(R.id.reason, text)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(context.getColor(R.color.ms_todo))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(card)
            .setCustomHeadsUpContentView(card)
            .setContentIntent(openApp())
            .setAutoCancel(true)
            // The reason can change while the agent still waits; that is not a new request.
            .setOnlyAlertOnce(true)
            .build()
        manager.notify(message.id, NOTIFICATION_ID, notification)
    }

    fun remove(id: String) = manager.cancel(id, NOTIFICATION_ID)

    private fun reason(code: String) = when (code) {
        "permission" -> R.string.attention_permission
        "blocked" -> R.string.attention_blocked
        "waiting" -> R.string.attention_waiting
        else -> R.string.attention_default
    }

    private fun openApp() = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL = "attention"
    }
}
