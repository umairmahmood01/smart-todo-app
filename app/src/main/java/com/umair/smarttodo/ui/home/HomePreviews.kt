package com.umair.smarttodo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.umair.smarttodo.ui.home.components.CategoryChipRow
import com.umair.smarttodo.ui.home.components.EmptyTasksState
import com.umair.smarttodo.ui.home.components.HomeHeader
import com.umair.smarttodo.ui.home.components.NoResultsState
import com.umair.smarttodo.ui.home.components.ProgressCard
import com.umair.smarttodo.ui.home.components.StatusTrackerRow
import com.umair.smarttodo.ui.home.components.TaskCard
import com.umair.smarttodo.ui.home.components.TaskListCard
import com.umair.smarttodo.ui.home.components.TaskSearchBar
import com.umair.smarttodo.ui.theme.AppBackground
import com.umair.smarttodo.ui.theme.SmartTodoDimens
import com.umair.smarttodo.ui.theme.SmartTodoTheme

private const val PhoneWidth = 392
private const val PhoneHeight = 830

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    SmartTodoTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppBackground)
                .padding(SmartTodoDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
    }
}

@Preview(name = "Home - populated", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun HomeScreenPreview() {
    SmartTodoTheme {
        HomeScreen(
            state = PreviewData.state,
            onSearchChange = {},
            onToggleCategory = {},
            onToggleStatus = {},
            onSortChange = {},
            onClearFilters = {},
            onAddTask = { _, _, _ -> },
            onEditTask = { _, _, _ -> },
            onToggleStatusOf = {},
            onSetStatus = { _, _ -> },
            onTogglePin = {},
            onDelete = {},
            onSetTaskReminder = { _, _ -> },
            onAddTaskList = { _, _, _ -> },
            previewCategory = { com.umair.smarttodo.domain.Category.OTHER },
            onToggleListItem = { _, _ -> },
            onAddListItem = { _, _, _ -> },
            onRemoveListItem = { _, _ -> },
            onSetItemQuantity = { _, _, _ -> },
            onSetAllItemsChecked = { _, _ -> },
            onDeleteList = {},
            onSetListReminder = { _, _ -> },
        )
    }
}

@Preview(name = "Home - empty", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun HomeScreenEmptyPreview() {
    SmartTodoTheme {
        HomeScreen(
            state = PreviewData.emptyState,
            onSearchChange = {},
            onToggleCategory = {},
            onToggleStatus = {},
            onSortChange = {},
            onClearFilters = {},
            onAddTask = { _, _, _ -> },
            onEditTask = { _, _, _ -> },
            onToggleStatusOf = {},
            onSetStatus = { _, _ -> },
            onTogglePin = {},
            onDelete = {},
            onSetTaskReminder = { _, _ -> },
            onAddTaskList = { _, _, _ -> },
            previewCategory = { com.umair.smarttodo.domain.Category.OTHER },
            onToggleListItem = { _, _ -> },
            onAddListItem = { _, _, _ -> },
            onRemoveListItem = { _, _ -> },
            onSetItemQuantity = { _, _, _ -> },
            onSetAllItemsChecked = { _, _ -> },
            onDeleteList = {},
            onSetListReminder = { _, _ -> },
        )
    }
}

@Preview(name = "Home - no results", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun HomeScreenNoResultsPreview() {
    SmartTodoTheme {
        HomeScreen(
            state = PreviewData.noResultsState,
            onSearchChange = {},
            onToggleCategory = {},
            onToggleStatus = {},
            onSortChange = {},
            onClearFilters = {},
            onAddTask = { _, _, _ -> },
            onEditTask = { _, _, _ -> },
            onToggleStatusOf = {},
            onSetStatus = { _, _ -> },
            onTogglePin = {},
            onDelete = {},
            onSetTaskReminder = { _, _ -> },
            onAddTaskList = { _, _, _ -> },
            previewCategory = { com.umair.smarttodo.domain.Category.OTHER },
            onToggleListItem = { _, _ -> },
            onAddListItem = { _, _, _ -> },
            onRemoveListItem = { _, _ -> },
            onSetItemQuantity = { _, _, _ -> },
            onSetAllItemsChecked = { _, _ -> },
            onDeleteList = {},
            onSetListReminder = { _, _ -> },
        )
    }
}

@Preview(name = "Home - live fake repository", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun HomeRouteWithFakeRepositoryPreview() {
    SmartTodoTheme {
        HomeRoute(
            viewModel = HomeViewModel(
                repository = PreviewTaskRepository(),
                taskListRepository = PreviewTaskListRepository(),
                taskCategorizer = PreviewTaskCategorizer(),
            ),
        )
    }
}

@Preview(name = "Header", widthDp = PhoneWidth)
@Composable
private fun HomeHeaderPreview() {
    PreviewSurface {
        HomeHeader(hourOfDay = 8)
        HomeHeader(hourOfDay = 14)
        HomeHeader(hourOfDay = 20)
    }
}

@Preview(name = "Progress card", widthDp = PhoneWidth)
@Composable
private fun ProgressCardPreview() {
    PreviewSurface {
        ProgressCard(percent = 20, doneCount = 1, totalCount = 5)
        ProgressCard(percent = 0, doneCount = 0, totalCount = 0)
        ProgressCard(percent = 100, doneCount = 4, totalCount = 4)
    }
}

@Preview(name = "Status tracker", widthDp = PhoneWidth)
@Composable
private fun StatusTrackerRowPreview() {
    PreviewSurface {
        StatusTrackerRow(
            counts = PreviewData.state.statusCounts,
            selectedStatuses = emptySet(),
            onStatusClick = {},
        )
        StatusTrackerRow(
            counts = PreviewData.state.statusCounts,
            selectedStatuses = setOf(com.umair.smarttodo.domain.TaskStatus.TODO),
            onStatusClick = {},
        )
    }
}

@Preview(name = "Search bar", widthDp = PhoneWidth)
@Composable
private fun TaskSearchBarPreview() {
    PreviewSurface {
        TaskSearchBar(query = "", onQueryChange = {})
        TaskSearchBar(query = "report", onQueryChange = {})
    }
}

@Preview(name = "Category chips", widthDp = PhoneWidth)
@Composable
private fun CategoryChipRowPreview() {
    SmartTodoTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppBackground)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryChipRow(
                categories = PreviewData.state.visibleCategories,
                counts = PreviewData.state.categoryCounts,
                selectedCategories = emptySet(),
                onCategoryClick = {},
            )
            CategoryChipRow(
                categories = PreviewData.state.visibleCategories,
                counts = PreviewData.state.categoryCounts,
                selectedCategories = setOf(com.umair.smarttodo.domain.Category.CODING),
                onCategoryClick = {},
            )
        }
    }
}

@Preview(name = "Task cards", widthDp = PhoneWidth)
@Composable
private fun TaskCardPreview() {
    PreviewSurface {
        PreviewData.tasks.take(3).forEach { task ->
            TaskCard(
                task = task,
                onToggleStatus = {},
                onSetStatus = { _, _ -> },
                onTogglePin = {},
                onDelete = {},
                onSetReminder = { _, _ -> },
                onEdit = {},
            )
        }
    }
}

@Preview(name = "Task list cards", widthDp = PhoneWidth)
@Composable
private fun TaskListCardPreview() {
    PreviewSurface {
        PreviewData.taskLists.forEach { taskList ->
            TaskListCard(
                taskList = taskList,
                onOpen = {},
                onDelete = {},
                onSetReminder = { _, _ -> },
                onSetAllItemsChecked = { _, _ -> },
            )
        }
    }
}

@Preview(name = "Empty states", widthDp = PhoneWidth)
@Composable
private fun EmptyStatesPreview() {
    PreviewSurface {
        EmptyTasksState()
        NoResultsState(onClearFilters = {})
    }
}
