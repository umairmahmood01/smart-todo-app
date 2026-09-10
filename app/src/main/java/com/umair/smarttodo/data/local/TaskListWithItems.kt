package com.umair.smarttodo.data.local

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room's parent+children projection for [TaskListDao.observeTaskLists] /
 * [TaskListDao.getTaskListById].
 *
 * Room resolves [items] with a second, batched query keyed on [TaskListItemEntity.listId];
 * it does not guarantee row order, so [com.umair.smarttodo.data.local.toDomain] sorts by
 * item id before exposing the domain [com.umair.smarttodo.domain.TaskList.items] list.
 */
data class TaskListWithItems(
    @Embedded
    val list: TaskListEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "listId",
    )
    val items: List<TaskListItemEntity>,
)
