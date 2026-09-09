package com.umair.smarttodo.data.enrichment

import com.umair.smarttodo.data.remote.EnrichmentError

/**
 * Result of asking [TaskEnricher] to enrich one task.
 *
 * Distinguishes "nothing to do" from "tried and failed", which is what lets the worker decide
 * between finishing quietly and scheduling a retry.
 */
sealed interface EnrichmentOutcome {

    /** The row now carries a normalisation and a refined category. */
    data object Enriched : EnrichmentOutcome

    /** The row already had a normalisation, so no call was made. The idempotency guard. */
    data object AlreadyEnriched : EnrichmentOutcome

    /** The task was deleted before (or while) enrichment ran. Nothing to retry. */
    data object TaskMissing : EnrichmentOutcome

    /** Enrichment is not configured in this build. Nothing was attempted. */
    data object Disabled : EnrichmentOutcome

    /**
     * The call was attempted and did not produce a usable result.
     *
     * @param error carries [EnrichmentError.isRetryable], the only input to the retry decision.
     */
    data class Failed(val error: EnrichmentError) : EnrichmentOutcome
}
