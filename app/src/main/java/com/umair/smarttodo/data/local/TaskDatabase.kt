package com.umair.smarttodo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The app's single Room database.
 *
 * `exportSchema = true` writes the schema JSON to the directory configured by the
 * `room.schemaLocation` KSP argument in `app/build.gradle.kts` (`app/schemas`). Keep that
 * file committed: it is the input for future auto-migration verification.
 *
 * Version 2 added `tasks.reminderAt` plus the `task_lists` / `task_list_items` tables for the
 * checklist feature; see [Migrations.MIGRATION_1_2] for the exact SQL and its verification
 * status.
 */
@Database(
    entities = [TaskEntity::class, TaskListEntity::class, TaskListItemEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {

    /** Data access object for the `tasks` table. */
    abstract fun taskDao(): TaskDao

    /** Data access object for the `task_lists` / `task_list_items` tables. */
    abstract fun taskListDao(): TaskListDao

    companion object {
        /**
         * Current schema version, mirroring the literal in `@Database`. Bump both together
         * with a migration, never on their own.
         */
        const val VERSION: Int = 2

        /** On-disk file name for the database. */
        const val NAME: String = "smart_todo.db"
    }
}
