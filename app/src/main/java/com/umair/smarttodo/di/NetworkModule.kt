package com.umair.smarttodo.di

import com.umair.smarttodo.BuildConfig
import com.umair.smarttodo.data.remote.EnrichmentApi
import com.umair.smarttodo.data.remote.EnrichmentConfig
import com.umair.smarttodo.data.remote.EnrichmentDataSource
import com.umair.smarttodo.data.remote.RetrofitEnrichmentDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Networking for the one remote feature this app has: task enrichment.
 *
 * Everything here is constructed lazily. [RetrofitEnrichmentDataSource] injects
 * `Lazy<EnrichmentApi>`, so in a build with no enrichment configuration the OkHttp client and
 * the Retrofit instance are never created at all.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val JSON_MEDIA_TYPE = "application/json".toMediaType()

    /**
     * Reads the enrichment configuration compiled into `BuildConfig`.
     *
     * Both fields default to the empty string when `local.properties` does not define them,
     * which switches the whole feature off. See [EnrichmentConfig] for why the key being
     * present in the APK is acceptable and what actually protects the endpoint.
     */
    @Provides
    @Singleton
    fun provideEnrichmentConfig(): EnrichmentConfig = EnrichmentConfig(
        baseUrl = BuildConfig.ENRICHMENT_BASE_URL,
        functionKey = BuildConfig.ENRICHMENT_FUNCTION_KEY,
    )

    /**
     * JSON codec shared by the Retrofit converter and by error-body parsing.
     *
     * `ignoreUnknownKeys` is what lets the service add fields without breaking older clients.
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    /**
     * HTTP client for the enrichment endpoint.
     *
     * The function key is attached by an interceptor so it exists in exactly one place, and
     * that same header is redacted from logs. Body logging is compiled to
     * [HttpLoggingInterceptor.Level.NONE] in release builds because request bodies contain
     * the user's task text, which is personal data and must not reach logcat on a user device.
     * Timeouts are generous on read: the server calls a language model.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(config: EnrichmentConfig): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader(EnrichmentConfig.FUNCTION_KEY_HEADER)
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val authorized = chain.request().newBuilder()
                    .header(EnrichmentConfig.FUNCTION_KEY_HEADER, config.functionKey)
                    .build()
                chain.proceed(authorized)
            }
            .addInterceptor(logging)
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Retrofit pointed at the configured deployment.
     *
     * An unconfigured build gets [EnrichmentConfig.PLACEHOLDER_BASE_URL], because
     * `Retrofit.Builder.baseUrl` rejects an empty string. No request is ever issued against it.
     */
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json, config: EnrichmentConfig): Retrofit =
        Retrofit.Builder()
            .baseUrl(config.retrofitBaseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()

    /** The enrichment endpoint binding. */
    @Provides
    @Singleton
    fun provideEnrichmentApi(retrofit: Retrofit): EnrichmentApi = retrofit.create(EnrichmentApi::class.java)

    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val CALL_TIMEOUT_SECONDS = 45L
}

/** Binds the network implementations to the interfaces the data layer depends on. */
@Module
@InstallIn(SingletonComponent::class)
interface NetworkBindingsModule {

    /** Retrofit-backed remote enrichment. */
    @Binds
    fun bindEnrichmentDataSource(impl: RetrofitEnrichmentDataSource): EnrichmentDataSource
}
