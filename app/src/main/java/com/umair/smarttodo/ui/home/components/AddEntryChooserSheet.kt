package com.umair.smarttodo.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChecklistRtl
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.umair.smarttodo.R
import com.umair.smarttodo.ui.theme.HairlineBorder
import com.umair.smarttodo.ui.theme.NeonPurple
import com.umair.smarttodo.ui.theme.SmartTodoDimens
import com.umair.smarttodo.ui.theme.SurfaceElevated
import com.umair.smarttodo.ui.theme.SurfaceMuted
import com.umair.smarttodo.ui.theme.TextMuted
import com.umair.smarttodo.ui.theme.TextPrimary

/**
 * Shown when the FAB is tapped: choose between a single task and a task list before
 * either of the real creation sheets opens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryChooserSheet(
    onDismiss: () -> Unit,
    onChooseSingleTask: () -> Unit,
    onChooseTaskList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                .navigationBarsPadding(),
        ) {
            Text(
                text = stringResource(R.string.chooser_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Spacer(Modifier.height(14.dp))
            ChooserOption(
                icon = Icons.Rounded.EditNote,
                title = stringResource(R.string.chooser_single_task),
                subtitle = stringResource(R.string.chooser_single_task_hint),
                onClick = onChooseSingleTask,
            )
            Spacer(Modifier.height(10.dp))
            ChooserOption(
                icon = Icons.Rounded.ChecklistRtl,
                title = stringResource(R.string.chooser_task_list),
                subtitle = stringResource(R.string.chooser_task_list_hint),
                onClick = onChooseTaskList,
            )
        }
    }
}

@Composable
private fun ChooserOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
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
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonPurple,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.height(3.dp))
            Text(text = subtitle, style = MaterialTheme.typography.labelLarge, color = TextMuted)
        }
    }
}
