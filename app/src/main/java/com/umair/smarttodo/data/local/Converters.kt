package com.umair.smarttodo.data.local

import androidx.room.TypeConverter
import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.TaskStatus

/**
 * Room type converters for the enum columns of [TaskEntity].
 *
 * Enums are persisted by their **constant name** (not their ordinal) so that reordering
 * or inserting enum constants in a later release cannot silently re-label existing rows.
 *
 * Reading is deliberately forgiving: an unrecognised string resolves to
 * [Category.OTHER] / [TaskStatus.TODO] instead of throwing. A row written by a newer
 * build (or corrupted by hand) therefore degrades to a sane default rather than
 * crashing the query that touched it.
 */
class Converters {

    /** Serialises [value] to its enum constant name. */
    @TypeConverter
    fun fromCategory(value: Category): String = value.name

    /** Resolves [value] to a [Category], falling back to [Category.OTHER]. */
    @TypeConverter
    fun toCategory(value: String): Category = CATEGORY_BY_NAME[value.trim().uppercase()] ?: Category.OTHER

    /** Serialises [value] to its enum constant name. */
    @TypeConverter
    fun fromTaskStatus(value: TaskStatus): String = value.name

    /** Resolves [value] to a [TaskStatus], falling back to [TaskStatus.TODO]. */
    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = STATUS_BY_NAME[value.trim().uppercase()] ?: TaskStatus.TODO

    private companion object {
        private val CATEGORY_BY_NAME: Map<String, Category> = Category.entries.associateBy { it.name }
        private val STATUS_BY_NAME: Map<String, TaskStatus> = TaskStatus.entries.associateBy { it.name }
    }
}
