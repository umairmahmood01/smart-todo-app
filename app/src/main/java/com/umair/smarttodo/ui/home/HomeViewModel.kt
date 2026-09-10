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
                    //
                    // With no status pill selected the feed deliberately excludes DONE.
                    // Finished work leaving the main screen is the point; it is not gone,
                    // it is one tap away behind the green "Done" pill. This is pushed down
                    // into the query as an explicit TODO + IN_PROGRESS set rather than
                    // filtered out of the result afterwards, so the database never returns
                    // rows the UI is going to throw away.
                    repository.observeTasks(
                        query = query,
                        categories = key.filters.categories,
                        statuses = key.filters.statuses.ifEmpty { DEFAULT_FEED_STATUSES },
                        sort = key.filters.sort,
                    ),
                    // The task counting scope: query only, so chips and tracker pills keep
                    // showing real totals while the user filters by them.
                    //
                    // DO NOT narrow this call. `statuses = emptySet()` here means "no
                    // status filter at all", which is what makes the tracker pill counts,
                    // the "x of y tasks" line and the progress ring keep counting DONE
                    // tasks even though the feed above hides them. Passing the feed
                    // statuses here instead would make the ring read 0 of 4 forever.
                    repository.observeTasks(
                        query = query,
                        categories = emptySet(),
                        statuses = emptySet(),
                        sort = key.filters.sort,
                    ),
                    // The lists the user sees. A task list has no status column at all -
                    // its completeness is derived from its items - so the status filter
                    // cannot be pushed into this query and is applied in buildFeed
                    // instead. See buildFeed for exactly what each pill does to lists.
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
                    selectedStatuses = filters.statuses,
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
     * Raw text and optional details in, categorization is the data layer job, not the UI
     * job.
     *
     * [reminderAt], when non-null, is the instant the user picked in the "Remind me" row
     * of [com.umair.smarttodo.ui.home.components.AddTaskSheet].
     */
    fun onAddTask(rawText: String, details: String? = null, reminderAt: Long? = null) {
        val text = rawText.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            val id = repository.addTask(text, details)
            if (id != 0L && reminderAt != null) {
                repository.setReminder(id, reminderAt)
            }
        }
    }

    /**
     * Edits an existing task's text and details. A thin passthrough: category
     * re-categorization, clearing the now-stale [Task.normalizedEnglishText], and
     * re-queuing enrichment for the new text are all handled by
     * [TaskRepository.updateTask] itself — see its KDoc for the full contract.
     */
    fun onEditTask(id: Long, rawText: String, details: String?) {
        viewModelScope.launch { repository.updateTask(id, rawText, details) }
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
     *
     * [items] carries each draft row's (text, quantity) pair exactly as typed in
     * [com.umair.smarttodo.ui.home.components.AddTaskListSheet]; blank texts are dropped
     * and quantities are trimmed to `null` when blank, the same "trim, or null if blank"
     * treatment [onAddTask] gives [Task.details].
     */
    fun onAddTaskList(
        title: String,
        items: List<Pair<String, String?>>,
        reminderAt: Long? = null,
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return
        val listItems = items.mapNotNull { (text, quantity) ->
            val trimmedText = text.trim()
            if (trimmedText.isEmpty()) return@mapNotNull null
            TaskListItem(text = trimmedText, quantity = quantity?.trim()?.ifBlank { null })
        }
        viewModelScope.launch {
            val category = taskCategorizer.categorize(trimmedTitle)
            val newId = taskListRepository.createTaskList(trimmedTitle, category, listItems)
            if (reminderAt != null) {
                taskListRepository.setReminder(newId, reminderAt)
            }
        }
    }

    /**
     * Pure passthrough to [TaskCategorizer], exposed so
     * [com.umair.smarttodo.ui.home.components.AddTaskListSheet] can preview which category
     * a not-yet-created list's title will land in — e.g. to decide whether to show
     * shopping-list quantity fields and grocery emoji before Save is even tapped — without
     * putting any categorization logic in the UI layer itself.
     */
    fun previewCategory(title: String): Category = taskCategorizer.categorize(title)

    /**
     * Checks or unchecks every item in list [listId] at once.
     *
     * This is how a list gets marked "done". A [TaskList] has no status of its own, so
     * completion is derived from its items ("every item checked, list non-empty"), and
     * before this existed the only way to complete a five-item list was five taps. The
     * repository does it as a single bulk write that emits once, so the feed re-sorts and
     * the card flips to complete in one frame.
     *
     * [checked] is a plain boolean and not a one-way "complete" flag on purpose: passing
     * false un-completes the list, which is what the UI offers once the list is already
     * fully checked.
     */
    fun onSetAllItemsChecked(listId: Long, checked: Boolean) {
        viewModelScope.launch { taskListRepository.setAllItemsChecked(listId, checked) }
    }

    fun onToggleListItem(taskList: TaskList, item: TaskListItem) {
        viewModelScope.launch {
            taskListRepository.setItemChecked(taskList.id, item.id, !item.isChecked)
        }
    }

    fun onAddListItem(taskList: TaskList, text: String, quantity: String? = null) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            taskListRepository.addItem(taskList.id, trimmed, quantity?.trim()?.ifBlank { null })
        }
    }

    /** Sets, or with `quantity = null` clears, the quantity on one list item. */
    fun onSetItemQuantity(listId: Long, itemId: Long, quantity: String?) {
        viewModelScope.launch {
            taskListRepository.setItemQuantity(listId, itemId, quantity?.trim()?.ifBlank { null })
        }
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

        /**
         * What the feed shows when no status pill is selected: everything that is not
         * finished. Note this is a real filter passed to the query, not "no filter" -
         * `emptySet()` would mean all statuses including DONE.
         */
        val DEFAULT_FEED_STATUSES = setOf(TaskStatus.TODO, TaskStatus.IN_PROGRESS)
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
 * Merges filtered tasks and lists into one feed, sorted together by createdDate. Pinned
 * tasks keep their pinned-first placement; lists have no pin concept, so they never jump
 * the queue.
 *
 * [visibleTasks] arrives already status-filtered by the query. [visibleLists] cannot be,
 * because a [TaskList] has no stored status — completion is derived from its items — so
 * the status pills are applied to lists here, by [matchesStatusFilter].
 */
internal fun buildFeed(
    visibleTasks: List<Task>,
    visibleLists: List<TaskList>,
    selectedStatuses: Set<TaskStatus>,
    sort: TaskSort,
): List<HomeFeedItem> {
    val pinned = visibleTasks.filter { it.isPinned }.map { HomeFeedItem.TaskEntry(it) }
    val unpinnedTasks = visibleTasks.filterNot { it.isPinned }.map { HomeFeedItem.TaskEntry(it) }
    val lists = visibleLists
        .filter { it.matchesStatusFilter(selectedStatuses) }
        .map { HomeFeedItem.ListEntry(it) }
    val rest = (unpinnedTasks + lists).sortedWith(
        if (sort == TaskSort.CREATED_ASC) {
            compareBy { it.createdDate }
        } else {
            compareByDescending { it.createdDate }
        },
    )
    return pinned + rest
}

/**
 * How the status pills apply to a task list, which has no [TaskStatus] of its own.
 *
 * - No pill selected: show lists that are not finished, mirroring how the feed hides
 *   done tasks by default.
 * - "Done" selected: show the finished lists, alongside the done tasks.
 * - "To-Do" or "Progress" selected: show no lists at all. A list genuinely has no such
 *   state — it is either finished or it is not — so hiding them is the correct answer
 *   rather than a gap in the implementation. This was true before completed lists were
 *   hidden by default and it is still true now.
 */
private fun TaskList.matchesStatusFilter(selectedStatuses: Set<TaskStatus>): Boolean = when {
    selectedStatuses.isEmpty() -> !isFullyChecked
    TaskStatus.DONE in selectedStatuses -> isFullyChecked
    else -> false
}
