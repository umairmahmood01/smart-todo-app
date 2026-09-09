package com.umair.smarttodo.data.remote

import com.umair.smarttodo.domain.Category

/**
 * Resolves a service-supplied category name to a [Category].
 *
 * Deliberately forgiving: an unknown, misspelled, differently-cased or missing value becomes
 * [Category.OTHER]. The server and the client version their enum independently, so a value
 * this build has never heard of is an expected event, not an error - and it must never crash
 * the app or discard an otherwise good normalisation.
 */
fun String?.toCategoryOrOther(): Category {
    val name = this?.trim()?.uppercase().orEmpty()
    return CATEGORY_BY_NAME[name] ?: Category.OTHER
}

/**
 * Converts a `200` body into an [EnrichmentResult].
 *
 * A blank or missing `englishText` is treated as [EnrichmentError.MALFORMED_RESPONSE] rather
 * than a success: the whole point of the call is the normalisation, and writing an empty
 * string into the row would permanently mark the task "enriched" with nothing to show.
 * Confidence is clamped to `0.0..1.0` and defaults to `0.0`.
 */
fun EnrichmentResponseDto.toEnrichmentResult(): EnrichmentResult {
    val text = englishText?.trim().orEmpty()
    if (text.isEmpty()) {
        return EnrichmentResult.Failure(
            error = EnrichmentError.MALFORMED_RESPONSE,
            message = "Response contained no englishText",
        )
    }
    return EnrichmentResult.Success(
        englishText = text,
        category = category.toCategoryOrOther(),
        confidence = (confidence ?: 0.0).coerceIn(0.0, 1.0),
    )
}

private val CATEGORY_BY_NAME: Map<String, Category> = Category.entries.associateBy { it.name }
