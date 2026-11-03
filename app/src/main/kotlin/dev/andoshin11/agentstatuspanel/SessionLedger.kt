package dev.andoshin11.agentstatuspanel

/**
 * Remembers per session what the phone last accepted. FCM does not keep messages in order, and a
 * Live Update the user dismissed must not come back. Entries are kept after a session ends, so a
 * late message cannot bring it back either, and removed once they are old.
 */
class SessionLedger(entries: Map<String, Long>, dismissed: Set<String>) {
    private val lastAt = entries.toMutableMap()
    private val dismissedIds = dismissed.toMutableSet()

    val entries: Map<String, Long> get() = lastAt
    val dismissed: Set<String> get() = dismissedIds

    /** What to do with a message, recording it unless it is out of date. */
    fun decide(message: ProgressMessage): Decision {
        val last = lastAt[message.id]
        if (last != null && message.at <= last) return Decision.IGNORE
        lastAt[message.id] = message.at
        return when {
            message is ProgressMessage.Ended -> {
                dismissedIds.remove(message.id)
                Decision.REMOVE
            }
            message is ProgressMessage.Update && message.finished -> {
                dismissedIds.remove(message.id)
                Decision.SHOW
            }
            message.id in dismissedIds -> Decision.HIDE
            else -> Decision.SHOW
        }
    }

    fun markDismissed(id: String) {
        dismissedIds.add(id)
    }

    fun prune(olderThan: Long) {
        val old = lastAt.filterValues { it < olderThan }.keys
        lastAt.keys.removeAll(old)
        dismissedIds.removeAll(old)
    }

    /** HIDE is an up-to-date message with no notification: the widget still follows the session. */
    enum class Decision { SHOW, REMOVE, HIDE, IGNORE }
}
