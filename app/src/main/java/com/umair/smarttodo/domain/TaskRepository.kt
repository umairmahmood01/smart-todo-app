package com.umair.smarttodo.domain

import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for tasks.
 *
 * An empty [categories] or [statuses] set means "no filter on that dimension",
 * not "match nothing". A null or blank [query] means "no text filter".
 */
interface TaskRepository {

    fun observeTasks(
        query: String? = null,
        categories: Set<Category> = emptySet(),
        statuses: Set<TaskStatus> = emptySet(),
        sort: TaskSort = TaskSort.CREATED_DESC,
    ): Flow<List<Task>>

    /**
     * Returns the new row's generated id, or `0L` for a blank/no-op input that was
     * not persisted (mirroring [Task.id]'s own documented convention: "0 means not
     * yet persisted").
     *
     * [details] is stored as-is (no blank-is-no-op rule, matching [updateTask]'s treatment of
     * `details`).
     */
    suspend fun addTask(rawText: String, details: String? = null): Long

    /**
     * Edits task [id]'s text and details.
     *
     * [rawText] is trimmed; if the trimmed result is blank, this call is a no-op — editing a
     * task's text down to nothing does not delete it or clear it, the same "ignore blank"
     * philosophy [addTask] applies to new tasks.
     *
     * On a non-blank edit, the implementation must, as a single logical unit:
     * 1. Re-run the categorizer on the new (trimmed) [rawText] — the category is never
     *    manually chosen anywhere in this app, and an edited text can change its category
     *    exactly as a freshly created one does.
     * 2. Persist the new [rawText], the newly computed category, and the new [details].
     * 3. Clear the stored `normalizedEnglishText` back to `null` — it described the *old*
     *    text and is now stale/wrong for the new one.
     * 4. Re-queue enrichment for the new [rawText], so a fresh translation/category
     *    refinement arrives the same way it does after [addTask].
     *
     * [details] is optional content, not a title: unlike [rawText], a null or blank
     * [details] is a valid value meaning "no details" and is passed through as-is — it is
     * never trimmed-and-treated-as-no-op the way [rawText] is. Passing `null` clears any
     * previously stored details.
     */
    suspend fun updateTask(id: Long, rawText: String, details: String?)

    suspend fun updateStatus(id: Long, status: TaskStatus)

    suspend fun setPinned(id: Long, pinned: Boolean)

    /**
     * Sets or clears the reminder for task [id]. Passing `atMillis = null` clears the
     * reminder. Implementations are responsible for scheduling/cancelling the actual
     * OS-level reminder as a side effect of this call — the caller does not do that
     * separately.
     */
    suspend fun setReminder(id: Long, atMillis: Long?)

    suspend fun delete(id: Long)
}
