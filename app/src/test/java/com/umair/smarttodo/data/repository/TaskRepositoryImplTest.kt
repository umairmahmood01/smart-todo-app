package com.umair.smarttodo.data.repository

import com.umair.smarttodo.data.local.TaskEntity
import com.umair.smarttodo.data.reminder.FakeReminderScheduler
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
    private val reminderScheduler = FakeReminderScheduler()
    private val repository = TaskRepositoryImpl(
        dao = dao,
        categorizer = categorizer,
        enrichmentScheduler = scheduler,
        reminderScheduler = reminderScheduler,
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

    @Test
    fun `addTask returns the id of the row that was actually inserted`() = runTest {
        val returnedId = repository.addTask("buy milk")

        val observed = repository.observeTasks().first()
        assertEquals(1, observed.size)
        assertEquals(observed.single().id, returnedId)
        assertTrue(returnedId != 0L)
    }

    @Test
    fun `addTask returns 0L for blank input`() = runTest {
        assertEquals(0L, repository.addTask(""))
        assertEquals(0L, repository.addTask("      "))
    }

    @Test
    fun `addTask defaults details to null when omitted`() = runTest {
        repository.addTask("buy milk")

        assertNull(dao.inserted.single().details)
    }

    @Test
    fun `addTask stores details as-is, including blank, without coercing to null`() = runTest {
        repository.addTask("buy milk", details = "2% and whole")

        assertEquals("2% and whole", dao.inserted.single().details)
    }

    @Test
    fun `addTask stores blank details as-is rather than coercing to null`() = runTest {
        repository.addTask("buy milk", details = "   ")

        assertEquals("   ", dao.inserted.single().details)
    }

    // --- updateTask --------------------------------------------------------------------

    @Test
    fun `updateTask re-categorizes the new text`() = runTest {
        dao.setRows(entity(id = 3L, rawText = "old text", category = Category.OTHER))
        categorizer.nextCategory = Category.FINANCE

        repository.updateTask(3L, "pay the bill", details = null)

        assertEquals("pay the bill", categorizer.lastInput)
        assertEquals(Category.FINANCE, dao.currentRows.single().category)
        assertEquals("pay the bill", dao.currentRows.single().rawText)
    }

    @Test
    fun `updateTask clears normalizedEnglishText`() = runTest {
        dao.setRows(
            entity(id = 3L, rawText = "old text", category = Category.OTHER)
                .copy(normalizedEnglishText = "stale normalization"),
        )

        repository.updateTask(3L, "new text", details = null)

        assertNull(dao.currentRows.single().normalizedEnglishText)
    }

    @Test
    fun `updateTask re-queues enrichment for the edited text`() = runTest {
        dao.setRows(entity(id = 3L, rawText = "old text", category = Category.OTHER))

        repository.updateTask(3L, "  new text  ", details = null)

        assertEquals(listOf(3L to "new text"), scheduler.scheduled)
    }

    @Test
    fun `updateTask persists details as-is, including blank`() = runTest {
        dao.setRows(entity(id = 3L, rawText = "old text", category = Category.OTHER))

        repository.updateTask(3L, "new text", details = "   ")

        assertEquals("   ", dao.currentRows.single().details)
    }

    @Test
    fun `updateTask persists a null details, clearing any previous value`() = runTest {
        dao.setRows(entity(id = 3L, rawText = "old text", category = Category.OTHER))
        repository.updateTask(3L, "new text", details = "some notes")

        repository.updateTask(3L, "new text", details = null)

        assertNull(dao.currentRows.single().details)
    }

    @Test
    fun `updateTask with blank rawText is a true no-op`() = runTest {
        val original = entity(id = 3L, rawText = "old text", category = Category.OTHER)
        dao.setRows(original)

        repository.updateTask(3L, "   ", details = "notes")

        assertEquals(original, dao.currentRows.single())
        assertEquals(0, dao.updateTaskCallCount)
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

    // --- setReminder -------------------------------------------------------------------

    @Test
    fun `setReminder with a non-null instant writes the column and schedules`() = runTest {
        dao.setRows(entity(id = 9L, rawText = "submit report", category = Category.WORK))

        repository.setReminder(9L, 1_800_000_000_000L)

        assertEquals(1, dao.updateReminderCallCount)
        assertEquals(1_800_000_000_000L, dao.currentRows.single().reminderAt)
        assertEquals(listOf(9L to 1_800_000_000_000L), reminderScheduler.scheduledTasks)
        assertTrue(reminderScheduler.cancelledTasks.isEmpty())
    }

    @Test
    fun `setReminder with null clears the column and cancels`() = runTest {
        dao.setRows(entity(id = 9L, rawText = "submit report", category = Category.WORK))
        repository.setReminder(9L, 1_800_000_000_000L)

        repository.setReminder(9L, null)

        assertEquals(2, dao.updateReminderCallCount)
        assertNull(dao.currentRows.single().reminderAt)
        assertEquals(listOf(9L), reminderScheduler.cancelledTasks)
        assertEquals(1, reminderScheduler.scheduledTasks.size)
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

/** Categorizer that always answers [nextCategory] and remembers what it was asked. */
private class RecordingCategorizer(var nextCategory: Category) : TaskCategorizer {
    var lastInput: String? = null
        private set

    override fun categorize(rawText: String): Category {
        lastInput = rawText
        return nextCategory
    }
}
