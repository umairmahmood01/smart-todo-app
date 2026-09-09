package com.umair.smarttodo.data.remote

import com.umair.smarttodo.domain.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Response mapping, including the "never trust the server's category" rule. */
class EnrichmentMappersTest {

    @Test
    fun `every known category name maps to its enum constant`() {
        Category.entries.forEach { category ->
            assertEquals(category, category.name.toCategoryOrOther())
        }
    }

    @Test
    fun `category matching tolerates case and padding`() {
        assertEquals(Category.HEALTH_FITNESS, "  health_fitness  ".toCategoryOrOther())
        assertEquals(Category.WORK, "Work".toCategoryOrOther())
    }

    @Test
    fun `an unrecognised category coerces to OTHER instead of throwing`() {
        assertEquals(Category.OTHER, "GARDENING".toCategoryOrOther())
        assertEquals(Category.OTHER, "".toCategoryOrOther())
        assertEquals(Category.OTHER, (null as String?).toCategoryOrOther())
    }

    @Test
    fun `a well formed response maps to Success`() {
        val result = EnrichmentResponseDto(
            englishText = "  Send the presentation to the client tomorrow  ",
            category = "WORK",
            confidence = 0.92,
        ).toEnrichmentResult()

        val success = result as EnrichmentResult.Success
        assertEquals("Send the presentation to the client tomorrow", success.englishText)
        assertEquals(Category.WORK, success.category)
        assertEquals(0.92, success.confidence, 0.0001)
    }

    @Test
    fun `an unknown category still yields Success, bucketed as OTHER`() {
        val result = EnrichmentResponseDto(
            englishText = "Water the plants",
            category = "GARDENING",
            confidence = 0.4,
        ).toEnrichmentResult()

        assertEquals(Category.OTHER, (result as EnrichmentResult.Success).category)
    }

    @Test
    fun `confidence is defaulted and clamped`() {
        fun confidenceOf(raw: Double?): Double {
            val result = EnrichmentResponseDto("Buy milk", "SHOPPING", raw).toEnrichmentResult()
            return (result as EnrichmentResult.Success).confidence
        }

        assertEquals(0.0, confidenceOf(null), 0.0001)
        assertEquals(1.0, confidenceOf(4.2), 0.0001)
        assertEquals(0.0, confidenceOf(-1.0), 0.0001)
    }

    @Test
    fun `a missing or blank englishText is a malformed response, not a Success`() {
        listOf(null, "", "   ").forEach { text ->
            val result = EnrichmentResponseDto(text, "WORK", 0.9).toEnrichmentResult()

            assertTrue("Expected failure for text=" + text, result is EnrichmentResult.Failure)
            assertEquals(
                EnrichmentError.MALFORMED_RESPONSE,
                (result as EnrichmentResult.Failure).error,
            )
        }
    }

    @Test
    fun `error codes map to their documented classification`() {
        assertEquals(EnrichmentError.INVALID_INPUT, EnrichmentError.fromCode("invalid_input"))
        assertEquals(EnrichmentError.RATE_LIMITED, EnrichmentError.fromCode("rate_limited"))
        assertEquals(EnrichmentError.UPSTREAM_ERROR, EnrichmentError.fromCode("  Upstream_Error "))
        assertNull(EnrichmentError.fromCode("teapot"))
        assertNull(EnrichmentError.fromCode(null))
    }

    @Test
    fun `retry semantics are attached to the error, not to the call site`() {
        assertTrue(EnrichmentError.RATE_LIMITED.isRetryable)
        assertTrue(EnrichmentError.UPSTREAM_ERROR.isRetryable)
        assertTrue(EnrichmentError.NETWORK.isRetryable)
        assertTrue(!EnrichmentError.INVALID_INPUT.isRetryable)
        assertTrue(!EnrichmentError.UNAUTHORIZED.isRetryable)
        assertTrue(!EnrichmentError.MALFORMED_RESPONSE.isRetryable)
        assertTrue(!EnrichmentError.UNEXPECTED.isRetryable)
    }
}
