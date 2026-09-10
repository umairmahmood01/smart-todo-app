package com.umair.smarttodo.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.umair.smarttodo.domain.Category

/**
 * Room row for a single task list (checklist) header.
 *
 * Column names mirror [com.umair.smarttodo.domain.TaskList] one for one, minus [items] which
 * live in [TaskListItemEntity] and are joined in via [TaskListWithItems]. Added in schema
 * version 2; see `Migrations.MIGRATION_1_2`.
 */
@Entity(
    tableName = "task_lists",
    indices = [
        Index(value = ["category"]),
        Index(value = ["createdDate"]),
    ],
)
data class TaskListEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "category")
    val category: Category,

    /** Epoch milliseconds (UTC). */
    @ColumnInfo(name = "createdDate")
    val createdDate: Long,

    /** Epoch milliseconds (UTC), or `null` when the list has no reminder scheduled. */
    @ColumnInfo(name = "reminderAt")
    val reminderAt: Long? = null,
)
