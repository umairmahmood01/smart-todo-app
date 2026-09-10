package com.umair.smarttodo.ui.home

import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.Task
import com.umair.smarttodo.domain.TaskList
import com.umair.smarttodo.domain.TaskListItem
import com.umair.smarttodo.domain.TaskListRepository
import com.umair.smarttodo.domain.TaskRepository
import com.umair.smarttodo.domain.TaskSort
import com.umair.smarttodo.domain.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Preview-only sample data and tiny in-memory repositories.
 *
 * This deliberately lives in the UI package so it can never collide with the real
 * data-layer implementation.
 */
internal object PreviewData {

    private const val FEB_18_2024 = 1_708_257_600_000L
    private const val DEC_01_2024 = 1_733_011_200_000L

    val tasks: List<Task> = listOf(
        Task(
            id = 1L,
            rawText = "Website Development",
            normalizedEnglishText = "Create a landing page with React.js for the client.",
            details = "Hero section, testimonials carousel, and a pricing table. " +
                "Client wants it mobile-first, dark mode optional.",
            category = Category.CODING,
            status = TaskStatus.IN_PROGRESS,
            isPinned = true,
            createdDate = FEB_18_2024,
        ),
        Task(
            id = 2L,
            rawText = "Workout",
            category = Category.HEALTH_FITNESS,
            status = TaskStatus.TODO,
            createdDate = DEC_01_2024,
            dueDate = DEC_01_2024 + 34_200_000L,
        ),
        Task(
            id = 3L,
            rawText = "Kal client ko report bhejni hai",
            normalizedEnglishText = "Send the report to the client tomorrow.",
            details = "Include the Q3 numbers and the churn chart.",
            category = Category.WORK,
            status = TaskStatus.TODO,
            createdDate = DEC_01_2024,
        ),
        Task(
            id = 4L,
            rawText = "Read two chapters of the algorithms book",
            category = Category.STUDY,
            status = TaskStatus.DONE,
            createdDate = DEC_01_2024,
        ),
        Task(
            id = 5L,
            rawText = "Renew the domain subscription",
            category = Category.FINANCE,
            status = TaskStatus.TODO,
            createdDate = DEC_01_2024,
        ),
    )

    val taskLists: List<TaskList> = listOf(
        TaskList(
            id = 1L,
            title = "Grocery list",
            category = Category.SHOPPING,
            items = listOf(
                TaskListItem(id = 1L, text = "Milk", isChecked = true),
                TaskListItem(id = 2L, text = "Eggs", isChecked = true),
                TaskListItem(id = 3L, text = "Bread"),
                TaskListItem(id = 4L, text = "Coffee"),
                TaskListItem(id = 5L, text = "Spinach"),
            ),
            createdDate = DEC_01_2024,
            reminderAt = DEC_01_2024 + 3_600_000L,
        ),
    )

    private val feed: List<HomeFeedItem> =
        tasks.map { HomeFeedItem.TaskEntry(it) } + taskLists.map { HomeFeedItem.ListEntry(it) }

    val state: HomeUiState = HomeUiState(
        feedItems = feed,
        statusCounts = tasks.groupingBy { it.status }.eachCount(),
        categoryCounts = (tasks.map { it.category } + taskLists.map { it.category })
            .groupingBy { it }
            .eachCount(),
        completionPercent = completionPercent(
            done = tasks.count { it.status == TaskStatus.DONE },
            total = tasks.size,
        ),
        isLoading = false,
    )

    val emptyState: HomeUiState = HomeUiState(isLoading = false)

    val noResultsState: HomeUiState = HomeUiState(
        searchQuery = "invoice",
        selectedCategories = setOf(Category.CODING),
        statusCounts = tasks.groupingBy { it.status }.eachCount(),
        categoryCounts = tasks.groupingBy { it.category }.eachCount(),
        completionPercent = 20,
        isLoading = false,
    )
}

/** In-memory stand-in used only by @Preview functions. */
internal class PreviewTaskRepository(
    initial: List<Task> = PreviewData.tasks,
) : TaskRepository {

    private val state = MutableStateFlow(initial)

    override fun observeTasks(
        query: String?,
        categories: Set<Category>,
        statuses: Set<TaskStatus>,
        sort: TaskSort,
    ): Flow<List<Task>> = state.map { tasks ->
        tasks.asSequence()
            .filter { task ->
                query.isNullOrBlank() ||
                    task.rawText.contains(query, ignoreCase = true) ||
                    task.normalizedEnglishText?.contains(query, ignoreCase = true) == true
            }
            .filter { categories.isEmpty() || it.category in categories }
            .filter { statuses.isEmpty() || it.status in statuses }
            .sortedWith(
                compareByDescending<Task> { it.isPinned }
                    .thenBy { if (sort == TaskSort.CREATED_ASC) it.createdDate else -it.createdDate },
            )
            .toList()
    }

    override suspend fun addTask(rawText: String, details: String?): Long {
        val nextId = (state.value.maxOfOrNull { it.id } ?: 0L) + 1L
        state.value = state.value + Task(
            id = nextId,
            rawText = rawText,
            details = details,
            category = Category.OTHER,
            status = TaskStatus.TODO,
            createdDate = System.currentTimeMillis(),
        )
        return nextId
    }

    // Preview-only: unlike the real implementation, this fake does not re-categorize or
    // re-queue enrichment on edit, it just updates the two fields in place.
    override suspend fun updateTask(id: Long, rawText: String, details: String?) {
        val text = rawText.trim()
        if (text.isEmpty()) return
        state.value = state.value.map {
            if (it.id == id) {
                it.copy(rawText = text, details = details, normalizedEnglishText = null)
            } else {
                it
            }
        }
    }

    override suspend fun updateStatus(id: Long, status: TaskStatus) {
        state.value = state.value.map { if (it.id == id) it.copy(status = status) else it }
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        state.value = state.value.map { if (it.id == id) it.copy(isPinned = pinned) else it }
    }

    override suspend fun setReminder(id: Long, atMillis: Long?) {
        state.value = state.value.map { if (it.id == id) it.copy(reminderAt = atMillis) else it }
    }

    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }
}

/** In-memory stand-in used only by @Preview functions. */
internal class PreviewTaskListRepository(
    initial: List<TaskList> = PreviewData.taskLists,
) : TaskListRepository {

    private val state = MutableStateFlow(initial)

    override fun observeTaskLists(
        query: String?,
        categories: Set<Category>,
        sort: TaskSort,
    ): Flow<List<TaskList>> = state.map { lists ->
        lists.asSequence()
            .filter { query.isNullOrBlank() || it.title.contains(query, ignoreCase = true) }
            .filter { categories.isEmpty() || it.category in categories }
            .sortedWith(
                compareBy { if (sort == TaskSort.CREATED_ASC) it.createdDate else -it.createdDate },
            )
            .toList()
    }

    override suspend fun createTaskList(
        title: String,
        category: Category,
        items: List<TaskListItem>,
    ): Long {
        val nextId = (state.value.maxOfOrNull { it.id } ?: 0L) + 1L
        // Per the interface contract: only text/quantity are taken from each supplied
        // item, id and isChecked are always freshly generated / false.
        val newItems = items.mapIndexed { index, item ->
            TaskListItem(id = index.toLong() + 1L, text = item.text, quantity = item.quantity)
        }
        state.value = state.value + TaskList(
            id = nextId,
            title = title,
            category = category,
            items = newItems,
            createdDate = System.currentTimeMillis(),
        )
        return nextId
    }

    override suspend fun addItem(listId: Long, text: String, quantity: String?) {
        state.value = state.value.map { list ->
            if (list.id != listId) return@map list
            val nextItemId = (list.items.maxOfOrNull { it.id } ?: 0L) + 1L
            list.copy(
                items = list.items + TaskListItem(id = nextItemId, text = text, quantity = quantity),
            )
        }
    }

    override suspend fun setItemQuantity(listId: Long, itemId: Long, quantity: String?) {
        state.value = state.value.map { list ->
            if (list.id != listId) return@map list
            list.copy(
                items = list.items.map { item ->
                    if (item.id == itemId) item.copy(quantity = quantity) else item
                },
            )
        }
    }

    override suspend fun setItemChecked(listId: Long, itemId: Long, checked: Boolean) {
        state.value = state.value.map { list ->
            if (list.id != listId) return@map list
            list.copy(
                items = list.items.map { item ->
                    if (item.id == itemId) item.copy(isChecked = checked) else item
                },
            )
        }
    }

    /**
     * Preview-only stand-in for the bulk check/uncheck. The real implementation does this
     * as a single database UPDATE; here one assignment to [state] is already a single
     * emission, so the "one write, one emission" half of the contract holds for free.
     *
     * An empty list is a no-op rather than an error, per the interface contract - and
     * correctly stays not-complete afterwards, since completion means "non-empty and every
     * item checked".
     */
    override suspend fun setAllItemsChecked(listId: Long, checked: Boolean) {
        state.value = state.value.map { list ->
            if (list.id != listId) return@map list
            list.copy(items = list.items.map { item -> item.copy(isChecked = checked) })
        }
    }

    override suspend fun removeItem(listId: Long, itemId: Long) {
        state.value = state.value.map { list ->
            if (list.id != listId) return@map list
            list.copy(items = list.items.filterNot { it.id == itemId })
        }
    }

    override suspend fun setReminder(listId: Long, atMillis: Long?) {
        state.value = state.value.map { if (it.id == listId) it.copy(reminderAt = atMillis) else it }
    }

    override suspend fun delete(listId: Long) {
        state.value = state.value.filterNot { it.id == listId }
    }
}

/** Deterministic no-op categorizer used only by @Preview functions. */
internal class PreviewTaskCategorizer : com.umair.smarttodo.domain.TaskCategorizer {
    override fun categorize(rawText: String): Category = Category.OTHER
}
