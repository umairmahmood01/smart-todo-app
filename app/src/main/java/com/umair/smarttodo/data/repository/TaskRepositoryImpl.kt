package com.umair.smarttodo.data.repository

import com.umair.smarttodo.data.enrichment.EnrichmentScheduler
import com.umair.smarttodo.data.local.TaskDao
import com.umair.smarttodo.data.local.TaskEntity
import com.umair.smarttodo.data.local.toDomain
import com.umair.smarttodo.data.reminder.ReminderScheduler
import com.umair.smarttodo.di.IoDispatcher
import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.Task
import com.umair.smarttodo.domain.TaskCategorizer
import com.umair.smarttodo.domain.TaskRepository
import com.umair.smarttodo.domain.TaskSort
import com.umair.smarttodo.domain.TaskStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed [TaskRepository]: the single source of truth for tasks.
 *
 * The local database is authoritative and always writable offline. The remote enrichment
 * service never sits on a read or a write path: it is scheduled *after* a successful insert
 * and edits the row later, at which point the existing `Flow` refreshes the UI on its own.
 *
 * @param dao data access for the `tasks` table.
 * @param categorizer assigns a [Category] to freshly captured text, offline and instantly.
 * @param enrichmentScheduler queues the optional background refinement of a new task.
 * @param reminderScheduler schedules/cancels the OS-level reminder behind [setReminder].
 * @param ioDispatcher injected rather than hardcoded to `Dispatchers.IO` so that tests can
 *   substitute a `TestDispatcher` and drive execution deterministically.
 */
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val dao: TaskDao,
    private val categorizer: TaskCategorizer,
    private val enrichmentScheduler: EnrichmentScheduler,
    private val reminderScheduler: ReminderScheduler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TaskRepository {

    /**
     * Streams the task list with search, filters and sorting applied by SQL.
     *
     * A blank [query] and empty [categories] / [statuses] sets mean "no filter on that
     * dimension". Filter values are sorted before binding so that two logically identical
     * calls produce an identical SQL argument list, which lets Room reuse its prepared
     * statement cache.
     */
    override fun observeTasks(
        query: String?,
        categories: Set<Category>,
        statuses: Set<TaskStatus>,
        sort: TaskSort,
    ): Flow<List<Task>> {
        val searchTerm = query?.trim()?.takeIf { it.isNotEmpty() }?.let { escapeLikeArgument(it) }
        return dao.observeTasks(
            query = searchTerm,
            ignoreCategories = if (categories.isEmpty()) 1 else 0,
            categories = categories.map { it.name }.sorted().ifEmpty { NO_FILTER },
            ignoreStatuses = if (statuses.isEmpty()) 1 else 0,
            statuses = statuses.map { it.name }.sorted().ifEmpty { NO_FILTER },
            ascending = if (sort == TaskSort.CREATED_ASC) 1 else 0,
        )
            .map { entities -> entities.toDomain() }
            .flowOn(ioDispatcher)
    }

    /**
     * Categorises [rawText] and stores it as a new [TaskStatus.TODO] task created "now".
     *
     * Two layers, in order:
     * 1. The offline rule-based [categorizer] assigns a category synchronously. No network,
     *    no waiting, no spinner - this call returns as soon as the row is written.
     * 2. Background enrichment is *queued* (never awaited) to refine that category and fill
     *    `normalizedEnglishText`. With no connectivity, no configuration, or a service
     *    outage, step two simply never lands and the task stays exactly as step one left it.
     *
     * Blank input is ignored rather than persisted as an empty row.
     */
    override suspend fun addTask(rawText: String): Long {
        val text = rawText.trim()
        if (text.isEmpty()) return 0L
        val entity = TaskEntity(
            rawText = text,
            normalizedEnglishText = null,
            category = categorizer.categorize(text),
            status = TaskStatus.TODO,
            isPinned = false,
            createdDate = System.currentTimeMillis(),
            dueDate = null,
        )
        val id = withContext(ioDispatcher) { dao.insert(entity) }
        enrichmentScheduler.scheduleEnrichment(taskId = id, rawText = text)
        return id
    }

    override suspend fun updateStatus(id: Long, status: TaskStatus) {
        withContext(ioDispatcher) { dao.updateStatus(id, status) }
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        withContext(ioDispatcher) { dao.setPinned(id, pinned) }
    }

    /**
     * Persists the reminder column first, then schedules or cancels the OS-level reminder as
     * a side effect - per the [TaskRepository.setReminder] contract, the caller never does
     * that scheduling separately.
     *
     * The task's current text is deliberately *not* looked up here: [ReminderPoster] reads it
     * fresh at fire time, so an edit made between now and delivery is reflected in the
     * notification instead of showing stale text.
     */
    override suspend fun setReminder(id: Long, atMillis: Long?) {
        withContext(ioDispatcher) { dao.updateReminder(id, atMillis) }
        if (atMillis == null) {
            reminderScheduler.cancelTaskReminder(id)
        } else {
            reminderScheduler.scheduleTaskReminder(id, atMillis)
        }
    }

    override suspend fun delete(id: Long) {
        withContext(ioDispatcher) { dao.deleteById(id) }
    }

    private companion object {
        /**
         * Bound in place of an empty list when a filter is disabled. The accompanying
         * `ignore` flag short-circuits the `IN` clause, so the value is never compared -
         * it only avoids emitting an empty `IN ()` list.
         */
        private val NO_FILTER = listOf("")

        /**
         * Escapes the SQL `LIKE` wildcards in a user-supplied search term.
         *
         * Without this, typing `50%` would match every row and `a_b` would match `axb`. The
         * escape character is `\`, matching the `ESCAPE` clause in
         * [TaskDao.observeTasks]. The backslash itself must be escaped first, otherwise the
         * escapes added afterwards would be double-escaped.
         */
        private fun escapeLikeArgument(term: String): String = term
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }
}
