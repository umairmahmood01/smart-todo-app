package com.umair.smarttodo.ui.home

import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.Task
import com.umair.smarttodo.domain.TaskRepository
import com.umair.smarttodo.domain.TaskSort
import com.umair.smarttodo.domain.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Preview-only sample data and a tiny in-memory [TaskRepository].
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

    val state: HomeUiState = HomeUiState(
        tasks = tasks,
        statusCounts = tasks.groupingBy { it.status }.eachCount(),
        categoryCounts = tasks.groupingBy { it.category }.eachCount(),
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

/** In-memory stand-in used only by `@Preview` functions. */
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

    override suspend fun addTask(rawText: String) {
        val nextId = (state.value.maxOfOrNull { it.id } ?: 0L) + 1L
        state.value = state.value + Task(
            id = nextId,
            rawText = rawText,
            category = Category.OTHER,
            status = TaskStatus.TODO,
            createdDate = System.currentTimeMillis(),
        )
    }

    override suspend fun updateStatus(id: Long, status: TaskStatus) {
        state.value = state.value.map { if (it.id == id) it.copy(status = status) else it }
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        state.value = state.value.map { if (it.id == id) it.copy(isPinned = pinned) else it }
    }

    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }
}
