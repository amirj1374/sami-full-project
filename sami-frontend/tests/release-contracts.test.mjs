import assert from 'node:assert/strict'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, resolve } from 'node:path'
import test from 'node:test'

const root = resolve(import.meta.dirname, '..')
const sourceRoot = join(root, 'src')

function read(path) {
  return readFileSync(join(root, path), 'utf8')
}

function flatten(value, prefix = '', result = new Map()) {
  for (const [key, child] of Object.entries(value)) {
    const path = prefix ? `${prefix}.${key}` : key
    if (child && typeof child === 'object' && !Array.isArray(child)) flatten(child, path, result)
    else result.set(path, child)
  }
  return result
}

function filesBelow(directory) {
  return readdirSync(directory).flatMap((name) => {
    const path = join(directory, name)
    return statSync(path).isDirectory() ? filesBelow(path) : [path]
  })
}

test('English and Persian localization keys stay in exact parity', () => {
  const english = flatten(JSON.parse(read('src/locales/en.json')))
  const persian = flatten(JSON.parse(read('src/locales/fa.json')))

  assert.deepEqual([...english.keys()].sort(), [...persian.keys()].sort())
})

test('every lazy route component exists in the production source tree', () => {
  const router = read('src/router/index.ts')
  const components = [...router.matchAll(/import\('@\/(.+?\.vue)'\)/g)].map((match) => match[1])

  assert.ok(components.length > 0)
  for (const component of components) {
    assert.doesNotThrow(() => statSync(join(sourceRoot, component)), component)
  }
})

test('every static route permission is seeded by Flyway', () => {
  const router = read('src/router/index.ts')
  const permissions = [...router.matchAll(/permission:\s*'([^']+)'/g)].map((match) => match[1])
  const migrationDirectory = resolve(root, '..', 'sami-backend', 'src', 'main', 'resources', 'db', 'migration')
  const migrations = filesBelow(migrationDirectory)
    .filter((path) => path.endsWith('.sql'))
    .map((path) => readFileSync(path, 'utf8'))
    .join('\n')

  for (const permission of permissions) {
    const [moduleCode, action] = permission.split(':')
    const isLiteral = migrations.includes(`'${permission}'`)
    const isComposedSeed = migrations.includes(`'${moduleCode}'`) && migrations.includes(`'${action}'`)
    assert.ok(isLiteral || isComposedSeed, `Missing seeded permission ${permission}`)
  }
})

test('every backend authorization permission is seeded by Flyway', () => {
  const backendRoot = resolve(root, '..', 'sami-backend')
  const migrations = filesBelow(join(backendRoot, 'src', 'main', 'resources', 'db', 'migration'))
    .filter((path) => path.endsWith('.sql'))
    .map((path) => readFileSync(path, 'utf8'))
    .join('\n')
  const permissions = new Set(
    filesBelow(join(backendRoot, 'src', 'main', 'java'))
      .filter((path) => path.endsWith('.java'))
      .flatMap((path) =>
        [...readFileSync(path, 'utf8').matchAll(/@authz\.has\('([^']+)'\)/g)].map((match) => match[1]),
      ),
  )

  assert.ok(permissions.size > 0)
  for (const permission of permissions) {
    const [moduleCode, action] = permission.split(':')
    const isLiteral = migrations.includes(`'${permission}'`)
    const isComposedSeed = migrations.includes(`'${moduleCode}'`) && migrations.includes(`'${action}'`)
    assert.ok(isLiteral || isComposedSeed, `Missing seeded permission ${permission}`)
  }
})

test('partial modules are not directly routed in the production bundle', () => {
  const router = read('src/router/index.ts')

  for (const path of ['data-quality', 'files', 'appointments']) {
    assert.doesNotMatch(router, new RegExp(`path:\\s*'${path}'`))
  }
})

test('frontend source does not hardcode company, branch, or tenant identity 1', () => {
  const offenders = filesBelow(sourceRoot)
    .filter((path) => /\.(ts|vue)$/.test(path))
    .filter((path) => /(?:tenantId|companyId|branchId)\s*:\s*1\b/.test(readFileSync(path, 'utf8')))

  assert.deepEqual(offenders, [])
})

test('Sales menu namespace matches its route permission and localized label', () => {
  const router = read('src/router/index.ts')
  const salesView = read('src/views/SalesView.vue')
  const salesMigration = read('../sami-backend/src/main/resources/db/migration/V36__align_sales_module_namespace.sql')
  const english = JSON.parse(read('src/locales/en.json'))
  const persian = JSON.parse(read('src/locales/fa.json'))

  assert.match(router, /path:\s*'sales'[\s\S]*?permission:\s*'sales:view'/)
  assert.match(salesView, /can\('sales:report'\)\?salesApi\.dashboard\(\):Promise\.resolve\(null\)/)
  assert.match(salesMigration, /SET code = 'sales'/)
  assert.equal(english.server.module.sales, 'Sales')
  assert.equal(persian.server.module.sales, 'فروش')
})

test('PWA banner dismissal is independent from permanent install capability', () => {
  const pwa = read('src/composables/usePwa.ts')
  const banner = read('src/components/AppPwaStatus.vue')
  const settings = read('src/views/ProfileView.vue')

  assert.match(pwa, /sami\.pwa\.install-banner-dismissed\.v1/)
  assert.match(banner, /dismissInstallBanner/)
  assert.match(settings, /promptInstall/)
  assert.match(settings, /settings\.application\.instructions/)
})

test('mobile navigation preferences are user-scoped and capped at four modules', () => {
  const shell = read('src/layouts/DefaultLayout.vue')
  const settings = read('src/views/ProfileView.vue')
  const api = read('src/api/userExperience.ts')

  assert.match(api, /\/v1\/users\/me\/preferences/)
  assert.match(shell, /\.slice\(0, 4\)/)
  assert.match(settings, /selectedCodes\.value\.length >= 4/)
  assert.doesNotMatch(shell, /localStorage.*mobile/i)
})
