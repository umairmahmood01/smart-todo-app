package com.umair.smarttodo.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit binding for the enrichment endpoint.
 *
 * The `x-functions-key` header is attached by an interceptor rather than declared here, so
 * that the key never travels through call sites and can be redacted from HTTP logs in one
 * place. The raw [Response] is returned (instead of the body) so that non-2xx statuses arrive
 * as data rather than as a thrown `HttpException`.
 */
interface EnrichmentApi {

    /** Normalises and re-categorises the task text carried by [request]. */
    @POST("api/enrich")
    suspend fun enrich(@Body request: EnrichmentRequestDto): Response<EnrichmentResponseDto>
}
