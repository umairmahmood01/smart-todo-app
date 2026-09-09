package com.umair.smarttodo.data.repository

import com.umair.smarttodo.data.enrichment.EnrichmentScheduler

/**
 * Records enrichment scheduling requests instead of touching WorkManager.
 *
 * Keeps [com.umair.smarttodo.data.repository.TaskRepositoryImplTest] a pure JVM test while
 * still proving that the insert path queues exactly one job with the right arguments.
 */
class FakeEnrichmentScheduler : EnrichmentScheduler {

    /** Every (taskId, rawText) pair passed to [scheduleEnrichment], in order. */
    val scheduled: MutableList<Pair<Long, String>> = mutableListOf()

    override fun scheduleEnrichment(taskId: Long, rawText: String) {
        scheduled += taskId to rawText
    }
}
