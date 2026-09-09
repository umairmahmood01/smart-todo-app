/**
 * In-memory sliding-window rate limiter.
 *
 * State lives in the worker process only. It is therefore per-instance: it
 * does not survive a cold start and it is not shared across scaled-out
 * instances, so the effective limit is (configured limit x live instances).
 * That is acceptable as a cheap abuse brake for a semi-public function key,
 * but a durable store (Redis or Azure Table Storage) is the production
 * answer. See README.md.
 */
export class SlidingWindowRateLimiter {
  /**
   * @param {object} [options]
   * @param {number} [options.windowMs] length of the sliding window
   * @param {number} [options.perKeyLimit] allowed requests per key per window
   * @param {number} [options.globalLimit] allowed requests across all keys
   * @param {number} [options.maxTrackedKeys] cap on distinct tracked keys, so
   *   a spoofed forwarding header cannot grow the map without bound
   */
  constructor({
    windowMs = 60_000,
    perKeyLimit = 30,
    globalLimit = 300,
    maxTrackedKeys = 5_000,
  } = {}) {
    this.windowMs = windowMs;
    this.perKeyLimit = perKeyLimit;
    this.globalLimit = globalLimit;
    this.maxTrackedKeys = maxTrackedKeys;
    /** @type {Map<string, number[]>} key to ascending hit timestamps */
    this.hitsByKey = new Map();
    /** @type {number[]} ascending timestamps of every accepted request */
    this.globalHits = [];
  }

  /**
   * Records an attempt and reports whether it is allowed. Rejected attempts
   * are not recorded, so a caller that backs off recovers after one window.
   *
   * @param {string} key caller identity, normally the client IP
   * @param {number} [now] current epoch millis, injectable for tests
   * @returns {{ allowed: boolean, scope: 'key'|'global'|null, retryAfterSeconds: number }}
   */
  check(key, now = Date.now()) {
    const cutoff = now - this.windowMs;
    prune(this.globalHits, cutoff);

    if (this.globalHits.length >= this.globalLimit) {
      return denied('global', this.globalHits[0], cutoff, this.windowMs);
    }

    const hits = this.hitsByKey.get(key) ?? [];
    prune(hits, cutoff);

    if (hits.length >= this.perKeyLimit) {
      this.hitsByKey.set(key, hits);
      return denied('key', hits[0], cutoff, this.windowMs);
    }

    hits.push(now);
    this.globalHits.push(now);
    // Re-insert so Map iteration order is least-recently-used first.
    this.hitsByKey.delete(key);
    this.hitsByKey.set(key, hits);
    this.evictIfNeeded(cutoff);

    return { allowed: true, scope: null, retryAfterSeconds: 0 };
  }

  /**
   * Drops keys whose windows have fully expired, then the least recently used
   * keys if the map is still over its cap.
   *
   * @param {number} cutoff timestamps at or before this are expired
   * @returns {void}
   */
  evictIfNeeded(cutoff) {
    if (this.hitsByKey.size <= this.maxTrackedKeys) {
      return;
    }
    for (const [key, hits] of this.hitsByKey) {
      prune(hits, cutoff);
      if (hits.length === 0) {
        this.hitsByKey.delete(key);
      }
    }
    while (this.hitsByKey.size > this.maxTrackedKeys) {
      const oldest = this.hitsByKey.keys().next();
      if (oldest.done) {
        break;
      }
      this.hitsByKey.delete(oldest.value);
    }
  }
}

/**
 * Removes leading timestamps at or before the cutoff, in place.
 *
 * @param {number[]} timestamps ascending timestamps
 * @param {number} cutoff
 * @returns {void}
 */
function prune(timestamps, cutoff) {
  let drop = 0;
  while (drop < timestamps.length && timestamps[drop] <= cutoff) {
    drop += 1;
  }
  if (drop > 0) {
    timestamps.splice(0, drop);
  }
}

/**
 * @param {'key'|'global'} scope
 * @param {number} oldestHit
 * @param {number} cutoff
 * @param {number} windowMs
 * @returns {{ allowed: false, scope: 'key'|'global', retryAfterSeconds: number }}
 */
function denied(scope, oldestHit, cutoff, windowMs) {
  const msUntilFree = oldestHit - cutoff;
  const retryAfterSeconds = Math.max(1, Math.ceil(msUntilFree / 1_000));
  return { allowed: false, scope, retryAfterSeconds: Math.min(retryAfterSeconds, Math.ceil(windowMs / 1_000)) };
}

/**
 * Extracts a rate-limit key from request headers. Azure fronts the worker
 * with a load balancer, so the socket address is useless; x-forwarded-for is
 * used instead. It is caller-controllable, hence the tracked-key cap above.
 *
 * @param {{ get(name: string): string|null }} headers request headers
 * @returns {string} client identity, or 'unknown' when no header is present
 */
export function clientKeyFromHeaders(headers) {
  const forwarded = headers.get('x-forwarded-for');
  if (!forwarded) {
    return 'unknown';
  }
  const first = forwarded.split(',')[0].trim();
  if (first.length === 0) {
    return 'unknown';
  }
  return stripPort(first);
}

/**
 * Strips the ":port" or "[v6]:port" suffix Azure appends to forwarded
 * addresses, leaving a stable per-client key.
 *
 * @param {string} address
 * @returns {string}
 */
function stripPort(address) {
  if (address.startsWith('[')) {
    const close = address.indexOf(']');
    return close === -1 ? address : address.slice(1, close);
  }
  const parts = address.split(':');
  // More than two parts means a bare IPv6 address, which has no port suffix.
  return parts.length === 2 ? parts[0] : address;
}
