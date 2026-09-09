package com.umair.smarttodo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.umair.smarttodo.domain.TaskStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data access for the `tasks` table.
 *
 * Reads go through the single [observeTasks] query so that search, filtering and sorting
 * always agree with each other and there is exactly one place to reason about ordering.
 */
@Dao
interface TaskDao {

    /**
     * Observes the task list with optional search / filters applied, as a hot-on-write
     * `Flow` that re-emits whenever the table changes.
     *
     * Filters use the "null or sentinel means no filter" SQL pattern so that a single
     * prepared statement covers every combination:
     *
     * - [query]: `null` disables the text filter. When present it is matched with `LIKE`
     *   against both `rawText` and `normalizedEnglishText`. SQLite's built-in `LIKE` folds
     *   case for ASCII, which is the whole alphabet this app accepts (Roman script only),
     *   so no `COLLATE NOCASE` is required — adding one would be a no-op because `COLLATE`
     *   does not influence `LIKE`. Callers **must** pass a value already escaped for the
     *   `ESCAPE '\'` clause (see `TaskRepositoryImpl.escapeLikeArgument`) so that a literal
     *   `%` or `_` typed by the user is not treated as a wildcard.
     * - [ignoreCategories] / [ignoreStatuses]: pass `1` to skip that filter entirely.
     *   The matching list is still bound (with a harmless sentinel value) because an empty
     *   `IN ()` list is needlessly implementation-specific.
     * - [ascending]: `1` sorts oldest first, `0` newest first.
     *
     * Pinned tasks always sort ahead of unpinned ones regardless of [ascending] — the
     * approved design renders them under a "Pinned" heading at the top of the list. `id` is
     * the final tiebreaker so that rows sharing a `createdDate` have a stable order.
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE (
                :query IS NULL
                OR rawText LIKE '%' || :query || '%' ESCAPE '\'
                OR (
                    normalizedEnglishText IS NOT NULL
                    AND normalizedEnglishText LIKE '%' || :query || '%' ESCAPE '\'
                )
              )
          AND (:ignoreCategories = 1 OR category IN (:categories))
          AND (:ignoreStatuses = 1 OR status IN (:statuses))
        ORDER BY isPinned DESC,
                 CASE WHEN :ascending = 1 THEN createdDate END ASC,
                 CASE WHEN :ascending = 0 THEN createdDate END DESC,
                 CASE WHEN :ascending = 1 THEN id END ASC,
                 CASE WHEN :ascending = 0 THEN id END DESC
        """
    )
    fun observeTasks(
        query: String?,
        ignoreCategories: Int,
        categories: List<String>,
        ignoreStatuses: Int,
        statuses: List<String>,
        ascending: Int,
    ): Flow<List<TaskEntity>>

    /** Observes a single row, emitting `null` once it is deleted. */
    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTask(id: Long): Flow<TaskEntity?>

    /** Inserts [task] and returns the generated row id. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(task: TaskEntity): Long

    /** Replaces every column of the row identified by [TaskEntity.id]. */
    @Update
    suspend fun update(task: TaskEntity)

    /** Sets the workflow status of a single row. No-op when [id] does not exist. */
    @Query("UPDATE tasks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TaskStatus)

    /** Pins or unpins a single row. No-op when [id] does not exist. */
    @Query("UPDATE tasks SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    /** Deletes a single row. No-op when [id] does not exist. */
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Removes every row. Intended for tests and "clear all data" style actions. */
    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
