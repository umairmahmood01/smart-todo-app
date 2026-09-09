package com.umair.smarttodo.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Runtime configuration for the remote enrichment service.
 *
 * Both values originate from `BuildConfig`, which is populated from the gitignored
 * `local.properties` (or the matching environment variables on CI) and defaults to the empty
 * string. A blank or unparseable [baseUrl], or a blank [functionKey], means the feature is
 * simply **not configured**: [isEnabled] is `false`, no request is ever made, and the app
 * behaves exactly as it does with the enrichment layer absent.
 *
 * Security, stated without varnish: [functionKey] ships inside the APK. Anyone who can read
 * the binary can extract it; obfuscation does not change that. It exists to identify the
 * client to the function, not to protect it. The real mitigation is server side - a
 * per-key rate limit on the Azure Function, a key scoped to this one endpoint, and rotation
 * without a client release.
 *
 * @param baseUrl root URL of the enrichment deployment, e.g. `https://example.azurewebsites.net`.
 * @param functionKey value sent in the `x-functions-key` header.
 */
data class EnrichmentConfig(
    val baseUrl: String,
    val functionKey: String,
) {

    /**
     * [baseUrl] trimmed and guaranteed to end in `/` (Retrofit rejects base URLs without a
     * trailing slash), or `null` when it is blank or not a valid http(s) URL.
     */
    val validatedBaseUrl: String? = baseUrl.trim()
        .takeIf { it.isNotEmpty() }
        ?.let { if (it.endsWith("/")) it else "$it/" }
        ?.takeIf { it.toHttpUrlOrNull() != null }

    /** True only when both a usable base URL and a non-blank key are present. */
    val isEnabled: Boolean = validatedBaseUrl != null && functionKey.isNotBlank()

    /**
     * Base URL to hand to `Retrofit.Builder`.
     *
     * When the feature is unconfigured this is [PLACEHOLDER_BASE_URL] rather than an empty
     * string, because `Retrofit.Builder.baseUrl` throws on invalid input and the Retrofit
     * instance is still constructed (lazily) in an unconfigured build. Nothing ever calls it:
     * every request path checks [isEnabled] first.
     */
    val retrofitBaseUrl: String get() = validatedBaseUrl ?: PLACEHOLDER_BASE_URL

    companion object {
        /** Syntactically valid, semantically unused stand-in for an unconfigured build. */
        const val PLACEHOLDER_BASE_URL: String = "http://localhost/"

        /** Header carrying [functionKey] on every request. */
        const val FUNCTION_KEY_HEADER: String = "x-functions-key"

        /** An explicitly unconfigured configuration; useful in tests and previews. */
        val Disabled: EnrichmentConfig = EnrichmentConfig(baseUrl = "", functionKey = "")
    }
}
