package com.umair.smarttodo.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umair.smarttodo.ui.theme.CardSurfaceBottom
import com.umair.smarttodo.ui.theme.CardSurfaceTop
import com.umair.smarttodo.ui.theme.SmartTodoDimens
import com.umair.smarttodo.ui.theme.TextPrimary

/**
 * The shared visual shell for every card in the feed - a plain task or a task list, both
 * read as one family because both are built from this.
 *
 * Design note, because this replaced something quite different. Cards used to be filled
 * edge to edge with a saturated two-stop category gradient. The user described that
 * colour as "too blunt", so the strategy changed rather than the values: a card is now a
 * dark navy surface consistent with the rest of the app, and the category colour appears
 * as an accent on it, four times over:
 *
 *  1. a bright vertical spine down the leading edge, the loudest instance of the colour
 *     but only a few dp wide;
 *  2. a soft directional glow blooming off that spine into the card;
 *  3. a barely-there tint mixed into the top of the surface gradient;
 *  4. a large, very-low-opacity category emoji watermark bleeding off the trailing edge,
 *     there for texture rather than for information.
 *
 * Plus a diffuse, accent-tinted drop shadow instead of a hard one, and more internal
 * breathing room despite the smaller type. Per-category identity is fully preserved (the
 * user asked for distinct colours per category and still wants them) - only how the
 * colour is applied changed. A useful side effect: the surface is always dark, so card
 * text is always white and there is no longer any per-category contrast to juggle.
 */
@Composable
fun CategoryAccentCard(
    accent: Color,
    watermarkEmoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        start = SmartTodoDimens.TaskCardPaddingHorizontal + AccentSpineWidth,
        end = SmartTodoDimens.TaskCardPaddingHorizontal,
        top = SmartTodoDimens.TaskCardPaddingVertical,
        bottom = SmartTodoDimens.TaskCardPaddingVertical,
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(SmartTodoDimens.TaskCardRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Soft and diffuse rather than a tight dark drop: a wide blur radius, low
            // alpha, and tinted with the category colour so the card looks lit by its own
            // accent instead of stamped onto the background.
            .shadow(
                elevation = 12.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.copy(alpha = 0.55f),
                spotColor = accent.copy(alpha = 0.42f),
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        // A whisper of the category colour in the surface itself: enough
                        // to tell two cards apart at a glance, nowhere near a fill.
                        lerp(CardSurfaceTop, accent, 0.12f),
                        CardSurfaceBottom,
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.34f),
                        Color.White.copy(alpha = 0.04f),
                    ),
                ),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .drawWithCache {
                val spine = AccentSpineWidth.toPx()
                val spineBrush = Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.95f),
                        accent.copy(alpha = 0.45f),
                    ),
                )
                // Directional bloom: originates on the spine, a little above centre, and
                // falls away across roughly three quarters of the card.
                val glowBrush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.26f),
                        accent.copy(alpha = 0.07f),
                        Color.Transparent,
                    ),
                    center = Offset(0f, size.height * 0.28f),
                    radius = size.width * 0.78f,
                )
                onDrawBehind {
                    drawRect(brush = glowBrush)
                    drawRect(brush = spineBrush, size = Size(spine, size.height))
                }
            },
    ) {
        // Oversized category glyph, mostly off the trailing edge and clipped by the card.
        // Purely textural: it is never the thing the user reads, so it sits far below the
        // contrast floor on purpose.
        Text(
            text = watermarkEmoji,
            fontSize = WatermarkFontSize,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 26.dp, y = 6.dp)
                .alpha(0.07f),
        )
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Width of the coloured spine running down the leading edge of a card. */
val AccentSpineWidth = 4.dp

private val WatermarkFontSize = 96.sp

/** Neutral fill behind pills and tiles on a card. */
internal val CardScrim = Color(0x14FFFFFF)

/** Neutral hairline around pills and tiles on a card. */
internal val CardHairline = Color(0x1FFFFFFF)

/** Secondary text colour on a card: white, dimmed, always legible on the dark surface. */
internal val CardTextMuted = Color(0xB8FFFFFF)

/** Tertiary text colour on a card, used for dates and hints. */
internal val CardTextFaint = Color(0x8FFFFFFF)

/**
 * Slim progress rail, filled in the category accent. Used by [TaskListCard] to show how
 * much of a checklist is done without spending a whole row on it. [fraction] is coerced,
 * so an empty list is simply an empty rail.
 */
@Composable
internal fun CardProgressRail(
    fraction: Float,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val filled = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(CardScrim),
    ) {
        // Nothing checked means no fill at all, rather than a zero-width child.
        if (filled > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(filled)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(accent.copy(alpha = 0.75f), accent),
                        ),
                    ),
            )
        }
    }
}

/**
 * Compact rounded pill used for card tags: category, status, and the details affordance.
 * [leading] is an optional slot for a dot, an icon or an emoji ahead of the label.
 */
@Composable
internal fun CardPill(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentColor: Color = TextPrimary,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(CardScrim)
            .border(width = 1.dp, color = CardHairline, shape = CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
        )
    }
}
