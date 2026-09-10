package com.umair.smarttodo.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room row for a single checkable line within a [TaskListEntity].
 *
 * Deleting the parent [TaskListEntity] cascades to its items at the database level
 * ([ForeignKey.CASCADE]), matching `Migrations.MIGRATION_1_2`'s raw SQL for existing
 * databases. Added in schema version 2.
 */
@Entity(
    tableName = "task_list_items",
    foreignKeys = [
        ForeignKey(
            entity = TaskListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["listId"]),
    ],
)
data class TaskListItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "listId")
    val listId: Long,

    /** Exactly what the user typed for this line. */
    @ColumnInfo(name = "text")
    val text: String,

    /**
     * Optional free text, not a structured number (e.g. "2", "1 kg", "500g", "a dozen").
     * `null` means no quantity was given. Added in schema version 3; see
     * `Migrations.MIGRATION_2_3`.
     */
    @ColumnInfo(name = "quantity")
    val quantity: String? = null,

    @ColumnInfo(name = "isChecked", defaultValue = "0")
    val isChecked: Boolean = false,
)
