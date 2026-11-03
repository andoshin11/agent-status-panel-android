package dev.andoshin11.agentstatuspanel

import org.json.JSONArray
import org.json.JSONObject

/** What the widget knows of a session. */
data class WidgetSession(
    val id: String,
    val firstSeen: Long,
    val at: Long,
    val finished: Boolean,
    val workspace: String,
    val goal: String,
    val step: String,
    val done: Int,
    val total: Int,
    val tasks: List<TaskRow>,
    val live: Boolean = true,
) {
    /** A task in progress turns the dot orange, as in the panel. */
    val running: Boolean
        get() = !finished && (step.isNotEmpty() || tasks.any { it.status == TaskStatus.IN_PROGRESS })
}

/** The sessions the widget shows. Pure, so it can be tested without a device. */
object SessionBoard {
    /**
     * If the Mac mini goes away, nothing would end these. An unchanged session gets no new messages,
     * but the sender ends it within 7 days, so this only has to be longer than that.
     */
    const val STALE_MS = 8L * 24 * 60 * 60 * 1000

    fun apply(sessions: List<WidgetSession>, message: ProgressMessage): List<WidgetSession> {
        val others = sessions.filter { it.id != message.id }
        return when (message) {
            is ProgressMessage.Ended -> others
            is ProgressMessage.Update -> others + WidgetSession(
                id = message.id,
                firstSeen = sessions.firstOrNull { it.id == message.id }?.firstSeen ?: message.at,
                at = message.at,
                finished = message.finished,
                workspace = message.workspace,
                goal = message.goal,
                step = message.step,
                done = message.done,
                total = message.total,
                tasks = message.tasks,
                live = message.live,
            )
        }
    }

    fun fresh(sessions: List<WidgetSession>, now: Long) = sessions.filter { now - it.at <= STALE_MS }

    /**
     * Unfinished sessions first, then the finished ones. Within each, live sessions come before
     * idle ones, so today's work is not buried under a list left for days, and then the order they
     * appeared. Sorting by activity would make the list jump while it is being scrolled.
     */
    fun ordered(sessions: List<WidgetSession>) =
        sessions.sortedWith(compareBy({ it.finished }, { !it.live }, { it.firstSeen }))

    fun toJson(sessions: List<WidgetSession>): String = JSONArray(
        sessions.map { session ->
            JSONObject()
                .put("id", session.id)
                .put("firstSeen", session.firstSeen)
                .put("at", session.at)
                .put("finished", session.finished)
                .put("workspace", session.workspace)
                .put("goal", session.goal)
                .put("step", session.step)
                .put("done", session.done)
                .put("total", session.total)
                .put("live", session.live)
                .put("tasks", JSONArray(session.tasks.map { JSONArray(listOf(it.status.code, it.subject)) }))
        },
    ).toString()

    fun fromJson(text: String): List<WidgetSession> {
        val list = JSONArray(text)
        return List(list.length()) { index ->
            val item = list.getJSONObject(index)
            val tasks = item.getJSONArray("tasks")
            WidgetSession(
                id = item.getString("id"),
                firstSeen = item.getLong("firstSeen"),
                at = item.getLong("at"),
                finished = item.getBoolean("finished"),
                workspace = item.getString("workspace"),
                goal = item.getString("goal"),
                step = item.getString("step"),
                done = item.getInt("done"),
                total = item.getInt("total"),
                live = item.optBoolean("live", true),
                tasks = List(tasks.length()) { row ->
                    val task = tasks.getJSONArray(row)
                    TaskRow(TaskStatus.fromCode(task.getString(0)), task.getString(1))
                },
            )
        }
    }
}
