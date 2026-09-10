package com.umair.smarttodo.domain

import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for task lists (checklists).
 *
 * An empty [categories] set means "no filter on that dimension", not "match nothing".
 * A null or blank [query] means "no text filter".
 */
interface TaskListRepository {

    fun observeTaskLists(
        query: String? = null,
        categories: Set<Category> = emptySet(),
        sort: TaskSort = TaskSort.CREATED_DESC,
    ): Flow<List<TaskList>>

    /**
     * Creates a new list and returns its generated id. [itemTexts] may be empty — a list
     * can start empty and grow via [addItem].
     */
    suspend fun createTaskList(title: String, category: Category, itemTexts: List<String>): Long

    suspend fun addItem(listId: Long, text: String)

    suspend fun setItemChecked(listId: Long, itemId: Long, checked: Boolean)

    suspend fun removeItem(listId: Long, itemId: Long)

    /**
     * Sets or clears the reminder for list [listId]. Passing `atMillis = null` clears the
     * reminder. Implementations are responsible for scheduling/cancelling the actual
     * OS-level reminder as a side effect of this call — the caller does not do that
     * separately.
     */
    suspend fun setReminder(listId: Long, atMillis: Long?)

    suspend fun delete(listId: Long)
}
