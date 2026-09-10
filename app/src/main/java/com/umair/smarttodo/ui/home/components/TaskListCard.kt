package com.umair.smarttodo.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.RemoveDone
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.umair.smarttodo.R
import com.umair.smarttodo.domain.TaskList
import com.umair.smarttodo.ui.home.checkedCount
import com.umair.smarttodo.ui.home.isFullyChecked
import com.umair.smarttodo.ui.theme.StatusDone
import com.umair.smarttodo.ui.theme.TextPrimary

/**
 * Feed card for a TaskList. Same shell as [TaskCard] - dark navy surface, category colour
 * as spine, glow and watermark - so tasks and lists read as one family, with the list
 * specifics layered on: the title, a slim accent-filled progress rail, "x/y done"
 * computed from [TaskList.items] (never stored), and a preview of the first few items.
 *
 * Tapping the card opens the full checklist detail. The overflow menu offers the same
 * "mark all complete" bulk action as the detail sheet, plus share, reminder and delete.
 * A list has no status column in the database: being complete is derived from the items,
 * so [onSetAllItemsChecked] is the only way to make a list complete or bring it back.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskListCard(
    taskList: TaskList,
    onOpen: (TaskList) -> Unit,
    onDelete: (TaskList) -> Unit,
    onSetReminder: (TaskList, Long?) -> Unit,
    onSetAllItemsChecked: (listId: Long, checked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = taskList.category.visual()
    val context = LocalContext.current
    val complete = taskList.isFullyChecked
    val fraction = if (taskList.items.isEmpty()) {
        0f
    } else {
        taskList.checkedCount.toFloat() / taskList.items.size
    }

    CategoryAccentCard(
        accent = visual.accent,
        watermarkEmoji = visual.emoji,
        onClick = { onOpen(taskList) },
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                if (taskList.reminderAt != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CardScrim)
                            .border(width = 1.dp, color = CardHairline, shape = CircleShape)
                            .padding(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = stringResource(R.string.cd_reminder_active),
                            tint = TextPrimary,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                }
                Text(
                    text = taskList.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Box {
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = true }, modifier = Modifier.size(26.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.cd_task_options),
                        tint = CardTextFaint,
                        modifier = Modifier.size(18.dp),
                    )
                }
                TaskListOverflowMenu(
                    expanded = expanded,
                    onDismiss = { expanded = false },
                    taskList = taskList,
                    onDelete = onDelete,
                    onSetReminder = onSetReminder,
                    onSetAllItemsChecked = onSetAllItemsChecked,
                    onShare = { context.shareAsPlainText(taskList.toShareText()) },
                )
            }
        }

        Spacer(Modifier.height(11.dp))

        CardProgressRail(fraction = fraction, accent = visual.accent)

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(
                R.string.list_progress_format,
                taskList.checkedCount,
                taskList.items.size,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = CardTextFaint,
        )

        if (taskList.items.isNotEmpty()) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = taskList.items.take(3).joinToString(separator = ", ") { it.text },
                style = MaterialTheme.typography.bodySmall,
                color = CardTextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            CardPill(
                text = taskList.category.label,
                leading = { Text(text = visual.emoji, style = MaterialTheme.typography.labelMedium) },
            )
            if (complete) {
                // A list has no stored status, so this pill is the only place its derived
                // completeness shows up on the card itself.
                CardPill(
                    text = stringResource(R.string.list_complete),
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(StatusDone),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun TaskListOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    taskList: TaskList,
    onDelete: (TaskList) -> Unit,
    onSetReminder: (TaskList, Long?) -> Unit,
    onSetAllItemsChecked: (listId: Long, checked: Boolean) -> Unit,
    onShare: () -> Unit,
) {
    var showReminderDialog by remember { mutableStateOf(false) }
    val permissionState = rememberReminderPermissionState()

    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        // Only offered for a list that has items: bulk-checking an empty list is a no-op
        // by contract, and an empty list is never "complete" under the derived rule, so
        // the item would be a dead button.
        if (taskList.items.isNotEmpty()) {
            MarkAllItemsMenuItem(
                complete = taskList.isFullyChecked,
                onClick = {
                    onDismiss()
                    onSetAllItemsChecked(taskList.id, !taskList.isFullyChecked)
                },
            )
        }
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(
                        if (taskList.reminderAt != null) {
                            R.string.action_change_reminder
                        } else {
                            R.string.action_set_reminder
                        },
                    ),
                )
            },
            onClick = {
                onDismiss()
                permissionState.requestIfNeeded()
                showReminderDialog = true
            },
            leadingIcon = {
                Icon(
                    imageVector = if (taskList.reminderAt != null) {
                        Icons.Rounded.NotificationsActive
                    } else {
                        Icons.Rounded.NotificationsNone
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
        if (taskList.reminderAt != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_clear_reminder)) },
                onClick = {
                    onDismiss()
                    onSetReminder(taskList, null)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_share)) },
            onClick = {
                onDismiss()
                onShare()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_delete)) },
            onClick = {
                onDismiss()
                onDelete(taskList)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
    }

    ReminderDialogFlow(
        show = showReminderDialog,
        initialMillis = taskList.reminderAt,
        onDismiss = { showReminderDialog = false },
        onPicked = { millis ->
            onSetReminder(taskList, millis)
            showReminderDialog = false
        },
    )
}

/**
 * The bulk check/uncheck action as an overflow-menu item. The detail sheet offers the same
 * action as a full-width button instead, since that is where a user goes looking for it;
 * this is the quick route from the feed, without opening the list at all.
 *
 * When the list is already fully checked the control flips to its inverse rather than
 * sitting there as a no-op: tapping "Mark all complete" on an already-complete list would
 * do nothing visible, which reads as a broken button. [complete] is the current state of
 * the list, and the action always sets the opposite.
 */
@Composable
private fun MarkAllItemsMenuItem(
    complete: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                stringResource(
                    if (complete) {
                        R.string.action_mark_all_incomplete
                    } else {
                        R.string.action_mark_all_complete
                    },
                ),
            )
        },
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = if (complete) Icons.Rounded.RemoveDone else Icons.Rounded.DoneAll,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}
