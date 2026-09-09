package com.umair.smarttodo.data.enrichment

import com.umair.smarttodo.data.remote.EnrichmentError
import org.junit.Assert.assertEquals
import org.junit.Test

/** The retry policy, isolated from WorkManager so it can be checked exhaustively. */
class EnrichmentWorkDecisionTest {

    @Test
    fun `terminal non-failure outcomes finish quietly`() {
        listOf(
            EnrichmentOutcome.Enriched,
            EnrichmentOutcome.AlreadyEnriched,
            EnrichmentOutcome.TaskMissing,
            EnrichmentOutcome.Disabled,
        ).forEach { outcome ->
            assertEquals(
                "Expected success for " + outcome,
                EnrichmentWorkDecision.SUCCESS,
                decide(outcome, runAttemptCount = 0),
            )
        }
    }

    @Test
    fun `retryable errors retry while attempts remain`() {
        listOf(
            EnrichmentError.RATE_LIMITED,
            EnrichmentError.UPSTREAM_ERROR,
            EnrichmentError.NETWORK,
        ).forEach { error ->
            assertEquals(
                "Expected retry for " + error,
                EnrichmentWorkDecision.RETRY,
                decide(EnrichmentOutcome.Failed(error), runAttemptCount = 0),
            )
        }
    }

    @Test
    fun `permanent errors never retry`() {
        listOf(
            EnrichmentError.INVALID_INPUT,
            EnrichmentError.UNAUTHORIZED,
            EnrichmentError.MALFORMED_RESPONSE,
            EnrichmentError.UNEXPECTED,
        ).forEach { error ->
            assertEquals(
                "Expected failure for " + error,
                EnrichmentWorkDecision.FAILURE,
                decide(EnrichmentOutcome.Failed(error), runAttemptCount = 0),
            )
        }
    }

    @Test
    fun `a retryable error gives up once the attempt ceiling is reached`() {
        val outcome = EnrichmentOutcome.Failed(EnrichmentError.RATE_LIMITED)

        assertEquals(EnrichmentWorkDecision.RETRY, decide(outcome, runAttemptCount = MAX - 2))
        assertEquals(EnrichmentWorkDecision.FAILURE, decide(outcome, runAttemptCount = MAX - 1))
        assertEquals(EnrichmentWorkDecision.FAILURE, decide(outcome, runAttemptCount = MAX + 10))
    }

    private fun decide(outcome: EnrichmentOutcome, runAttemptCount: Int) =
        decideEnrichmentWork(outcome, runAttemptCount, MAX)

    private companion object {
        private const val MAX = TaskEnrichmentWorker.MAX_ATTEMPTS
    }
}
