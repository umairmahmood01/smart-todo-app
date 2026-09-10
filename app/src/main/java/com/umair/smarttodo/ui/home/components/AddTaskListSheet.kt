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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umair.smarttodo.R
import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.ui.theme.HairlineBorder
import com.umair.smarttodo.ui.theme.NeonPurple
import com.umair.smarttodo.ui.theme.SmartTodoDimens
import com.umair.smarttodo.ui.theme.SurfaceElevated
import com.umair.smarttodo.ui.theme.SurfaceMuted
import com.umair.smarttodo.ui.theme.TextMuted
import com.umair.smarttodo.ui.theme.TextOnAccent
import com.umair.smarttodo.ui.theme.TextPlaceholder
import com.umair.smarttodo.ui.theme.TextPrimary
import kotlinx.coroutines.delay

private const val FocusDelayMillis = 180L

/**
 * One growable-list row: a stable local id so text-field state survives reordering.
 * [quantity] is only ever shown/editable when the in-progress list's [previewCategory]
 * is [Category.SHOPPING]; it is otherwise carried along blank and dropped on save.
 */
private data class DraftItemRow(val id: Long, val text: String, val quantity: String = "")

/**
 * Title, a dynamically growing list of plain item rows (add/remove), and an optional
 * reminder. Category is not picked here: the ViewModel categorizes the title the same
 * way [AddTaskSheet] categorizes a plain task's text — [previewCategory] is that same
 * categorizer, exposed read-only so this sheet can decide, live as the user types the
 * title, whether to show shopping-list affordances (quantity fields, grocery emoji)
 * before the list even exists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskListSheet(
    onDismiss: () -> Unit,
    onSave: (title: String, items: List<Pair<String, String?>>, reminderAt: Long?) -> Unit,
    previewCategory: (String) -> Category,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var reminderAt by remember { mutableStateOf<Long?>(null) }
    var nextRowId by remember { mutableStateOf(1L) }
    val rows = remember { mutableStateListOf(DraftItemRow(id = 0L, text = "")) }
    val titleFocusRequester = remember { FocusRequester() }
    val isShoppingList = previewCategory(title) == Category.SHOPPING

    LaunchedEffect(Unit) {
        delay(FocusDelayMillis)
        titleFocusRequester.requestFocus()
    }

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
            Text(
                text = stringResource(R.string.add_list_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(14.dp))
            DraftTitleField(
                value = title,
                onValueChange = { title = it },
                focusRequester = titleFocusRequester,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.add_list_items_label),
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted,
            )
            Spacer(Modifier.height(8.dp))
            rows.forEachIndexed { index, row ->
                DraftItemField(
                    value = row.text,
                    onValueChange = { newText ->
                        rows[index] = row.copy(text = newText)
                    },
                    quantity = row.quantity,
                    onQuantityChange = { newQuantity ->
                        rows[index] = row.copy(quantity = newQuantity)
                    },
                    showQuantity = isShoppingList,
                    showGroceryIcon = isShoppingList,
                    onRemove = if (rows.size > 1) {
                        { rows.removeAt(index) }
                    } else {
                        null
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
            AddItemRowButton(
                onClick = {
                    rows.add(DraftItemRow(id = nextRowId, text = ""))
                    nextRowId += 1
                },
            )
            Spacer(Modifier.height(14.dp))
            ReminderRow(
                reminderAt = reminderAt,
                onReminderChange = { reminderAt = it },
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.action_cancel), color = TextMuted)
                }
                Spacer(Modifier.padding(horizontal = 4.dp))
                Button(
                    onClick = {
                        val items = rows
                            .filter { it.text.isNotBlank() }
                            .map { it.text to it.quantity.trim().ifBlank { null } }
                        if (title.isBlank()) return@Button
                        onSave(title, items, reminderAt)
                        onDismiss()
                    },
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(SmartTodoDimens.SurfaceRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPurple,
                        contentColor = TextOnAccent,
                    ),
                ) {
                    Text(text = stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun DraftTitleField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(SmartTodoDimens.SurfaceRadius))
            .background(SurfaceMuted)
            .border(
                width = 1.dp,
                color = HairlineBorder,
                shape = RoundedCornerShape(SmartTodoDimens.SurfaceRadius),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = stringResource(R.string.add_list_title_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = TextPlaceholder,
                modifier = Modifier.padding(end = MicGutter),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            cursorBrush = SolidColor(NeonPurple),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = MicGutter)
                .focusRequester(focusRequester),
        )
        // Speaking the title matters more than it looks: the title is what gets
        // categorized, so a dictated "grocery list" still lands in Shopping and still
        // switches this sheet into its quantity-showing shopping mode as it is spoken.
        VoiceInputButton(
            onTextRecognized = { spoken -> onValueChange(appendSpokenText(value, spoken)) },
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

/** Room reserved at the trailing edge of a text field for the microphone button. */
private val MicGutter = 40.dp

@Composable
private fun DraftItemField(
    value: String,
    onValueChange: (String) -> Unit,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    showQuantity: Boolean,
    showGroceryIcon: Boolean,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showGroceryIcon) {
            Text(
                text = groceryItemEmoji(value),
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
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
                keyboardActions = KeyboardActions(onDone = {}),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        VoiceInputButton(
            onTextRecognized = { spoken -> onValueChange(appendSpokenText(value, spoken)) },
            buttonSize = 34.dp,
            iconSize = 17.dp,
        )
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
                    keyboardActions = KeyboardActions(onDone = {}),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (onRemove != null) {
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.action_remove_item),
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AddItemRowButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SmartTodoDimens.SurfaceRadius))
            .border(
                width = 1.dp,
                color = HairlineBorder,
                shape = RoundedCornerShape(SmartTodoDimens.SurfaceRadius),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = NeonPurple,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.action_add_item),
            style = MaterialTheme.typography.labelLarge,
            color = NeonPurple,
        )
    }
}

