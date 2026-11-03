package dev.andoshin11.agentstatuspanel

import org.json.JSONArray
import org.json.JSONException

enum class TaskStatus(val code: String) {
    COMPLETED("c"),
    IN_PROGRESS("i"),
    PENDING("p");

    companion object {
        fun fromCode(code: String) = entries.firstOrNull { it.code == code } ?: PENDING
    }
}

data class TaskRow(val status: TaskStatus, val subject: String)

/** A message from agent-status-panel. Its format is described in that repository's README. */
sealed interface ProgressMessage {
    val id: String
    val at: Long

    data class Ended(override val id: String, override val at: Long) : ProgressMessage

    data class Update(
        override val id: String,
        override val at: Long,
        val finished: Boolean,
        val agent: String,
        val workspace: String,
        val goal: String,
        val step: String,
        val done: Int,
        val total: Int,
        val tasks: List<TaskRow> = emptyList(),
        /** False for a session left idle. The widget lists these below the ones moving now. */
        val live: Boolean = true,
    ) : ProgressMessage

    companion object {
        private const val SCHEMA_VERSION = "1"

        /** Null for a message this version does not understand. */
        fun parse(data: Map<String, String>): ProgressMessage? {
            if (data["v"] != SCHEMA_VERSION) return null
            val id = data["id"]?.takeIf { it.isNotEmpty() } ?: return null
            val at = data["at"]?.toLongOrNull() ?: return null
            return when (data["state"]) {
                "ended" -> Ended(id, at)
                "active", "finished" -> {
                    val done = data["done"]?.toIntOrNull()?.takeIf { it >= 0 } ?: return null
                    val total = data["total"]?.toIntOrNull()?.takeIf { it >= done } ?: return null
                    Update(
                        id = id,
                        at = at,
                        finished = data["state"] == "finished",
                        agent = data["agent"].orEmpty(),
                        workspace = data["workspace"].orEmpty(),
                        goal = data["goal"].orEmpty(),
                        step = data["step"].orEmpty(),
                        done = done,
                        total = total,
                        tasks = parseTasks(data["tasks"]),
                        live = data["live"] != "0",
                    )
                }
                else -> null
            }
        }

        /** The list is only for display, so a bad one costs the list and not the message. */
        private fun parseTasks(text: String?): List<TaskRow> {
            if (text.isNullOrEmpty()) return emptyList()
            return try {
                val list = JSONArray(text)
                List(list.length()) { index ->
                    val row = list.getJSONArray(index)
                    TaskRow(TaskStatus.fromCode(row.getString(0)), row.getString(1))
                }
            } catch (_: JSONException) {
                emptyList()
            }
        }
    }
}
