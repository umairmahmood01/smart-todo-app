package com.umair.smarttodo.data.enrichment

import com.umair.smarttodo.data.remote.EnrichmentDataSource
import com.umair.smarttodo.data.remote.EnrichmentResult
import com.umair.smarttodo.domain.Category

/**
 * Scripted [EnrichmentDataSource] that records what it was asked.
 *
 * Lets [TaskEnricherTest] assert the two things that matter for the offline-first design:
 * what gets written when a call succeeds, and that no call is made at all when it should not be.
 */
class FakeEnrichmentDataSource(
    var result: EnrichmentResult = defaultSuccess,
) : EnrichmentDataSource {

    /** Every text passed to [enrich], in order. */
    val requests: MutableList<String> = mutableListOf()

    /** Number of times the remote call was attempted. */
    val callCount: Int get() = requests.size

    override suspend fun enrich(text: String): EnrichmentResult {
        requests += text
        return result
    }

    companion object {
        /** The worked example from the API contract. */
        val defaultSuccess: EnrichmentResult.Success = EnrichmentResult.Success(
            englishText = "Send the presentation to the client tomorrow",
            category = Category.WORK,
            confidence = 0.92,
        )
    }
}
