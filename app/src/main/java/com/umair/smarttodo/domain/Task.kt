package com.umair.smarttodo.domain

/**
 * A single todo item.
 *
 * @param id persistence identity; 0 means "not yet persisted".
 * @param rawText exactly what the user typed, in whatever language/script they used.
 * @param normalizedEnglishText English normalization of [rawText] when the input was
 *   non-English Roman script; null when no normalization was produced.
 * @param details optional longer free-text notes/description, exactly what the user typed.
 *   Distinct from [rawText] (the short title-like text) and never touched by enrichment —
 *   unlike [rawText], it is never translated or normalized; it is stored verbatim or not
 *   at all.
 * @param createdDate creation time as epoch milliseconds (UTC).
 * @param dueDate optional due time as epoch milliseconds (UTC).
 * @param reminderAt optional reminder time as epoch milliseconds (UTC); null means no
 *   reminder is scheduled. Distinct from [dueDate] — a task can have a due date with no
 *   reminder, a reminder with no due date, both, or neither.
 */
data class Task(
    val id: Long = 0,
    val rawText: String,
    val normalizedEnglishText: String? = null,
    val details: String? = null,
    val category: Category,
    val status: TaskStatus,
    val isPinned: Boolean = false,
    val createdDate: Long,
    val dueDate: Long? = null,
    val reminderAt: Long? = null,
)
