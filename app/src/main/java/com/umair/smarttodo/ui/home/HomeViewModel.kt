package com.umair.smarttodo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.Task
import com.umair.smarttodo.domain.TaskCategorizer
import com.umair.smarttodo.domain.TaskList
import com.umair.smarttodo.domain.TaskListItem
import com.umair.smarttodo.domain.TaskListRepository
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
    private val taskListRepository: TaskListRepository,
    private val taskCategorizer: TaskCategorizer,
) : ViewModel() {

    /** What the user has typed right now, echoed straight back to the text field. */
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
    private val feedSnapshot: Flow<FeedSnapshot> =
        combine(debouncedQuery, filterInput) { query, filters -> QueryKey(query, filters) }
            .distinctUntilChanged()
            .flatMapLatest { key ->
                val query = key.query.ifBlank { null }
                combine(
                    // The tasks the user sees: every filter applied.
                    repository.observeTasks(
                        query = query,
                        categories = key.filters.categories,
                        statuses = key.filters.statuses,
                        sort = key.filters.sort,
                    ),
                    // The task counting scope: query only, so chips and tracker pills keep
                    // showing real totals while the user filters by them.
                    repository.observeTasks(
                        query = query,
                        categories = emptySet(),
                        statuses = emptySet(),
                        sort = key.filters.sort,
                    ),
                    // The lists the user sees. Lists have no status, so the status filter
                    // is applied afterwards (it simply hides all lists, not a bug).
                    taskListRepository.observeTaskLists(
                        query = query,
                        categories = key.filters.categories,
                        sort = key.filters.sort,
                    ),
                    // The list counting scope: query only.
                    taskListRepository.observeTaskLists(
                        query = query,
                        categories = emptySet(),
                        sort = key.filters.sort,
                    ),
                ) { visibleTasks, scopeTasks, visibleLists, scopeLists ->
                    FeedSnapshot(visibleTasks, scopeTasks, visibleLists, scopeLists)
                }
            }

    val uiState: StateFlow<HomeUiState> =
        combine(searchInput, filterInput, feedSnapshot) { query, filters, snapshot ->
            val statusCounts = snapshot.scopeTasks.groupingBy { it.status }.eachCount()
            val categoryCounts = (
                snapshot.scopeTasks.asSequence().map { it.category } +
                    snapshot.scopeLists.asSequence().map { it.category }
                )
                .toList()
                .groupingBy { it }
                .eachCount()
            HomeUiState(
                feedItems = buildFeed(
                    visibleTasks = snapshot.visibleTasks,
                    visibleLists = snapshot.visibleLists,
                    statusFilterActive = filters.statuses.isNotEmpty(),
                    sort = filters.sort,
                ),
                searchQuery = query,
                selectedCategories = filters.categories,
                selectedStatuses = filters.statuses,
                sort = filters.sort,
                statusCounts = statusCounts,
                categoryCounts = categoryCounts,
                completionPercent = completionPercent(
                    done = statusCounts[TaskStatus.DONE] ?: 0,
                    total = snapshot.scopeTasks.size,
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

    /**
     * Raw text in, categorization is the data layer job, not the UI job.
     *
     * [reminderAt], when non-null, is the instant the user picked in the "Remind me" row
     * of [com.umair.smarttodo.ui.home.components.AddTaskSheet].
     */
    fun onAddTask(rawText: String, reminderAt: Long? = null) {
        val text = rawText.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            val id = repository.addTask(text)
            if (id != 0L && reminderAt != null) {
                repository.setReminder(id, reminderAt)
            }
        }
    }

    /** Cycles To Do to In Progress to Done to To Do. */
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

    /** Sets, or with `atMillis = null` clears, the reminder on an existing task. */
    fun onSetTaskReminder(task: Task, atMillis: Long?) {
        viewModelScope.launch { repository.setReminder(task.id, atMillis) }
    }

    /**
     * Creates a task list. There is no auto-categorize entry point for lists in the
     * frozen [TaskListRepository] contract the way [TaskRepository.addTask] auto-
     * categorizes plain tasks; `createTaskList` takes an explicit [Category]. Rather
     * than defaulting every list to [Category.OTHER], this runs [title] through the
     * same [TaskCategorizer] the data layer uses for tasks (already exposed as a plain,
     * deterministic, side-effect-free domain interface, see [TaskCategorizer]) so a
     * list titled e.g. "Grocery list" still lands in Shopping instead of Other.
     *
     * [reminderAt], when non-null, is applied immediately after creation: unlike
     * [onAddTask], this works cleanly because [TaskListRepository.createTaskList]
     * returns the new id.
     */
    fun onAddTaskList(title: String, itemTexts: List<String>, reminderAt: Long? = null) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return
        val items = itemTexts.map { it.trim() }.filter { it.isNotEmpty() }
        viewModelScope.launch {
            val category = taskCategorizer.categorize(trimmedTitle)
            val newId = taskListRepository.createTaskList(trimmedTitle, category, items)
            if (reminderAt != null) {
                taskListRepository.setReminder(newId, reminderAt)
            }
        }
    }

    fun onToggleListItem(taskList: TaskList, item: TaskListItem) {
        viewModelScope.launch {
            taskListRepository.setItemChecked(taskList.id, item.id, !item.isChecked)
        }
    }

    fun onAddListItem(taskList: TaskList, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { taskListRepository.addItem(taskList.id, trimmed) }
    }

    fun onRemoveListItem(taskList: TaskList, item: TaskListItem) {
        viewModelScope.launch { taskListRepository.removeItem(taskList.id, item.id) }
    }

    fun onDeleteList(taskList: TaskList) {
        viewModelScope.launch { taskListRepository.delete(taskList.id) }
    }

    /** Sets, or with `atMillis = null` clears, the reminder on an existing task list. */
    fun onSetListReminder(taskList: TaskList, atMillis: Long?) {
        viewModelScope.launch { taskListRepository.setReminder(taskList.id, atMillis) }
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

    private data class FeedSnapshot(
        val visibleTasks: List<Task>,
        val scopeTasks: List<Task>,
        val visibleLists: List<TaskList>,
        val scopeLists: List<TaskList>,
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

/** Guarded percentage: an empty list is 0 percent, never a divide-by-zero. */
internal fun completionPercent(done: Int, total: Int): Int =
    if (total <= 0) 0 else ((done * 100f) / total).toInt().coerceIn(0, 100)

/**
 * Merges filtered tasks and lists into one feed, sorted together by createdDate.
 * Pinned tasks keep their pinned-first placement (lists have no pin concept, so they
 * never jump the queue); when a status filter is active, lists are excluded entirely
 * because they have no [TaskStatus] to filter by, that is correct, not a bug.
 */
internal fun buildFeed(
    visibleTasks: List<Task>,
    visibleLists: List<TaskList>,
    statusFilterActive: Boolean,
    sort: TaskSort,
): List<HomeFeedItem> {
    if (statusFilterActive) {
        return visibleTasks.map { HomeFeedItem.TaskEntry(it) }
    }
    val pinned = visibleTasks.filter { it.isPinned }.map { HomeFeedItem.TaskEntry(it) }
    val unpinnedTasks = visibleTasks.filterNot { it.isPinned }.map { HomeFeedItem.TaskEntry(it) }
    val lists = visibleLists.map { HomeFeedItem.ListEntry(it) }
    val rest = (unpinnedTasks + lists).sortedWith(
        if (sort == TaskSort.CREATED_ASC) {
            compareBy { it.createdDate }
        } else {
            compareByDescending { it.createdDate }
        },
    )
    return pinned + rest
}
