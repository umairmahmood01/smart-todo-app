package com.umair.smarttodo.data.repository

import com.umair.smarttodo.data.local.TaskDao
import com.umair.smarttodo.data.local.TaskEntity
import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [TaskDao] for unit tests.
 *
 * It records the arguments the repository binds to the query so the tests can assert on the
 * SQL contract (filter sentinels, sort direction, `LIKE` escaping) without a real database.
 * Filtering itself is only approximated - Room's SQL is verified by CI's build step, not here.
 */
class FakeTaskDao : TaskDao {

    /** Arguments captured from the most recent [observeTasks] call. */
    data class ObserveArgs(
        val query: String?,
        val ignoreCategories: Int,
        val categories: List<String>,
        val ignoreStatuses: Int,
        val statuses: List<String>,
        val ascending: Int,
    )

    private val rows = MutableStateFlow<List<TaskEntity>>(emptyList())

    var lastObserveArgs: ObserveArgs? = null
        private set

    var insertCount: Int = 0
        private set

    val inserted: MutableList<TaskEntity> = mutableListOf()

    /** How many times [applyEnrichment] was called, including calls that matched no row. */
    var applyEnrichmentCount: Int = 0
        private set

    /** Current contents of the fake table. */
    val currentRows: List<TaskEntity> get() = rows.value

    private var nextId = 1L

    /** Seeds the fake table, replacing whatever was there. */
    fun setRows(vararg entities: TaskEntity) {
        rows.value = entities.toList()
    }

    override fun observeTasks(
        query: String?,
        ignoreCategories: Int,
        categories: List<String>,
        ignoreStatuses: Int,
        statuses: List<String>,
        ascending: Int,
    ): Flow<List<TaskEntity>> {
        lastObserveArgs = ObserveArgs(query, ignoreCategories, categories, ignoreStatuses, statuses, ascending)
        return rows
    }

    override fun observeTask(id: Long): Flow<TaskEntity?> = rows.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun insert(task: TaskEntity): Long {
        insertCount++
        val id = nextId++
        val stored = task.copy(id = id)
        inserted += stored
        rows.value = rows.value + stored
        return id
    }

    override suspend fun update(task: TaskEntity) {
        rows.value = rows.value.map { if (it.id == task.id) task else it }
    }

    override suspend fun updateStatus(id: Long, status: TaskStatus) {
        rows.value = rows.value.map { if (it.id == id) it.copy(status = status) else it }
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        rows.value = rows.value.map { if (it.id == id) it.copy(isPinned = pinned) else it }
    }

    /** How many rows [updateReminder] actually changed, summed across every call. */
    var updateReminderCallCount: Int = 0
        private set

    override suspend fun updateReminder(id: Long, atMillis: Long?): Int {
        updateReminderCallCount++
        var updated = 0
        rows.value = rows.value.map { row ->
            if (row.id == id) {
                updated++
                row.copy(reminderAt = atMillis)
            } else {
                row
            }
        }
        return updated
    }

    override suspend fun getById(id: Long): TaskEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun applyEnrichment(
        id: Long,
        normalizedEnglishText: String,
        category: Category,
    ): Int {
        applyEnrichmentCount++
        var updated = 0
        rows.value = rows.value.map { row ->
            if (row.id == id) {
                updated++
                row.copy(normalizedEnglishText = normalizedEnglishText, category = category)
            } else {
                row
            }
        }
        return updated
    }

    override suspend fun deleteById(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }
}
