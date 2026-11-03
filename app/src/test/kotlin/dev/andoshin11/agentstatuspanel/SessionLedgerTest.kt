package dev.andoshin11.agentstatuspanel

import dev.andoshin11.agentstatuspanel.SessionLedger.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLedgerTest {
    private fun update(at: Long, finished: Boolean = false) =
        ProgressMessage.Update("a", at, finished, "claude", "w", "g", "s", 1, 2)

    private fun ledger() = SessionLedger(emptyMap(), emptySet())

    @Test
    fun `a newer message shows and an older one is ignored`() {
        val ledger = ledger()
        assertEquals(Decision.SHOW, ledger.decide(update(10)))
        assertEquals(Decision.SHOW, ledger.decide(update(20)))
        assertEquals(Decision.IGNORE, ledger.decide(update(15)))
        assertEquals(Decision.IGNORE, ledger.decide(update(20)))
    }

    @Test
    fun `a late message does not bring an ended session back`() {
        val ledger = ledger()
        ledger.decide(update(10))
        assertEquals(Decision.REMOVE, ledger.decide(ProgressMessage.Ended("a", 30)))
        assertEquals(Decision.IGNORE, ledger.decide(update(20)))
    }

    @Test
    fun `a dismissed session stays away until it finishes`() {
        val ledger = ledger()
        ledger.decide(update(10))
        ledger.markDismissed("a")
        assertEquals(Decision.HIDE, ledger.decide(update(20)))
        assertEquals(Decision.SHOW, ledger.decide(update(30, finished = true)))
        assertEquals(Decision.SHOW, ledger.decide(update(40)))
    }

    @Test
    fun `an end clears a dismissal`() {
        val ledger = ledger()
        ledger.markDismissed("a")
        assertEquals(Decision.REMOVE, ledger.decide(ProgressMessage.Ended("a", 10)))
        assertTrue(ledger.dismissed.isEmpty())
    }

    @Test
    fun `old entries are removed`() {
        val ledger = SessionLedger(mapOf("old" to 1L, "new" to 100L), setOf("old"))
        ledger.prune(50)
        assertEquals(setOf("new"), ledger.entries.keys)
        assertTrue(ledger.dismissed.isEmpty())
    }
}
