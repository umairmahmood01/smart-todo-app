package com.umair.smarttodo.data.reminder

import com.umair.smarttodo.data.local.TaskEntity
import com.umair.smarttodo.data.local.TaskListEntity
import com.umair.smarttodo.data.repository.FakeTaskDao
import com.umair.smarttodo.data.repository.FakeTaskListDao
import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.TaskStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The fire-time decision logic [ReminderWorker] delegates to: what to post, and when to post
 * nothing at all. Exercised here with fake DAOs and a fake notifier so it runs as a plain JVM
 * test - no `Context`, no `WorkerParameters`, no real Android notification.
 */
class ReminderPosterTest {

    private val taskDao = FakeTaskDao()
    private val taskListDao = FakeTaskListDao()
    private val notifier = FakeReminderNotifier()
    private val poster = ReminderPoster(taskDao, taskListDao, notifier)

    // --- task reminders ------------------------------------------------------------------

    @Test
    fun `a normal task posts with its normalized text when available`() = runTest {
        taskDao.setRows(task(id = 1L, rawText = "gym jana hai", normalized = "Go to the gym", status = TaskStatus.TODO))

        poster.postTaskReminder(1L)

        assertEquals(listOf(1L to "Go to the gym"), notifier.taskNotifications)
    }

    @Test
    fun `a normal task falls back to raw text when there is no normalization`() = runTest {
        taskDao.setRows(task(id = 1L, rawText = "gym jana hai", normalized = null, status = TaskStatus.IN_PROGRESS))

        poster.postTaskReminder(1L)

        assertEquals(listOf(1L to "gym jana hai"), notifier.taskNotifications)
    }

    @Test
    fun `a deleted task posts nothing`() = runTest {
        poster.postTaskReminder(404L)

        assertTrue(notifier.taskNotifications.isEmpty())
    }

    @Test
    fun `an already-done task posts nothing`() = runTest {
        taskDao.setRows(task(id = 1L, rawText = "gym jana hai", normalized = null, status = TaskStatus.DONE))

        poster.postTaskReminder(1L)

        assertTrue(notifier.taskNotifications.isEmpty())
    }

    // --- list reminders ------------------------------------------------------------------

    @Test
    fun `a normal list posts with its title`() = runTest {
        taskListDao.seed(listOf(list(id = 5L, title = "Grocery list")))

        poster.postListReminder(5L)

        assertEquals(listOf(5L to "Grocery list"), notifier.listNotifications)
    }

    @Test
    fun `a deleted list posts nothing`() = runTest {
        poster.postListReminder(404L)

        assertTrue(notifier.listNotifications.isEmpty())
    }

    @Test
    fun `a list with every item checked still posts - lists have no done state of their own`() = runTest {
        taskListDao.seed(
            lists = listOf(list(id = 5L, title = "Grocery list")),
        )

        poster.postListReminder(5L)

        assertEquals(listOf(5L to "Grocery list"), notifier.listNotifications)
    }

    private fun task(
        id: Long,
        rawText: String,
        normalized: String?,
        status: TaskStatus,
    ) = TaskEntity(
        id = id,
        rawText = rawText,
        normalizedEnglishText = normalized,
        category = Category.PERSONAL,
        status = status,
        createdDate = 1_725_000_000_000L,
    )

    private fun list(id: Long, title: String) = TaskListEntity(
        id = id,
        title = title,
        category = Category.SHOPPING,
        createdDate = 1_725_000_000_000L,
    )
}
