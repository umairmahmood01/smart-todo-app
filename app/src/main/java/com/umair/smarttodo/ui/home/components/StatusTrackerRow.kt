package com.umair.smarttodo.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.umair.smarttodo.R
import com.umair.smarttodo.domain.TaskStatus
import com.umair.smarttodo.ui.theme.HairlineBorder
import com.umair.smarttodo.ui.theme.SmartTodoDimens
import com.umair.smarttodo.ui.theme.SurfaceMuted
import com.umair.smarttodo.ui.theme.TextMuted
import com.umair.smarttodo.ui.theme.TextPrimary

/**
 * The three status pills. Tapping one toggles that status as a filter.
 */
@Composable
fun StatusTrackerRow(
    counts: Map<TaskStatus, Int>,
    selectedStatuses: Set<TaskStatus>,
    onStatusClick: (TaskStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        TaskStatus.entries.forEach { status ->
            StatusPill(
                status = status,
                count = counts[status] ?: 0,
                selected = status in selectedStatuses,
                dimmed = selectedStatuses.isNotEmpty() && status !in selectedStatuses,
                onClick = { onStatusClick(status) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatusPill(
    status: TaskStatus,
    count: Int,
    selected: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = status.color()
    val label = stringResource(status.shortLabelRes())
    val description = stringResource(R.string.cd_filter_by_status, label)
    val shape = RoundedCornerShape(SmartTodoDimens.TrackerPillRadius)

    Row(
        modifier = modifier
            .height(40.dp)
            .alpha(if (dimmed) 0.55f else 1f)
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.18f) else SurfaceMuted)
            .border(
                width = 1.dp,
                color = if (selected) accent.copy(alpha = 0.7f) else HairlineBorder,
                shape = shape,
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The 3px coloured left accent from the mockup.
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accent),
        )
        Spacer(Modifier.width(7.dp))
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.count_in_parens, count),
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            maxLines = 1,
        )
        Spacer(Modifier.width(8.dp))
    }
}
