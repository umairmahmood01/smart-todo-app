import assert from 'node:assert/strict';
import test from 'node:test';
import { API_KEY_SETTING } from '../src/lib/claude.js';

// The handler module reads its limits and configuration once, at import time,
// so both are set before the dynamic import below.
process.env.ENRICH_RATE_LIMIT_PER_MINUTE = '3';
process.env.ENRICH_GLOBAL_RATE_LIMIT_PER_MINUTE = '100';
delete process.env[API_KEY_SETTING];

const { enrichHandler } = await import('../src/functions/enrich.js');

/**
 * Minimal stand-in for HttpRequest covering the surface the handler uses.
 *
 * @param {object} options
 * @param {unknown} [options.body] value resolved by request.json()
 * @param {boolean} [options.invalidJson] make request.json() reject
 * @param {string} [options.ip] value of the x-forwarded-for header
 * @returns {object}
 */
function request({ body, invalidJson = false, ip = '198.51.100.1' }) {
  return {
    headers: { get: (name) => (name.toLowerCase() === 'x-forwarded-for' ? ip : null) },
    json: async () => {
      if (invalidJson) {
        throw new SyntaxError('Unexpected end of JSON input');
      }
      return body;
    },
  };
}

/**
 * @returns {{ log: Function, lines: string[] }} invocation context stub
 */
function context() {
  /** @type {string[]} */
  const lines = [];
  return { lines, log: (line) => lines.push(String(line)) };
}

test('a body that is not valid JSON returns 400 invalid_input', async () => {
  const ctx = context();
  const response = await enrichHandler(request({ invalidJson: true, ip: '198.51.100.10' }), ctx);

  assert.equal(response.status, 400);
  assert.equal(response.jsonBody.error, 'invalid_input');
  assert.equal(typeof response.jsonBody.message, 'string');
});

test('missing text returns 400 invalid_input', async () => {
  const response = await enrichHandler(request({ body: {}, ip: '198.51.100.11' }), context());
  assert.equal(response.status, 400);
  assert.equal(response.jsonBody.error, 'invalid_input');
});

test('over-long text returns 400 invalid_input', async () => {
  const response = await enrichHandler(
    request({ body: { text: 'a'.repeat(501) }, ip: '198.51.100.12' }),
    context(),
  );
  assert.equal(response.status, 400);
  assert.equal(response.jsonBody.error, 'invalid_input');
});

test('a missing API key setting returns 502 upstream_error without leaking config', async () => {
  const response = await enrichHandler(
    request({ body: { text: 'kal client ko presentation bhejni hai' }, ip: '198.51.100.13' }),
    context(),
  );

  assert.equal(response.status, 502);
  assert.equal(response.jsonBody.error, 'upstream_error');
  assert.equal(response.jsonBody.message.includes(API_KEY_SETTING), false);
});

test('logs record the outcome and duration but never the task text', async () => {
  const ctx = context();
  const secret = 'ammi ko doctor ke paas le jana hai';
  await enrichHandler(request({ body: { text: secret }, ip: '198.51.100.14' }), ctx);

  assert.equal(ctx.lines.length, 1);
  const entry = JSON.parse(ctx.lines[0]);
  assert.equal(entry.event, 'enrich');
  assert.equal(entry.status, 502);
  assert.equal(typeof entry.durationMs, 'number');
  assert.equal(ctx.lines[0].includes(secret), false);
  assert.equal(ctx.lines[0].includes('doctor'), false);
});

test('exceeding the per-caller limit returns 429 rate_limited with Retry-After', async () => {
  const ip = '198.51.100.99';
  for (let i = 0; i < 3; i += 1) {
    const allowed = await enrichHandler(request({ body: {}, ip }), context());
    assert.equal(allowed.status, 400, 'request ' + i + ' should reach validation');
  }

  const response = await enrichHandler(request({ body: {}, ip }), context());
  assert.equal(response.status, 429);
  assert.equal(response.jsonBody.error, 'rate_limited');
  assert.equal(Number.parseInt(response.headers['Retry-After'], 10) > 0, true);

  // A different caller is unaffected.
  const other = await enrichHandler(request({ body: {}, ip: '198.51.100.98' }), context());
  assert.equal(other.status, 400);
});
