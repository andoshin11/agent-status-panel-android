package dev.andoshin11.agentstatuspanel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionBoardTest {
    private fun update(id: String, at: Long, finished: Boolean = false, step: String = "s", tasks: List<TaskRow> = emptyList()) =
        ProgressMessage.Update(id, at, finished, "claude", "w-$id", "g", step, 1, 2, tasks)

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
    fun `the newest session at work gets the card and the rows keep their order`() {
        val sessions = board(update("a", 10), update("b", 20), update("c", 30), update("done", 40, finished = true), update("a", 25))
        val arrangement = SessionBoard.arrange(sessions)!!
        assertEquals("c", arrangement.main.id)
        assertEquals(listOf("a", "b", "done"), arrangement.others.map { it.id })
    }

    @Test
    fun `a finished session gets the card only when nothing is at work`() {
        assertEquals("done", SessionBoard.arrange(board(update("done", 40, finished = true)))!!.main.id)
        assertNull(SessionBoard.arrange(emptyList()))
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
        val sessions = board(update("a", 10, tasks = tasks(1, 3)), update("b", 20, finished = true))
        assertEquals(sessions, SessionBoard.fromJson(SessionBoard.toJson(sessions)))
    }

    @Test
    fun `a tall widget shows every task and row`() {
        assertEquals(WidgetFit(0, 6, 2), fitWidget(600, tasks(2, 6), 2))
    }

    @Test
    fun `tasks that do not fit show around the current one`() {
        val fit = fitWidget(140 + 23 * 3, tasks(5, 10), 0)
        assertEquals(WidgetFit(4, 3, 0), fit)
    }

    @Test
    fun `the window stays inside the list`() {
        assertEquals(WidgetFit(7, 3, 0), fitWidget(140 + 23 * 3, tasks(9, 10), 0))
        assertEquals(WidgetFit(0, 3, 0), fitWidget(140 + 23 * 3, tasks(0, 10), 0))
        assertEquals(WidgetFit(7, 3, 0), fitWidget(140 + 23 * 3, tasks(10, 10), 0))
    }

    @Test
    fun `a short widget shows no rows and no negative counts`() {
        assertEquals(WidgetFit(0, 0, 0), fitWidget(100, tasks(1, 3), 3))
    }

    @Test
    fun `another session always gets a row when there is room`() {
        assertEquals(WidgetFit(0, 4, 1), fitWidget(320, tasks(1, 6), 2))
    }

    @Test
    fun `rows take only part of the space, leaving room for tasks`() {
        val fit = fitWidget(360, tasks(0, 20), 10)
        assertEquals(1, fit.otherCount)
        assertEquals((360 - 140 - 41 - 34) / 23, fit.taskCount)
    }
}
