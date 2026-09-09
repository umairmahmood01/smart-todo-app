package com.umair.smarttodo.data.repository

import com.umair.smarttodo.data.local.TaskEntity
import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.TaskCategorizer
import com.umair.smarttodo.domain.TaskSort
import com.umair.smarttodo.domain.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the repository's contract with the database and with the enrichment layer:
 * what it binds to the query, what it writes on insert, and that a new task is queued for
 * background enrichment without ever waiting on it.
 */
class TaskRepositoryImplTest {

    private val dao = FakeTaskDao()
    private val categorizer = RecordingCategorizer(Category.WORK)
    private val scheduler = FakeEnrichmentScheduler()
    private val repository = TaskRepositoryImpl(
        dao = dao,
        categorizer = categorizer,
        enrichmentScheduler = scheduler,
        ioDispatcher = Dispatchers.Unconfined,
    )

    // --- addTask ---------------------------------------------------------------------

    @Test
    fun `addTask persists a trimmed, categorized, not-yet-enriched todo`() = runTest {
        val before = System.currentTimeMillis()

        repository.addTask("   kal client ko presentation bhejni hai   ")

        assertEquals(1, dao.insertCount)
        val stored = dao.inserted.single()
        assertEquals("kal client ko presentation bhejni hai", stored.rawText)
        assertEquals("kal client ko presentation bhejni hai", categorizer.lastInput)
        assertEquals(Category.WORK, stored.category)
        assertEquals(TaskStatus.TODO, stored.status)
        assertEquals(false, stored.isPinned)
        assertNull(stored.normalizedEnglishText)
        assertNull(stored.dueDate)
        assertTrue(stored.createdDate >= before)
        assertTrue(stored.createdDate <= System.currentTimeMillis())
    }

    @Test
    fun `addTask schedules enrichment for the row that was actually inserted`() = runTest {
        repository.addTask("  gym jana hai  ")

        val insertedId = dao.inserted.single().id
        assertEquals(listOf(insertedId to "gym jana hai"), scheduler.scheduled)
    }

    @Test
    fun `addTask schedules one job per task`() = runTest {
        repository.addTask("first")
        repository.addTask("second")

        assertEquals(2, dao.insertCount)
        assertEquals(2, scheduler.scheduled.size)
        assertEquals(listOf(1L, 2L), scheduler.scheduled.map { it.first })
    }

    @Test
    fun `addTask ignores blank input and schedules nothing`() = runTest {
        repository.addTask("")
        repository.addTask("      ")

        assertEquals(0, dao.insertCount)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    // --- observeTasks ----------------------------------------------------------------

    @Test
    fun `observeTasks maps rows to domain tasks`() = runTest {
        dao.setRows(
            entity(id = 1L, rawText = "pay bill", category = Category.FINANCE),
            entity(id = 2L, rawText = "read chapter 4", category = Category.STUDY),
        )

        val tasks = repository.observeTasks().first()

        assertEquals(listOf(1L, 2L), tasks.map { it.id })
        assertEquals(listOf("pay bill", "read chapter 4"), tasks.map { it.rawText })
        assertEquals(listOf(Category.FINANCE, Category.STUDY), tasks.map { it.category })
    }

    @Test
    fun `no filters bind the ignore sentinels`() = runTest {
        repository.observeTasks().first()

        val args = requireNotNull(dao.lastObserveArgs)
        assertNull(args.query)
        assertEquals(1, args.ignoreCategories)
        assertEquals(1, args.ignoreStatuses)
        assertEquals(listOf(""), args.categories)
        assertEquals(listOf(""), args.statuses)
        assertEquals(0, args.ascending)
    }

    @Test
    fun `filters bind sorted enum names with the ignore flags cleared`() = runTest {
        repository.observeTasks(
            categories = setOf(Category.WORK, Category.CODING),
            statuses = setOf(TaskStatus.DONE, TaskStatus.TODO),
        ).first()

        val args = requireNotNull(dao.lastObserveArgs)
        assertEquals(0, args.ignoreCategories)
        assertEquals(0, args.ignoreStatuses)
        assertEquals(listOf("CODING", "WORK"), args.categories)
        assertEquals(listOf("DONE", "TODO"), args.statuses)
    }

    @Test
    fun `blank query means no text filter`() = runTest {
        repository.observeTasks(query = "   ").first()

        assertNull(requireNotNull(dao.lastObserveArgs).query)
    }

    @Test
    fun `query is trimmed and LIKE wildcards are escaped`() = runTest {
        repository.observeTasks(query = "  50% off_now  ").first()

        assertEquals("""50\% off\_now""", requireNotNull(dao.lastObserveArgs).query)
    }

    @Test
    fun `a literal backslash in the query is escaped before the wildcards`() = runTest {
        repository.observeTasks(query = """a\b%""").first()

        assertEquals("""a\\b\%""", requireNotNull(dao.lastObserveArgs).query)
    }

    @Test
    fun `ascending sort flips the sort flag`() = runTest {
        repository.observeTasks(sort = TaskSort.CREATED_ASC).first()

        assertEquals(1, requireNotNull(dao.lastObserveArgs).ascending)
    }

    // --- writes ----------------------------------------------------------------------

    @Test
    fun `updateStatus, setPinned and delete reach the dao`() = runTest {
        dao.setRows(entity(id = 7L, rawText = "submit report", category = Category.WORK))

        repository.updateStatus(7L, TaskStatus.IN_PROGRESS)
        assertEquals(TaskStatus.IN_PROGRESS, dao.currentRows.single().status)

        repository.setPinned(7L, true)
        assertEquals(true, dao.currentRows.single().isPinned)

        repository.delete(7L)
        assertTrue(dao.currentRows.isEmpty())
    }

    private fun entity(
        id: Long,
        rawText: String,
        category: Category,
    ) = TaskEntity(
        id = id,
        rawText = rawText,
        normalizedEnglishText = null,
        category = category,
        status = TaskStatus.TODO,
        isPinned = false,
        createdDate = 1_725_000_000_000L + id,
        dueDate = null,
    )
}

/** Categorizer that always answers [category] and remembers what it was asked. */
private class RecordingCategorizer(private val category: Category) : TaskCategorizer {
    var lastInput: String? = null
        private set

    override fun categorize(rawText: String): Category {
        lastInput = rawText
        return category
    }
}
