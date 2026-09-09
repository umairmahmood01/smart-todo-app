package com.umair.smarttodo.data.local

import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.Task
import com.umair.smarttodo.domain.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifies that entity and domain representations map onto each other without loss. */
class TaskMappersTest {

    private val entity = TaskEntity(
        id = 42L,
        rawText = "bijli ka bill pay karna hai",
        normalizedEnglishText = "pay the electricity bill",
        category = Category.FINANCE,
        status = TaskStatus.IN_PROGRESS,
        isPinned = true,
        createdDate = 1_725_000_000_000L,
        dueDate = 1_725_600_000_000L,
    )

    @Test
    fun `entity maps to domain field for field`() {
        val task = entity.toDomain()
        assertEquals(entity.id, task.id)
        assertEquals(entity.rawText, task.rawText)
        assertEquals(entity.normalizedEnglishText, task.normalizedEnglishText)
        assertEquals(entity.category, task.category)
        assertEquals(entity.status, task.status)
        assertEquals(entity.isPinned, task.isPinned)
        assertEquals(entity.createdDate, task.createdDate)
        assertEquals(entity.dueDate, task.dueDate)
    }

    @Test
    fun `entity to domain and back is a round trip`() {
        assertEquals(entity, entity.toDomain().toEntity())
    }

    @Test
    fun `domain to entity and back is a round trip`() {
        val task = Task(
            id = 7L,
            rawText = "gym jana hai",
            normalizedEnglishText = null,
            category = Category.HEALTH_FITNESS,
            status = TaskStatus.TODO,
            isPinned = false,
            createdDate = 1_700_000_000_000L,
            dueDate = null,
        )
        assertEquals(task, task.toEntity().toDomain())
    }

    @Test
    fun `nullable fields survive the mapping`() {
        val mapped = entity.copy(normalizedEnglishText = null, dueDate = null).toDomain()
        assertNull(mapped.normalizedEnglishText)
        assertNull(mapped.dueDate)
    }

    @Test
    fun `unsaved task keeps id zero so Room can autogenerate it`() {
        val task = Task(
            rawText = "sabzi leni hai",
            category = Category.SHOPPING,
            status = TaskStatus.TODO,
            createdDate = 1L,
        )
        assertEquals(0L, task.toEntity().id)
    }

    @Test
    fun `list mapping preserves order`() {
        val entities = listOf(
            entity.copy(id = 1, rawText = "one"),
            entity.copy(id = 2, rawText = "two"),
            entity.copy(id = 3, rawText = "three"),
        )
        assertEquals(listOf("one", "two", "three"), entities.toDomain().map { it.rawText })
        assertEquals(listOf(1L, 2L, 3L), entities.toDomain().map { it.id })
    }

    @Test
    fun `empty list maps to empty list`() {
        assertEquals(emptyList<Task>(), emptyList<TaskEntity>().toDomain())
    }
}
