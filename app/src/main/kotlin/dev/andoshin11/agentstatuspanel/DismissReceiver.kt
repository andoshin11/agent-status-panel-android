package dev.andoshin11.agentstatuspanel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** A Live Update the user dismissed stays away until its session finishes or ends. */
class DismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        LedgerStore(context).update(System.currentTimeMillis()) { it.markDismissed(id) }
    }

    companion object {
        const val EXTRA_ID = "id"
    }
}
