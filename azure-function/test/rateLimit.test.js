import assert from 'node:assert/strict';
import test from 'node:test';
import { SlidingWindowRateLimiter, clientKeyFromHeaders } from '../src/lib/rateLimit.js';

/**
 * Builds a headers stub with the same surface the function uses.
 *
 * @param {Record<string, string>} values header name to value
 * @returns {{ get(name: string): string|null }}
 */
function headers(values) {
  return { get: (name) => values[name.toLowerCase()] ?? null };
}

test('allows exactly the per-key limit inside one window', () => {
  const limiter = new SlidingWindowRateLimiter({ windowMs: 60_000, perKeyLimit: 3 });
  const now = 1_000_000;

  for (let i = 0; i < 3; i += 1) {
    assert.equal(limiter.check('1.1.1.1', now + i).allowed, true, 'request ' + i);
  }
  const denial = limiter.check('1.1.1.1', now + 3);
  assert.equal(denial.allowed, false);
  assert.equal(denial.scope, 'key');
});

test('limits are tracked per key', () => {
  const limiter = new SlidingWindowRateLimiter({ perKeyLimit: 1, globalLimit: 100 });
  assert.equal(limiter.check('a', 0).allowed, true);
  assert.equal(limiter.check('a', 1).allowed, false);
  assert.equal(limiter.check('b', 2).allowed, true);
});

test('the window slides rather than resetting on a fixed boundary', () => {
  const limiter = new SlidingWindowRateLimiter({ windowMs: 1_000, perKeyLimit: 2 });
  assert.equal(limiter.check('a', 0).allowed, true);
  assert.equal(limiter.check('a', 600).allowed, true);
  assert.equal(limiter.check('a', 900).allowed, false);

  // The hit at t=0 has aged out by t=1001, freeing exactly one slot.
  assert.equal(limiter.check('a', 1_001).allowed, true);
  assert.equal(limiter.check('a', 1_002).allowed, false);
});

test('rejected attempts are not recorded, so a backing-off caller recovers', () => {
  const limiter = new SlidingWindowRateLimiter({ windowMs: 1_000, perKeyLimit: 1 });
  assert.equal(limiter.check('a', 0).allowed, true);
  for (let t = 100; t < 1_000; t += 100) {
    assert.equal(limiter.check('a', t).allowed, false);
  }
  assert.equal(limiter.check('a', 1_001).allowed, true);
});

test('the global cap trips even when no single key is over its limit', () => {
  const limiter = new SlidingWindowRateLimiter({ windowMs: 60_000, perKeyLimit: 10, globalLimit: 3 });
  assert.equal(limiter.check('a', 0).allowed, true);
  assert.equal(limiter.check('b', 1).allowed, true);
  assert.equal(limiter.check('c', 2).allowed, true);

  const denial = limiter.check('d', 3);
  assert.equal(denial.allowed, false);
  assert.equal(denial.scope, 'global');
});

test('retryAfterSeconds is at least one second and never exceeds the window', () => {
  const limiter = new SlidingWindowRateLimiter({ windowMs: 60_000, perKeyLimit: 1 });
  limiter.check('a', 0);

  const immediate = limiter.check('a', 1);
  assert.equal(immediate.retryAfterSeconds, 60);

  const nearlyFree = limiter.check('a', 59_999);
  assert.equal(nearlyFree.retryAfterSeconds, 1);
});

test('tracked keys are capped so a spoofed header cannot grow memory unbounded', () => {
  const limiter = new SlidingWindowRateLimiter({ perKeyLimit: 5, globalLimit: 10_000, maxTrackedKeys: 10 });
  for (let i = 0; i < 500; i += 1) {
    limiter.check('key-' + i, i);
  }
  assert.ok(limiter.hitsByKey.size <= 10, 'tracked keys: ' + limiter.hitsByKey.size);
});

test('expired keys are dropped during eviction', () => {
  const limiter = new SlidingWindowRateLimiter({ windowMs: 100, perKeyLimit: 5, maxTrackedKeys: 2 });
  limiter.check('old-a', 0);
  limiter.check('old-b', 1);
  limiter.check('fresh', 500);
  assert.equal(limiter.hitsByKey.has('old-a'), false);
  assert.equal(limiter.hitsByKey.has('fresh'), true);
});

test('client key comes from x-forwarded-for with the port stripped', () => {
  assert.equal(clientKeyFromHeaders(headers({ 'x-forwarded-for': '203.0.113.7:51234' })), '203.0.113.7');
  assert.equal(clientKeyFromHeaders(headers({ 'x-forwarded-for': '203.0.113.7' })), '203.0.113.7');
});

test('only the first forwarded hop is used as the key', () => {
  const value = '203.0.113.7:443, 70.37.0.1:80, 10.0.0.1';
  assert.equal(clientKeyFromHeaders(headers({ 'x-forwarded-for': value })), '203.0.113.7');
});

test('IPv6 forwarded addresses keep their address part', () => {
  const bracketed = '[2001:db8::1]:51234';
  assert.equal(clientKeyFromHeaders(headers({ 'x-forwarded-for': bracketed })), '2001:db8::1');
  assert.equal(clientKeyFromHeaders(headers({ 'x-forwarded-for': '2001:db8::1' })), '2001:db8::1');
});

test('a missing or empty forwarded header falls back to a shared key', () => {
  assert.equal(clientKeyFromHeaders(headers({})), 'unknown');
  assert.equal(clientKeyFromHeaders(headers({ 'x-forwarded-for': '   ' })), 'unknown');
});
