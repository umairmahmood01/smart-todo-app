package com.umair.smarttodo.domain

/**
 * One checkable line within a [TaskList] (e.g. "milk" in a "Grocery list").
 *
 * @param id persistence identity; 0 means "not yet persisted".
 * @param text exactly what the user typed for this line.
 * @param quantity optional free text, not a structured number — matches how people actually
 *   write shopping-list quantities, e.g. "2", "1 kg", "500g", "a dozen". Null means no
 *   quantity was given.
 * @param isChecked whether this line has been checked off.
 */
data class TaskListItem(
    val id: Long = 0,
    val text: String,
    val quantity: String? = null,
    val isChecked: Boolean = false,
)
