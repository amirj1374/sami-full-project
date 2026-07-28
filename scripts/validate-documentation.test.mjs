import assert from 'node:assert/strict'
import { mkdirSync, mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import test from 'node:test'
import { validateRepository } from './validate-documentation.mjs'

function fixture(tasks, link = '') {
  const root = mkdtempSync(join(tmpdir(), 'sami-doc-validation-'))
  mkdirSync(join(root, 'docs'), { recursive: true })
  mkdirSync(join(root, 'PROJECT_BACKLOG', 'high'), { recursive: true })
  writeFileSync(join(root, 'README.md'), link)
  writeFileSync(join(root, 'docs', 'README.md'), '# Docs\n')
  writeFileSync(join(root, 'PROJECT_BACKLOG', 'BACKLOG_INDEX.md'),
    tasks.map((task) => `| ${task.id} | title |`).join('\n'))
  for (const task of tasks) {
    writeFileSync(join(root, 'PROJECT_BACKLOG', 'high', `${task.file ?? task.id}.md`), `---
id: ${task.id}
title: ${task.title ?? task.id}
status: ${task.status ?? 'ready'}
priority: ${task.priority ?? 'high'}
depends_on: [${(task.dependencies ?? []).join(', ')}]
source_refs: [docs]
---
# ${task.id}
`)
  }
  return root
}

test('accepts a valid backlog', () => {
  assert.deepEqual(validateRepository(fixture([{ id: 'HIGH-001' }])), [])
})

test('detects duplicate IDs', () => {
  const errors = validateRepository(fixture([
    { id: 'HIGH-001', file: 'one' },
    { id: 'HIGH-001', file: 'two', title: 'two' },
  ]))
  assert.ok(errors.some((error) => error.includes('duplicate task id')))
})

test('detects missing dependencies', () => {
  const errors = validateRepository(fixture([
    { id: 'HIGH-001', dependencies: ['HIGH-999'] },
  ]))
  assert.ok(errors.some((error) => error.includes('missing dependency')))
})

test('detects dependency cycles', () => {
  const errors = validateRepository(fixture([
    { id: 'HIGH-001', dependencies: ['HIGH-002'] },
    { id: 'HIGH-002', dependencies: ['HIGH-001'] },
  ]))
  assert.ok(errors.some((error) => error.includes('dependency cycle')))
})

test('detects broken links and invalid enums', () => {
  const errors = validateRepository(fixture([
    { id: 'HIGH-001', priority: 'urgent', status: 'finished' },
  ], '[missing](docs/missing.md)'))
  assert.ok(errors.some((error) => error.includes('broken link')))
  assert.ok(errors.some((error) => error.includes('invalid priority')))
  assert.ok(errors.some((error) => error.includes('invalid status')))
})
