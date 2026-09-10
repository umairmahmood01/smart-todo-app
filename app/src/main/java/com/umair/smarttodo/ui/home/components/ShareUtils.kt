package com.umair.smarttodo.ui.home.components

import android.content.Context
import android.content.Intent
import com.umair.smarttodo.R
import com.umair.smarttodo.domain.Task
import com.umair.smarttodo.domain.TaskList

/**
 * Plain-text share bodies. Pure formatting, no side effects, kept out of the
 * composables that trigger the share so those stay thin.
 */
internal fun Task.toShareText(): String {
    val lines = mutableListOf(rawText)
    val normalized = normalizedEnglishText
    if (!normalized.isNullOrBlank() && normalized != rawText) {
        lines += normalized
    }
    return lines.joinToString(separator = "\n")
}

internal fun TaskList.toShareText(): String {
    val lines = mutableListOf(title)
    items.forEach { item ->
        val box = if (item.isChecked) "[x]" else "[ ]"
        lines += "- $box ${item.text}"
    }
    return lines.joinToString(separator = "\n")
}

/** Fires `ACTION_SEND` wrapped in a chooser. No permission is required for this. */
internal fun Context.shareAsPlainText(body: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, body)
    }
    val chooser = Intent.createChooser(sendIntent, getString(R.string.action_share))
    startActivity(chooser)
}
