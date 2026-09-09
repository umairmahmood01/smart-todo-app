/**
 * The closed set of task categories. This list mirrors the app-side Kotlin
 * enum exactly; changing it is a breaking API contract change and requires a
 * matching app release.
 *
 * @type {readonly string[]}
 */
export const CATEGORIES = Object.freeze([
  'WORK',
  'PERSONAL',
  'HEALTH_FITNESS',
  'CODING',
  'STUDY',
  'SHOPPING',
  'FINANCE',
  'OTHER',
]);

/** Category used whenever the model returns anything outside {@link CATEGORIES}. */
export const FALLBACK_CATEGORY = 'OTHER';

/**
 * Confidence ceiling applied when the model's category had to be coerced to
 * the fallback. A high confidence reported alongside an off-contract category
 * is not trustworthy, so it is capped before being handed to the client.
 */
export const COERCED_CONFIDENCE_CEILING = 0.5;

const CATEGORY_SET = new Set(CATEGORIES);

/**
 * Maps an arbitrary model-supplied value onto the closed category set.
 * Matching is case-insensitive and tolerant of surrounding whitespace; every
 * unrecognised value collapses to {@link FALLBACK_CATEGORY}.
 *
 * @param {unknown} value raw value returned by the model
 * @returns {{ category: string, coerced: boolean }} the safe category, and
 *   whether the input had to be replaced by the fallback
 */
export function coerceCategory(value) {
  if (typeof value === 'string') {
    const normalised = value.trim().toUpperCase();
    if (CATEGORY_SET.has(normalised)) {
      return { category: normalised, coerced: false };
    }
  }
  return { category: FALLBACK_CATEGORY, coerced: true };
}

/**
 * Clamps a model-supplied confidence into [0, 1] and rounds it to two
 * decimals. Returns null for values that are not finite numbers, which the
 * caller treats as an unparseable upstream response.
 *
 * @param {unknown} value raw value returned by the model
 * @returns {number|null} clamped confidence, or null when unusable
 */
export function coerceConfidence(value) {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    return null;
  }
  const clamped = Math.min(1, Math.max(0, value));
  return Math.round(clamped * 100) / 100;
}
