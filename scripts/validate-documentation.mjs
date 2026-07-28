import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { dirname, join, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const PRIORITIES = new Set(['critical', 'high', 'medium', 'low'])
const STATUSES = new Set([
  'proposed', 'needs-decision', 'ready', 'in-progress',
  'blocked', 'done', 'cancelled',
])

function filesUnder(directory, predicate) {
  if (!existsSync(directory)) return []
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = join(directory, entry.name)
    return entry.isDirectory() ? filesUnder(path, predicate) : predicate(path) ? [path] : []
  })
}

function frontMatter(text) {
  const match = text.match(/^---\r?\n([\s\S]*?)\r?\n---/)
  if (!match) return {}
  return Object.fromEntries(match[1].split(/\r?\n/).flatMap((line) => {
    const separator = line.indexOf(':')
    return separator < 0 ? [] : [[line.slice(0, separator).trim(), line.slice(separator + 1).trim()]]
  }))
}

function listValue(value = '[]') {
  const match = value.match(/^\[(.*)]$/)
  if (!match || !match[1].trim()) return []
  return match[1].split(',').map((item) => item.trim()).filter(Boolean)
}

export function validateRepository(root) {
  const errors = []
  const markdown = [
    ...filesUnder(join(root, 'docs'), (path) => path.endsWith('.md')),
    ...filesUnder(join(root, 'PROJECT_BACKLOG'), (path) => path.endsWith('.md')),
    join(root, 'README.md'),
  ].filter(existsSync)

  for (const file of markdown) {
    const text = readFileSync(file, 'utf8')
    for (const match of text.matchAll(/\[[^\]]+]\(([^)]+)\)/g)) {
      const target = match[1].split('#')[0]
      if (!target || /^(https?:|mailto:|\/)/.test(target)) continue
      if (!existsSync(resolve(dirname(file), target))) {
        errors.push(`${relative(root, file)}: broken link ${target}`)
      }
    }
  }

  const taskFiles = ['critical', 'high', 'medium', 'low', 'completed']
    .flatMap((directory) => filesUnder(
      join(root, 'PROJECT_BACKLOG', directory),
      (path) => path.endsWith('.md'),
    )).filter((path) => !path.endsWith(join('completed', 'README.md')))
  const tasks = new Map()

  for (const file of taskFiles) {
    const metadata = frontMatter(readFileSync(file, 'utf8'))
    if (!metadata.id) {
      errors.push(`${relative(root, file)}: missing id`)
      continue
    }
    if (tasks.has(metadata.id)) errors.push(`duplicate task id ${metadata.id}`)
    if (!PRIORITIES.has(metadata.priority)) errors.push(`${metadata.id}: invalid priority ${metadata.priority}`)
    if (!STATUSES.has(metadata.status)) errors.push(`${metadata.id}: invalid status ${metadata.status}`)
    tasks.set(metadata.id, {
      file,
      title: metadata.title,
      dependencies: listValue(metadata.depends_on),
      references: listValue(metadata.source_refs),
    })
  }

  for (const [id, task] of tasks) {
    for (const dependency of task.dependencies) {
      if (!tasks.has(dependency)) errors.push(`${id}: missing dependency ${dependency}`)
    }
    for (const source of task.references) {
      if (!existsSync(resolve(root, source))) errors.push(`${id}: missing source_ref ${source}`)
    }
  }

  const state = new Map()
  function visit(id, path = []) {
    if (state.get(id) === 1) {
      errors.push(`dependency cycle ${[...path, id].join(' -> ')}`)
      return
    }
    if (state.get(id) === 2) return
    state.set(id, 1)
    for (const dependency of tasks.get(id)?.dependencies ?? []) visit(dependency, [...path, id])
    state.set(id, 2)
  }
  for (const id of tasks.keys()) visit(id)

  const indexPath = join(root, 'PROJECT_BACKLOG', 'BACKLOG_INDEX.md')
  if (existsSync(indexPath)) {
    const index = readFileSync(indexPath, 'utf8')
    for (const id of tasks.keys()) {
      if (!new RegExp(`\\|\\s*${id}\\s*\\|`).test(index)) errors.push(`index missing ${id}`)
    }
  }

  const titles = new Set()
  for (const [id, task] of tasks) {
    if (titles.has(task.title)) errors.push(`${id}: duplicate title ${task.title}`)
    titles.add(task.title)
  }

  return errors
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const root = resolve(process.argv[2] ?? '.')
  const errors = validateRepository(root)
  if (errors.length) {
    errors.forEach((error) => console.error(error))
    process.exitCode = 1
  } else {
    console.log('Documentation and backlog validation passed.')
  }
}
