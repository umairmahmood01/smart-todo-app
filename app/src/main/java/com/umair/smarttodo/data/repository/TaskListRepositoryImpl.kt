package com.umair.smarttodo.data.repository

import com.umair.smarttodo.data.local.TaskListDao
import com.umair.smarttodo.data.local.TaskListEntity
import com.umair.smarttodo.data.local.TaskListItemEntity
import com.umair.smarttodo.data.local.toDomain
import com.umair.smarttodo.data.reminder.ReminderScheduler
import com.umair.smarttodo.di.IoDispatcher
import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.TaskList
import com.umair.smarttodo.domain.TaskListRepository
import com.umair.smarttodo.domain.TaskSort
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed [TaskListRepository]: the single source of truth for task lists (checklists).
 *
 * Structured exactly like [TaskRepositoryImpl]: the local database is authoritative, reminder
 * scheduling is a side effect of [setReminder] rather than something callers coordinate
 * separately, and all disk work runs on [ioDispatcher].
 *
 * @param dao data access for the `task_lists` / `task_list_items` tables.
 * @param reminderScheduler schedules/cancels the OS-level reminder behind [setReminder].
 * @param ioDispatcher injected rather than hardcoded to `Dispatchers.IO` so that tests can
 *   substitute a `TestDispatcher` and drive execution deterministically.
 */
@Singleton
class TaskListRepositoryImpl @Inject constructor(
    private val dao: TaskListDao,
    private val reminderScheduler: ReminderScheduler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TaskListRepository {

    /**
     * Streams the task list collection with search, filters and sorting applied by SQL - same
     * "blank/empty means no filter on that dimension" contract as
     * [TaskRepositoryImpl.observeTasks].
     */
    override fun observeTaskLists(
        query: String?,
        categories: Set<Category>,
        sort: TaskSort,
    ): Flow<List<TaskList>> {
        val searchTerm = query?.trim()?.takeIf { it.isNotEmpty() }?.let { escapeLikeArgument(it) }
        return dao.observeTaskLists(
            query = searchTerm,
            ignoreCategories = if (categories.isEmpty()) 1 else 0,
            categories = categories.map { it.name }.sorted().ifEmpty { NO_FILTER },
            ascending = if (sort == TaskSort.CREATED_ASC) 1 else 0,
        )
            .map { rows -> rows.toDomain() }
            .flowOn(ioDispatcher)
    }

    /**
     * Creates a new list header plus any non-blank [itemTexts], and returns the generated
     * list id.
     *
     * Blank item texts are dropped rather than persisted as empty checklist lines - same
     * "trim, then ignore blank" treatment [TaskRepositoryImpl.addTask] gives task text.
     */
    override suspend fun createTaskList(title: String, category: Category, itemTexts: List<String>): Long =
        withContext(ioDispatcher) {
            val listId = dao.insertTaskList(
                TaskListEntity(
                    title = title.trim(),
                    category = category,
                    createdDate = System.currentTimeMillis(),
                    reminderAt = null,
                ),
            )
            val items = itemTexts.map { it.trim() }.filter { it.isNotEmpty() }
            if (items.isNotEmpty()) {
                dao.insertItems(items.map { text -> TaskListItemEntity(listId = listId, text = text) })
            }
            listId
        }

    override suspend fun addItem(listId: Long, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        withContext(ioDispatcher) {
            dao.insertItem(TaskListItemEntity(listId = listId, text = trimmed))
        }
    }

    override suspend fun setItemChecked(listId: Long, itemId: Long, checked: Boolean) {
        withContext(ioDispatcher) { dao.setItemChecked(itemId, checked) }
    }

    override suspend fun removeItem(listId: Long, itemId: Long) {
        withContext(ioDispatcher) { dao.deleteItem(itemId) }
    }

    /**
     * Persists the reminder column first, then schedules or cancels the OS-level reminder as
     * a side effect - per the [TaskListRepository.setReminder] contract, the caller never
     * does that scheduling separately.
     */
    override suspend fun setReminder(listId: Long, atMillis: Long?) {
        withContext(ioDispatcher) { dao.updateReminder(listId, atMillis) }
        if (atMillis == null) {
            reminderScheduler.cancelListReminder(listId)
        } else {
            reminderScheduler.scheduleListReminder(listId, atMillis)
        }
    }

    override suspend fun delete(listId: Long) {
        withContext(ioDispatcher) { dao.deleteTaskList(listId) }
    }

    private companion object {
        /**
         * Bound in place of an empty list when the category filter is disabled - same
         * reasoning as [TaskRepositoryImpl]'s `NO_FILTER`.
         */
        private val NO_FILTER = listOf("")

        /**
         * Escapes the SQL `LIKE` wildcards in a user-supplied search term.
         *
         * Identical to [TaskRepositoryImpl.escapeLikeArgument] - duplicated rather than
         * shared because the two repositories are otherwise independent and this is the only
         * overlap; promoting it to a shared utility is a reasonable follow-up if a third
         * caller appears.
         */
        private fun escapeLikeArgument(term: String): String = term
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }
}
