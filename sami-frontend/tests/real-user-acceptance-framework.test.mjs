import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')

test('real-user acceptance framework requires persisted cross-screen evidence', async () => {
  const skill = await readFile(path.join(root, '.agents/skills/real-user-acceptance/SKILL.md'), 'utf8')
  const governance = await readFile(path.join(root, 'docs/agent-system/GOVERNANCE.md'), 'utf8')
  for (const marker of [
    'ACTION → VISIBLE RESULT → BUSINESS RESULT → PERSISTED RESULT → CROSS-SCREEN CONSISTENCY',
    'refresh/reopen',
    'duplicate-submit',
    'NOT TESTED',
    'BLOCKED BY HARNESS',
    'console/network',
  ]) {
    assert.ok(skill.includes(marker) || governance.includes(marker), `missing QA gate marker: ${marker}`)
  }
})
