package com.umair.smarttodo.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Hand-written Room migrations for [TaskDatabase].
 *
 * **Verification status:** these migrations have *not* been run against a real device or
 * emulator. Room migration testing requires an instrumented `androidTest` (a real SQLite
 * engine via `MigrationTestHelper`), and this development machine has no JDK/Android
 * SDK/Gradle to run one. The SQL below is reviewable on its own merits - it was checked by
 * hand against [TaskDatabase]'s entity definitions - but it is unverified until it runs in
 * CI or on a device. Treat it as reviewed, not proven.
 */
object Migrations {

    /**
     * Version 1 -> 2:
     * - adds the nullable `tasks.reminderAt` column (no default needed; existing rows get
     *   SQL `NULL`, which maps to Kotlin `null`, meaning "no reminder", matching every
     *   pre-migration task's actual state).
     * - creates `task_lists` and `task_list_items` for the new checklist feature, with
     *   `task_list_items.listId` cascading on delete of its parent `task_lists` row.
     * - creates the index Room expects for `TaskListItemEntity`'s `@Index(["listId"])`,
     *   named to match Room's own default index-naming convention
     *   (`index_<table>_<column>`) so a schema comparison against the generated schema JSON
     *   lines up column-for-column.
     */
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN reminderAt INTEGER")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS task_lists (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    category TEXT NOT NULL,
                    createdDate INTEGER NOT NULL,
                    reminderAt INTEGER
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS task_list_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    listId INTEGER NOT NULL,
                    text TEXT NOT NULL,
                    isChecked INTEGER NOT NULL,
                    FOREIGN KEY(listId) REFERENCES task_lists(id) ON DELETE CASCADE
                )
                """.trimIndent(),
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_task_list_items_listId ON task_list_items(listId)",
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_task_lists_category ON task_lists(category)",
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_task_lists_createdDate ON task_lists(createdDate)",
            )
        }
    }
}
