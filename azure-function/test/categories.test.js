import assert from 'node:assert/strict';
import test from 'node:test';
import { CATEGORIES, coerceCategory, coerceConfidence } from '../src/lib/categories.js';

test('category list matches the frozen API contract exactly', () => {
  assert.deepEqual(
    [...CATEGORIES],
    ['WORK', 'PERSONAL', 'HEALTH_FITNESS', 'CODING', 'STUDY', 'SHOPPING', 'FINANCE', 'OTHER'],
  );
});

test('category list is immutable', () => {
  assert.throws(() => {
    CATEGORIES.push('NEW');
  });
});

test('known categories pass through unchanged', () => {
  for (const category of CATEGORIES) {
    assert.deepEqual(coerceCategory(category), { category, coerced: false });
  }
});

test('casing and surrounding whitespace are normalised', () => {
  assert.deepEqual(coerceCategory('  health_fitness '), {
    category: 'HEALTH_FITNESS',
    coerced: false,
  });
  assert.deepEqual(coerceCategory('Work'), { category: 'WORK', coerced: false });
});

test('unknown category values collapse to OTHER', () => {
  for (const value of ['GROCERIES', 'work stuff', '', 'OTHER_THING']) {
    assert.deepEqual(coerceCategory(value), { category: 'OTHER', coerced: true });
  }
});

test('non-string category values collapse to OTHER', () => {
  for (const value of [undefined, null, 7, {}, ['WORK']]) {
    assert.deepEqual(coerceCategory(value), { category: 'OTHER', coerced: true });
  }
});

test('confidence is clamped into [0, 1] and rounded to two decimals', () => {
  assert.equal(coerceConfidence(0.923456), 0.92);
  assert.equal(coerceConfidence(1.7), 1);
  assert.equal(coerceConfidence(-3), 0);
  assert.equal(coerceConfidence(0), 0);
});

test('unusable confidence values return null', () => {
  for (const value of [undefined, null, '0.9', Number.NaN, Number.POSITIVE_INFINITY, {}]) {
    assert.equal(coerceConfidence(value), null);
  }
});
