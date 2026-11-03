package dev.andoshin11.agentstatuspanel

/** The system shows at most this many segments, and a single plain bar beyond it. */
private const val MAX_SEGMENTS = 15

/** One segment per task, or a single one when there are too many to show apart. */
fun progressSegments(total: Int): List<Int> = when {
    total <= 0 -> listOf(1)
    total <= MAX_SEGMENTS -> List(total) { 1 }
    else -> listOf(total)
}
