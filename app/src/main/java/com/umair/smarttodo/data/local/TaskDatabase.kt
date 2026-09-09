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
 */
@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {

    /** Data access object for the `tasks` table. */
    abstract fun taskDao(): TaskDao

    companion object {
        /**
         * Current schema version, mirroring the literal in `@Database`. Bump both together
         * with a migration, never on their own.
         */
        const val VERSION: Int = 1

        /** On-disk file name for the database. */
        const val NAME: String = "smart_todo.db"
    }
}
