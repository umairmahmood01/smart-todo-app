package com.umair.smarttodo.ui.home.components

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.umair.smarttodo.R
import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.TaskStatus
import com.umair.smarttodo.ui.theme.StatusDone
import com.umair.smarttodo.ui.theme.StatusInProgress
import com.umair.smarttodo.ui.theme.StatusTodo
import com.umair.smarttodo.ui.theme.TextOnAccent

/**
 * Pure presentation mapping for domain enums: emoji, category gradient, a readable
 * content colour for gradient-filled surfaces, and a single accent colour for dark ones.
 *
 * Two different surfaces consume this, and they use it differently:
 *
 * - Feed cards ([TaskCard], [TaskListCard]) use [accent] only. They are dark navy
 *   surfaces that carry the category colour as a spine, a soft glow and a low-opacity
 *   watermark, so their text is always white and [contentColor] is irrelevant to them.
 * - [CategoryChipRow] still fills its chips with the full [brush], so it still needs
 *   [contentColor] to stay legible on the lighter category gradients (Health & Fitness
 *   is a light amber and needs near-black text).
 *
 * [contentColor] therefore survives as a chip concern, not a card concern — that is why
 * it was not deleted when the cards stopped needing per-category contrast juggling.
 *
 * [accent] defaults to [start] and is only overridden where [start] is too dark to read
 * as a thin line or a faint glow against the navy card surface.
 */
data class CategoryVisual(
    val emoji: String,
    val start: Color,
    val end: Color,
    val contentColor: Color = TextOnAccent,
    private val accentOverride: Color? = null,
) {
    /** Full-bleed two-stop gradient. Chips only — see the class KDoc. */
    val brush: Brush get() = Brush.linearGradient(listOf(start, end))

    /** The one colour that stands for this category on a dark surface. */
    val accent: Color get() = accentOverride ?: start
}

fun Category.visual(): CategoryVisual = when (this) {
    Category.CODING -> CategoryVisual(
        emoji = "💻",
        start = Color(0xFF7A28C9),
        end = Color(0xFF5B1CA8),
        accentOverride = Color(0xFFA96BF5),
    )
    Category.WORK -> CategoryVisual(
        emoji = "💼",
        start = Color(0xFF3B4BD8),
        end = Color(0xFF2A3399),
        accentOverride = Color(0xFF6E7CF5),
    )
    Category.HEALTH_FITNESS -> CategoryVisual(
        emoji = "🏃",
        start = Color(0xFFF0A32A),
        end = Color(0xFFD97A0C),
        contentColor = Color(0xFF2A1A02),
    )
    Category.STUDY -> CategoryVisual(
        emoji = "📚",
        start = Color(0xFF1FA88A),
        end = Color(0xFF157A66),
        accentOverride = Color(0xFF35D6B0),
    )
    Category.PERSONAL -> CategoryVisual(
        emoji = "🏡",
        start = Color(0xFFE0568F),
        end = Color(0xFFB3316C),
        accentOverride = Color(0xFFF178AB),
    )
    Category.SHOPPING -> CategoryVisual(
        emoji = "🛒",
        start = Color(0xFFEF6A3B),
        end = Color(0xFFC7431B),
        accentOverride = Color(0xFFFB8A5F),
    )
    Category.FINANCE -> CategoryVisual(
        emoji = "💰",
        start = Color(0xFF2E9E5B),
        end = Color(0xFF1C7040),
        accentOverride = Color(0xFF4BCB80),
    )
    Category.OTHER -> CategoryVisual(
        emoji = "🗂️",
        start = Color(0xFF4B4A73),
        end = Color(0xFF37365A),
        accentOverride = Color(0xFF9A98D0),
    )
}

/** Accent colour of a status, straight from the design tokens. */
fun TaskStatus.color(): Color = when (this) {
    TaskStatus.TODO -> StatusTodo
    TaskStatus.IN_PROGRESS -> StatusInProgress
    TaskStatus.DONE -> StatusDone
}

/** Short label used by the tracker pills ("To-Do", "Progress", "Done"). */
@StringRes
fun TaskStatus.shortLabelRes(): Int = when (this) {
    TaskStatus.TODO -> R.string.status_todo_short
    TaskStatus.IN_PROGRESS -> R.string.status_in_progress_short
    TaskStatus.DONE -> R.string.status_done_short
}

/** Full label used by menus and accessibility text. */
@StringRes
fun TaskStatus.labelRes(): Int = when (this) {
    TaskStatus.TODO -> R.string.status_todo
    TaskStatus.IN_PROGRESS -> R.string.status_in_progress
    TaskStatus.DONE -> R.string.status_done
}
