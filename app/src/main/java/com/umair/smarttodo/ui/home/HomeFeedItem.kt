package com.umair.smarttodo.ui.home

import androidx.compose.runtime.Immutable
import com.umair.smarttodo.domain.Task
import com.umair.smarttodo.domain.TaskList

/**
 * UI-layer wrapper so a single feed can hold either a [Task] or a [TaskList] card. Purely
 * a presentation concern: the domain stays task/list-specific, and "done" for a list is
 * always computed from [TaskList.items], never stored.
 */
@Immutable
sealed interface HomeFeedItem {

    /** Stable identity for `LazyColumn` keys, unique across both kinds. */
    val feedKey: String

    val createdDate: Long

    @Immutable
    data class TaskEntry(val task: Task) : HomeFeedItem {
        override val feedKey: String get() = "task-${task.id}"
        override val createdDate: Long get() = task.createdDate
    }

    @Immutable
    data class ListEntry(val taskList: TaskList) : HomeFeedItem {
        override val feedKey: String get() = "list-${taskList.id}"
        override val createdDate: Long get() = taskList.createdDate
    }
}

/** Items checked out of total, e.g. "2/5 done" — computed, never stored. */
val TaskList.checkedCount: Int get() = items.count { it.isChecked }

/** True once every item is checked (and the list is non-empty). */
val TaskList.isFullyChecked: Boolean get() = items.isNotEmpty() && items.all { it.isChecked }
