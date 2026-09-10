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
     * Creates a new list and returns its generated id. [items] may be empty — a list can
     * start empty and grow via [addItem].
     *
     * Each supplied [TaskListItem] contributes its `text` and `quantity` only — `id` and
     * `isChecked` are ignored on creation, every created item starts unchecked with a
     * freshly generated id regardless of what the caller passed in those two fields.
     */
    suspend fun createTaskList(title: String, category: Category, items: List<TaskListItem>): Long

    suspend fun addItem(listId: Long, text: String, quantity: String? = null)

    suspend fun setItemChecked(listId: Long, itemId: Long, checked: Boolean)

    /**
     * Sets the checked state of *every* item in list [listId] to [checked] in one
     * operation, instead of the caller looping over [setItemChecked].
     *
     * `checked = true` marks the whole list complete; `checked = false` un-completes it by
     * clearing every checkbox. Both directions are supported — this is not a one-way
     * "complete" action.
     *
     * A list with no items is a valid target and is a no-op, not an error. Note that an
     * empty list is *not* considered complete under the derived-completion rule ("every
     * item is checked"), so bulk-checking an empty list correctly leaves it not-complete.
     *
     * Implementations must perform this as a single bulk database update that emits one
     * change to observers, not one write per item.
     */
    suspend fun setAllItemsChecked(listId: Long, checked: Boolean)

    /**
     * Sets or clears the quantity on item [itemId] within list [listId]. Passing `null`
     * clears the quantity.
     */
    suspend fun setItemQuantity(listId: Long, itemId: Long, quantity: String?)

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
