package dev.andoshin11.agentstatuspanel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLedgerTest {
    private fun update(at: Long) = ProgressMessage.Update("a", at, false, "claude", "w", "g", "s", 1, 2)

    private fun ledger() = SessionLedger(emptyMap())

    @Test
    fun `a newer message is accepted and an older one is not`() {
        val ledger = ledger()
        assertTrue(ledger.accept(update(10)))
        assertTrue(ledger.accept(update(20)))
        assertFalse(ledger.accept(update(15)))
        assertFalse(ledger.accept(update(20)))
    }

    @Test
    fun `a late message does not bring an ended session back`() {
        val ledger = ledger()
        ledger.accept(update(10))
        assertTrue(ledger.accept(ProgressMessage.Ended("a", 30)))
        assertFalse(ledger.accept(update(20)))
    }

    @Test
    fun `old entries are removed`() {
        val ledger = SessionLedger(mapOf("old" to 1L, "new" to 100L))
        ledger.prune(50)
        assertEquals(setOf("new"), ledger.entries.keys)
    }
}
