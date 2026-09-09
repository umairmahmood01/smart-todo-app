package com.umair.smarttodo.data.enrichment

import com.umair.smarttodo.data.local.TaskEntity
import com.umair.smarttodo.data.remote.EnrichmentConfig
import com.umair.smarttodo.data.remote.EnrichmentDataSource
import com.umair.smarttodo.data.remote.EnrichmentError
import com.umair.smarttodo.data.remote.EnrichmentResult
import com.umair.smarttodo.data.repository.FakeTaskDao
import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Layer two of the categorisation design: it may improve a row, but it must never damage
 * one, never repeat work, and never do anything at all when unconfigured.
 */
class TaskEnricherTest {

    private val dao = FakeTaskDao()
    private val dataSource = FakeEnrichmentDataSource()

    // --- success ---------------------------------------------------------------------

    @Test
    fun `a successful call writes the normalisation and the refined category`() = runTest {
        dao.setRows(row())

        val outcome = enricher().enrich(TASK_ID, RAW_TEXT)

        assertEquals(EnrichmentOutcome.Enriched, outcome)
        assertEquals(listOf(RAW_TEXT), dataSource.requests)
        val stored = dao.currentRows.single()
        assertEquals("Send the presentation to the client tomorrow", stored.normalizedEnglishText)
        assertEquals(Category.WORK, stored.category)
    }

    @Test
    fun `enrichment leaves every column it does not own alone`() = runTest {
        dao.setRows(row(status = TaskStatus.IN_PROGRESS, isPinned = true))

        enricher().enrich(TASK_ID, RAW_TEXT)

        val stored = dao.currentRows.single()
        assertEquals(RAW_TEXT, stored.rawText)
        assertEquals(TaskStatus.IN_PROGRESS, stored.status)
        assertEquals(true, stored.isPinned)
        assertEquals(CREATED_AT, stored.createdDate)
    }

    @Test
    fun `an unknown category coerced to OTHER is still written`() = runTest {
        dao.setRows(row(category = Category.PERSONAL))
        dataSource.result = EnrichmentResult.Success("Water the plants", Category.OTHER, 0.3)

        assertEquals(EnrichmentOutcome.Enriched, enricher().enrich(TASK_ID, RAW_TEXT))
        assertEquals(Category.OTHER, dao.currentRows.single().category)
    }

    // --- idempotency -----------------------------------------------------------------

    @Test
    fun `an already enriched row is skipped without a network call`() = runTest {
        dao.setRows(row(normalizedEnglishText = "Already normalised"))

        val outcome = enricher().enrich(TASK_ID, RAW_TEXT)

        assertEquals(EnrichmentOutcome.AlreadyEnriched, outcome)
        assertEquals(0, dataSource.callCount)
        assertEquals(0, dao.applyEnrichmentCount)
        assertEquals("Already normalised", dao.currentRows.single().normalizedEnglishText)
    }

    @Test
    fun `running the same enrichment twice is harmless`() = runTest {
        dao.setRows(row())
        val enricher = enricher()

        assertEquals(EnrichmentOutcome.Enriched, enricher.enrich(TASK_ID, RAW_TEXT))
        val afterFirstRun = dao.currentRows.single()

        assertEquals(EnrichmentOutcome.AlreadyEnriched, enricher.enrich(TASK_ID, RAW_TEXT))

        assertEquals(1, dataSource.callCount)
        assertEquals(1, dao.applyEnrichmentCount)
        assertEquals(afterFirstRun, dao.currentRows.single())
    }

    @Test
    fun `a blank stored normalisation is treated as not yet enriched`() = runTest {
        dao.setRows(row(normalizedEnglishText = "   "))

        assertEquals(EnrichmentOutcome.Enriched, enricher().enrich(TASK_ID, RAW_TEXT))
        assertEquals(1, dataSource.callCount)
    }

    // --- nothing to do ---------------------------------------------------------------

    @Test
    fun `a deleted task is reported as missing, with no network call`() = runTest {
        val outcome = enricher().enrich(TASK_ID, RAW_TEXT)

        assertEquals(EnrichmentOutcome.TaskMissing, outcome)
        assertEquals(0, dataSource.callCount)
    }

    @Test
    fun `a task deleted mid-flight is reported as missing rather than resurrected`() = runTest {
        dao.setRows(row())
        val deletingSource = object : EnrichmentDataSource {
            override suspend fun enrich(text: String): EnrichmentResult {
                dao.deleteById(TASK_ID)
                return FakeEnrichmentDataSource.defaultSuccess
            }
        }

        val outcome = enricher(dataSource = deletingSource).enrich(TASK_ID, RAW_TEXT)

        assertEquals(EnrichmentOutcome.TaskMissing, outcome)
        assertEquals(0, dao.currentRows.size)
    }

    // --- disabled and failing ---------------------------------------------------------

    @Test
    fun `an unconfigured build never touches the row or the network`() = runTest {
        dao.setRows(row())

        val outcome = enricher(config = EnrichmentConfig.Disabled).enrich(TASK_ID, RAW_TEXT)

        assertEquals(EnrichmentOutcome.Disabled, outcome)
        assertEquals(0, dataSource.callCount)
        assertEquals(0, dao.applyEnrichmentCount)
        assertNull(dao.currentRows.single().normalizedEnglishText)
        assertEquals(Category.PERSONAL, dao.currentRows.single().category)
    }

    @Test
    fun `a partially configured build is also disabled`() = runTest {
        dao.setRows(row())

        val outcome = enricher(config = EnrichmentConfig("https://smart-todo.example", ""))
            .enrich(TASK_ID, RAW_TEXT)

        assertEquals(EnrichmentOutcome.Disabled, outcome)
        assertEquals(0, dataSource.callCount)
    }

    @Test
    fun `a failed call is reported with its error and leaves the row untouched`() = runTest {
        dao.setRows(row())
        dataSource.result = EnrichmentResult.Failure(EnrichmentError.RATE_LIMITED, "quota")

        val outcome = enricher().enrich(TASK_ID, RAW_TEXT)

        assertEquals(EnrichmentOutcome.Failed(EnrichmentError.RATE_LIMITED), outcome)
        assertEquals(0, dao.applyEnrichmentCount)
        assertNull(dao.currentRows.single().normalizedEnglishText)
        assertEquals(Category.PERSONAL, dao.currentRows.single().category)
    }

    @Test
    fun `a data source reporting Disabled is propagated, not treated as a failure`() = runTest {
        dao.setRows(row())
        dataSource.result = EnrichmentResult.Disabled

        assertEquals(EnrichmentOutcome.Disabled, enricher().enrich(TASK_ID, RAW_TEXT))
    }

    // --- input handling ----------------------------------------------------------------

    @Test
    fun `a blank work payload falls back to the stored text`() = runTest {
        dao.setRows(row())

        assertEquals(EnrichmentOutcome.Enriched, enricher().enrich(TASK_ID, "   "))
        assertEquals(listOf(RAW_TEXT), dataSource.requests)
    }

    @Test
    fun `text is trimmed before it is sent`() = runTest {
        dao.setRows(row())

        enricher().enrich(TASK_ID, "  " + RAW_TEXT + "  ")

        assertEquals(listOf(RAW_TEXT), dataSource.requests)
    }

    // --- helpers ------------------------------------------------------------------------

    private fun enricher(
        dataSource: EnrichmentDataSource = this.dataSource,
        config: EnrichmentConfig = ENABLED_CONFIG,
    ) = TaskEnricher(
        dao = dao,
        dataSource = dataSource,
        config = config,
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun row(
        normalizedEnglishText: String? = null,
        category: Category = Category.PERSONAL,
        status: TaskStatus = TaskStatus.TODO,
        isPinned: Boolean = false,
    ) = TaskEntity(
        id = TASK_ID,
        rawText = RAW_TEXT,
        normalizedEnglishText = normalizedEnglishText,
        category = category,
        status = status,
        isPinned = isPinned,
        createdDate = CREATED_AT,
        dueDate = null,
    )

    private companion object {
        private const val TASK_ID = 11L
        private const val RAW_TEXT = "kal client ko presentation bhejni hai"
        private const val CREATED_AT = 1_725_000_000_000L
        private val ENABLED_CONFIG = EnrichmentConfig(
            baseUrl = "https://smart-todo.example",
            functionKey = "test-key",
        )
    }
}
