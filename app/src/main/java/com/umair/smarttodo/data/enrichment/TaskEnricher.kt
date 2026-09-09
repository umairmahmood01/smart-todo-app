package com.umair.smarttodo.data.enrichment

import com.umair.smarttodo.data.local.TaskDao
import com.umair.smarttodo.data.remote.EnrichmentConfig
import com.umair.smarttodo.data.remote.EnrichmentDataSource
import com.umair.smarttodo.data.remote.EnrichmentError
import com.umair.smarttodo.data.remote.EnrichmentResult
import com.umair.smarttodo.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Layer two of the two-layer categorisation design.
 *
 * Layer one - the offline rule-based categorizer - has already given the task a category by
 * the time this runs, and the user has already seen it. This class only *improves* an
 * existing row: it asks the remote service for a plain-English normalisation and a refined
 * category, then writes both onto the row. The Room `Flow` pushes the change to the UI.
 *
 * Guarantees the rest of the app relies on:
 * - **Idempotent.** A row that already has `normalizedEnglishText` is skipped without any
 *   network call, so re-running enrichment for the same task is harmless and free.
 * - **Non-destructive.** Only the two enrichment columns are written; a status change or pin
 *   made while the call was in flight survives.
 * - **Silent when unconfigured.** With a blank base URL or key nothing is attempted.
 * - **Total.** It reports failure as a value; it does not throw. Cancellation propagates.
 *
 * @param dao source of truth for the row being enriched.
 * @param dataSource the remote call, already failure-safe.
 * @param config decides whether the feature exists at all in this build.
 * @param ioDispatcher injected so tests can run the database work deterministically.
 */
@Singleton
class TaskEnricher @Inject constructor(
    private val dao: TaskDao,
    private val dataSource: EnrichmentDataSource,
    private val config: EnrichmentConfig,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Enriches the task identified by [taskId].
     *
     * @param taskId row to enrich.
     * @param rawText the text captured when the work was scheduled. The stored row's text is
     *   used instead when this is blank, so a lost or truncated work payload still produces a
     *   correct request rather than a wasted one.
     */
    suspend fun enrich(taskId: Long, rawText: String): EnrichmentOutcome {
        if (!config.isEnabled) return EnrichmentOutcome.Disabled

        val existing = withContext(ioDispatcher) { dao.getById(taskId) }
            ?: return EnrichmentOutcome.TaskMissing

        if (!existing.normalizedEnglishText.isNullOrBlank()) return EnrichmentOutcome.AlreadyEnriched

        val text = rawText.trim().ifEmpty { existing.rawText.trim() }
        if (text.isEmpty()) return EnrichmentOutcome.Failed(EnrichmentError.INVALID_INPUT)

        return when (val result = dataSource.enrich(text)) {
            is EnrichmentResult.Success -> applyResult(taskId, result)
            is EnrichmentResult.Failure -> EnrichmentOutcome.Failed(result.error)
            EnrichmentResult.Disabled -> EnrichmentOutcome.Disabled
        }
    }

    /** Writes [result] onto the row, reporting [EnrichmentOutcome.TaskMissing] if it vanished. */
    private suspend fun applyResult(taskId: Long, result: EnrichmentResult.Success): EnrichmentOutcome {
        val updatedRows = withContext(ioDispatcher) {
            dao.applyEnrichment(
                id = taskId,
                normalizedEnglishText = result.englishText,
                category = result.category,
            )
        }
        return if (updatedRows > 0) EnrichmentOutcome.Enriched else EnrichmentOutcome.TaskMissing
    }
}
