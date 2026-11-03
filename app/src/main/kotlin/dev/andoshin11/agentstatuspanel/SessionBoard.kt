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
) {
    /** A task in progress turns the dot orange, as in the panel. */
    val running: Boolean
        get() = !finished && (step.isNotEmpty() || tasks.any { it.status == TaskStatus.IN_PROGRESS })
}

/** The sessions the widget shows. Pure, so it can be tested without a device. */
object SessionBoard {
    /** If the Mac mini goes away, nothing would end these. The same time as the notification. */
    const val STALE_MS = 3L * 60 * 60 * 1000

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
            )
        }
    }

    fun fresh(sessions: List<WidgetSession>, now: Long) = sessions.filter { now - it.at <= STALE_MS }

    /**
     * The newest session at work gets the card. The rest keep the order they appeared in, since
     * sorting them by activity would make the rows jump.
     */
    fun arrange(sessions: List<WidgetSession>): Arrangement? {
        val main = sessions.filter { !it.finished }.maxByOrNull { it.at }
            ?: sessions.maxByOrNull { it.at }
            ?: return null
        return Arrangement(main, sessions.filter { it.id != main.id }.sortedBy { it.firstSeen })
    }

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
                tasks = List(tasks.length()) { row ->
                    val task = tasks.getJSONArray(row)
                    TaskRow(TaskStatus.fromCode(task.getString(0)), task.getString(1))
                },
            )
        }
    }
}

data class Arrangement(val main: WidgetSession, val others: List<WidgetSession>)

/** What fits in a widget of some height. */
data class WidgetFit(val taskStart: Int, val taskCount: Int, val otherCount: Int)

// In dp, measured on the device: padding, header, the card's padding, a two-line goal, count and bar.
private const val BASE_HEIGHT = 24 + 32 + 20 + 44 + 20
private const val TASK_HEIGHT = 23
private const val OTHERS_HEADER_HEIGHT = 41
private const val ROW_HEIGHT = 34
private const val OTHERS_SHARE = 0.45

/**
 * Other sessions must not go unseen, so at least one row shows when there is room for it and two
 * tasks. Beyond that the rows take at most part of the space. The tasks shown are those around the
 * current one.
 */
fun fitWidget(heightDp: Int, tasks: List<TaskRow>, others: Int): WidgetFit {
    var space = heightDp - BASE_HEIGHT
    val firstRow = OTHERS_HEADER_HEIGHT + ROW_HEIGHT
    val shared = ((space * OTHERS_SHARE - OTHERS_HEADER_HEIGHT) / ROW_HEIGHT).toInt()
    val wantedRows = if (space >= firstRow + 2 * TASK_HEIGHT) shared.coerceAtLeast(1) else 0
    val rows = wantedRows.coerceIn(0, others)
    if (rows > 0) space -= OTHERS_HEADER_HEIGHT + rows * ROW_HEIGHT
    val count = (space / TASK_HEIGHT).coerceIn(0, tasks.size)
    val unfinished = tasks.indexOfFirst { it.status != TaskStatus.COMPLETED }
    val wanted = if (unfinished == -1) tasks.size - count else unfinished - 1
    return WidgetFit(wanted.coerceIn(0, tasks.size - count), count, rows)
}
