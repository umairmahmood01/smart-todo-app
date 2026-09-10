package com.umair.smarttodo.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umair.smarttodo.R
import com.umair.smarttodo.domain.TaskList
import com.umair.smarttodo.ui.home.checkedCount
import com.umair.smarttodo.ui.theme.SmartTodoDimens

/**
 * Feed card for a TaskList: same card language as TaskCard (category gradient, rounded
 * corners) but shows the list title, "x/y done" progress computed from TaskList.items
 * (never stored), and a preview of the first few item texts. Tapping the card opens
 * the full checklist detail; the overflow menu offers share, reminder and delete.
 */
@Composable
fun TaskListCard(
    taskList: TaskList,
    onOpen: (TaskList) -> Unit,
    onDelete: (TaskList) -> Unit,
    onSetReminder: (TaskList, Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = taskList.category.visual()
    val onCard = visual.contentColor
    val onCardScrim = onCard.copy(alpha = 0.20f)
    val onCardBorder = onCard.copy(alpha = 0.18f)
    val onCardMuted = onCard.copy(alpha = 0.76f)
    val shape = RoundedCornerShape(SmartTodoDimens.TaskCardRadius)
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(visual.brush)
            .clickable { onOpen(taskList) }
            .drawWithCache {
                val brush = Brush.radialGradient(
                    colors = listOf(onCard.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width - 40.dp.toPx(), -20.dp.toPx()),
                    radius = 110.dp.toPx(),
                )
                onDrawBehind { drawRect(brush) }
            },
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = SmartTodoDimens.TaskCardPaddingHorizontal,
                vertical = SmartTodoDimens.TaskCardPaddingVertical,
            ),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    if (taskList.reminderAt != null) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(onCardScrim)
                                .border(width = 1.dp, color = onCardBorder, shape = CircleShape)
                                .padding(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = stringResource(R.string.cd_reminder_active),
                                tint = onCard,
                                modifier = Modifier.size(11.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        text = taskList.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = onCard,
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
                            tint = onCardMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    TaskListOverflowMenu(
                        expanded = expanded,
                        onDismiss = { expanded = false },
                        taskList = taskList,
                        onDelete = onDelete,
                        onSetReminder = onSetReminder,
                        onShare = { context.shareAsPlainText(taskList.toShareText()) },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(SmartTodoDimens.TaskTileSize)
                        .clip(RoundedCornerShape(12.dp))
                        .background(onCardScrim)
                        .border(width = 1.dp, color = onCardBorder, shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = visual.emoji, fontSize = 18.sp)
                }
                Spacer(Modifier.width(11.dp))
                Column {
                    Text(
                        text = stringResource(
                            R.string.list_progress_format,
                            taskList.checkedCount,
                            taskList.items.size,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = onCard,
                    )
                    if (taskList.items.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = taskList.items.take(3).joinToString(separator = ", ") { it.text },
                            style = MaterialTheme.typography.bodySmall,
                            color = onCardMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = visual.emoji + "  " + taskList.category.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = onCard,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(onCardScrim)
                        .border(width = 1.dp, color = onCardBorder, shape = CircleShape)
                        .padding(horizontal = 11.dp, vertical = 5.dp),
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
    onShare: () -> Unit,
) {
    var showReminderDialog by remember { mutableStateOf(false) }
    val permissionState = rememberReminderPermissionState()

    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
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
