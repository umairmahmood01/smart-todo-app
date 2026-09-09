import OpenAI from 'openai';
import {
  COERCED_CONFIDENCE_CEILING,
  coerceCategory,
  coerceConfidence,
} from './categories.js';
import { ENRICH_TOOL, MAX_TOKENS, SYSTEM_PROMPT, TOOL_CHOICE, TOOL_NAME } from './prompt.js';

/**
 * App Setting that must hold the API key for the OpenAI-compatible endpoint.
 * Provider-neutral: the value is a DeepInfra key today, but switching hosts
 * must not require renaming settings.
 */
export const API_KEY_SETTING = 'LLM_API_KEY';

/** App Setting holding the OpenAI-compatible base URL. */
export const BASE_URL_SETTING = 'LLM_BASE_URL';

/** App Setting holding the model id. */
export const MODEL_SETTING = 'LLM_MODEL';

/**
 * DeepInfra's OpenAI-compatible base URL. Confirmed against the live
 * endpoint: a GET of {DEFAULT_BASE_URL}/models returns an OpenAI-shaped
 * model list containing the default model below.
 */
export const DEFAULT_BASE_URL = 'https://api.deepinfra.com/v1/openai';

/**
 * Haiku is the default deliberately: this is a short translate-and-classify
 * task with a tiny output, so the cheapest capable model in the family is the
 * correct trade-off. Override with the LLM_MODEL App Setting to A/B another
 * model without a code change; see README.md.
 */
export const DEFAULT_MODEL = 'anthropic/claude-haiku-4-5';

/** Upper bound on the English text handed back to the app. */
const MAX_ENGLISH_TEXT_LENGTH = 1_000;

/** Wall-clock budget for a single model call, in milliseconds. */
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

/** @type {OpenAI|null} */
let cachedClient = null;

/**
 * Reads a non-empty string App Setting, falling back when unset or blank.
 *
 * @param {string} name App Setting name
 * @param {string} fallback value used when the setting is absent or blank
 * @returns {string}
 */
function stringSetting(name, fallback) {
  const value = process.env[name];
  return typeof value === 'string' && value.trim().length > 0 ? value.trim() : fallback;
}

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
 * Base URL of the OpenAI-compatible endpoint currently configured.
 *
 * @returns {string}
 */
export function resolveBaseUrl() {
  return stringSetting(BASE_URL_SETTING, DEFAULT_BASE_URL);
}

/**
 * Model id currently configured. Read per call, so switching models needs a
 * setting change and a restart, never a code change.
 *
 * @returns {string}
 */
export function resolveModel() {
  return stringSetting(MODEL_SETTING, DEFAULT_MODEL);
}

/**
 * Returns a process-wide OpenAI-compatible client, created on first use so
 * the connection pool is reused across invocations of a warm worker.
 *
 * @returns {OpenAI}
 * @throws {ConfigError} when the API key App Setting is missing or empty
 */
export function getClient() {
  if (!isConfigured()) {
    throw new ConfigError(
      'App Setting ' + API_KEY_SETTING + ' is missing or empty; the function cannot call the model.',
    );
  }
  cachedClient ??= new OpenAI({
    apiKey: process.env[API_KEY_SETTING],
    baseURL: resolveBaseUrl(),
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
 * Sends the sanitised task text to the model and returns the structured
 * result.
 *
 * The text is passed as the whole user message, never spliced into the
 * system prompt, so it cannot alter the instructions.
 *
 * @param {Pick<OpenAI, 'chat'>} client OpenAI-compatible client or test double
 * @param {string} text sanitised task text
 * @returns {Promise<EnrichResult>}
 * @throws {UpstreamError} when the call fails or the output is unusable
 */
export async function enrichTask(client, text) {
  let completion;
  try {
    completion = await client.chat.completions.create({
      model: resolveModel(),
      max_tokens: MAX_TOKENS,
      tools: [ENRICH_TOOL],
      tool_choice: TOOL_CHOICE,
      messages: [
        { role: 'system', content: SYSTEM_PROMPT },
        { role: 'user', content: text },
      ],
    });
  } catch (cause) {
    throw new UpstreamError('The model request failed.', { cause });
  }
  return parseEnrichResponse(completion);
}

/**
 * Extracts the structured arguments from a chat completion and coerces them
 * onto the frozen contract.
 *
 * The happy path is the forced tool call, whose arguments arrive as a JSON
 * string that must be parsed. Not every model behind an OpenAI-compatible
 * endpoint honours tool_choice, so JSON in message.content is accepted as a
 * fallback. Anything else, unparseable JSON included, is an upstream error
 * rather than a guess.
 *
 * @param {unknown} completion raw chat completion response
 * @returns {EnrichResult}
 * @throws {UpstreamError} when no usable structured output is present
 */
export function parseEnrichResponse(completion) {
  const choices = /** @type {{ choices?: unknown }} */ (completion ?? {}).choices;
  if (!Array.isArray(choices) || choices.length === 0) {
    throw new UpstreamError('The model response had no choices.');
  }

  const message = choices[0]?.message;
  if (message === null || message === undefined || typeof message !== 'object') {
    throw new UpstreamError('The model response had no assistant message.');
  }

  const args = parseJsonObject(extractArgumentsJson(message));
  const { englishText, category, confidence } = args;
  const cleanedText = typeof englishText === 'string' ? englishText.trim() : '';
  if (cleanedText.length === 0) {
    throw new UpstreamError('The model returned an empty englishText.');
  }

  const clampedConfidence = coerceConfidence(confidence);
  if (clampedConfidence === null) {
    throw new UpstreamError('The model returned a non-numeric confidence.');
  }

  const { category: safeCategory, coerced } = coerceCategory(category);
  const usage = /** @type {{ usage?: { prompt_tokens?: number, completion_tokens?: number } }} */ (
    completion
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
    inputTokens: usage?.prompt_tokens ?? 0,
    outputTokens: usage?.completion_tokens ?? 0,
  };
}

/**
 * Returns the JSON text carrying the structured output: the forced tool
 * call's arguments when present, otherwise the assistant content.
 *
 * @param {{ tool_calls?: unknown, content?: unknown }} message assistant message
 * @returns {string} raw JSON text, not yet parsed
 * @throws {UpstreamError} when neither source is usable
 */
function extractArgumentsJson(message) {
  const toolCalls = message.tool_calls;
  if (Array.isArray(toolCalls) && toolCalls.length > 0) {
    const call = toolCalls.find(
      (entry) => entry !== null && typeof entry === 'object' && entry.function?.name === TOOL_NAME,
    );
    if (call === undefined) {
      throw new UpstreamError('The model called a tool other than ' + TOOL_NAME + '.');
    }
    const args = call.function.arguments;
    if (typeof args !== 'string' || args.trim().length === 0) {
      throw new UpstreamError('The tool call carried no arguments.');
    }
    return args;
  }

  const content = message.content;
  if (typeof content !== 'string' || content.trim().length === 0) {
    throw new UpstreamError('The model did not return the required structured output.');
  }
  return content;
}

/**
 * Parses JSON into a plain object, converting every failure into an upstream
 * error. A malformed payload is never repaired or guessed at.
 *
 * A model that ignores tool_choice often wraps its JSON in a Markdown fence
 * or a sentence, so one narrow retry on the span between the outermost
 * braces is attempted. That span still has to parse and still has to satisfy
 * the same validation as a tool call.
 *
 * @param {string} raw JSON text
 * @returns {Record<string, unknown>}
 * @throws {UpstreamError} when the text is not a JSON object
 */
function parseJsonObject(raw) {
  let parsed;
  try {
    parsed = JSON.parse(raw);
  } catch (cause) {
    parsed = parseBracedSpan(raw, cause);
  }
  if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new UpstreamError('The structured output was not a JSON object.');
  }
  return parsed;
}

/**
 * Parses the span from the first opening brace to the last closing brace.
 *
 * @param {string} raw text that failed a direct parse
 * @param {unknown} originalCause the failure from the direct parse
 * @returns {unknown} parsed value
 * @throws {UpstreamError} when there is no such span, or it does not parse
 */
function parseBracedSpan(raw, originalCause) {
  const start = raw.indexOf('{');
  const end = raw.lastIndexOf('}');
  if (start === -1 || end <= start) {
    throw new UpstreamError('The structured output was not valid JSON.', { cause: originalCause });
  }
  try {
    return JSON.parse(raw.slice(start, end + 1));
  } catch (cause) {
    throw new UpstreamError('The structured output was not valid JSON.', { cause });
  }
}
