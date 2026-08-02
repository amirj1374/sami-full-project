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

test('remaining partial modules are not directly routed in the production bundle', () => {
  const router = read('src/router/index.ts')

  for (const path of ['files', 'appointments']) {
    assert.doesNotMatch(router, new RegExp(`path:\\s*'${path}'`))
  }
})

test('Data Quality is routed through its seeded permission and activated lifecycle', () => {
  const router = read('src/router/index.ts')
  const migration = read('../sami-backend/src/main/resources/db/migration/V39__activate_data_quality_module.sql')
  assert.match(router, /path:\s*'data-quality'/)
  assert.match(router, /permission:\s*'data-quality:view'/)
  assert.match(migration, /WHERE code = 'data-quality'/)
  assert.match(migration, /is_production_ready = TRUE/)
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
  assert.match(salesView, /can\(["']sales:report["']\)[\s\S]*?salesApi\.dashboard\(\)[\s\S]*?Promise\.resolve\(null\)/)
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

test('all forms inherit the shared mobile-first rhythm and persistent labels', () => {
  const vuetify = read('src/plugins/vuetify.ts')
  const styles = read('src/styles/global.css')

  for (const control of ['VTextField', 'VTextarea', 'VSelect', 'VAutocomplete', 'VCombobox', 'VFileInput']) {
    assert.match(vuetify, new RegExp(`${control}: \\{[^}]*density: 'compact'[^}]*active: true`), control)
  }
  for (const token of [
    '--app-form-control-height',
    '--app-form-label-gap',
    '--app-form-message-gap',
    '--app-form-field-gap',
    '--app-form-section-gap',
    '--app-form-dialog-padding',
    '--app-form-footer-padding',
  ]) {
    assert.match(styles, new RegExp(token), token)
  }
  assert.match(styles, /\.v-dialog \.v-card-actions[\s\S]*position: sticky/)
  assert.match(styles, /env\(safe-area-inset-bottom\)/)
  assert.match(styles, /max-height: calc\(100dvh/)
})

test('Legacy Asan archive upload uses the shared multipart request convention', () => {
  const legacyClient = read('src/api/legacyImports.ts')

  assert.match(legacyClient, /new FormData\(\)/)
  assert.match(legacyClient, /headers:\s*\{\s*'Content-Type':\s*'multipart\/form-data'\s*\}/)
})

test('active integration menu modules have localized server labels', () => {
  const english = JSON.parse(read('src/locales/en.json'))
  const persian = JSON.parse(read('src/locales/fa.json'))

  for (const code of ['legacy-import', 'hamta', 'market-sync']) {
    assert.ok(english.server.module[code], `Missing English module label for ${code}`)
    assert.ok(persian.server.module[code], `Missing Persian module label for ${code}`)
  }
})

test('HAMTA workflow is localized, permission-gated, IMEI-linked, and printable', () => {
  const router = read('src/router/index.ts')
  const view = read('src/views/HamtaView.vue')
  const purchase = read('src/components/PurchaseDetailDialog.vue')
  const sales = read('src/views/SalesView.vue')
  const english = JSON.parse(read('src/locales/en.json'))
  const persian = JSON.parse(read('src/locales/fa.json'))

  assert.match(router, /path:\s*'hamta'[\s\S]*?permission:\s*'hamta:view'/)
  assert.match(view, /can\('hamta:settings-update'\)/)
  assert.match(view, /can\('hamta:deliver'\)/)
  assert.match(view, /window\.print\(\)/)
  assert.match(purchase, /detail\?\.itemCondition === 'USED'/)
  assert.match(purchase, /hamtaActivationCode/)
  assert.match(sales, /hamtaApi\.invoice\(s\.id\)/)
  assert.match(sales, /hamtaApi\.deliver\(selected\.value\.id\)/)
  assert.ok(english.hamta.activationCode)
  assert.ok(persian.hamta.activationCode)
  assert.equal(Object.keys(english.hamta).sort().join(','), Object.keys(persian.hamta).sort().join(','))
})

test('Market Sync is localized, permission-gated, responsive, and backend-priced', () => {
  const router = read('src/router/index.ts'); const view = read('src/views/MarketSyncView.vue');
  const en = JSON.parse(read('src/locales/en.json')); const fa = JSON.parse(read('src/locales/fa.json'))
  assert.match(router, /path:\s*'market-sync'[\s\S]*?permission:\s*'market-sync:view'/)
  for (const permission of ['manage-sources','manage-pricing','manage-publication','execute']) assert.match(view, new RegExp(`market-sync:${permission}`))
  assert.match(view, /marketSyncApi\.preview\(profile\)/); assert.match(view, /overflow-x:hidden/)
  assert.equal(Object.keys(en.marketSync).sort().join(','), Object.keys(fa.marketSync).sort().join(','))
})
