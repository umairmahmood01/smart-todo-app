package com.umair.smarttodo.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.umair.smarttodo.R
import com.umair.smarttodo.domain.Task
import kotlinx.coroutines.delay
import com.umair.smarttodo.ui.theme.HairlineBorder
import com.umair.smarttodo.ui.theme.NeonPurple
import com.umair.smarttodo.ui.theme.SmartTodoDimens
import com.umair.smarttodo.ui.theme.SurfaceElevated
import com.umair.smarttodo.ui.theme.SurfaceMuted
import com.umair.smarttodo.ui.theme.TextMuted
import com.umair.smarttodo.ui.theme.TextOnAccent
import com.umair.smarttodo.ui.theme.TextPlaceholder
import com.umair.smarttodo.ui.theme.TextPrimary

private const val FocusDelayMillis = 180L

/**
 * Raw text and optional details in, plus an optional reminder instant: no category
 * picker, because categorization is automatic (re-run server-side on every save,
 * including edits).
 *
 * Doubles as both the "add task" and "edit task" surface: pass [task] as `null` for
 * create mode (empty fields, title "New task", no reminder editing here — the reminder
 * is set immediately after creation) or a non-null [Task] for edit mode (fields
 * pre-filled from [task], title "Edit task"). Edit mode has no reminder row: reminders on
 * an existing task are already editable from its card's overflow menu, and the "Save"
 * action here only ever calls back with `(rawText, details, reminderAt = null)` — callers
 * should ignore the third value in edit mode and route to their own edit function instead
 * of their add function.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    onDismiss: () -> Unit,
    onSave: (rawText: String, details: String?, reminderAt: Long?) -> Unit,
    modifier: Modifier = Modifier,
    task: Task? = null,
) {
    val isEditMode = task != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf(task?.rawText.orEmpty()) }
    var details by remember { mutableStateOf(task?.details.orEmpty()) }
    var reminderAt by remember { mutableStateOf<Long?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // Let the sheet finish animating in before grabbing focus, otherwise the
        // FocusRequester is not attached yet.
        delay(FocusDelayMillis)
        focusRequester.requestFocus()
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
                text = stringResource(
                    if (isEditMode) R.string.edit_task_title else R.string.add_task_title,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(14.dp))
            // Labelled explicitly because the card now shows the English rendering as its
            // heading: without this it would not be obvious that the field edits the
            // user's own words rather than the machine translation.
            Text(
                text = stringResource(R.string.add_task_field_label),
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted,
            )
            Spacer(Modifier.height(6.dp))
            AddTaskField(
                value = text,
                onValueChange = { text = it },
                onSubmit = { submit(text, details, reminderAt, onSave, onDismiss) },
                focusRequester = focusRequester,
            )
            Spacer(Modifier.height(10.dp))
            val english = task?.normalizedEnglishText
            if (isEditMode && !english.isNullOrBlank()) {
                // Read-only on purpose. This is derived from rawText, is cleared and
                // regenerated by the repository on every edit, and is not something the
                // user owns or can meaningfully hand-correct here.
                EnglishRenderingNote(text = english)
            } else {
                Text(
                    text = stringResource(R.string.add_task_hint),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                )
            }
            Spacer(Modifier.height(14.dp))
            DetailsField(
                value = details,
                onValueChange = { details = it },
            )
            if (!isEditMode) {
                Spacer(Modifier.height(14.dp))
                ReminderRow(
                    reminderAt = reminderAt,
                    onReminderChange = { reminderAt = it },
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        color = TextMuted,
                    )
                }
                Spacer(Modifier.padding(horizontal = 4.dp))
                Button(
                    onClick = { submit(text, details, reminderAt, onSave, onDismiss) },
                    enabled = text.isNotBlank(),
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

/**
 * Optional multi-line notes field: 2-4 lines visible, scrolls internally beyond that
 * rather than growing the sheet without bound.
 */
@Composable
private fun DetailsField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp, max = 108.dp)
            .clip(RoundedCornerShape(SmartTodoDimens.SurfaceRadius))
            .background(SurfaceMuted)
            .border(
                width = 1.dp,
                color = HairlineBorder,
                shape = RoundedCornerShape(SmartTodoDimens.SurfaceRadius),
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = stringResource(R.string.add_task_details_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = TextPlaceholder,
                modifier = Modifier.padding(end = MicGutter),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            cursorBrush = SolidColor(NeonPurple),
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = MicGutter),
        )
        VoiceInputButton(
            onTextRecognized = { spoken -> onValueChange(appendSpokenText(value, spoken)) },
            modifier = Modifier.align(Alignment.TopEnd),
            buttonSize = 34.dp,
            iconSize = 17.dp,
        )
    }
}

@Composable
private fun AddTaskField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp)
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
                text = stringResource(R.string.add_task_placeholder),
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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = MicGutter)
                .focusRequester(focusRequester),
        )
        // Dictation appends to whatever is already typed rather than replacing it, so a
        // second thought spoken into a half-written task adds to it instead of wiping it.
        VoiceInputButton(
            onTextRecognized = { spoken -> onValueChange(appendSpokenText(value, spoken)) },
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

/** Room reserved at the trailing edge of a text field for the microphone button. */
private val MicGutter = 40.dp

/**
 * The English rendering of the task being edited, shown read-only next to the editable
 * field so it is obvious which of the two texts is being changed.
 *
 * The card heading is this text; the field above holds [Task.rawText]. Editing rawText
 * clears this value and re-queues enrichment (see [TaskRepository.updateTask]), so it is
 * labelled as regenerating rather than presented as something the user can keep.
 */
@Composable
private fun EnglishRenderingNote(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SmartTodoDimens.SurfaceRadius))
            .background(SurfaceMuted)
            .border(
                width = 1.dp,
                color = HairlineBorder,
                shape = RoundedCornerShape(SmartTodoDimens.SurfaceRadius),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = stringResource(R.string.edit_task_english_label),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.edit_task_english_hint),
            style = MaterialTheme.typography.labelSmall,
            color = TextPlaceholder,
        )
    }
}

private fun submit(
    text: String,
    details: String,
    reminderAt: Long?,
    onSave: (String, String?, Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    if (text.isBlank()) return
    onSave(text, details.trim().ifBlank { null }, reminderAt)
    onDismiss()
}
