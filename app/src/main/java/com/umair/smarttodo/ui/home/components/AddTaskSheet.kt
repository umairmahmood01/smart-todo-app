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
 * Raw text in, plus an optional reminder instant: no category picker, because
 * categorization is automatic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    onDismiss: () -> Unit,
    onSave: (rawText: String, reminderAt: Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }
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
                text = stringResource(R.string.add_task_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(14.dp))
            AddTaskField(
                value = text,
                onValueChange = { text = it },
                onSubmit = { submit(text, reminderAt, onSave, onDismiss) },
                focusRequester = focusRequester,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.add_task_hint),
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted,
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
                    Text(
                        text = stringResource(R.string.action_cancel),
                        color = TextMuted,
                    )
                }
                Spacer(Modifier.padding(horizontal = 4.dp))
                Button(
                    onClick = { submit(text, reminderAt, onSave, onDismiss) },
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
                .focusRequester(focusRequester),
        )
    }
}

private fun submit(
    text: String,
    reminderAt: Long?,
    onSave: (String, Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    if (text.isBlank()) return
    onSave(text, reminderAt)
    onDismiss()
}
