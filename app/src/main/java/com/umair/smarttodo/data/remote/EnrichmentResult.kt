package com.umair.smarttodo.data.remote

import com.umair.smarttodo.domain.Category

/**
 * Outcome of a single call to the enrichment service.
 *
 * Exhaustive by construction: the data source never throws, so every caller can reason about
 * the network purely through this type.
 */
sealed interface EnrichmentResult {

    /**
     * The service answered with a usable normalisation.
     *
     * @param englishText plain-English rendering of the user's text; never blank.
     * @param category refined category; already coerced to [Category.OTHER] if the service
     *   returned a value this build does not know about.
     * @param confidence service-reported confidence in `0.0..1.0`, `0.0` when absent.
     */
    data class Success(
        val englishText: String,
        val category: Category,
        val confidence: Double,
    ) : EnrichmentResult

    /**
     * The call did not produce a usable result.
     *
     * @param error classification driving the retry decision.
     * @param message diagnostic detail for logs; may be `null`, never shown to the user.
     */
    data class Failure(
        val error: EnrichmentError,
        val message: String? = null,
    ) : EnrichmentResult

    /** Enrichment is not configured for this build, so no call was attempted. */
    data object Disabled : EnrichmentResult
}

/**
 * Why an enrichment call failed, and whether trying again could plausibly help.
 *
 * [isRetryable] is the single place that decides retry semantics; the worker only reads it.
 *
 * @param isRetryable `true` when the same request may succeed later (transient conditions),
 *   `false` when repeating it would fail identically.
 */
enum class EnrichmentError(val isRetryable: Boolean) {

    /** `400 invalid_input` - the payload is unacceptable; resending changes nothing. */
    INVALID_INPUT(isRetryable = false),

    /** `429 rate_limited` - server-side throttle; backoff is exactly the right response. */
    RATE_LIMITED(isRetryable = true),

    /** `502 upstream_error` - the model provider failed; usually transient. */
    UPSTREAM_ERROR(isRetryable = true),

    /** `401` / `403` - missing, wrong or revoked function key. Only a new build or key fixes it. */
    UNAUTHORIZED(isRetryable = false),

    /** No usable connection, DNS failure, timeout. */
    NETWORK(isRetryable = true),

    /** A 2xx response that could not be parsed, or that violated the contract. */
    MALFORMED_RESPONSE(isRetryable = false),

    /** Anything not covered above. Treated as permanent so it cannot spin forever. */
    UNEXPECTED(isRetryable = false),
    ;

    companion object {

        /**
         * Maps a documented error code from the response body, or `null` when [code] is absent
         * or unrecognised (in which case the caller falls back to [fromHttpStatus]).
         */
        fun fromCode(code: String?): EnrichmentError? = when (code?.trim()?.lowercase()) {
            "invalid_input" -> INVALID_INPUT
            "rate_limited" -> RATE_LIMITED
            "upstream_error" -> UPSTREAM_ERROR
            else -> null
        }

        /** Maps an HTTP status to an error class. Total: every status resolves to something. */
        fun fromHttpStatus(status: Int): EnrichmentError = when {
            status == 400 -> INVALID_INPUT
            status == 401 || status == 403 -> UNAUTHORIZED
            status == 429 -> RATE_LIMITED
            status in 500..599 -> UPSTREAM_ERROR
            else -> UNEXPECTED
        }
    }
}
