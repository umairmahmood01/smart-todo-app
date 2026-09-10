package com.umair.smarttodo.data.local

import com.umair.smarttodo.domain.TaskList
import com.umair.smarttodo.domain.TaskListItem

/**
 * Converts a persistence row into its domain representation.
 *
 * Items are sorted by id (their insertion order) because Room's `@Relation` query does not
 * itself guarantee an order - see [TaskListWithItems].
 */
fun TaskListWithItems.toDomain(): TaskList = TaskList(
    id = list.id,
    title = list.title,
    category = list.category,
    items = items.sortedBy { it.id }.map { it.toDomain() },
    createdDate = list.createdDate,
    reminderAt = list.reminderAt,
)

/** Converts a single checklist line into its domain representation. */
fun TaskListItemEntity.toDomain(): TaskListItem = TaskListItem(
    id = id,
    text = text,
    quantity = quantity,
    isChecked = isChecked,
)

/**
 * Converts a domain [TaskList] into its header row, dropping [TaskList.items] - those are
 * persisted separately as [TaskListItemEntity] rows once the header has a real id.
 *
 * A [TaskList.id] of `0` is left as-is so Room's `autoGenerate` primary key assigns a real id
 * on insert.
 */
fun TaskList.toEntity(): TaskListEntity = TaskListEntity(
    id = id,
    title = title,
    category = category,
    createdDate = createdDate,
    reminderAt = reminderAt,
)

/**
 * Converts a domain [TaskListItem] into a row owned by [listId].
 *
 * A [TaskListItem.id] of `0` is left as-is so Room's `autoGenerate` primary key assigns a
 * real id on insert.
 */
fun TaskListItem.toEntity(listId: Long): TaskListItemEntity = TaskListItemEntity(
    id = id,
    listId = listId,
    text = text,
    quantity = quantity,
    isChecked = isChecked,
)

/** Convenience bulk mapping used by the repository's `Flow` transform. */
fun List<TaskListWithItems>.toDomain(): List<TaskList> = map(TaskListWithItems::toDomain)
