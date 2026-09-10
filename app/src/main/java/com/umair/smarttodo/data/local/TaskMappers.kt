package com.umair.smarttodo.data.local

import com.umair.smarttodo.domain.Task

/**
 * Converts a persistence row into its domain representation.
 *
 * The mapping is total and lossless: every column has a matching domain property.
 */
fun TaskEntity.toDomain(): Task = Task(
    id = id,
    rawText = rawText,
    normalizedEnglishText = normalizedEnglishText,
    details = details,
    category = category,
    status = status,
    isPinned = isPinned,
    createdDate = createdDate,
    dueDate = dueDate,
    reminderAt = reminderAt,
)

/**
 * Converts a domain task into a persistence row.
 *
 * An [Task.id] of `0` is left as-is so Room's `autoGenerate` primary key assigns a real id
 * on insert.
 */
fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    rawText = rawText,
    normalizedEnglishText = normalizedEnglishText,
    details = details,
    category = category,
    status = status,
    isPinned = isPinned,
    createdDate = createdDate,
    dueDate = dueDate,
    reminderAt = reminderAt,
)

/** Convenience bulk mapping used by the repository's `Flow` transform. */
fun List<TaskEntity>.toDomain(): List<Task> = map(TaskEntity::toDomain)
