package com.umair.smarttodo.data.remote

import kotlinx.serialization.Serializable

/**
 * Request body for `POST <base-url>/api/enrich`.
 *
 * @param text the user's raw task text, in whatever language or script they typed it.
 */
@Serializable
data class EnrichmentRequestDto(
    val text: String,
)

/**
 * Successful (`200`) response body.
 *
 * Every field is nullable with a default even though the service contract says they are
 * always present: a malformed or truncated response must degrade to a handled failure, never
 * to a deserialization crash. [category] is intentionally a `String` rather than the
 * [com.umair.smarttodo.domain.Category] enum so that an unknown value coerces to
 * `OTHER` instead of throwing.
 */
@Serializable
data class EnrichmentResponseDto(
    val englishText: String? = null,
    val category: String? = null,
    val confidence: Double? = null,
)

/**
 * Error response body, returned with `400`, `429` and `502`.
 *
 * @param error machine-readable code: `invalid_input`, `rate_limited` or `upstream_error`.
 * @param message human-readable detail; for logs only, never surfaced to the user.
 */
@Serializable
data class EnrichmentErrorDto(
    val error: String? = null,
    val message: String? = null,
)
