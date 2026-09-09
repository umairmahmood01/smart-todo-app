package com.umair.smarttodo.data.repository

import com.umair.smarttodo.data.local.TaskDao
import com.umair.smarttodo.data.local.TaskEntity
import com.umair.smarttodo.data.local.toDomain
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
 * The local database is authoritative. There is no remote source yet, so every read is a
 * `Flow` off [TaskDao] and every write is a suspending call on [ioDispatcher].
 *
 * @param dao data access for the `tasks` table.
 * @param categorizer assigns a [Category] to freshly captured text.
 * @param ioDispatcher injected rather than hardcoded to `Dispatchers.IO` so that tests can
 *   substitute a `TestDispatcher` and drive execution deterministically.
 */
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val dao: TaskDao,
    private val categorizer: TaskCategorizer,
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
     * `normalizedEnglishText` is left `null`; the enrichment layer fills it in later.
     * Blank input is ignored rather than persisted as an empty row.
     */
    override suspend fun addTask(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty()) return
        val entity = TaskEntity(
            rawText = text,
            normalizedEnglishText = null,
            category = categorizer.categorize(text),
            status = TaskStatus.TODO,
            isPinned = false,
            createdDate = System.currentTimeMillis(),
            dueDate = null,
        )
        withContext(ioDispatcher) { dao.insert(entity) }
    }

    override suspend fun updateStatus(id: Long, status: TaskStatus) {
        withContext(ioDispatcher) { dao.updateStatus(id, status) }
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        withContext(ioDispatcher) { dao.setPinned(id, pinned) }
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
            .replace("%", "\%")
            .replace("_", "\_")
    }
}
