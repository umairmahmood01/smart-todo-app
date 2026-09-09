package com.umair.smarttodo.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umair.smarttodo.R
import com.umair.smarttodo.ui.theme.NeonPurple
import com.umair.smarttodo.ui.theme.SmartTodoDimens
import com.umair.smarttodo.ui.theme.TextMuted
import com.umair.smarttodo.ui.theme.TextPrimary

/**
 * Shown when there is nothing at all to do yet.
 */
@Composable
fun EmptyTasksState(modifier: Modifier = Modifier) {
    EmptyStateLayout(
        emoji = stringResource(R.string.empty_emoji),
        title = stringResource(R.string.empty_title),
        body = stringResource(R.string.empty_body),
        modifier = modifier,
    )
}

/**
 * Shown when tasks exist but the current search / chips hide all of them.
 */
@Composable
fun NoResultsState(
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyStateLayout(
        emoji = stringResource(R.string.empty_filtered_emoji),
        title = stringResource(R.string.empty_filtered_title),
        body = stringResource(R.string.empty_filtered_body),
        modifier = modifier,
    ) {
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onClearFilters,
            shape = RoundedCornerShape(SmartTodoDimens.PillRadius),
        ) {
            Text(
                text = stringResource(R.string.action_clear_filters),
                style = MaterialTheme.typography.labelLarge,
                color = NeonPurple,
            )
        }
    }
}

@Composable
private fun EmptyStateLayout(
    emoji: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emoji, fontSize = 40.sp)
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
        )
        action()
    }
}
