import Anthropic from '@anthropic-ai/sdk';
import {
  COERCED_CONFIDENCE_CEILING,
  coerceCategory,
  coerceConfidence,
} from './categories.js';
import { ENRICH_TOOL, MAX_TOKENS, MODEL, SYSTEM_PROMPT, TOOL_CHOICE, TOOL_NAME } from './prompt.js';

/** App Setting that must hold the Anthropic API key. */
export const API_KEY_SETTING = 'ANTHROPIC_API_KEY';

/** Upper bound on the English text handed back to the app. */
const MAX_ENGLISH_TEXT_LENGTH = 1_000;

/** Wall-clock budget for a single Claude call, in milliseconds. */
const REQUEST_TIMEOUT_MS = 15_000;

/** One retry only: the caller is a phone waiting on a foreground action. */
const MAX_RETRIES = 1;

/** Raised when the model call fails or its output cannot be used. */
export class UpstreamError extends Error {
  /**
   * @param {string} message safe, non-sensitive description
   * @param {object} [options]
   * @param {unknown} [options.cause]
   */
  constructor(message, options = {}) {
    super(message, options);
    this.name = 'UpstreamError';
  }
}

/** Raised when required configuration is absent. */
export class ConfigError extends Error {
  /** @param {string} message */
  constructor(message) {
    super(message);
    this.name = 'ConfigError';
  }
}

/** @type {Anthropic|null} */
let cachedClient = null;

/**
 * Reports whether the API key App Setting is present. Used at module load to
 * surface a misconfigured deployment loudly instead of failing per request.
 *
 * @returns {boolean}
 */
export function isConfigured() {
  const key = process.env[API_KEY_SETTING];
  return typeof key === 'string' && key.trim().length > 0;
}

/**
 * Returns a process-wide Anthropic client, created on first use so the
 * connection pool is reused across invocations of a warm worker.
 *
 * @returns {Anthropic}
 * @throws {ConfigError} when the API key App Setting is missing or empty
 */
export function getClient() {
  if (!isConfigured()) {
    throw new ConfigError(
      'App Setting ' + API_KEY_SETTING + ' is missing or empty; the function cannot call the model.',
    );
  }
  cachedClient ??= new Anthropic({
    apiKey: process.env[API_KEY_SETTING],
    timeout: REQUEST_TIMEOUT_MS,
    maxRetries: MAX_RETRIES,
  });
  return cachedClient;
}

/** Discards the cached client. Exposed for tests. */
export function resetClient() {
  cachedClient = null;
}

/**
 * @typedef {object} Enrichment
 * @property {string} englishText plain-English rendering of the task
 * @property {string} category a member of the frozen category set
 * @property {number} confidence model certainty, clamped to [0, 1]
 */

/**
 * @typedef {object} EnrichResult
 * @property {Enrichment} enrichment payload to return to the app
 * @property {boolean} categoryCoerced whether the model's category was
 *   off-contract and had to be replaced by the fallback
 * @property {number} inputTokens tokens billed on the request
 * @property {number} outputTokens tokens billed on the response
 */

/**
 * Sends the sanitised task text to Claude and returns the structured result.
 *
 * The text is passed as the whole user message, never spliced into the
 * system prompt, so it cannot alter the instructions.
 *
 * @param {Pick<Anthropic, 'messages'>} client Anthropic client or test double
 * @param {string} text sanitised task text
 * @returns {Promise<EnrichResult>}
 * @throws {UpstreamError} when the call fails or the output is unusable
 */
export async function enrichTask(client, text) {
  let message;
  try {
    message = await client.messages.create({
      model: MODEL,
      max_tokens: MAX_TOKENS,
      system: SYSTEM_PROMPT,
      tools: [ENRICH_TOOL],
      tool_choice: TOOL_CHOICE,
      messages: [{ role: 'user', content: text }],
    });
  } catch (cause) {
    throw new UpstreamError('The model request failed.', { cause });
  }
  return parseEnrichResponse(message);
}

/**
 * Extracts the forced tool call from a Messages API response and coerces it
 * onto the frozen contract. Free-form prose is never accepted.
 *
 * @param {unknown} message raw Messages API response
 * @returns {EnrichResult}
 * @throws {UpstreamError} when no usable tool call is present
 */
export function parseEnrichResponse(message) {
  const content = /** @type {{ content?: unknown, usage?: unknown }} */ (message ?? {}).content;
  if (!Array.isArray(content)) {
    throw new UpstreamError('The model response had no content blocks.');
  }

  const toolUse = content.find(
    (block) => block && block.type === 'tool_use' && block.name === TOOL_NAME,
  );
  if (!toolUse || toolUse.input === null || typeof toolUse.input !== 'object') {
    throw new UpstreamError('The model did not return the required structured output.');
  }

  const { englishText, category, confidence } = toolUse.input;
  const cleanedText = typeof englishText === 'string' ? englishText.trim() : '';
  if (cleanedText.length === 0) {
    throw new UpstreamError('The model returned an empty englishText.');
  }

  const clampedConfidence = coerceConfidence(confidence);
  if (clampedConfidence === null) {
    throw new UpstreamError('The model returned a non-numeric confidence.');
  }

  const { category: safeCategory, coerced } = coerceCategory(category);
  const usage = /** @type {{ usage?: { input_tokens?: number, output_tokens?: number } }} */ (
    message
  ).usage;

  return {
    enrichment: {
      englishText: cleanedText.slice(0, MAX_ENGLISH_TEXT_LENGTH),
      category: safeCategory,
      confidence: coerced
        ? Math.min(clampedConfidence, COERCED_CONFIDENCE_CEILING)
        : clampedConfidence,
    },
    categoryCoerced: coerced,
    inputTokens: usage?.input_tokens ?? 0,
    outputTokens: usage?.output_tokens ?? 0,
  };
}
