package com.umair.smarttodo.domain

/**
 * One checkable line within a [TaskList] (e.g. "milk" in a "Grocery list").
 *
 * @param id persistence identity; 0 means "not yet persisted".
 * @param text exactly what the user typed for this line.
 * @param isChecked whether this line has been checked off.
 */
data class TaskListItem(
    val id: Long = 0,
    val text: String,
    val isChecked: Boolean = false,
)
