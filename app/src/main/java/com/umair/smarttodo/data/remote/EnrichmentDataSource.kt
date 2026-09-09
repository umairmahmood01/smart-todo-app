package com.umair.smarttodo.data.remote

import com.umair.smarttodo.di.IoDispatcher
import dagger.Lazy
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.Response

/**
 * Single remote operation of the app: turn raw task text into a normalisation plus a refined
 * category.
 *
 * Implementations must never throw and must never block on the caller's thread.
 */
interface EnrichmentDataSource {

    /**
     * Calls the enrichment service for [text].
     *
     * @return [EnrichmentResult.Disabled] when the feature is unconfigured,
     *   [EnrichmentResult.Success] on a usable answer, otherwise [EnrichmentResult.Failure].
     *   Cancellation still propagates - a cancelled coroutine is not a failure.
     */
    suspend fun enrich(text: String): EnrichmentResult
}

/**
 * Retrofit-backed [EnrichmentDataSource].
 *
 * Every failure mode - HTTP status, unparseable body, dead network, unexpected runtime error -
 * is converted into an [EnrichmentResult.Failure]. Nothing escapes except
 * [CancellationException], which must propagate for structured concurrency to work.
 *
 * @param api injected as [Lazy] so an unconfigured build never even constructs Retrofit.
 * @param config decides whether a call is attempted at all.
 * @param json used to read error bodies, which Retrofit does not deserialize for us.
 * @param ioDispatcher injected rather than hardcoded so tests stay deterministic.
 */
@Singleton
class RetrofitEnrichmentDataSource @Inject constructor(
    private val api: Lazy<EnrichmentApi>,
    private val config: EnrichmentConfig,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : EnrichmentDataSource {

    override suspend fun enrich(text: String): EnrichmentResult {
        if (!config.isEnabled) return EnrichmentResult.Disabled

        val payload = text.trim()
        if (payload.isEmpty()) {
            return EnrichmentResult.Failure(EnrichmentError.INVALID_INPUT, "Blank task text")
        }

        return withContext(ioDispatcher) {
            try {
                val response = api.get().enrich(EnrichmentRequestDto(payload))
                if (response.isSuccessful) {
                    response.body()?.toEnrichmentResult()
                        ?: EnrichmentResult.Failure(
                            EnrichmentError.MALFORMED_RESPONSE,
                            "Empty 200 body",
                        )
                } else {
                    response.toFailure()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (io: IOException) {
                // No connectivity, DNS failure, timeout, connection reset.
                EnrichmentResult.Failure(EnrichmentError.NETWORK, io.message)
            } catch (unexpected: RuntimeException) {
                // Includes deserialization failures, which Retrofit wraps before rethrowing.
                EnrichmentResult.Failure(EnrichmentError.MALFORMED_RESPONSE, unexpected.message)
            }
        }
    }

    /**
     * Classifies a non-2xx response, preferring the documented `error` code in the body and
     * falling back to the HTTP status when the body is missing or unreadable.
     */
    private fun Response<EnrichmentResponseDto>.toFailure(): EnrichmentResult.Failure {
        val rawBody = runCatching { errorBody()?.string() }.getOrNull()
        val parsed = rawBody
            ?.takeIf { it.isNotBlank() }
            ?.let { body -> runCatching { json.decodeFromString(EnrichmentErrorDto.serializer(), body) }.getOrNull() }
        val error = EnrichmentError.fromCode(parsed?.error) ?: EnrichmentError.fromHttpStatus(code())
        return EnrichmentResult.Failure(
            error = error,
            message = parsed?.message ?: "HTTP " + code(),
        )
    }
}
