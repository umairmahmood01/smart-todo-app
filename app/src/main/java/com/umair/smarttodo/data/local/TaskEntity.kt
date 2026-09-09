package com.umair.smarttodo.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.TaskStatus

/**
 * Room row for a single task.
 *
 * Column names mirror the domain [com.umair.smarttodo.domain.Task] property names one for one,
 * which keeps the hand written `@Query` in [TaskDao] readable.
 *
 * `createdDate` / `dueDate` are epoch milliseconds (UTC) stored as `INTEGER`, so no date
 * type converter is involved. `category` / `status` are stored as `TEXT` enum names via
 * [Converters].
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["category"]),
        Index(value = ["status"]),
        Index(value = ["isPinned"]),
        Index(value = ["createdDate"]),
    ],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /** Exactly what the user typed, untouched. */
    @ColumnInfo(name = "rawText")
    val rawText: String,

    /** English normalisation of [rawText]; `null` until the enrichment layer fills it in. */
    @ColumnInfo(name = "normalizedEnglishText")
    val normalizedEnglishText: String? = null,

    @ColumnInfo(name = "category")
    val category: Category,

    @ColumnInfo(name = "status")
    val status: TaskStatus,

    @ColumnInfo(name = "isPinned", defaultValue = "0")
    val isPinned: Boolean = false,

    /** Epoch milliseconds (UTC). */
    @ColumnInfo(name = "createdDate")
    val createdDate: Long,

    /** Epoch milliseconds (UTC), or `null` when the task has no due date. */
    @ColumnInfo(name = "dueDate")
    val dueDate: Long? = null,
)
