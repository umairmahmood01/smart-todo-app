package com.umair.smarttodo.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.umair.smarttodo.R
import com.umair.smarttodo.ui.theme.HairlineBorder
import com.umair.smarttodo.ui.theme.NeonPurple
import com.umair.smarttodo.ui.theme.SmartTodoDimens
import com.umair.smarttodo.ui.theme.SurfaceMuted
import com.umair.smarttodo.ui.theme.TextMuted
import com.umair.smarttodo.ui.theme.TextPrimary
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The "Remind me" row shown in AddTaskSheet / AddTaskListSheet: current value (or an
 * "off" placeholder), tap to pick a new one via ReminderDialogFlow, and a clear
 * button once a reminder is set.
 */
@Composable
internal fun ReminderRow(
    reminderAt: Long?,
    onReminderChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val permissionState = rememberReminderPermissionState()
    var showFlow by remember { mutableStateOf(false) }

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
            .clickable {
                permissionState.requestIfNeeded()
                showFlow = true
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (reminderAt != null) {
                Icons.Rounded.NotificationsActive
            } else {
                Icons.Rounded.NotificationsNone
            },
            contentDescription = null,
            tint = if (reminderAt != null) NeonPurple else TextMuted,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = reminderAt?.let { formatReminder(it) }
                ?: stringResource(R.string.reminder_row_label),
            style = MaterialTheme.typography.bodyMedium,
            color = if (reminderAt != null) TextPrimary else TextMuted,
            modifier = Modifier.weight(1f),
        )
        if (reminderAt != null) {
            IconButton(onClick = { onReminderChange(null) }, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.action_clear_reminder),
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }

    if (permissionState.showDeniedHint()) {
        Text(
            text = stringResource(R.string.reminder_permission_hint),
            style = MaterialTheme.typography.labelLarge,
            color = TextMuted,
            modifier = Modifier.padding(top = 6.dp),
        )
    }

    ReminderDialogFlow(
        show = showFlow,
        initialMillis = reminderAt,
        onDismiss = { showFlow = false },
        onPicked = { millis ->
            onReminderChange(millis)
            showFlow = false
        },
    )
}

/**
 * Date-then-time dialog pair shared by ReminderRow (new tasks/lists) and the card
 * overflow menus (existing tasks/lists). Fully self-contained: callers just flip show.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderDialogFlow(
    show: Boolean,
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onPicked: (Long) -> Unit,
) {
    var step by remember { mutableStateOf(DialogStep.NONE) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(show) {
        step = if (show) DialogStep.DATE else DialogStep.NONE
    }

    when (step) {
        DialogStep.DATE -> {
            val seedMillis = initialMillis ?: System.currentTimeMillis()
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = seedMillis)
            DatePickerDialog(
                onDismissRequest = {
                    step = DialogStep.NONE
                    onDismiss()
                },
                confirmButton = {
                    TextButton(onClick = {
                        pendingDateMillis = datePickerState.selectedDateMillis ?: seedMillis
                        step = DialogStep.TIME
                    }) {
                        Text(stringResource(R.string.action_next))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        step = DialogStep.NONE
                        onDismiss()
                    }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            ) {
                DatePicker(state = datePickerState)
            }
        }

        DialogStep.TIME -> {
            val initial = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(initialMillis ?: System.currentTimeMillis()),
                ZoneId.systemDefault(),
            )
            val timePickerState = rememberTimePickerState(
                initialHour = initial.hour,
                initialMinute = initial.minute,
                is24Hour = true,
            )
            AlertDialog(
                onDismissRequest = {
                    step = DialogStep.NONE
                    onDismiss()
                },
                confirmButton = {
                    TextButton(onClick = {
                        val datePart = Instant
                            .ofEpochMilli(pendingDateMillis ?: System.currentTimeMillis())
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        val combined = LocalDateTime.of(
                            datePart,
                            LocalTime.of(timePickerState.hour, timePickerState.minute),
                        ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        step = DialogStep.NONE
                        onPicked(combined)
                    }) {
                        Text(stringResource(R.string.action_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        step = DialogStep.NONE
                        onDismiss()
                    }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
                text = { TimePicker(state = timePickerState) },
            )
        }

        DialogStep.NONE -> Unit
    }
}

private enum class DialogStep { NONE, DATE, TIME }

internal fun formatReminder(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.getDefault())
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
