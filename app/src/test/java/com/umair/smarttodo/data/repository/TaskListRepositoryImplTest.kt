package com.umair.smarttodo.data.repository

import com.umair.smarttodo.data.local.TaskListEntity
import com.umair.smarttodo.data.reminder.FakeReminderScheduler
import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.TaskListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the task-list repository's contract with the database and with the reminder
 * scheduler: creation returns the id the caller needs, item mutations reach the DAO, deletion
 * cascades, and `setReminder` always schedules-or-cancels alongside the write.
 */
class TaskListRepositoryImplTest {

    private val dao = FakeTaskListDao()
    private val scheduler = FakeReminderScheduler()
    private val repository = TaskListRepositoryImpl(
        dao = dao,
        reminderScheduler = scheduler,
        ioDispatcher = Dispatchers.Unconfined,
    )

    // --- createTaskList ---------------------------------------------------------------

    @Test
    fun `createTaskList returns the generated id`() = runTest {
        val firstId = repository.createTaskList("Grocery list", Category.SHOPPING, emptyList())
        val secondId = repository.createTaskList("Packing list", Category.PERSONAL, emptyList())

        assertEquals(1L, firstId)
        assertEquals(2L, secondId)
    }

    @Test
    fun `createTaskList persists a trimmed title and its non-blank items`() = runTest {
        val id = repository.createTaskList(
            "  Grocery list  ",
            Category.SHOPPING,
            listOf(
                TaskListItem(text = "  milk  "),
                TaskListItem(text = ""),
                TaskListItem(text = "eggs"),
                TaskListItem(text = "   "),
            ),
        )

        val stored = dao.currentLists.single()
        assertEquals("Grocery list", stored.title)
        assertEquals(Category.SHOPPING, stored.category)
        assertNull(stored.reminderAt)

        val items = dao.currentItems.filter { it.listId == id }
        assertEquals(listOf("milk", "eggs"), items.map { it.text })
        assertTrue(items.all { !it.isChecked })
    }

    @Test
    fun `createTaskList with no items inserts no item rows`() = runTest {
        repository.createTaskList("Empty list", Category.OTHER, emptyList())

        assertEquals(0, dao.insertItemsCount)
        assertTrue(dao.currentItems.isEmpty())
    }

    @Test
    fun `createTaskList persists each item's quantity`() = runTest {
        repository.createTaskList(
            "Grocery list",
            Category.SHOPPING,
            listOf(TaskListItem(text = "milk", quantity = "2"), TaskListItem(text = "eggs", quantity = null)),
        )

        val items = dao.currentItems.sortedBy { it.text }
        assertEquals(listOf("eggs", "milk"), items.map { it.text })
        assertEquals(listOf(null, "2"), items.map { it.quantity })
    }

    @Test
    fun `createTaskList ignores a caller-supplied id and isChecked on input items`() = runTest {
        repository.createTaskList(
            "Grocery list",
            Category.SHOPPING,
            listOf(TaskListItem(id = 999L, text = "milk", isChecked = true)),
        )

        val stored = dao.currentItems.single()
        assertTrue(stored.id != 999L)
        assertEquals(false, stored.isChecked)
    }

    // --- item mutation -------------------------------------------------------------------

    @Test
    fun `addItem appends a trimmed item to the right list`() = runTest {
        val listId = repository.createTaskList("Grocery list", Category.SHOPPING, emptyList())

        repository.addItem(listId, "  bread  ")

        val item = dao.currentItems.single()
        assertEquals(listId, item.listId)
        assertEquals("bread", item.text)
        assertEquals(false, item.isChecked)
    }

    @Test
    fun `addItem ignores blank text`() = runTest {
        val listId = repository.createTaskList("Grocery list", Category.SHOPPING, emptyList())

        repository.addItem(listId, "   ")

        assertTrue(dao.currentItems.isEmpty())
    }

    @Test
    fun `addItem persists the supplied quantity`() = runTest {
        val listId = repository.createTaskList("Grocery list", Category.SHOPPING, emptyList())

        repository.addItem(listId, "milk", quantity = "1 kg")

        assertEquals("1 kg", dao.currentItems.single().quantity)
    }

    @Test
    fun `addItem defaults quantity to null when omitted`() = runTest {
        val listId = repository.createTaskList("Grocery list", Category.SHOPPING, emptyList())

        repository.addItem(listId, "milk")

        assertNull(dao.currentItems.single().quantity)
    }

    // --- setItemQuantity -----------------------------------------------------------------

    @Test
    fun `setItemQuantity sets the quantity`() = runTest {
        val listId = repository.createTaskList("Grocery list", Category.SHOPPING, listOf(TaskListItem(text = "milk")))
        val itemId = dao.currentItems.single().id

        repository.setItemQuantity(listId, itemId, "2 kg")

        assertEquals("2 kg", dao.currentItems.single().quantity)
    }

    @Test
    fun `setItemQuantity with null clears the quantity`() = runTest {
        val listId = repository.createTaskList(
            "Grocery list",
            Category.SHOPPING,
            listOf(TaskListItem(text = "milk", quantity = "2 kg")),
        )
        val itemId = dao.currentItems.single().id

        repository.setItemQuantity(listId, itemId, null)

        assertNull(dao.currentItems.single().quantity)
    }

    @Test
    fun `setItemChecked reaches the dao`() = runTest {
        val listId = repository.createTaskList("Grocery list", Category.SHOPPING, listOf(TaskListItem(text = "milk")))
        val itemId = dao.currentItems.single().id

        repository.setItemChecked(listId, itemId, true)

        assertEquals(true, dao.currentItems.single().isChecked)
    }

    @Test
    fun `removeItem deletes only that item`() = runTest {
        val listId = repository.createTaskList(
            "Grocery list",
            Category.SHOPPING,
            listOf(TaskListItem(text = "milk"), TaskListItem(text = "eggs")),
        )
        val (milk, eggs) = dao.currentItems

        repository.removeItem(listId, milk.id)

        assertEquals(listOf(eggs.id), dao.currentItems.map { it.id })
    }

    // --- setReminder -----------------------------------------------------------------

    @Test
    fun `setReminder with a non-null instant writes the column and schedules`() = runTest {
        val listId = repository.createTaskList("Grocery list", Category.SHOPPING, emptyList())

        repository.setReminder(listId, 1_800_000_000_000L)

        assertEquals(1_800_000_000_000L, dao.currentLists.single().reminderAt)
        assertEquals(listOf(listId to 1_800_000_000_000L), scheduler.scheduledLists)
        assertTrue(scheduler.cancelledLists.isEmpty())
    }

    @Test
    fun `setReminder with null clears the column and cancels`() = runTest {
        val listId = repository.createTaskList("Grocery list", Category.SHOPPING, emptyList())
        repository.setReminder(listId, 1_800_000_000_000L)

        repository.setReminder(listId, null)

        assertNull(dao.currentLists.single().reminderAt)
        assertEquals(listOf(listId), scheduler.cancelledLists)
        assertEquals(1, scheduler.scheduledLists.size)
    }

    // --- delete ------------------------------------------------------------------------

    @Test
    fun `delete removes the list and cascades to its items`() = runTest {
        val listId = repository.createTaskList(
            "Grocery list",
            Category.SHOPPING,
            listOf(TaskListItem(text = "milk"), TaskListItem(text = "eggs")),
        )

        repository.delete(listId)

        assertTrue(dao.currentLists.isEmpty())
        assertTrue(dao.currentItems.isEmpty())
    }

    // --- observeTaskLists --------------------------------------------------------------

    @Test
    fun `observeTaskLists maps rows and attaches their items`() = runTest {
        dao.seed(
            lists = listOf(entity(id = 1L, title = "Grocery list", category = Category.SHOPPING)),
        )
        repository.addItem(1L, "milk")

        val lists = repository.observeTaskLists().first()

        assertEquals(listOf("Grocery list"), lists.map { it.title })
        assertEquals(listOf("milk"), lists.single().items.map { it.text })
    }

    @Test
    fun `no filters bind the ignore sentinel`() = runTest {
        repository.observeTaskLists().first()

        val args = requireNotNull(dao.lastObserveArgs)
        assertNull(args.query)
        assertEquals(1, args.ignoreCategories)
        assertEquals(listOf(""), args.categories)
    }

    @Test
    fun `filters bind sorted enum names with the ignore flag cleared`() = runTest {
        repository.observeTaskLists(categories = setOf(Category.SHOPPING, Category.PERSONAL)).first()

        val args = requireNotNull(dao.lastObserveArgs)
        assertEquals(0, args.ignoreCategories)
        assertEquals(listOf("PERSONAL", "SHOPPING"), args.categories)
    }

    @Test
    fun `blank query means no text filter`() = runTest {
        repository.observeTaskLists(query = "   ").first()

        assertNull(requireNotNull(dao.lastObserveArgs).query)
    }

    @Test
    fun `query is trimmed and LIKE wildcards are escaped`() = runTest {
        repository.observeTaskLists(query = "  50% off_now  ").first()

        assertEquals("""50\% off\_now""", requireNotNull(dao.lastObserveArgs).query)
    }

    private fun entity(id: Long, title: String, category: Category) = TaskListEntity(
        id = id,
        title = title,
        category = category,
        createdDate = 1_725_000_000_000L + id,
        reminderAt = null,
    )
}
