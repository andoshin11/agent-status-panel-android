package dev.andoshin11.agentstatuspanel

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ProgressMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        LedgerStore(this).saveToken(token)
    }

    override fun onMessageReceived(remote: RemoteMessage) {
        val message = ProgressMessage.parse(remote.data) ?: return
        val now = System.currentTimeMillis()
        if (!LedgerStore(this).update(now) { it.accept(message) }) return
        val notifier = AttentionNotifier(this)
        when (message) {
            is ProgressMessage.Attention -> notifier.show(message)
            is ProgressMessage.Ended -> notifier.remove(message.id)
            is ProgressMessage.Update -> Unit
        }
        SessionStore(this).apply(message, now)
        AgentWidgetProvider.refresh(this)
    }
}
