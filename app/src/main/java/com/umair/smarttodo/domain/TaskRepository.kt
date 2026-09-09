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

    suspend fun addTask(rawText: String)

    suspend fun updateStatus(id: Long, status: TaskStatus)

    suspend fun setPinned(id: Long, pinned: Boolean)

    suspend fun delete(id: Long)
}
