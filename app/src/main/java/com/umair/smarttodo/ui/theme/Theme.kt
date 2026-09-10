package com.umair.smarttodo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Dark colour scheme lifted from the approved mockup. This app is dark-theme by
 * design: there is deliberately no light scheme and no dynamic colour.
 */
private val SmartTodoDarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = TextOnAccent,
    primaryContainer = TaskGradientEnd,
    onPrimaryContainer = TextOnAccent,
    secondary = NeonPurpleLight,
    onSecondary = TextOnAccent,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = NeonPurpleDeep,
    onTertiary = TextOnAccent,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = AppBackground,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextMuted,
    surfaceContainer = SurfaceMuted,
    surfaceContainerHigh = SurfaceElevated,
    surfaceContainerHighest = SurfaceElevated,
    surfaceContainerLow = SurfaceProgressBottom,
    surfaceContainerLowest = AppBackground,
    outline = TextPlaceholder,
    outlineVariant = HairlineBorderStrong,
    error = StatusTodo,
    onError = TextOnAccent,
    scrim = AppBackground,
)

/** Corner radii from the mockup: cards 22dp, surfaces 20dp, tracker pills 12dp. */
val SmartTodoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

object SmartTodoDimens {
    val ScreenPadding = 20.dp
    val TaskCardRadius = 18.dp
    val SurfaceRadius = 20.dp
    val TrackerPillRadius = 12.dp
    val PillRadius = 999.dp
    val SearchHeight = 46.dp
    val ChipHeight = 34.dp
    val AvatarSize = 44.dp
    val ProgressRingSize = 62.dp
    val ProgressRingStroke = 7.dp
    val TaskTileSize = 38.dp
    val FabSize = 60.dp
    val ListBottomFade = 96.dp
    /** Horizontal/vertical padding inside a task or task-list card, tuned ~15-20% smaller. */
    val TaskCardPaddingHorizontal = 13.dp
    val TaskCardPaddingVertical = 11.dp
    /** Gap between stacked cards in the feed `LazyColumn`. */
    val CardListSpacing = 11.dp
}

@Composable
fun SmartTodoTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SmartTodoDarkColorScheme,
        typography = SmartTodoTypography,
        shapes = SmartTodoShapes,
        content = content,
    )
}
