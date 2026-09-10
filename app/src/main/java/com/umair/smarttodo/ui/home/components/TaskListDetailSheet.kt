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
import androidx.compose.ui.unit.sp
import com.umair.smarttodo.R
import com.umair.smarttodo.domain.Category
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
 *
 * When [TaskList.category] is [Category.SHOPPING], each item also shows a small grocery
 * emoji (looked up from its text, purely presentational — see [groceryItemEmoji]) and its
 * quantity, if any, tappable to edit inline via [onSetItemQuantity]. Non-shopping lists
 * show neither — quantity would be meaningless clutter on e.g. a packing list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListDetailSheet(
    taskList: TaskList,
    onDismiss: () -> Unit,
    onToggleItem: (TaskListItem) -> Unit,
    onAddItem: (text: String, quantity: String?) -> Unit,
    onRemoveItem: (TaskListItem) -> Unit,
    onSetItemQuantity: (TaskListItem, String?) -> Unit,
    onSetReminder: (Long?) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isShoppingList = taskList.category == Category.SHOPPING
    var newItemText by remember { mutableStateOf("") }
    var newItemQuantity by remember { mutableStateOf("") }
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
                    showGroceryIcon = isShoppingList,
                    showQuantity = isShoppingList,
                    onToggle = { onToggleItem(item) },
                    onRemove = { onRemoveItem(item) },
                    onQuantityChange = { onSetItemQuantity(item, it) },
                )
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(8.dp))

            AddChecklistItemRow(
                value = newItemText,
                onValueChange = { newItemText = it },
                quantity = newItemQuantity,
                onQuantityChange = { newItemQuantity = it },
                showQuantity = isShoppingList,
                onSubmit = {
                    if (newItemText.isNotBlank()) {
                        onAddItem(newItemText, newItemQuantity.trim().ifBlank { null })
                        newItemText = ""
                        newItemQuantity = ""
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
    showGroceryIcon: Boolean,
    showQuantity: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    onQuantityChange: (String?) -> Unit,
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
        if (showGroceryIcon) {
            Text(
                text = groceryItemEmoji(item.text),
                fontSize = 15.sp,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.isChecked) TextMuted else TextPrimary,
            textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        if (showQuantity) {
            QuantityEditor(
                quantity = item.quantity,
                onQuantityChange = onQuantityChange,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
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

/**
 * Inline "milk — 2 L" quantity display, tap-to-edit: tapping the current value (or the
 * "+ qty" placeholder when there is none) swaps in a small text field; committing on
 * Done calls [onQuantityChange] with the trimmed value, or `null` when left blank.
 */
@Composable
private fun QuantityEditor(
    quantity: String?,
    onQuantityChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(quantity) { mutableStateOf(quantity.orEmpty()) }

    if (editing) {
        Box(
            modifier = modifier
                .width(56.dp)
                .clip(RoundedCornerShape(SmartTodoDimens.SurfaceRadius))
                .background(SurfaceElevated)
                .border(
                    width = 1.dp,
                    color = HairlineBorder,
                    shape = RoundedCornerShape(SmartTodoDimens.SurfaceRadius),
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                textStyle = MaterialTheme.typography.labelMedium.copy(color = TextPrimary),
                cursorBrush = SolidColor(NeonPurple),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onQuantityChange(draft.trim().ifBlank { null })
                    editing = false
                }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Text(
            text = quantity?.let { stringResource(R.string.list_item_quantity_format, it) }
                ?: stringResource(R.string.action_add_quantity),
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            modifier = modifier
                .clip(RoundedCornerShape(SmartTodoDimens.SurfaceRadius))
                .clickable {
                    draft = quantity.orEmpty()
                    editing = true
                }
                .padding(horizontal = 6.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun AddChecklistItemRow(
    value: String,
    onValueChange: (String) -> Unit,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    showQuantity: Boolean,
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
        if (showQuantity) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(SmartTodoDimens.SurfaceRadius))
                    .background(SurfaceMuted)
                    .border(
                        width = 1.dp,
                        color = HairlineBorder,
                        shape = RoundedCornerShape(SmartTodoDimens.SurfaceRadius),
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp),
            ) {
                if (quantity.isEmpty()) {
                    Text(
                        text = stringResource(R.string.add_list_item_quantity_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPlaceholder,
                    )
                }
                BasicTextField(
                    value = quantity,
                    onValueChange = onQuantityChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    cursorBrush = SolidColor(NeonPurple),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
