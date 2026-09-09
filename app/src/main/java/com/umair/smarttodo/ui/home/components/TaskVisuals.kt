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
 * Pure presentation mapping for domain enums: emoji, gradient and content colour.
 * The mockup only specified four categories, the rest follow the same recipe
 * (a saturated 135deg two-stop gradient with white text).
 */
data class CategoryVisual(
    val emoji: String,
    val start: Color,
    val end: Color,
    val contentColor: Color = TextOnAccent,
) {
    val brush: Brush get() = Brush.linearGradient(listOf(start, end))
}

fun Category.visual(): CategoryVisual = when (this) {
    Category.CODING -> CategoryVisual("💻", Color(0xFF7A28C9), Color(0xFF5B1CA8))
    Category.WORK -> CategoryVisual("💼", Color(0xFF3B4BD8), Color(0xFF2A3399))
    Category.HEALTH_FITNESS -> CategoryVisual(
        emoji = "🏃",
        start = Color(0xFFF0A32A),
        end = Color(0xFFD97A0C),
        contentColor = Color(0xFF2A1A02),
    )
    Category.STUDY -> CategoryVisual("📚", Color(0xFF1FA88A), Color(0xFF157A66))
    Category.PERSONAL -> CategoryVisual("🏡", Color(0xFFE0568F), Color(0xFFB3316C))
    Category.SHOPPING -> CategoryVisual("🛒", Color(0xFFEF6A3B), Color(0xFFC7431B))
    Category.FINANCE -> CategoryVisual("💰", Color(0xFF2E9E5B), Color(0xFF1C7040))
    Category.OTHER -> CategoryVisual("🗂️", Color(0xFF4B4A73), Color(0xFF37365A))
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
