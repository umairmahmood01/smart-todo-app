package com.umair.smarttodo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data access for the `task_lists` / `task_list_items` tables.
 *
 * Reads go through the single [observeTaskLists] query, mirroring [TaskDao.observeTasks]'s
 * "null or sentinel means no filter" SQL pattern so search, filtering and sorting always
 * agree with each other.
 */
@Dao
interface TaskListDao {

    /**
     * Observes the task list collection - each with its checklist items attached - with
     * optional search / filters applied, as a hot-on-write `Flow` that re-emits whenever
     * either table changes.
     *
     * - [query]: `null` disables the text filter. When present it is matched with `LIKE`
     *   against `title`. Callers **must** pass a value already escaped for the `ESCAPE '\'`
     *   clause (see `TaskListRepositoryImpl.escapeLikeArgument`), same as [TaskDao.observeTasks].
     * - [ignoreCategories]: pass `1` to skip the category filter entirely. The matching list
     *   is still bound (with a harmless sentinel value) for the same reason as [TaskDao].
     * - [ascending]: `1` sorts oldest first, `0` newest first. `id` is the tiebreaker so rows
     *   sharing a `createdDate` have a stable order.
     *
     * `@Transaction` makes the parent query and each batched child-items query for
     * [TaskListWithItems] resolve against one consistent snapshot of the database.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM task_lists
        WHERE (
                :query IS NULL
                OR title LIKE '%' || :query || '%' ESCAPE '\'
              )
          AND (:ignoreCategories = 1 OR category IN (:categories))
        ORDER BY CASE WHEN :ascending = 1 THEN createdDate END ASC,
                 CASE WHEN :ascending = 0 THEN createdDate END DESC,
                 CASE WHEN :ascending = 1 THEN id END ASC,
                 CASE WHEN :ascending = 0 THEN id END DESC
        """
    )
    fun observeTaskLists(
        query: String?,
        ignoreCategories: Int,
        categories: List<String>,
        ascending: Int,
    ): Flow<List<TaskListWithItems>>

    /** Inserts the list header and returns the generated row id. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTaskList(entity: TaskListEntity): Long

    /** Bulk-inserts checklist lines, e.g. the initial items passed to `createTaskList`. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<TaskListItemEntity>)

    /** Inserts a single checklist line and returns its generated row id. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: TaskListItemEntity): Long

    /**
     * Checks or unchecks a single item. No-op when [itemId] does not exist.
     *
     * @return the number of rows changed: `1` normally, `0` when the item was already deleted.
     */
    @Query("UPDATE task_list_items SET isChecked = :checked WHERE id = :itemId")
    suspend fun setItemChecked(itemId: Long, checked: Boolean): Int

    /**
     * Deletes a single checklist item.
     *
     * @return the number of rows changed: `1` normally, `0` when [itemId] does not exist.
     */
    @Query("DELETE FROM task_list_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long): Int

    /**
     * Sets or clears the reminder time on a single list header, touching only that column so
     * a concurrent item edit can never be clobbered - same pattern as [TaskDao.updateReminder]
     * / [TaskDao.applyEnrichment].
     *
     * @return the number of rows changed: `1` normally, `0` when [listId] does not exist.
     */
    @Query("UPDATE task_lists SET reminderAt = :atMillis WHERE id = :listId")
    suspend fun updateReminder(listId: Long, atMillis: Long?): Int

    /** Deletes a single list header. Item rows cascade via the foreign key. */
    @Query("DELETE FROM task_lists WHERE id = :listId")
    suspend fun deleteTaskList(listId: Long): Int

    /** Reads a single list with its items once, or `null` when [listId] does not exist. */
    @Transaction
    @Query("SELECT * FROM task_lists WHERE id = :listId")
    suspend fun getTaskListById(listId: Long): TaskListWithItems?
}
