package com.umair.smarttodo.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umair.smarttodo.R
import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.ui.theme.SmartTodoDimens

/**
 * Horizontally scrolling, multi-select category filter chips.
 */
@Composable
fun CategoryChipRow(
    categories: List<Category>,
    counts: Map<Category, Int>,
    selectedCategories: Set<Category>,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = SmartTodoDimens.ScreenPadding),
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        items(items = categories, key = { it.name }) { category ->
            CategoryChip(
                category = category,
                count = counts[category] ?: 0,
                selected = category in selectedCategories,
                dimmed = selectedCategories.isNotEmpty() && category !in selectedCategories,
                onClick = { onCategoryClick(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: Category,
    count: Int,
    selected: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = category.visual()
    val description = stringResource(R.string.cd_filter_by_category, category.label)

    Row(
        modifier = modifier
            .height(SmartTodoDimens.ChipHeight)
            .alpha(if (dimmed) 0.45f else 1f)
            .clip(CircleShape)
            .background(visual.brush)
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) visual.contentColor.copy(alpha = 0.9f) else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 11.dp)
            .semantics {
                contentDescription = description
                this.selected = selected
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = visual.emoji, fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelMedium,
            color = visual.contentColor,
            maxLines = 1,
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = stringResource(R.string.count_in_parens, count),
            style = MaterialTheme.typography.labelSmall,
            color = visual.contentColor.copy(alpha = 0.75f),
            maxLines = 1,
        )
    }
}
