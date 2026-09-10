package com.umair.smarttodo.domain

import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for tasks.
 *
 * An empty [categories] or [statuses] set means "no filter on that dimension",
 * not "match nothing". A null or blank [query] means "no text filter".
 */
interface TaskRepository {

    fun observeTasks(
        query: String? = null,
        categories: Set<Category> = emptySet(),
        statuses: Set<TaskStatus> = emptySet(),
        sort: TaskSort = TaskSort.CREATED_DESC,
    ): Flow<List<Task>>

    /**
     * Returns the new row's generated id, or `0L` for a blank/no-op input that was
     * not persisted (mirroring [Task.id]'s own documented convention: "0 means not
     * yet persisted").
     */
    suspend fun addTask(rawText: String): Long

    suspend fun updateStatus(id: Long, status: TaskStatus)

    suspend fun setPinned(id: Long, pinned: Boolean)

    /**
     * Sets or clears the reminder for task [id]. Passing `atMillis = null` clears the
     * reminder. Implementations are responsible for scheduling/cancelling the actual
     * OS-level reminder as a side effect of this call — the caller does not do that
     * separately.
     */
    suspend fun setReminder(id: Long, atMillis: Long?)

    suspend fun delete(id: Long)
}
