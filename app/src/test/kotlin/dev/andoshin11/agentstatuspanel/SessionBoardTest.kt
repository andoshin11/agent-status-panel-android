package dev.andoshin11.agentstatuspanel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionBoardTest {
    private fun update(id: String, at: Long, finished: Boolean = false, step: String = "s", tasks: List<TaskRow> = emptyList(), live: Boolean = true) =
        ProgressMessage.Update(id, at, finished, "claude", "w-$id", "g", step, 1, 2, tasks, live)

    private fun board(vararg messages: ProgressMessage) =
        messages.fold(emptyList<WidgetSession>(), SessionBoard::apply)

    private fun tasks(completed: Int, total: Int) = List(total) { index ->
        TaskRow(
            when {
                index < completed -> TaskStatus.COMPLETED
                index == completed -> TaskStatus.IN_PROGRESS
                else -> TaskStatus.PENDING
            },
            "T$index",
        )
    }

    @Test
    fun `an update replaces a session and keeps when it first appeared`() {
        val sessions = board(update("a", 10), update("a", 20))
        assertEquals(1, sessions.size)
        assertEquals(10, sessions[0].firstSeen)
        assertEquals(20, sessions[0].at)
    }

    @Test
    fun `an end removes a session`() {
        assertTrue(board(update("a", 10), ProgressMessage.Ended("a", 20)).isEmpty())
    }

    @Test
    fun `sessions the Mac mini stopped telling about go away`() {
        val sessions = board(update("old", 0), update("new", SessionBoard.STALE_MS))
        assertEquals(listOf("new"), SessionBoard.fresh(sessions, SessionBoard.STALE_MS + 1).map { it.id })
    }

    @Test
    fun `sessions at work come first and keep their order when one becomes active`() {
        val sessions = board(update("a", 10), update("done", 15, finished = true), update("b", 20), update("c", 30), update("a", 40))
        assertEquals(listOf("a", "b", "c", "done"), SessionBoard.ordered(sessions).map { it.id })
    }

    @Test
    fun `an idle session stays on the board for days, below the live ones`() {
        val sessions = board(update("idle", 10, live = false), update("doneIdle", 11, finished = true, live = false), update("new", 20))
        assertEquals(listOf("new", "idle", "doneIdle"), SessionBoard.ordered(sessions).map { it.id })
        val days = 7L * 24 * 60 * 60 * 1000
        assertEquals(3, SessionBoard.fresh(sessions, days).size)
    }

    @Test
    fun `the running state comes from the step or the list`() {
        assertTrue(board(update("a", 1))[0].running)
        assertFalse(board(update("a", 1, step = ""))[0].running)
        assertTrue(board(update("a", 1, step = "", tasks = tasks(0, 2)))[0].running)
        assertFalse(board(update("a", 1, finished = true))[0].running)
    }

    @Test
    fun `sessions survive being saved`() {
        val sessions = board(update("a", 10, tasks = tasks(1, 3)), update("b", 20, finished = true, live = false))
        assertEquals(sessions, SessionBoard.fromJson(SessionBoard.toJson(sessions)))
    }
}
