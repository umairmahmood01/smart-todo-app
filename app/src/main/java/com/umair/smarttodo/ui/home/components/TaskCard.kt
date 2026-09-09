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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umair.smarttodo.R
import com.umair.smarttodo.domain.Task
import com.umair.smarttodo.domain.TaskStatus
import com.umair.smarttodo.ui.theme.OnCardBorder
import com.umair.smarttodo.ui.theme.OnCardScrim
import com.umair.smarttodo.ui.theme.OnCardTextMuted
import com.umair.smarttodo.ui.theme.SmartTodoDimens
import com.umair.smarttodo.ui.theme.TaskCardBrush
import com.umair.smarttodo.ui.theme.TextOnAccent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DatePattern = "dd.MM.yyyy"
private const val DateTimePattern = "dd.MM.yyyy HH:mm"

/**
 * A single neon task card. Every interaction is delegated upwards; the only state
 * held here is whether the overflow menu is open, which is pure UI state.
 */
@Composable
fun TaskCard(
    task: Task,
    onToggleStatus: (Task) -> Unit,
    onSetStatus: (Task, TaskStatus) -> Unit,
    onTogglePin: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = task.category.visual()
    val shape = RoundedCornerShape(SmartTodoDimens.TaskCardRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TaskCardBrush)
            .drawWithCache {
                // Soft white bloom in the top-right corner of the card.
                val brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width - 40.dp.toPx(), -20.dp.toPx()),
                    radius = 110.dp.toPx(),
                )
                onDrawBehind { drawRect(brush) }
            },
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    if (task.isPinned) {
                        PinnedBadge()
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        text = task.rawText,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextOnAccent,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = rememberFormattedDate(task.createdDate),
                    style = MaterialTheme.typography.labelMedium,
                    color = OnCardTextMuted,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 3.dp),
                )
                TaskOverflowMenu(
                    task = task,
                    onSetStatus = onSetStatus,
                    onTogglePin = onTogglePin,
                    onDelete = onDelete,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Top) {
                CategoryTile(emoji = visual.emoji)
                Spacer(Modifier.width(12.dp))
                Column {
                    // Filled in later by the LLM normalization layer, and absent for
                    // English input, so it has to degrade gracefully.
                    val normalized = task.normalizedEnglishText
                    if (!normalized.isNullOrBlank() && normalized != task.rawText) {
                        Text(
                            text = normalized,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextOnAccent.copy(alpha = 0.94f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(7.dp))
                    }
                    TaskMetaLine(task = task)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskTag(text = visual.emoji + "  " + task.category.label)
                StatusTag(status = task.status, onClick = { onToggleStatus(task) })
            }
        }
    }
}

@Composable
private fun PinnedBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.task_pinned),
        style = MaterialTheme.typography.labelSmall,
        color = TextOnAccent,
        modifier = modifier
            .clip(CircleShape)
            .background(OnCardScrim)
            .border(width = 1.dp, color = OnCardBorder, shape = CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun CategoryTile(emoji: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(SmartTodoDimens.TaskTileSize)
            .clip(RoundedCornerShape(14.dp))
            .background(OnCardScrim)
            .border(width = 1.dp, color = OnCardBorder, shape = RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = 22.sp)
    }
}

@Composable
private fun TaskMetaLine(task: Task, modifier: Modifier = Modifier) {
    val dueDate = task.dueDate
    val text = if (dueDate != null) {
        stringResource(R.string.task_due, rememberFormattedDateTime(dueDate))
    } else {
        stringResource(R.string.task_added, rememberFormattedDate(task.createdDate))
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            tint = TextOnAccent.copy(alpha = 0.85f),
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = TextOnAccent.copy(alpha = 0.82f),
        )
    }
}

@Composable
private fun TaskTag(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TextOnAccent,
        modifier = modifier
            .clip(CircleShape)
            .background(OnCardScrim)
            .border(width = 1.dp, color = OnCardBorder, shape = CircleShape)
            .padding(horizontal = 11.dp, vertical = 5.dp),
    )
}

@Composable
private fun StatusTag(status: TaskStatus, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(OnCardScrim)
            .border(width = 1.dp, color = OnCardBorder, shape = CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(status.color()),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(status.labelRes()),
            style = MaterialTheme.typography.labelMedium,
            color = TextOnAccent,
        )
    }
}

@Composable
private fun TaskOverflowMenu(
    task: Task,
    onSetStatus: (Task, TaskStatus) -> Unit,
    onTogglePin: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.cd_task_options),
                tint = TextOnAccent.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TaskStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                R.string.action_mark_as,
                                stringResource(status.labelRes()),
                            ),
                        )
                    },
                    onClick = {
                        expanded = false
                        onSetStatus(task, status)
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(status.color()),
                        )
                    },
                    trailingIcon = {
                        if (task.status == status) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    },
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (task.isPinned) R.string.action_unpin else R.string.action_pin,
                        ),
                    )
                },
                onClick = {
                    expanded = false
                    onTogglePin(task)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete)) },
                onClick = {
                    expanded = false
                    onDelete(task)
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
    }
}

@Composable
private fun rememberFormattedDate(epochMillis: Long): String = remember(epochMillis) {
    formatEpoch(epochMillis, DatePattern)
}

@Composable
private fun rememberFormattedDateTime(epochMillis: Long): String = remember(epochMillis) {
    formatEpoch(epochMillis, DateTimePattern)
}

private fun formatEpoch(epochMillis: Long, pattern: String): String =
    DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
