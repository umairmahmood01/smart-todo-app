package com.umair.smarttodo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.Task
import com.umair.smarttodo.domain.TaskRepository
import com.umair.smarttodo.domain.TaskSort
import com.umair.smarttodo.domain.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns every piece of home-screen state. Composables below this point are pure
 * renderers: they receive [HomeUiState] and emit intents back through the `on*`
 * functions.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TaskRepository,
) : ViewModel() {

    /** What the user has typed *right now* — echoed straight back to the text field. */
    private val searchInput = MutableStateFlow("")

    /** Chip/sort selections, which do not need debouncing. */
    private val filterInput = MutableStateFlow(FilterInput())

    @OptIn(FlowPreview::class)
    private val debouncedQuery: Flow<String> = searchInput
        // Typing should not hit the database on every keystroke, but clearing the
        // field (or the very first emission) should feel instant.
        .debounce { query -> if (query.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .map { it.trim() }
        .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val tasksSnapshot: Flow<TasksSnapshot> =
        combine(debouncedQuery, filterInput) { query, filters -> QueryKey(query, filters) }
            .distinctUntilChanged()
            .flatMapLatest { key ->
                val query = key.query.ifBlank { null }
                combine(
                    // The list the user sees: every filter applied.
                    repository.observeTasks(
                        query = query,
                        categories = key.filters.categories,
                        statuses = key.filters.statuses,
                        sort = key.filters.sort,
                    ),
                    // The counting scope: query only, so chips and tracker pills keep
                    // showing real totals while the user filters by them.
                    repository.observeTasks(
                        query = query,
                        categories = emptySet(),
                        statuses = emptySet(),
                        sort = key.filters.sort,
                    ),
                ) { visible, scope -> TasksSnapshot(visible, scope) }
            }

    val uiState: StateFlow<HomeUiState> =
        combine(searchInput, filterInput, tasksSnapshot) { query, filters, snapshot ->
            val statusCounts = snapshot.scope.groupingBy { it.status }.eachCount()
            val categoryCounts = snapshot.scope.groupingBy { it.category }.eachCount()
            HomeUiState(
                tasks = snapshot.visible,
                searchQuery = query,
                selectedCategories = filters.categories,
                selectedStatuses = filters.statuses,
                sort = filters.sort,
                statusCounts = statusCounts,
                categoryCounts = categoryCounts,
                completionPercent = completionPercent(
                    done = statusCounts[TaskStatus.DONE] ?: 0,
                    total = snapshot.scope.size,
                ),
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = HomeUiState(),
        )

    fun onSearchChange(query: String) {
        searchInput.value = query
    }

    fun onToggleCategory(category: Category) {
        filterInput.update { current ->
            current.copy(categories = current.categories.toggled(category))
        }
    }

    fun onToggleStatus(status: TaskStatus) {
        filterInput.update { current ->
            current.copy(statuses = current.statuses.toggled(status))
        }
    }

    fun onSortChange(sort: TaskSort) {
        filterInput.update { current -> current.copy(sort = sort) }
    }

    fun onClearFilters() {
        searchInput.value = ""
        filterInput.value = FilterInput(sort = filterInput.value.sort)
    }

    /** Raw text in — categorization is the data layer's job, not the UI's. */
    fun onAddTask(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty()) return
        viewModelScope.launch { repository.addTask(text) }
    }

    /** Cycles To Do → In Progress → Done → To Do. */
    fun onToggleStatusOf(task: Task) {
        onSetStatus(task, task.status.next())
    }

    fun onSetStatus(task: Task, status: TaskStatus) {
        if (task.status == status) return
        viewModelScope.launch { repository.updateStatus(task.id, status) }
    }

    fun onTogglePin(task: Task) {
        viewModelScope.launch { repository.setPinned(task.id, !task.isPinned) }
    }

    fun onDelete(task: Task) {
        viewModelScope.launch { repository.delete(task.id) }
    }

    private data class FilterInput(
        val categories: Set<Category> = emptySet(),
        val statuses: Set<TaskStatus> = emptySet(),
        val sort: TaskSort = TaskSort.CREATED_DESC,
    )

    private data class QueryKey(
        val query: String,
        val filters: FilterInput,
    )

    private data class TasksSnapshot(
        val visible: List<Task>,
        val scope: List<Task>,
    )

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private fun <T> Set<T>.toggled(value: T): Set<T> =
    if (value in this) this - value else this + value

private fun TaskStatus.next(): TaskStatus = when (this) {
    TaskStatus.TODO -> TaskStatus.IN_PROGRESS
    TaskStatus.IN_PROGRESS -> TaskStatus.DONE
    TaskStatus.DONE -> TaskStatus.TODO
}

/** Guarded percentage: an empty list is 0%, never a divide-by-zero. */
internal fun completionPercent(done: Int, total: Int): Int =
    if (total <= 0) 0 else ((done * 100f) / total).toInt().coerceIn(0, 100)
