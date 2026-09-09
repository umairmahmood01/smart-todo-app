package com.umair.smarttodo.data.enrichment

/**
 * What WorkManager should be told after one enrichment attempt.
 *
 * Kept as a plain enum in a framework-free file so the policy can be unit tested on the JVM
 * without a `Context`, a `WorkerParameters` or Robolectric.
 */
enum class EnrichmentWorkDecision {
    /** Finish quietly. Nothing more to do for this task, successfully or otherwise. */
    SUCCESS,

    /** Transient problem: ask WorkManager to try again after its backoff delay. */
    RETRY,

    /** Permanent problem, or out of attempts. Stop and leave the task unenriched. */
    FAILURE,
}

/**
 * Decides the fate of an enrichment work attempt.
 *
 * Enrichment is a best-effort improvement, never a user-visible failure, so every terminal
 * state that is not worth retrying reports [EnrichmentWorkDecision.SUCCESS] - including "task
 * deleted" and "feature disabled". Only a retryable error with attempts left retries; a
 * permanent error reports [EnrichmentWorkDecision.FAILURE] so the work is not rescheduled.
 *
 * @param outcome what [TaskEnricher] reported.
 * @param runAttemptCount WorkManager's zero-based attempt counter for this work.
 * @param maxAttempts hard ceiling; without it a permanently throttled key would retry forever.
 */
fun decideEnrichmentWork(
    outcome: EnrichmentOutcome,
    runAttemptCount: Int,
    maxAttempts: Int,
): EnrichmentWorkDecision = when (outcome) {
    EnrichmentOutcome.Enriched,
    EnrichmentOutcome.AlreadyEnriched,
    EnrichmentOutcome.TaskMissing,
    EnrichmentOutcome.Disabled,
    -> EnrichmentWorkDecision.SUCCESS

    is EnrichmentOutcome.Failed -> {
        val attemptsLeft = runAttemptCount + 1 < maxAttempts
        if (outcome.error.isRetryable && attemptsLeft) {
            EnrichmentWorkDecision.RETRY
        } else {
            EnrichmentWorkDecision.FAILURE
        }
    }
}
