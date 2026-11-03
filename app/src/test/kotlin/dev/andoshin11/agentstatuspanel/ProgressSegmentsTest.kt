package dev.andoshin11.agentstatuspanel

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressSegmentsTest {
    @Test
    fun `one segment per task, up to the limit the system shows`() {
        assertEquals(listOf(1, 1, 1), progressSegments(3))
        assertEquals(15, progressSegments(15).size)
        assertEquals(listOf(16), progressSegments(16))
    }

    @Test
    fun `never empty, which the style refuses`() {
        assertEquals(listOf(1), progressSegments(0))
    }
}
