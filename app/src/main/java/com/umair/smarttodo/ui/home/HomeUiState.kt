package com.umair.smarttodo.ui.home

import androidx.compose.runtime.Immutable
import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.Task
import com.umair.smarttodo.domain.TaskSort
import com.umair.smarttodo.domain.TaskStatus

/**
 * Everything the home screen needs to render, and nothing else.
 *
 * [tasks] is the fully filtered + sorted list. [statusCounts] / [categoryCounts] /
 * [completionPercent] are deliberately scoped to the *search query only* (they ignore
 * the category and status chips) so that the tracker pills and category chips keep
 * showing meaningful totals while the user is filtering by them.
 */
@Immutable
data class HomeUiState(
    val tasks: List<Task> = emptyList(),
    val searchQuery: String = "",
    val selectedCategories: Set<Category> = emptySet(),
    val selectedStatuses: Set<TaskStatus> = emptySet(),
    val sort: TaskSort = TaskSort.CREATED_DESC,
    val statusCounts: Map<TaskStatus, Int> = emptyMap(),
    val categoryCounts: Map<Category, Int> = emptyMap(),
    val completionPercent: Int = 0,
    val isLoading: Boolean = true,
) {
    /** Total number of tasks in scope (all statuses). */
    val totalCount: Int = statusCounts.values.sum()

    /** Number of completed tasks in scope. */
    val doneCount: Int = statusCounts[TaskStatus.DONE] ?: 0

    /** True when the user has narrowed the list in any way. */
    val hasActiveFilters: Boolean =
        searchQuery.isNotBlank() || selectedCategories.isNotEmpty() || selectedStatuses.isNotEmpty()

    /** True when there is genuinely nothing to show (and we are not still loading). */
    val isEmpty: Boolean = !isLoading && tasks.isEmpty()

    /** True when the list is empty only because of the current filters. */
    val isFilteredEmpty: Boolean = isEmpty && hasActiveFilters

    /**
     * Categories worth rendering as chips: everything that currently has tasks, plus
     * anything the user has selected (so a selected chip never disappears mid-filter).
     */
    val visibleCategories: List<Category> = Category.entries.filter {
        (categoryCounts[it] ?: 0) > 0 || it in selectedCategories
    }

    fun countOf(status: TaskStatus): Int = statusCounts[status] ?: 0

    fun countOf(category: Category): Int = categoryCounts[category] ?: 0

    fun isSelected(status: TaskStatus): Boolean = status in selectedStatuses

    fun isSelected(category: Category): Boolean = category in selectedCategories
}
