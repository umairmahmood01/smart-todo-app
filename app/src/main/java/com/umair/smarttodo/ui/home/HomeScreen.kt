package com.umair.smarttodo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.umair.smarttodo.R
import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.Task
import com.umair.smarttodo.domain.TaskList
import com.umair.smarttodo.domain.TaskListItem
import com.umair.smarttodo.domain.TaskSort
import com.umair.smarttodo.domain.TaskStatus
import com.umair.smarttodo.ui.home.components.AddEntryChooserSheet
import com.umair.smarttodo.ui.home.components.AddTaskListSheet
import com.umair.smarttodo.ui.home.components.AddTaskSheet
import com.umair.smarttodo.ui.home.components.CategoryChipRow
import com.umair.smarttodo.ui.home.components.EmptyTasksState
import com.umair.smarttodo.ui.home.components.HomeHeader
import com.umair.smarttodo.ui.home.components.NoResultsState
import com.umair.smarttodo.ui.home.components.ProgressCard
import com.umair.smarttodo.ui.home.components.StatusTrackerRow
import com.umair.smarttodo.ui.home.components.TaskCard
import com.umair.smarttodo.ui.home.components.TaskListCard
import com.umair.smarttodo.ui.home.components.TaskListDetailSheet
import com.umair.smarttodo.ui.home.components.TaskSearchBar
import com.umair.smarttodo.ui.theme.AmbientBottomGlow
import com.umair.smarttodo.ui.theme.AmbientTopGlow
import com.umair.smarttodo.ui.theme.AppBackground
import com.umair.smarttodo.ui.theme.FabBrush
import com.umair.smarttodo.ui.theme.NeonPurple
import com.umair.smarttodo.ui.theme.SmartTodoDimens
import com.umair.smarttodo.ui.theme.TextOnAccent
import com.umair.smarttodo.ui.theme.TextPrimary

/**
 * Stateful entry point. Everything below this function is a pure renderer.
 */
@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    HomeScreen(
        state = state,
        onSearchChange = viewModel::onSearchChange,
        onToggleCategory = viewModel::onToggleCategory,
        onToggleStatus = viewModel::onToggleStatus,
        onSortChange = viewModel::onSortChange,
        onClearFilters = viewModel::onClearFilters,
        onAddTask = viewModel::onAddTask,
        onEditTask = viewModel::onEditTask,
        onToggleStatusOf = viewModel::onToggleStatusOf,
        onSetStatus = viewModel::onSetStatus,
        onTogglePin = viewModel::onTogglePin,
        onDelete = viewModel::onDelete,
        onSetTaskReminder = viewModel::onSetTaskReminder,
        onAddTaskList = viewModel::onAddTaskList,
        previewCategory = viewModel::previewCategory,
        onToggleListItem = viewModel::onToggleListItem,
        onAddListItem = viewModel::onAddListItem,
        onRemoveListItem = viewModel::onRemoveListItem,
        onSetItemQuantity = { taskList, item, quantity ->
            viewModel.onSetItemQuantity(taskList.id, item.id, quantity)
        },
        onSetAllItemsChecked = viewModel::onSetAllItemsChecked,
        onDeleteList = viewModel::onDeleteList,
        onSetListReminder = viewModel::onSetListReminder,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onSearchChange: (String) -> Unit,
    onToggleCategory: (Category) -> Unit,
    onToggleStatus: (TaskStatus) -> Unit,
    onSortChange: (TaskSort) -> Unit,
    onClearFilters: () -> Unit,
    onAddTask: (String, String?, Long?) -> Unit,
    onEditTask: (Long, String, String?) -> Unit,
    onToggleStatusOf: (Task) -> Unit,
    onSetStatus: (Task, TaskStatus) -> Unit,
    onTogglePin: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onSetTaskReminder: (Task, Long?) -> Unit,
    onAddTaskList: (String, List<Pair<String, String?>>, Long?) -> Unit,
    previewCategory: (String) -> Category,
    onToggleListItem: (TaskList, TaskListItem) -> Unit,
    onAddListItem: (TaskList, String, String?) -> Unit,
    onRemoveListItem: (TaskList, TaskListItem) -> Unit,
    onSetItemQuantity: (TaskList, TaskListItem, String?) -> Unit,
    onSetAllItemsChecked: (listId: Long, checked: Boolean) -> Unit,
    onDeleteList: (TaskList) -> Unit,
    onSetListReminder: (TaskList, Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var addSheetStep by rememberSaveable { mutableStateOf(AddSheetStep.NONE) }
    var openListId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    val screenPadding = SmartTodoDimens.ScreenPadding

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .drawBehind { drawAmbientGlows() },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(top = 6.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(SmartTodoDimens.CardListSpacing),
        ) {
            item(key = "header") {
                Column(modifier = Modifier.padding(horizontal = screenPadding)) {
                    HomeHeader()
                    Spacer(Modifier.height(20.dp))
                    ProgressCard(
                        percent = state.completionPercent,
                        doneCount = state.doneCount,
                        totalCount = state.totalCount,
                    )
                    Spacer(Modifier.height(16.dp))
                    StatusTrackerRow(
                        counts = state.statusCounts,
                        selectedStatuses = state.selectedStatuses,
                        onStatusClick = onToggleStatus,
                    )
                    Spacer(Modifier.height(16.dp))
                    TaskSearchBar(
                        query = state.searchQuery,
                        onQueryChange = onSearchChange,
                    )
                }
            }

            if (state.visibleCategories.isNotEmpty()) {
                item(key = "chips") {
                    CategoryChipRow(
                        categories = state.visibleCategories,
                        counts = state.categoryCounts,
                        selectedCategories = state.selectedCategories,
                        onCategoryClick = onToggleCategory,
                        contentPadding = PaddingValues(horizontal = screenPadding),
                    )
                }
            }

            item(key = "section") {
                TaskSectionHeader(
                    sort = state.sort,
                    onSortChange = onSortChange,
                    modifier = Modifier.padding(horizontal = screenPadding),
                )
            }

            when {
                state.isLoading -> item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = NeonPurple)
                    }
                }

                state.isFilteredEmpty -> item(key = "no-results") {
                    NoResultsState(onClearFilters = onClearFilters)
                }

                state.isEmpty -> item(key = "empty") {
                    EmptyTasksState()
                }

                else -> items(items = state.feedItems, key = { it.feedKey }) { feedItem ->
                    when (feedItem) {
                        is HomeFeedItem.TaskEntry -> TaskCard(
                            task = feedItem.task,
                            onToggleStatus = onToggleStatusOf,
                            onSetStatus = onSetStatus,
                            onTogglePin = onTogglePin,
                            onDelete = onDelete,
                            onSetReminder = onSetTaskReminder,
                            onEdit = { editingTaskId = it.id },
                            modifier = Modifier.padding(horizontal = screenPadding),
                        )

                        is HomeFeedItem.ListEntry -> TaskListCard(
                            taskList = feedItem.taskList,
                            onOpen = { openListId = it.id },
                            onDelete = onDeleteList,
                            onSetReminder = onSetListReminder,
                            onSetAllItemsChecked = onSetAllItemsChecked,
                            modifier = Modifier.padding(horizontal = screenPadding),
                        )
                    }
                }
            }
        }

        // Fade the list out into the background at the bottom edge, as in the mockup.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(SmartTodoDimens.ListBottomFade)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            AppBackground.copy(alpha = 0.72f),
                            AppBackground,
                        ),
                    ),
                ),
        )

        AddTaskFab(
            onClick = { addSheetStep = AddSheetStep.CHOOSER },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 22.dp, bottom = 26.dp),
        )
    }

    when (addSheetStep) {
        AddSheetStep.CHOOSER -> AddEntryChooserSheet(
            onDismiss = { addSheetStep = AddSheetStep.NONE },
            onChooseSingleTask = { addSheetStep = AddSheetStep.TASK },
            onChooseTaskList = { addSheetStep = AddSheetStep.LIST },
        )

        AddSheetStep.TASK -> AddTaskSheet(
            onDismiss = { addSheetStep = AddSheetStep.NONE },
            onSave = { rawText, details, reminderAt ->
                onAddTask(rawText, details, reminderAt)
                addSheetStep = AddSheetStep.NONE
            },
        )

        AddSheetStep.LIST -> AddTaskListSheet(
            onDismiss = { addSheetStep = AddSheetStep.NONE },
            onSave = { title, items, reminderAt ->
                onAddTaskList(title, items, reminderAt)
                addSheetStep = AddSheetStep.NONE
            },
            previewCategory = previewCategory,
        )

        AddSheetStep.NONE -> Unit
    }

    val openList = openListId?.let { id ->
        state.feedItems
            .filterIsInstance<HomeFeedItem.ListEntry>()
            .map { it.taskList }
            .find { it.id == id }
    }
    if (openList != null) {
        TaskListDetailSheet(
            taskList = openList,
            onDismiss = { openListId = null },
            onToggleItem = { item -> onToggleListItem(openList, item) },
            onAddItem = { text, quantity -> onAddListItem(openList, text, quantity) },
            onRemoveItem = { item -> onRemoveListItem(openList, item) },
            onSetItemQuantity = { item, quantity -> onSetItemQuantity(openList, item, quantity) },
            onSetAllItemsChecked = { checked -> onSetAllItemsChecked(openList.id, checked) },
            onSetReminder = { atMillis -> onSetListReminder(openList, atMillis) },
            onDelete = {
                onDeleteList(openList)
                openListId = null
            },
        )
    }
    // The list may disappear (e.g. deleted from another surface) while its detail
    // sheet is open; clear the stale id via a LaunchedEffect rather than mutating
    // state directly during composition.
    //
    // Note this now also fires when a list becomes complete while its sheet is open,
    // because completed lists leave the default feed: ticking the last checkbox, or
    // tapping "Mark all complete", closes the sheet and the list reappears under the
    // "Done" pill. That is the same behaviour for both routes, deliberately.
    LaunchedEffect(openListId, openList) {
        if (openListId != null && openList == null) {
            openListId = null
        }
    }

    val editingTask = editingTaskId?.let { id ->
        state.feedItems
            .filterIsInstance<HomeFeedItem.TaskEntry>()
            .map { it.task }
            .find { it.id == id }
    }
    if (editingTask != null) {
        AddTaskSheet(
            task = editingTask,
            onDismiss = { editingTaskId = null },
            onSave = { rawText, details, _ ->
                onEditTask(editingTask.id, rawText, details)
                editingTaskId = null
            },
        )
    }
    // Same stale-id guard as openListId above: the task may disappear (e.g. deleted)
    // while its edit sheet is open.
    LaunchedEffect(editingTaskId, editingTask) {
        if (editingTaskId != null && editingTask == null) {
            editingTaskId = null
        }
    }
}

private enum class AddSheetStep { NONE, CHOOSER, TASK, LIST }

@Composable
private fun TaskSectionHeader(
    sort: TaskSort,
    onSortChange: (TaskSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = stringResource(R.string.section_your_tasks),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        val nextSort = if (sort == TaskSort.CREATED_DESC) {
            TaskSort.CREATED_ASC
        } else {
            TaskSort.CREATED_DESC
        }
        val label = if (sort == TaskSort.CREATED_DESC) {
            R.string.sort_newest_first
        } else {
            R.string.sort_oldest_first
        }
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            color = NeonPurple,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onSortChange(nextSort) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun AddTaskFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            // The soft outer glow ring from the mockup.
            .background(NeonPurple.copy(alpha = 0.14f), CircleShape)
            .padding(6.dp)
            .size(SmartTodoDimens.FabSize)
            .clip(CircleShape)
            .background(FabBrush)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = stringResource(R.string.cd_add_task),
            tint = TextOnAccent,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Two soft radial washes behind the whole screen (mockup ::before / ::after). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAmbientGlows() {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(AmbientTopGlow, Color.Transparent),
            center = Offset(size.width * 0.25f, -size.height * 0.02f),
            radius = size.width * 0.85f,
        ),
        radius = size.width * 0.85f,
        center = Offset(size.width * 0.25f, -size.height * 0.02f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(AmbientBottomGlow, Color.Transparent),
            center = Offset(size.width * 0.95f, size.height * 0.95f),
            radius = size.width * 0.8f,
        ),
        radius = size.width * 0.8f,
        center = Offset(size.width * 0.95f, size.height * 0.95f),
    )
}
