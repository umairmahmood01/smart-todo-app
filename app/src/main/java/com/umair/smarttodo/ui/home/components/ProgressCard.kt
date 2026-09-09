package com.umair.smarttodo.ui.home.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.umair.smarttodo.R
import com.umair.smarttodo.ui.theme.HairlineBorder
import com.umair.smarttodo.ui.theme.ProgressCardBrush
import com.umair.smarttodo.ui.theme.ProgressRingColors
import com.umair.smarttodo.ui.theme.ProgressTrack
import com.umair.smarttodo.ui.theme.SmartTodoDimens
import com.umair.smarttodo.ui.theme.TextMuted
import com.umair.smarttodo.ui.theme.TextPrimary

/**
 * "You've completed X out of Y tasks" card with the circular ring on the left.
 */
@Composable
fun ProgressCard(
    percent: Int,
    doneCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SmartTodoDimens.SurfaceRadius))
            .background(ProgressCardBrush)
            .border(
                width = 1.dp,
                color = HairlineBorder,
                shape = RoundedCornerShape(SmartTodoDimens.SurfaceRadius),
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressRing(percent = percent)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = stringResource(R.string.progress_headline, doneCount, totalCount),
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = stringResource(progressSubRes(percent = percent, totalCount = totalCount)),
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted,
            )
        }
    }
}

/**
 * The ring itself: a rounded-cap arc over a flat track, drawn with [Canvas].
 */
@Composable
fun ProgressRing(
    percent: Int,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = SmartTodoDimens.ProgressRingSize,
    strokeWidth: androidx.compose.ui.unit.Dp = SmartTodoDimens.ProgressRingStroke,
) {
    val target = (percent.coerceIn(0, 100)) / 100f
    val sweepFraction by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 600),
        label = "progressSweep",
    )
    val description = stringResource(R.string.cd_progress_ring, percent)

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = ProgressTrack,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (sweepFraction > 0f) {
                drawArc(
                    brush = Brush.linearGradient(
                        colors = ProgressRingColors,
                        start = Offset.Zero,
                        end = Offset(this.size.width, this.size.height),
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * sweepFraction,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text = stringResource(R.string.progress_percent, percent),
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
        )
    }
}

@StringRes
internal fun progressSubRes(percent: Int, totalCount: Int): Int = when {
    totalCount == 0 -> R.string.progress_sub_empty
    percent >= 100 -> R.string.progress_sub_complete
    percent >= 75 -> R.string.progress_sub_almost
    percent >= 40 -> R.string.progress_sub_halfway
    else -> R.string.progress_sub_starting
}
