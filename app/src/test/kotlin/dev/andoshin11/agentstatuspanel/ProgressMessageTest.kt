package dev.andoshin11.agentstatuspanel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressMessageTest {
    private val active = mapOf(
        "v" to "1", "id" to "abc", "at" to "100", "state" to "active", "agent" to "claude",
        "workspace" to "app", "goal" to "Ship", "step" to "Testing", "done" to "1", "total" to "3",
    )

    @Test
    fun `reads an active message`() {
        assertEquals(
            ProgressMessage.Update("abc", 100, false, "claude", "app", "Ship", "Testing", 1, 3),
            ProgressMessage.parse(active),
        )
    }

    @Test
    fun `reads finished and ended messages`() {
        assertEquals(true, (ProgressMessage.parse(active + ("state" to "finished")) as ProgressMessage.Update).finished)
        assertEquals(
            ProgressMessage.Ended("abc", 100),
            ProgressMessage.parse(mapOf("v" to "1", "id" to "abc", "at" to "100", "state" to "ended")),
        )
    }

    @Test
    fun `empty text from sendText off is accepted`() {
        val message = ProgressMessage.parse(active + mapOf("goal" to "", "step" to "", "workspace" to ""))
        assertEquals("", (message as ProgressMessage.Update).goal)
    }

    @Test
    fun `reads the task list`() {
        val message = ProgressMessage.parse(active + ("tasks" to """[["c","A"],["i","B"],["p","C"],["x","D"]]"""))
        assertEquals(
            listOf(
                TaskRow(TaskStatus.COMPLETED, "A"),
                TaskRow(TaskStatus.IN_PROGRESS, "B"),
                TaskRow(TaskStatus.PENDING, "C"),
                TaskRow(TaskStatus.PENDING, "D"),
            ),
            (message as ProgressMessage.Update).tasks,
        )
    }

    @Test
    fun `a missing or bad list costs only the list`() {
        assertEquals(emptyList<TaskRow>(), (ProgressMessage.parse(active) as ProgressMessage.Update).tasks)
        val bad = ProgressMessage.parse(active + ("tasks" to "not json")) as ProgressMessage.Update
        assertEquals(emptyList<TaskRow>(), bad.tasks)
        assertEquals("Ship", bad.goal)
    }

    @Test
    fun `reads an attention message and the mark on a session`() {
        assertEquals(
            ProgressMessage.Attention("x", 100, "app", "permission"),
            ProgressMessage.parse(mapOf("v" to "1", "id" to "x", "at" to "100", "state" to "attention", "workspace" to "app", "reason" to "permission")),
        )
        val update = ProgressMessage.parse(mapOf("v" to "1", "id" to "x", "at" to "100", "state" to "active", "done" to "0", "total" to "0", "attention" to "waiting"))
        assertEquals("waiting", (update as ProgressMessage.Update).attention)
    }

    @Test
    fun `refuses what it does not understand`() {
        assertNull(ProgressMessage.parse(active + ("v" to "2")))
        assertNull(ProgressMessage.parse(active - "id"))
        assertNull(ProgressMessage.parse(active - "at"))
        assertNull(ProgressMessage.parse(active + ("state" to "other")))
        assertNull(ProgressMessage.parse(active + ("done" to "x")))
        assertNull(ProgressMessage.parse(active + ("done" to "4")))
    }
}
