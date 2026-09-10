package com.umair.smarttodo.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.umair.smarttodo.R
import com.umair.smarttodo.domain.TaskList
import com.umair.smarttodo.domain.TaskListItem
import com.umair.smarttodo.ui.home.checkedCount
import com.umair.smarttodo.ui.theme.HairlineBorder
import com.umair.smarttodo.ui.theme.NeonPurple
import com.umair.smarttodo.ui.theme.SmartTodoDimens
import com.umair.smarttodo.ui.theme.SurfaceElevated
import com.umair.smarttodo.ui.theme.SurfaceMuted
import com.umair.smarttodo.ui.theme.TextMuted
import com.umair.smarttodo.ui.theme.TextPlaceholder
import com.umair.smarttodo.ui.theme.TextPrimary

/**
 * Full checklist view for one TaskList: every item with its own checkbox (toggling
 * calls setItemChecked through onToggleItem), an add-item row, and a delete button per
 * item. The overflow menu offers share, reminder and delete for the whole list, mirroring
 * TaskListCard's card-level menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListDetailSheet(
    taskList: TaskList,
    onDismiss: () -> Unit,
    onToggleItem: (TaskListItem) -> Unit,
    onAddItem: (String) -> Unit,
    onRemoveItem: (TaskListItem) -> Unit,
    onSetReminder: (Long?) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newItemText by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    val permissionState = rememberReminderPermissionState()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceElevated,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SmartTodoDimens.ScreenPadding)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = taskList.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stringResource(
                            R.string.list_progress_format,
                            taskList.checkedCount,
                            taskList.items.size,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMuted,
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.cd_task_options),
                            tint = TextMuted,
                        )
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
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
                                menuExpanded = false
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
                                )
                            },
                        )
                        if (taskList.reminderAt != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_clear_reminder)) },
                                onClick = {
                                    menuExpanded = false
                                    onSetReminder(null)
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Rounded.NotificationsNone, contentDescription = null)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share)) },
                            onClick = {
                                menuExpanded = false
                                context.shareAsPlainText(taskList.toShareText())
                            },
                            leadingIcon = { Icon(imageVector = Icons.Rounded.Share, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                                onDismiss()
                            },
                            leadingIcon = { Icon(imageVector = Icons.Rounded.Delete, contentDescription = null) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            taskList.items.forEach { item ->
                ChecklistItemRow(
                    item = item,
                    onToggle = { onToggleItem(item) },
                    onRemove = { onRemoveItem(item) },
                )
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(8.dp))

            AddChecklistItemRow(
                value = newItemText,
                onValueChange = { newItemText = it },
                onSubmit = {
                    if (newItemText.isNotBlank()) {
                        onAddItem(newItemText)
                        newItemText = ""
                    }
                },
            )
        }
    }

    ReminderDialogFlow(
        show = showReminderDialog,
        initialMillis = taskList.reminderAt,
        onDismiss = { showReminderDialog = false },
        onPicked = { millis ->
            onSetReminder(millis)
            showReminderDialog = false
        },
    )
}

@Composable
private fun ChecklistItemRow(
    item: TaskListItem,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SmartTodoDimens.SurfaceRadius))
            .background(SurfaceMuted)
            .border(
                width = 1.dp,
                color = HairlineBorder,
                shape = RoundedCornerShape(SmartTodoDimens.SurfaceRadius),
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = NeonPurple),
        )
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.isChecked) TextMuted else TextPrimary,
            textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.action_remove_item),
                tint = TextMuted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun AddChecklistItemRow(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(SmartTodoDimens.SurfaceRadius))
                .background(SurfaceMuted)
                .border(
                    width = 1.dp,
                    color = HairlineBorder,
                    shape = RoundedCornerShape(SmartTodoDimens.SurfaceRadius),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.add_list_item_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPlaceholder,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                cursorBrush = SolidColor(NeonPurple),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = onSubmit, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.action_add_item),
                tint = NeonPurple,
            )
        }
    }
}
