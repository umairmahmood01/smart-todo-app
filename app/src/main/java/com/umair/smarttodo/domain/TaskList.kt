package com.umair.smarttodo.domain

/**
 * A named checklist (shopping list, grocery list, packing list, etc.).
 *
 * Distinct from [Task]: a [Task] is one atomic thing to do, tracked individually with its
 * own [TaskStatus]. A [TaskList] is a named collection of short checklist items — each a
 * [TaskListItem] — sharing one [category], one [reminderAt], and one card in the UI. For
 * example a "Grocery list" [TaskList] contains "milk", "eggs", and "bread" as separately
 * checkable [items], rather than being represented as three independent [Task]s.
 *
 * Reuses the same frozen [Category] enum as [Task]; there is no separate categorization
 * concept for lists.
 *
 * @param id persistence identity; 0 means "not yet persisted".
 * @param title user-facing name of the list, e.g. "Grocery list".
 * @param category the single category shared by the whole list.
 * @param items the checklist lines, in display order.
 * @param createdDate creation time as epoch milliseconds (UTC).
 * @param reminderAt optional reminder time as epoch milliseconds (UTC) for the whole list;
 *   null means no reminder is scheduled. There is no per-item reminder.
 */
data class TaskList(
    val id: Long = 0,
    val title: String,
    val category: Category,
    val items: List<TaskListItem> = emptyList(),
    val createdDate: Long,
    val reminderAt: Long? = null,
)
