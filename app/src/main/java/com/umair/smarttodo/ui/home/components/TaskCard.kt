package com.umair.smarttodo.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.umair.smarttodo.R
import com.umair.smarttodo.domain.Task
import com.umair.smarttodo.domain.TaskStatus
import com.umair.smarttodo.ui.theme.TextPrimary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DatePattern = "dd.MM.yyyy"
private const val DateTimePattern = "dd.MM.yyyy HH:mm"

/**
 * A single task card, built on the shared [CategoryAccentCard] shell so tasks and task
 * lists read as one family. Every interaction is delegated upwards; the only state held
 * here is whether the overflow menu or the reminder dialog is open, which is pure UI
 * state. All card text is white-on-dark, so there is no per-category content colour to
 * thread through any more.
 *
 * WHICH TEXT IS THE HEADING. The heading is [Task.normalizedEnglishText] whenever it is
 * non-blank, falling back to [Task.rawText] otherwise - not yet enriched, offline, or
 * enrichment not configured all have to degrade to something, and an empty heading is
 * never acceptable. What the user actually typed then appears underneath as a quiet
 * secondary line, and only when it differs from the heading, so an English-only task does
 * not print itself twice.
 *
 * This is a display swap and nothing more. [Task.rawText] is never copied into
 * [Task.details] - `details` belongs to the user, and the app neither populates nor
 * overwrites it. Editing still edits `rawText` (see [AddTaskSheet]), because the English
 * rendering is derived from it and regenerates on save.
 *
 * Tapping anywhere on the card body (outside the status pill, the details pill and the
 * overflow menu, all of which own their own clickables and consume the tap first) calls
 * [onEdit] to open the same sheet used for adding a task, pre-filled and in edit mode.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCard(
    task: Task,
    onToggleStatus: (Task) -> Unit,
    onSetStatus: (Task, TaskStatus) -> Unit,
    onTogglePin: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onSetReminder: (Task, Long?) -> Unit,
    onEdit: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = task.category.visual()
    val context = LocalContext.current
    val heading = task.headingText()
    val original = task.originalTextOrNull()

    CategoryAccentCard(
        accent = visual.accent,
        watermarkEmoji = visual.emoji,
        onClick = { onEdit(task) },
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.isPinned) {
                        PinnedBadge()
                    }
                    if (task.reminderAt != null) {
                        if (task.isPinned) Spacer(Modifier.width(6.dp))
                        ReminderBadge()
                    }
                }
                if (task.isPinned || task.reminderAt != null) {
                    Spacer(Modifier.height(9.dp))
                }
                Text(
                    text = heading,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (original != null) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = stringResource(R.string.task_original_format, original),
                        style = MaterialTheme.typography.bodySmall,
                        color = CardTextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = rememberFormattedDate(task.createdDate),
                style = MaterialTheme.typography.labelMedium,
                color = CardTextFaint,
                maxLines = 1,
                modifier = Modifier.padding(top = 3.dp),
            )
            TaskOverflowMenu(
                task = task,
                onSetStatus = onSetStatus,
                onTogglePin = onTogglePin,
                onDelete = onDelete,
                onSetReminder = onSetReminder,
                onEdit = onEdit,
                onShare = { context.shareAsPlainText(task.toShareText()) },
            )
        }

        Spacer(Modifier.height(12.dp))

        TaskMetaLine(task = task)

        Spacer(Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            CardPill(
                text = task.category.label,
                leading = { Text(text = visual.emoji, style = MaterialTheme.typography.labelMedium) },
            )
            CardPill(
                text = stringResource(task.status.labelRes()),
                onClick = { onToggleStatus(task) },
                leading = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(task.status.color()),
                    )
                },
            )
            if (!task.details.isNullOrBlank()) {
                // Feature: a card with notes used to advertise them with nothing but a
                // bare glyph, which nobody read as "there is more here, tap it". A short
                // labelled pill says so outright, and still costs one line.
                CardPill(
                    text = stringResource(R.string.action_read_more),
                    onClick = { onEdit(task) },
                    leading = {
                        Icon(
                            imageVector = Icons.Rounded.Notes,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(13.dp),
                        )
                    },
                )
            }
        }
    }
}

/**
 * The card heading: the English rendering when there is one, otherwise exactly what the
 * user typed. Blank is impossible - [Task.rawText] is never blank by repository contract.
 */
internal fun Task.headingText(): String =
    normalizedEnglishText?.takeIf { it.isNotBlank() } ?: rawText

/**
 * The user's own words, or null when showing them would just repeat the heading (English
 * input, or no enrichment yet).
 */
internal fun Task.originalTextOrNull(): String? =
    rawText.takeIf { it.isNotBlank() && it != headingText() }

@Composable
private fun PinnedBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.task_pinned),
        style = MaterialTheme.typography.labelSmall,
        color = TextPrimary,
        modifier = modifier
            .clip(CircleShape)
            .background(CardScrim)
            .border(width = 1.dp, color = CardHairline, shape = CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun ReminderBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(CardScrim)
            .border(width = 1.dp, color = CardHairline, shape = CircleShape)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.NotificationsActive,
            contentDescription = stringResource(R.string.cd_reminder_active),
            tint = TextPrimary,
            modifier = Modifier.size(11.dp),
        )
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
            tint = CardTextFaint,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = CardTextFaint,
        )
    }
}

@Composable
private fun TaskOverflowMenu(
    task: Task,
    onSetStatus: (Task, TaskStatus) -> Unit,
    onTogglePin: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onSetReminder: (Task, Long?) -> Unit,
    onEdit: (Task) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = CardTextFaint,
) {
    var expanded by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    val permissionState = rememberReminderPermissionState()

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(26.dp)) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.cd_task_options),
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_edit)) },
                onClick = {
                    expanded = false
                    onEdit(task)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
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
                text = {
                    Text(
                        stringResource(
                            if (task.reminderAt != null) {
                                R.string.action_change_reminder
                            } else {
                                R.string.action_set_reminder
                            },
                        ),
                    )
                },
                onClick = {
                    expanded = false
                    permissionState.requestIfNeeded()
                    showReminderDialog = true
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (task.reminderAt != null) {
                            Icons.Rounded.NotificationsActive
                        } else {
                            Icons.Rounded.NotificationsNone
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            if (task.reminderAt != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_clear_reminder)) },
                    onClick = {
                        expanded = false
                        onSetReminder(task, null)
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
                    expanded = false
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

    ReminderDialogFlow(
        show = showReminderDialog,
        initialMillis = task.reminderAt,
        onDismiss = { showReminderDialog = false },
        onPicked = { millis ->
            onSetReminder(task, millis)
            showReminderDialog = false
        },
    )
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
