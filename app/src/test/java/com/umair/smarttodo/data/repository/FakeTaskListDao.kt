package com.umair.smarttodo.data.repository

import com.umair.smarttodo.data.local.TaskListDao
import com.umair.smarttodo.data.local.TaskListEntity
import com.umair.smarttodo.data.local.TaskListItemEntity
import com.umair.smarttodo.data.local.TaskListWithItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * In-memory [TaskListDao] for unit tests.
 *
 * Mirrors [FakeTaskDao]: it records the arguments the repository binds to the query and
 * approximates the join/cascade behaviour a real database enforces (foreign-key delete
 * cascade, item ownership) without a real SQLite engine. Filtering itself is only
 * approximated here - Room's SQL is verified by CI's build step, not this fake.
 */
class FakeTaskListDao : TaskListDao {

    /** Arguments captured from the most recent [observeTaskLists] call. */
    data class ObserveArgs(
        val query: String?,
        val ignoreCategories: Int,
        val categories: List<String>,
        val ascending: Int,
    )

    private val listRows = MutableStateFlow<List<TaskListEntity>>(emptyList())
    private val itemRows = MutableStateFlow<List<TaskListItemEntity>>(emptyList())

    var lastObserveArgs: ObserveArgs? = null
        private set

    var insertTaskListCount: Int = 0
        private set

    var insertItemsCount: Int = 0
        private set

    var insertItemCount: Int = 0
        private set

    var deleteTaskListCount: Int = 0
        private set

    /** Number of per-item [setItemChecked] calls, to prove bulk updates do not loop. */
    var setItemCheckedCount: Int = 0
        private set

    /** Number of [setAllItemsChecked] calls, i.e. bulk writes issued. */
    var setAllItemsCheckedCount: Int = 0
        private set

    private var nextListId = 1L
    private var nextItemId = 1L

    /** Current header rows, for assertions. */
    val currentLists: List<TaskListEntity> get() = listRows.value

    /** Current item rows across every list, for assertions. */
    val currentItems: List<TaskListItemEntity> get() = itemRows.value

    /** Seeds the fake tables, replacing whatever was there. */
    fun seed(lists: List<TaskListEntity>, items: List<TaskListItemEntity> = emptyList()) {
        listRows.value = lists
        itemRows.value = items
        nextListId = (lists.maxOfOrNull { it.id } ?: 0L) + 1L
        nextItemId = (items.maxOfOrNull { it.id } ?: 0L) + 1L
    }

    override fun observeTaskLists(
        query: String?,
        ignoreCategories: Int,
        categories: List<String>,
        ascending: Int,
    ): Flow<List<TaskListWithItems>> {
        lastObserveArgs = ObserveArgs(query, ignoreCategories, categories, ascending)
        return combine(listRows, itemRows) { lists, items ->
            lists.map { list -> TaskListWithItems(list, items.filter { it.listId == list.id }) }
        }
    }

    override suspend fun insertTaskList(entity: TaskListEntity): Long {
        insertTaskListCount++
        val id = nextListId++
        listRows.value = listRows.value + entity.copy(id = id)
        return id
    }

    override suspend fun insertItems(items: List<TaskListItemEntity>) {
        insertItemsCount++
        val stored = items.map { it.copy(id = nextItemId++) }
        itemRows.value = itemRows.value + stored
    }

    override suspend fun insertItem(item: TaskListItemEntity): Long {
        insertItemCount++
        val id = nextItemId++
        itemRows.value = itemRows.value + item.copy(id = id)
        return id
    }

    override suspend fun setItemChecked(itemId: Long, checked: Boolean): Int {
        setItemCheckedCount++
        var updated = 0
        itemRows.value = itemRows.value.map { row ->
            if (row.id == itemId) {
                updated++
                row.copy(isChecked = checked)
            } else {
                row
            }
        }
        return updated
    }

    /**
     * Bulk update, scoped by `listId` exactly like the real `@Query`'s `WHERE listId` clause:
     * items of other lists keep their state. One assignment to [itemRows] means observers see
     * a single change, matching the "one write, not one per item" DAO contract.
     */
    override suspend fun setAllItemsChecked(listId: Long, checked: Boolean): Int {
        setAllItemsCheckedCount++
        var updated = 0
        itemRows.value = itemRows.value.map { row ->
            if (row.listId == listId) {
                updated++
                row.copy(isChecked = checked)
            } else {
                row
            }
        }
        return updated
    }

    override suspend fun updateItemQuantity(itemId: Long, quantity: String?): Int {
        var updated = 0
        itemRows.value = itemRows.value.map { row ->
            if (row.id == itemId) {
                updated++
                row.copy(quantity = quantity)
            } else {
                row
            }
        }
        return updated
    }

    override suspend fun deleteItem(itemId: Long): Int {
        val before = itemRows.value.size
        itemRows.value = itemRows.value.filterNot { it.id == itemId }
        return before - itemRows.value.size
    }

    override suspend fun updateReminder(listId: Long, atMillis: Long?): Int {
        var updated = 0
        listRows.value = listRows.value.map { row ->
            if (row.id == listId) {
                updated++
                row.copy(reminderAt = atMillis)
            } else {
                row
            }
        }
        return updated
    }

    override suspend fun deleteTaskList(listId: Long): Int {
        deleteTaskListCount++
        val before = listRows.value.size
        listRows.value = listRows.value.filterNot { it.id == listId }
        val removed = before - listRows.value.size
        // Foreign key ON DELETE CASCADE, approximated: a deleted list takes its items with it.
        if (removed > 0) {
            itemRows.value = itemRows.value.filterNot { it.listId == listId }
        }
        return removed
    }

    override suspend fun getTaskListById(listId: Long): TaskListWithItems? {
        val list = listRows.value.firstOrNull { it.id == listId } ?: return null
        return TaskListWithItems(list, itemRows.value.filter { it.listId == listId })
    }
}
