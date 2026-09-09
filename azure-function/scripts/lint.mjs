/**
 * Dependency-free syntax check: parses every JavaScript source in the
 * function app with "node --check". Catches syntax and ESM/CJS mistakes in
 * files that no test happens to import, without pulling a linter and its
 * transitive tree into a deployed function app.
 */
import { spawnSync } from 'node:child_process';
import { readdirSync } from 'node:fs';
import { join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('..', import.meta.url));
const roots = ['src', 'test', 'scripts'];

/**
 * Lists JavaScript files under a directory, recursively.
 *
 * @param {string} dir absolute directory path
 * @returns {string[]} absolute file paths
 */
function collect(dir) {
  /** @type {string[]} */
  const found = [];
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) {
      found.push(...collect(path));
    } else if (entry.name.endsWith('.js') || entry.name.endsWith('.mjs')) {
      found.push(path);
    }
  }
  return found;
}

const files = roots.flatMap((dir) => collect(join(root, dir)));
let failures = 0;

for (const file of files) {
  const result = spawnSync(process.execPath, ['--check', file], { stdio: 'inherit' });
  if (result.status !== 0) {
    failures += 1;
  }
}

console.log('Checked ' + files.length + ' files, ' + failures + ' with errors.');
if (failures > 0) {
  process.exitCode = 1;
} else {
  console.log(files.map((file) => relative(root, file)).join(', '));
}
