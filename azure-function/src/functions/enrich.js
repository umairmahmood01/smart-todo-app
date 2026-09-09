import { app } from '@azure/functions';
import { API_KEY_SETTING, ConfigError, enrichTask, getClient, isConfigured } from '../lib/claude.js';
import { SlidingWindowRateLimiter, clientKeyFromHeaders } from '../lib/rateLimit.js';
import { validateEnrichRequest } from '../lib/validate.js';

/**
 * Reads a positive integer App Setting, falling back when unset or invalid.
 *
 * @param {string} name App Setting name
 * @param {number} fallback value used when the setting is absent or unusable
 * @returns {number}
 */
function intSetting(name, fallback) {
  const parsed = Number.parseInt(process.env[name] ?? '', 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

/**
 * Process-wide limiter. The function key is embedded in a distributed APK and
 * is therefore only semi-secret, so requests are additionally capped per
 * client IP and in aggregate. This state is per worker process; see README.md.
 */
const limiter = new SlidingWindowRateLimiter({
  windowMs: 60_000,
  perKeyLimit: intSetting('ENRICH_RATE_LIMIT_PER_MINUTE', 30),
  globalLimit: intSetting('ENRICH_GLOBAL_RATE_LIMIT_PER_MINUTE', 300),
});

if (!isConfigured()) {
  console.error(
    'FATAL CONFIG: App Setting ' +
      API_KEY_SETTING +
      ' is not set. /api/enrich will answer 502 until it is configured.',
  );
}

/**
 * Builds a JSON error response in the shape the app expects.
 *
 * @param {number} status HTTP status code
 * @param {string} error machine-readable error code
 * @param {string} message human-readable text, never containing user data
 * @param {Record<string, string>} [headers] extra response headers
 * @returns {import('@azure/functions').HttpResponseInit}
 */
function errorResponse(status, error, message, headers = {}) {
  return { status, headers, jsonBody: { error, message } };
}

/**
 * POST /api/enrich
 *
 * Translates Roman-script task text into plain English and classifies it into
 * the app's frozen category set.
 *
 * @param {import('@azure/functions').HttpRequest} request
 * @param {import('@azure/functions').InvocationContext} context
 * @returns {Promise<import('@azure/functions').HttpResponseInit>}
 */
export async function enrichHandler(request, context) {
  const startedAt = Date.now();

  /**
   * Emits one structured log line. The task text and the API key are never
   * logged: the text is user personal data.
   *
   * @param {string} outcome short outcome tag
   * @param {number} status HTTP status returned
   * @param {Record<string, unknown>} [extra] additional non-sensitive fields
   * @returns {void}
   */
  const logOutcome = (outcome, status, extra = {}) => {
    context.log(
      JSON.stringify({
        event: 'enrich',
        outcome,
        status,
        durationMs: Date.now() - startedAt,
        ...extra,
      }),
    );
  };

  const decision = limiter.check(clientKeyFromHeaders(request.headers));
  if (!decision.allowed) {
    logOutcome('rate_limited', 429, { scope: decision.scope });
    return errorResponse(429, 'rate_limited', 'Too many requests. Try again shortly.', {
      'Retry-After': String(decision.retryAfterSeconds),
    });
  }

  /** @type {unknown} */
  let body;
  try {
    body = await request.json();
  } catch {
    logOutcome('invalid_json', 400);
    return errorResponse(400, 'invalid_input', 'Request body must be valid JSON.');
  }

  const validation = validateEnrichRequest(body);
  if (!validation.ok) {
    logOutcome('invalid_input', 400);
    return errorResponse(400, validation.error, validation.message);
  }

  try {
    const result = await enrichTask(getClient(), validation.text);
    logOutcome('ok', 200, {
      inputChars: validation.text.length,
      category: result.enrichment.category,
      categoryCoerced: result.categoryCoerced,
      inputTokens: result.inputTokens,
      outputTokens: result.outputTokens,
    });
    return { status: 200, jsonBody: result.enrichment };
  } catch (error) {
    const configError = error instanceof ConfigError;
    const cause = error instanceof Error ? error.cause : undefined;
    logOutcome(configError ? 'not_configured' : 'upstream_error', 502, {
      reason: error instanceof Error ? error.message : 'unknown',
      causeName: cause instanceof Error ? cause.name : undefined,
      causeStatus: cause && typeof cause === 'object' ? cause.status : undefined,
    });
    return errorResponse(
      502,
      'upstream_error',
      configError
        ? 'The service is not configured to reach the model.'
        : 'The language model could not be reached. Try again.',
    );
  }
}

app.http('enrich', {
  methods: ['POST'],
  authLevel: 'function',
  route: 'enrich',
  handler: enrichHandler,
});
