package com.umair.smarttodo.data.enrichment

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.umair.smarttodo.data.remote.EnrichmentConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Requests background enrichment for a task.
 *
 * An interface rather than a direct `WorkManager` call so the repository stays unit testable
 * without the WorkManager runtime.
 */
interface EnrichmentScheduler {

    /**
     * Schedules enrichment for [taskId].
     *
     * Must return promptly and must never throw: it is called on the task-creation path, and
     * creating a task cannot be allowed to fail because a background job could not be queued.
     */
    fun scheduleEnrichment(taskId: Long, rawText: String)
}

/**
 * WorkManager-backed [EnrichmentScheduler].
 *
 * Uses unique work keyed on the task id with [ExistingWorkPolicy.KEEP], so scheduling the same
 * task twice collapses into one job instead of racing two identical network calls.
 *
 * `WorkManager.getInstance` is resolved lazily inside [scheduleEnrichment] rather than
 * injected, to keep this class independent of WorkManager's initialisation order at startup.
 */
@Singleton
class WorkManagerEnrichmentScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: EnrichmentConfig,
) : EnrichmentScheduler {

    override fun scheduleEnrichment(taskId: Long, rawText: String) {
        // Unconfigured build: never queue work that can only ever no-op.
        if (!config.isEnabled) return

        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                TaskEnrichmentWorker.uniqueWorkName(taskId),
                ExistingWorkPolicy.KEEP,
                TaskEnrichmentWorker.request(taskId, rawText),
            )
        }.onFailure { error ->
            // Enrichment is optional; task creation has already succeeded and must stay that way.
            Log.w(TAG, "Could not schedule enrichment for task " + taskId, error)
        }
    }

    private companion object {
        private const val TAG = "EnrichmentScheduler"
    }
}
