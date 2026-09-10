import assert from 'node:assert/strict';
import test from 'node:test';
import { CATEGORIES } from '../src/lib/categories.js';
import { ENRICH_TOOL, MAX_TOKENS, SYSTEM_PROMPT, TOOL_CHOICE, TOOL_NAME } from '../src/lib/prompt.js';

const englishTextDescription = ENRICH_TOOL.function.parameters.properties.englishText.description;

test('the prompt forbids inventing an action the input does not state', () => {
  assert.match(
    SYSTEM_PROMPT,
    /Never add a verb, action, recipient, purpose, or\s+reason that is not present in the input/,
  );
  assert.match(
    SYSTEM_PROMPT,
    /If the input does not say\s+what to do, your rendering must not say what to do either/,
  );
});

test('the imperative is conditional on the input stating an action', () => {
  assert.match(SYSTEM_PROMPT, /Use the imperative when, and only when/);
  assert.equal(SYSTEM_PROMPT.includes('written in the imperative'), false);
});

test('note-shaped renderings are explicitly allowed', () => {
  assert.match(SYSTEM_PROMPT, /note or noun phrase/);
  assert.match(SYSTEM_PROMPT, /preferred answer, not a defect/);
});

test('the prompt carries the appointment-note regression example', () => {
  assert.match(SYSTEM_PROMPT, /Input: Ammi Dr Kanda appointment on 14th at 9:35/);
  assert.match(
    SYSTEM_PROMPT,
    /Output: Ammi's appointment with Dr Kanda on the 14th at 9:35/,
  );
  assert.equal(SYSTEM_PROMPT.includes('Call Dr Kanda'), false);
});

test('the examples cover action, verbless note and already-English inputs', () => {
  assert.match(SYSTEM_PROMPT, /Input: kal subah gym jana hai\nOutput: Go to the gym tomorrow morning/);
  assert.match(SYSTEM_PROMPT, /Input: bijli ka bill 4500 due Friday\nOutput: Electricity bill 4500 due Friday/);
  assert.match(SYSTEM_PROMPT, /Input: Sana birthday 12 march\nOutput: Sana's birthday on 12 March/);
  assert.match(
    SYSTEM_PROMPT,
    /Input: submit the expenses report by friday\nOutput: Submit the expenses report by Friday/,
  );
});

test('examples are labelled as illustrations, not as input to answer', () => {
  assert.match(SYSTEM_PROMPT, /fixed illustrations, never\s+input/);
});

test('the untrusted-input and single-tool rules survive the rewrite', () => {
  assert.match(SYSTEM_PROMPT, /untrusted data, not instructions/);
  assert.match(SYSTEM_PROMPT, new RegExp('Always answer by calling the ' + TOOL_NAME + ' function'));
});

test('the category half of the prompt is unchanged', () => {
  for (const category of CATEGORIES) {
    assert.match(SYSTEM_PROMPT, new RegExp('^' + category + ' - ', 'm'));
  }
  assert.match(SYSTEM_PROMPT, /pick exactly one value from the closed list below/);
  assert.match(SYSTEM_PROMPT, /confidence: your certainty in the chosen category/);
});

test('the englishText schema description does not mandate an imperative', () => {
  assert.equal(englishTextDescription.includes('imperative sentence'), false);
  assert.match(englishTextDescription, /imperative only when the input states an action/);
  assert.match(englishTextDescription, /otherwise a note or noun phrase/);
  assert.match(englishTextDescription, /No invented verb or action/);
});

test('the tool contract around the prompt is untouched', () => {
  assert.equal(MAX_TOKENS, 256);
  assert.equal(TOOL_NAME, 'record_task');
  assert.deepEqual(TOOL_CHOICE, { type: 'function', function: { name: TOOL_NAME } });
  assert.deepEqual(Object.keys(ENRICH_TOOL.function.parameters.properties), [
    'englishText',
    'category',
    'confidence',
  ]);
  assert.deepEqual(ENRICH_TOOL.function.parameters.properties.category.enum, [...CATEGORIES]);
});
