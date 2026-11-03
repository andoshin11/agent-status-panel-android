package dev.andoshin11.agentstatuspanel

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ProgressMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        LedgerStore(this).saveToken(token)
    }

    override fun onMessageReceived(remote: RemoteMessage) {
        val message = ProgressMessage.parse(remote.data) ?: return
        val decision = LedgerStore(this).update(System.currentTimeMillis()) { it.decide(message) }
        val notifier = ProgressNotifier(this)
        when (decision) {
            SessionLedger.Decision.SHOW -> notifier.show(message as ProgressMessage.Update)
            SessionLedger.Decision.REMOVE -> notifier.remove(message.id)
            SessionLedger.Decision.IGNORE -> Unit
        }
    }
}
