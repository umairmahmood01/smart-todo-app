package com.umair.smarttodo.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The gate that keeps an unconfigured build offline.
 *
 * Every one of these cases must resolve to "disabled", because a fresh clone with no
 * `local.properties` and a CI build with no secrets both land here.
 */
class EnrichmentConfigTest {

    @Test
    fun `fully configured is enabled`() {
        val config = EnrichmentConfig("https://smart-todo.azurewebsites.net", "abc123")

        assertTrue(config.isEnabled)
        assertEquals("https://smart-todo.azurewebsites.net/", config.validatedBaseUrl)
        assertEquals("https://smart-todo.azurewebsites.net/", config.retrofitBaseUrl)
    }

    @Test
    fun `an existing trailing slash is not duplicated`() {
        val config = EnrichmentConfig("https://smart-todo.azurewebsites.net/", "abc123")

        assertEquals("https://smart-todo.azurewebsites.net/", config.validatedBaseUrl)
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        val config = EnrichmentConfig("  https://smart-todo.azurewebsites.net  ", "abc123")

        assertTrue(config.isEnabled)
        assertEquals("https://smart-todo.azurewebsites.net/", config.validatedBaseUrl)
    }

    @Test
    fun `empty defaults disable the feature`() {
        val config = EnrichmentConfig("", "")

        assertFalse(config.isEnabled)
        assertNull(config.validatedBaseUrl)
        assertEquals(EnrichmentConfig.PLACEHOLDER_BASE_URL, config.retrofitBaseUrl)
    }

    @Test
    fun `a blank key disables the feature even with a valid url`() {
        assertFalse(EnrichmentConfig("https://smart-todo.azurewebsites.net", "").isEnabled)
        assertFalse(EnrichmentConfig("https://smart-todo.azurewebsites.net", "   ").isEnabled)
    }

    @Test
    fun `a blank url disables the feature even with a key`() {
        assertFalse(EnrichmentConfig("", "abc123").isEnabled)
        assertFalse(EnrichmentConfig("   ", "abc123").isEnabled)
    }

    @Test
    fun `an unparseable url disables the feature instead of crashing Retrofit later`() {
        val config = EnrichmentConfig("not a url", "abc123")

        assertFalse(config.isEnabled)
        assertNull(config.validatedBaseUrl)
        assertEquals(EnrichmentConfig.PLACEHOLDER_BASE_URL, config.retrofitBaseUrl)
    }

    @Test
    fun `the shared Disabled instance is disabled`() {
        assertFalse(EnrichmentConfig.Disabled.isEnabled)
    }
}
