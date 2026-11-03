package dev.andoshin11.agentstatuspanel

/**
 * Remembers per session the newest message the phone accepted, because FCM does not keep messages
 * in order. Entries are kept after a session ends, so a late message cannot bring it back, and
 * removed once they are old.
 */
class SessionLedger(entries: Map<String, Long>) {
    private val lastAt = entries.toMutableMap()

    val entries: Map<String, Long> get() = lastAt

    /** Whether a message is newer than the last one, recording it if so. */
    fun accept(message: ProgressMessage): Boolean {
        val last = lastAt[message.id]
        if (last != null && message.at <= last) return false
        lastAt[message.id] = message.at
        return true
    }

    fun prune(olderThan: Long) {
        lastAt.values.removeAll { it < olderThan }
    }
}
