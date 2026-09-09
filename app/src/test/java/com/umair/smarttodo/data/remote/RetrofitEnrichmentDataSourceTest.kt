package com.umair.smarttodo.data.remote

import com.umair.smarttodo.di.NetworkModule
import com.umair.smarttodo.domain.Category
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * End-to-end mapping of real HTTP responses onto [EnrichmentResult], over a local
 * [MockWebServer] rather than the network.
 *
 * Covers every documented status of the frozen /api/enrich contract plus the undocumented
 * ways a server can misbehave, because a weird response must never become a crash.
 */
class RetrofitEnrichmentDataSourceTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // --- happy path -------------------------------------------------------------------

    @Test
    fun `a 200 response maps to Success and posts to the contract path`() = runTest {
        server.enqueueJson(
            200,
            """{"englishText":"Send the presentation to the client tomorrow","category":"WORK","confidence":0.92}""",
        )

        val result = dataSource().enrich("kal client ko presentation bhejni hai")

        val success = result as EnrichmentResult.Success
        assertEquals("Send the presentation to the client tomorrow", success.englishText)
        assertEquals(Category.WORK, success.category)
        assertEquals(0.92, success.confidence, 0.0001)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/enrich", request.path)
        assertEquals(
            """{"text":"kal client ko presentation bhejni hai"}""",
            request.body.readUtf8(),
        )
    }

    @Test
    fun `an unknown category from the server becomes OTHER`() = runTest {
        server.enqueueJson(200, """{"englishText":"Water the plants","category":"GARDENING","confidence":0.7}""")

        val success = dataSource().enrich("paudhon ko paani dena hai") as EnrichmentResult.Success

        assertEquals(Category.OTHER, success.category)
        assertEquals("Water the plants", success.englishText)
    }

    @Test
    fun `unknown extra fields in the response are ignored`() = runTest {
        server.enqueueJson(
            200,
            """{"englishText":"Buy milk","category":"SHOPPING","confidence":0.8,"model":"m","tokens":42}""",
        )

        val success = dataSource().enrich("doodh lena hai") as EnrichmentResult.Success

        assertEquals(Category.SHOPPING, success.category)
    }

    // --- documented error codes -------------------------------------------------------

    @Test
    fun `400 invalid_input is a permanent failure`() = runTest {
        server.enqueueJson(400, """{"error":"invalid_input","message":"text must not be empty"}""")

        val failure = dataSource().enrich("something") as EnrichmentResult.Failure

        assertEquals(EnrichmentError.INVALID_INPUT, failure.error)
        assertFalse(failure.error.isRetryable)
        assertEquals("text must not be empty", failure.message)
    }

    @Test
    fun `429 rate_limited is retryable`() = runTest {
        server.enqueueJson(429, """{"error":"rate_limited","message":"slow down"}""")

        val failure = dataSource().enrich("something") as EnrichmentResult.Failure

        assertEquals(EnrichmentError.RATE_LIMITED, failure.error)
        assertTrue(failure.error.isRetryable)
    }

    @Test
    fun `502 upstream_error is retryable`() = runTest {
        server.enqueueJson(502, """{"error":"upstream_error","message":"provider unavailable"}""")

        val failure = dataSource().enrich("something") as EnrichmentResult.Failure

        assertEquals(EnrichmentError.UPSTREAM_ERROR, failure.error)
        assertTrue(failure.error.isRetryable)
    }

    // --- undocumented but possible ----------------------------------------------------

    @Test
    fun `the body error code wins over the http status`() = runTest {
        server.enqueueJson(400, """{"error":"rate_limited","message":"quota"}""")

        val failure = dataSource().enrich("something") as EnrichmentResult.Failure

        assertEquals(EnrichmentError.RATE_LIMITED, failure.error)
    }

    @Test
    fun `a rejected function key is a permanent failure`() = runTest {
        server.enqueueJson(401, "")

        val failure = dataSource().enrich("something") as EnrichmentResult.Failure

        assertEquals(EnrichmentError.UNAUTHORIZED, failure.error)
        assertFalse(failure.error.isRetryable)
    }

    @Test
    fun `an unreadable error body falls back to the status`() = runTest {
        server.enqueueJson(503, "<html>gateway</html>")

        val failure = dataSource().enrich("something") as EnrichmentResult.Failure

        assertEquals(EnrichmentError.UPSTREAM_ERROR, failure.error)
    }

    @Test
    fun `an unmapped status is UNEXPECTED rather than an exception`() = runTest {
        server.enqueueJson(418, """{"error":"teapot","message":"short and stout"}""")

        val failure = dataSource().enrich("something") as EnrichmentResult.Failure

        assertEquals(EnrichmentError.UNEXPECTED, failure.error)
    }

    @Test
    fun `a 200 with unparseable json is a malformed response`() = runTest {
        server.enqueueJson(200, "this is not json")

        val failure = dataSource().enrich("something") as EnrichmentResult.Failure

        assertEquals(EnrichmentError.MALFORMED_RESPONSE, failure.error)
    }

    @Test
    fun `a 200 with no englishText is a malformed response`() = runTest {
        server.enqueueJson(200, """{"category":"WORK","confidence":0.9}""")

        val failure = dataSource().enrich("something") as EnrichmentResult.Failure

        assertEquals(EnrichmentError.MALFORMED_RESPONSE, failure.error)
    }

    @Test
    fun `a dead endpoint is a retryable network failure`() = runTest {
        val source = dataSource()
        server.shutdown()

        val failure = source.enrich("something") as EnrichmentResult.Failure

        assertEquals(EnrichmentError.NETWORK, failure.error)
        assertTrue(failure.error.isRetryable)
    }

    // --- configuration gate -----------------------------------------------------------

    @Test
    fun `an unconfigured build reports Disabled without making a request`() = runTest {
        val result = dataSource(EnrichmentConfig.Disabled).enrich("something")

        assertEquals(EnrichmentResult.Disabled, result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a missing function key alone disables the call`() = runTest {
        val result = dataSource(EnrichmentConfig(server.url("/").toString(), "")).enrich("x")

        assertEquals(EnrichmentResult.Disabled, result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `blank text never reaches the network`() = runTest {
        val failure = dataSource().enrich("    ") as EnrichmentResult.Failure

        assertEquals(EnrichmentError.INVALID_INPUT, failure.error)
        assertEquals(0, server.requestCount)
    }

    // --- helpers ----------------------------------------------------------------------

    private fun MockWebServer.enqueueJson(code: Int, body: String) {
        enqueue(
            MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body),
        )
    }

    private fun dataSource(
        config: EnrichmentConfig = EnrichmentConfig(server.url("/").toString(), "test-key"),
    ): RetrofitEnrichmentDataSource {
        val json = NetworkModule.provideJson()
        val api = Retrofit.Builder()
            .baseUrl(config.retrofitBaseUrl)
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(EnrichmentApi::class.java)

        return RetrofitEnrichmentDataSource(
            api = eager(api),
            config = config,
            json = json,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    /** Minimal [Lazy] handing back an already built instance. */
    private fun <T : Any> eager(instance: T): Lazy<T> = object : Lazy<T> {
        override fun get(): T = instance
    }
}
