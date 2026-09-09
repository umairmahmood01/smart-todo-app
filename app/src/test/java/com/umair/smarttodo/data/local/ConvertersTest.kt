package com.umair.smarttodo.data.local

import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the two properties the persistence layer relies on: enums round-trip by name, and a
 * value the current build does not recognise degrades to a default instead of throwing.
 */
class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `every category round-trips`() {
        for (category in Category.entries) {
            assertEquals(category, converters.toCategory(converters.fromCategory(category)))
        }
    }

    @Test
    fun `every status round-trips`() {
        for (status in TaskStatus.entries) {
            assertEquals(status, converters.toTaskStatus(converters.fromTaskStatus(status)))
        }
    }

    @Test
    fun `categories are stored by name not ordinal`() {
        assertEquals("HEALTH_FITNESS", converters.fromCategory(Category.HEALTH_FITNESS))
        assertEquals("IN_PROGRESS", converters.fromTaskStatus(TaskStatus.IN_PROGRESS))
    }

    @Test
    fun `unknown category string falls back to OTHER instead of throwing`() {
        listOf(
            "",
            "   ",
            "NOT_A_CATEGORY",
            "GARDENING",
            "null",
            "\uD83D\uDE00",
            "WORK; DROP TABLE tasks",
        ).forEach { stored ->
            assertEquals("stored value: \"$stored\"", Category.OTHER, converters.toCategory(stored))
        }
    }

    @Test
    fun `unknown status string falls back to TODO instead of throwing`() {
        listOf("", "   ", "ARCHIVED", "not a status", "0").forEach { stored ->
            assertEquals("stored value: \"$stored\"", TaskStatus.TODO, converters.toTaskStatus(stored))
        }
    }

    @Test
    fun `stored values are read case-insensitively and trimmed`() {
        assertEquals(Category.WORK, converters.toCategory("work"))
        assertEquals(Category.WORK, converters.toCategory("  Work  "))
        assertEquals(TaskStatus.DONE, converters.toTaskStatus("done"))
        assertEquals(TaskStatus.DONE, converters.toTaskStatus("\tDone\n"))
    }
}
