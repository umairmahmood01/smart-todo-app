package com.umair.smarttodo.ui.home.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umair.smarttodo.R
import com.umair.smarttodo.ui.theme.AvatarBrush
import com.umair.smarttodo.ui.theme.NeonPurple
import com.umair.smarttodo.ui.theme.SmartTodoDimens
import com.umair.smarttodo.ui.theme.TaglineTextStyle
import com.umair.smarttodo.ui.theme.TextMuted
import com.umair.smarttodo.ui.theme.TextPrimary
import java.time.LocalTime

/**
 * Greeting + tagline on the left, ringed avatar on the right.
 *
 * [hourOfDay] is injected so the greeting is deterministic in previews and tests;
 * callers on the real screen let it default to the device clock.
 */
@Composable
fun HomeHeader(
    modifier: Modifier = Modifier,
    hourOfDay: Int = remember { LocalTime.now().hour },
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = stringResource(greetingResFor(hourOfDay), stringResource(R.string.profile_name)),
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.tagline),
                style = TaglineTextStyle,
                color = TextMuted,
            )
        }
        Spacer(Modifier.size(12.dp))
        Avatar()
    }
}

@Composable
private fun Avatar(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.cd_avatar)
    Box(
        modifier = modifier
            .border(width = 2.dp, color = NeonPurple.copy(alpha = 0.55f), shape = CircleShape)
            .padding(3.dp)
            .size(SmartTodoDimens.AvatarSize)
            .background(brush = AvatarBrush, shape = CircleShape)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(R.string.avatar_emoji), fontSize = 20.sp)
    }
}

/** 05:00–11:59 morning, 12:00–17:59 afternoon, otherwise evening. */
@StringRes
internal fun greetingResFor(hourOfDay: Int): Int = when (hourOfDay) {
    in 5..11 -> R.string.greeting_morning
    in 12..17 -> R.string.greeting_afternoon
    else -> R.string.greeting_evening
}
