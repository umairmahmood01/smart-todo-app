package com.umair.smarttodo.domain

/**
 * A single todo item.
 *
 * @param id persistence identity; 0 means "not yet persisted".
 * @param rawText exactly what the user typed, in whatever language/script they used.
 * @param normalizedEnglishText English normalization of [rawText] when the input was
 *   non-English Roman script; null when no normalization was produced.
 * @param createdDate creation time as epoch milliseconds (UTC).
 * @param dueDate optional due time as epoch milliseconds (UTC).
 */
data class Task(
    val id: Long = 0,
    val rawText: String,
    val normalizedEnglishText: String? = null,
    val category: Category,
    val status: TaskStatus,
    val isPinned: Boolean = false,
    val createdDate: Long,
    val dueDate: Long? = null,
)
