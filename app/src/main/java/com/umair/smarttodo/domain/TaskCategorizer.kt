package com.umair.smarttodo.domain

/**
 * Maps free-form task text to a [Category]. Implementations must be deterministic
 * and must never throw; unrecognized input resolves to [Category.OTHER].
 */
interface TaskCategorizer {
    fun categorize(rawText: String): Category
}
