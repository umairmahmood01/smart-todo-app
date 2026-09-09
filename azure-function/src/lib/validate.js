/** Maximum accepted length of the sanitised task text, in UTF-16 code units. */
export const MAX_TEXT_LENGTH = 500;

/**
 * Hard ceiling on the raw string before sanitising. Sanitising is O(n) per
 * character, so oversized payloads are rejected without touching them.
 */
export const MAX_RAW_TEXT_LENGTH = MAX_TEXT_LENGTH * 4;

const SPACE = ' ';

/**
 * Removes control characters and collapses whitespace runs into single
 * spaces. Tabs, newlines and other C0/C1 controls become spaces rather than
 * being deleted, so words never get glued together.
 *
 * Implemented as a single pass over code points instead of a regular
 * expression so the module stays free of escape sequences.
 *
 * @param {string} raw untrusted input
 * @returns {string} sanitised, trimmed text
 */
export function sanitizeText(raw) {
  let out = '';
  let pendingSpace = false;
  for (const ch of raw) {
    const code = ch.codePointAt(0);
    const isControl = code < 0x20 || code === 0x7f || (code >= 0x80 && code <= 0x9f);
    const isSpace = isControl || ch === SPACE;
    if (isSpace) {
      pendingSpace = out.length > 0;
      continue;
    }
    if (pendingSpace) {
      out += SPACE;
      pendingSpace = false;
    }
    out += ch;
  }
  return out;
}

/**
 * @typedef {object} ValidationSuccess
 * @property {true} ok
 * @property {string} text sanitised task text, guaranteed non-empty and
 *   no longer than {@link MAX_TEXT_LENGTH}
 */

/**
 * @typedef {object} ValidationFailure
 * @property {false} ok
 * @property {'invalid_input'} error machine-readable error code
 * @property {string} message human-readable reason, never echoing the input
 */

/**
 * Validates and sanitises a parsed request body against the /api/enrich
 * contract.
 *
 * @param {unknown} body parsed JSON request body
 * @returns {ValidationSuccess|ValidationFailure}
 */
export function validateEnrichRequest(body) {
  if (body === null || typeof body !== 'object' || Array.isArray(body)) {
    return fail('Request body must be a JSON object.');
  }

  const { text } = /** @type {{ text?: unknown }} */ (body);
  if (typeof text !== 'string') {
    return fail('Field "text" is required and must be a string.');
  }
  if (text.length > MAX_RAW_TEXT_LENGTH) {
    return fail('Field "text" must be at most ' + MAX_TEXT_LENGTH + ' characters.');
  }

  const sanitized = sanitizeText(text);
  if (sanitized.length === 0) {
    return fail('Field "text" must not be empty.');
  }
  if (sanitized.length > MAX_TEXT_LENGTH) {
    return fail('Field "text" must be at most ' + MAX_TEXT_LENGTH + ' characters.');
  }

  return { ok: true, text: sanitized };
}

/**
 * @param {string} message
 * @returns {ValidationFailure}
 */
function fail(message) {
  return { ok: false, error: 'invalid_input', message };
}
