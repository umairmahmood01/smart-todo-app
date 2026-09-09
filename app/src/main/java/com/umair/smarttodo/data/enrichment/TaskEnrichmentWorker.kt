package com.umair.smarttodo.data.enrichment

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.umair.smarttodo.data.remote.EnrichmentError
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs one background enrichment attempt for a single task.
 *
 * Deliberately thin: all behaviour lives in [TaskEnricher] (what to do) and
 * [decideEnrichmentWork] (what to tell WorkManager), both of which are plain JVM-testable
 * code. The worker only unpacks its input and translates the outcome.
 *
 * Scheduling properties, all set in [request]:
 * - `NetworkType.CONNECTED`, so an offline device never even starts an attempt.
 * - Exponential backoff from [INITIAL_BACKOFF_SECONDS], so a throttled service is not hammered.
 * - Enqueued as unique work per task id (see [uniqueWorkName]), so duplicate scheduling
 *   collapses instead of piling up.
 *
 * Combined with the enricher's "already enriched" guard, running this worker twice for the
 * same task is a no-op the second time.
 */
@HiltWorker
class TaskEnrichmentWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val enricher: TaskEnricher,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, NO_TASK_ID)
        if (taskId == NO_TASK_ID) return Result.failure()

        val rawText = inputData.getString(KEY_RAW_TEXT).orEmpty()

        val outcome = try {
            enricher.enrich(taskId, rawText)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (unexpected: Exception) {
            // The enricher is failure-safe, so reaching here means something below it broke
            // (a database error, most likely). Enrichment is optional: record it as a
            // permanent failure rather than retrying into a loop.
            EnrichmentOutcome.Failed(EnrichmentError.UNEXPECTED)
        }

        return when (decideEnrichmentWork(outcome, runAttemptCount, MAX_ATTEMPTS)) {
            EnrichmentWorkDecision.SUCCESS -> Result.success()
            EnrichmentWorkDecision.RETRY -> Result.retry()
            EnrichmentWorkDecision.FAILURE -> Result.failure()
        }
    }

    companion object {
        /** Input key for the row id to enrich. */
        const val KEY_TASK_ID: String = "taskId"

        /**
         * Input key for the text to send.
         *
         * The text is personal data, so note where it lives: WorkManager's own database,
         * inside the app's private storage, deleted with the work record once it completes.
         */
        const val KEY_RAW_TEXT: String = "rawText"

        /** Tag for observing or cancelling every enrichment job at once. */
        const val TAG: String = "task-enrichment"

        /** Attempt ceiling, including the first attempt. */
        const val MAX_ATTEMPTS: Int = 5

        /** First backoff delay; doubles on each retry. */
        const val INITIAL_BACKOFF_SECONDS: Long = 30L

        private const val NO_TASK_ID = -1L
        private const val WORK_NAME_PREFIX = "enrich-task-"

        /** Unique work name for [taskId]; the deduplication key for scheduling. */
        fun uniqueWorkName(taskId: Long): String = WORK_NAME_PREFIX + taskId

        /** Builds the network-constrained, exponentially backed-off request for one task. */
        fun request(taskId: Long, rawText: String): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<TaskEnrichmentWorker>()
                .setInputData(
                    workDataOf(
                        KEY_TASK_ID to taskId,
                        KEY_RAW_TEXT to rawText,
                    ),
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    INITIAL_BACKOFF_SECONDS,
                    TimeUnit.SECONDS,
                )
                .addTag(TAG)
                .build()
    }
}
