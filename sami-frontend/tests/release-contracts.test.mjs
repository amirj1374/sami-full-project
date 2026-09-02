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

test('keyboard shortcuts cover dynamic buttons and remain a persisted user preference', () => {
  const shell = read('src/layouts/DefaultLayout.vue')
  const shortcuts = read('src/components/AppKeyboardShortcuts.vue')
  const settings = read('src/views/ProfileView.vue')
  const types = read('src/types/userExperience.ts')
  const migration = read('../sami-backend/src/main/resources/db/migration/V44__keyboard_shortcut_preferences.sql')

  assert.match(shell, /AppKeyboardShortcuts/)
  assert.match(shortcuts, /MutationObserver/)
  assert.match(shortcuts, /event\.code/)
  assert.match(shortcuts, /event\.shiftKey/)
  assert.match(shortcuts, /isTypingTarget/)
  assert.match(shortcuts, /aria-keyshortcuts/)
  assert.doesNotMatch(shortcuts, /activePrefix/)
  assert.match(shortcuts, /\['main', 'header', 'nav'\]/)
  assert.match(shortcuts, /a\[href\]/)
  assert.match(shortcuts, /\[role="menuitem"\]/)
  assert.match(shortcuts, /\[role="tab"\]/)
  assert.match(shortcuts, /\[role="dialog"\]/)
  assert.match(shortcuts, /ariaHidden = 'true'/)
  assert.match(shortcuts, /removeEventListener\('keydown'/)
  assert.match(settings, /settings\.keyboard\.enabled/)
  assert.match(shell, /app-main-content/)
  assert.match(shell, /skipToContent/)
  assert.match(shell, /data-sami-shortcut-skip/)
  assert.match(shell, /function focusMain/)
  assert.match(shell, /focus\(\{ preventScroll: true \}\)/)
  assert.match(shell, /commandShortcutLabel/)
  assert.match(types, /keyboardShortcutsEnabled: boolean/)
  assert.match(migration, /keyboard_shortcuts_enabled BOOLEAN NOT NULL DEFAULT TRUE/)
})

test('SAMI brand links consistently navigate to the dashboard', () => {
  const shell = read('src/layouts/DefaultLayout.vue')
  const authShell = read('src/layouts/AuthLayout.vue')

  for (const layout of [shell, authShell]) {
    assert.match(layout, /<router-link[\s\S]*?:to="\{ name: 'dashboard' \}"[\s\S]*?class="[^"]*(?:app-brand|auth-layout__brand)/)
    assert.match(layout, /(?:app-brand|auth-layout__brand):focus-visible/)
  }
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

test('Legacy Asan upload supports RAR, manifest ZIP, and staged XLSX accounting reports', () => {
  const legacyClient = read('src/api/legacyImports.ts')
  const legacyView = read('src/views/LegacyImportsView.vue')
  const fa = JSON.parse(read('src/locales/fa.json'))
  const en = JSON.parse(read('src/locales/en.json'))

  assert.match(legacyClient, /new FormData\(\)/)
  assert.match(legacyClient, /headers:\s*\{\s*'Content-Type':\s*'multipart\/form-data'\s*\}/)
  assert.match(legacyView, /accept="\.rar,\.zip,\.xlsx"/)
  assert.match(legacyView, /rawRecordsIncluded:\s*false/)
  assert.match(legacyView, /canonicalWrites:\s*0/)
  assert.match(legacyView, /downloadValidationReport/)
  assert.match(legacyView, /customerIssue/)
  assert.match(legacyView, /problemReason/)
  assert.match(legacyView, /problemSolution/)
  assert.match(legacyView, /selectedFiles\.value = failed/)
  for (const code of ['unsupportedFormat', 'unsafeArchive', 'manifestRequired', 'unreadable', 'tooLarge', 'duplicate', 'toolUnavailable']) {
    assert.ok(fa.legacy.customerErrors[code], `missing Persian customer import error: ${code}`)
    assert.ok(en.legacy.customerErrors[code], `missing English customer import error: ${code}`)
  }
})

test('reverse proxy and backend multipart limits admit verified Asan accounting workbooks', () => {
  const nginx = read('nginx.conf')
  const backendConfiguration = read('../sami-backend/src/main/resources/application.yml')
  assert.match(nginx, /client_max_body_size\s+21m/)
  assert.match(nginx, /proxy_read_timeout\s+10m/)
  assert.match(nginx, /proxy_send_timeout\s+10m/)
  assert.match(backendConfiguration, /max-file-size:\s*\$\{SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE:20MB\}/)
  assert.match(backendConfiguration, /max-request-size:\s*\$\{SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE:21MB\}/)
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

test('0912 investment module is migration-backed, importable, localized, and mobile-first', () => {
  const migration = read('../sami-backend/src/main/resources/db/migration/V47__sim_0912_investment.sql')
  const controller = read('../sami-backend/src/main/java/com/sami/app/siminvestment/SimInvestmentController.java')
  const api = read('src/api/simInvestment.ts')
  const view = read('src/views/SimInvestmentView.vue')
  const router = read('src/router/index.ts')
  assert.match(migration, /sim-investment/)
  assert.match(migration, /m\.code\|\|':'\|\|a\.action/)
  for (const permission of ['view', 'import', 'recalculate', 'view-history']) assert.match(migration, new RegExp(`\\('${permission}'`))
  assert.match(controller, /@RequestMapping\("\/api\/v1\/sim-investment"\)/)
  assert.match(api, /new FormData\(\)/)
  assert.match(api, /multipart\/form-data/)
  assert.match(view, /AppMobileRecordCard/)
  assert.match(view, /<v-form @submit\.prevent="applyFilters">/)
  assert.match(view, /sim-investment-import-fields/)
  assert.match(view, /accept="\.csv,\.xlsx/)
  assert.match(view, /sim-investment:import/)
  assert.match(router, /permission: 'sim-investment:view'/)
})

test('Persian date, locale, dark theme, and form rhythm are centralized', () => {
  const i18n = read('src/i18n.ts')
  const vuetify = read('src/plugins/vuetify.ts')
  const themeMode = read('src/composables/useThemeMode.ts')
  const styles = read('src/styles/global.css')
  const picker = read('src/components/AppPersianDatePicker.vue')
  const frontendSource = filesBelow(sourceRoot)
    .filter((file) => file.endsWith('.vue'))
    .map((file) => readFileSync(file, 'utf8'))
    .join('\n')

  assert.match(i18n, /locale: 'fa'/)
  assert.match(i18n, /stored : 'fa'/)
  assert.match(vuetify, /defaultTheme: 'dark'/)
  assert.match(themeMode, /\? v : 'dark'/)
  assert.match(styles, /--app-form-field-gap: 20px/)
  assert.match(picker, /u-ca-persian/)
  assert.doesNotMatch(frontendSource, /type=["']date["']/)
})

test('money fields use the shared Toman formatter and mobile-safe input rhythm', () => {
  const field = read('src/components/AppMoneyField.vue')
  const formatter = read('src/composables/useFormat.ts')
  const styles = read('src/styles/global.css')
  const i18n = read('src/i18n.ts')
  const vuetify = read('src/plugins/vuetify.ts')

  assert.match(field, /Intl\.NumberFormat/)
  assert.match(field, /\[٬,\\s\]/)
  assert.match(field, /t\('common\.currency'\)/)
  assert.match(formatter, /function formatMoney/)
  assert.match(formatter, /تومان/)
  assert.match(i18n, /locale: 'fa'/)
  assert.match(i18n, /stored : 'fa'/)
  assert.match(vuetify, /defaultTheme: 'dark'/)
  assert.match(styles, /--app-form-field-gap: 24px/)
  assert.match(styles, /\.app-form-section__body > \.v-row/)
})

test('primary ERP lists use direction-aware expandable mobile record cards', () => {
  const card = read('src/components/AppMobileRecordCard.vue')
  assert.match(card, /@pointerdown="pointerDown"/)
  assert.match(card, /rtl\.value \? props\.actionsWidth : -props\.actionsWidth/)
  assert.match(card, /prefers-reduced-motion/)
  assert.match(card, /@keydown\.enter\.prevent/)
  assert.match(card, /common\.showDetails/)

  for (const view of ['src/views/ProductsView.vue', 'src/views/PurchasesView.vue', 'src/views/SalesView.vue']) {
    assert.match(read(view), /AppMobileRecordCard/, `${view} does not use mobile record cards`)
  }
})

test('Employees and Attendance is migration-backed, permission-gated, localized, and mobile-first', () => {
  const migration = read('../sami-backend/src/main/resources/db/migration/V43__employees_and_attendance.sql')
  const controller = read('../sami-backend/src/main/java/com/sami/app/attendance/web/AttendanceController.java')
  const router = read('src/router/index.ts')
  const view = read('src/views/AttendanceView.vue')
  const en = JSON.parse(read('src/locales/en.json'))
  const fa = JSON.parse(read('src/locales/fa.json'))
  assert.match(migration, /CREATE TABLE employees/)
  assert.match(migration, /CREATE TABLE attendance_records/)
  assert.match(migration, /uq_attendance_open_record/)
  for (const permission of ['view', 'manage-employees', 'clock', 'correct', 'report']) {
    assert.match(migration, new RegExp(`'${permission}'`))
    assert.match(controller, new RegExp(`attendance:${permission}`))
  }
  assert.match(router, /path: 'attendance'[\s\S]*permission: 'attendance:view'/)
  assert.match(view, /AppMobileRecordCard/)
  assert.deepEqual(Object.keys(en.attendance).sort(), Object.keys(fa.attendance).sort())
  assert.equal(en.server.module.attendance, 'Employees & Attendance')
  assert.equal(fa.server.module.attendance, 'کارکنان و حضور و غیاب')
})

test('customer purchases can resolve pending settlement before receiving goods', () => {
  const service = read('../sami-backend/src/main/java/com/sami/app/purchasing/service/PurchaseService.java')
  const controller = read('../sami-backend/src/main/java/com/sami/app/purchasing/web/PurchaseController.java')
  const api = read('src/api/purchases.ts')
  const detail = read('src/components/PurchaseDetailDialog.vue')

  assert.match(service, /updateSettlement\(Long id, SettlementRequest request\)/)
  assert.match(service, /PurchaseSettlementStatus\.PENDING/)
  assert.match(controller, /@PutMapping\("\/\{id\}\/settlement"\)/)
  assert.match(controller, /purchasing:receive/)
  assert.match(api, /updateSettlement/)
  assert.match(detail, /needsSettlement/)
  assert.match(detail, /purchases\.settlement\.receiveBlocked/)
})

test('Licensing mock responses preserve the production API shapes', () => {
  const handlers = read('src/mocks/handlers.ts')

  assert.match(handlers, /licensing\/catalog'[\s\S]*licenseStatuses:\s*\[\]/)
  assert.match(handlers, /licensing\/reports\/summary'[\s\S]*byStatus:\s*\{\}/)
  assert.match(handlers, /licensing\/reports\/expiring'[\s\S]*ok\(\[\]\)/)
})

test('creatable reference fields provide permission-gated inline creation and selection', () => {
  const targets = [
    ['src/views/SalesView.vue', ['customers:create', 'products:create']],
    ['src/components/PurchaseFormDialog.vue', ['suppliers:create', 'customers:create', 'products:create']],
    ['src/components/LostSalesPanel.vue', ['customers:create', 'products:create']],
    ['src/views/AppointmentsView.vue', ['customers:create']],
    ['src/components/inventory/InventoryTransfersPanel.vue', ['products:create']],
    ['src/components/inventory/InventoryBalancesPanel.vue', ['products:create']],
  ]

  for (const [file, permissions] of targets) {
    const source = read(file)
    assert.match(source, /AppQuickCreateButton/, `${file} has no inline create affordance`)
    assert.match(source, /@created=/, `${file} does not handle the created entity`)
    for (const permission of permissions) {
      assert.match(source, new RegExp(`can\\('${permission}'\\)`), `${file} does not gate ${permission}`)
    }
  }

  for (const entity of ['Customer', 'Supplier', 'Product']) {
    const wrapper = read(`src/components/Quick${entity}CreateDialog.vue`)
    assert.match(wrapper, /emit\('created', \w+\)/)
  }
})

test('workflow status enums have Persian labels and raw status views use the shared translator', () => {
  const english = JSON.parse(read('src/locales/en.json'))
  const persian = JSON.parse(read('src/locales/fa.json'))
  const requiredStatuses = [
    'ACTIVE', 'INACTIVE', 'DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'CONFIRMED',
    'COMPLETED', 'CANCELLED', 'PARTIALLY_RETURNED', 'RETURNED', 'OPEN', 'RESOLVED',
    'IGNORED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'SKIPPED', 'TIMED_OUT', 'READY',
    'UPLOADED', 'ANALYZING', 'IMPORTING', 'COMPLETED_WITH_WARNINGS', 'SETTLED',
    'WAIVED', 'POSTED', 'RECEIVING', 'RECEIVED', 'SHIPPED', 'AVAILABLE', 'RESERVED',
    'IN_TRANSIT', 'ISSUED', 'QUARANTINED', 'RETURNED_TO_SUPPLIER', 'RELEASED',
    'FULFILLED', 'COUNTED', 'CALCULATED', 'DEGRADED', 'FAILING', 'DISABLED', 'EXPIRED',
  ]

  for (const status of requiredStatuses) {
    assert.ok(english.server.enum[status], `Missing English enum label for ${status}`)
    assert.ok(persian.server.enum[status], `Missing Persian enum label for ${status}`)
    assert.notEqual(persian.server.enum[status], status, `Persian enum label is still raw for ${status}`)
  }

  for (const file of ['src/views/LegacyImportsView.vue', 'src/views/MarketSyncView.vue', 'src/components/SaleActionPanel.vue']) {
    assert.match(read(file), /enumLabel/, `${file} bypasses the shared enum translator`)
  }
})

test('business enum fields and audit values use Persian labels instead of raw codes', () => {
  const english = JSON.parse(read('src/locales/en.json'))
  const persian = JSON.parse(read('src/locales/fa.json'))
  const requiredEnums = [
    'AMOUNT', 'PERCENT', 'VIP', 'CAMPAIGN', 'COUPON', 'CASH', 'CARD', 'TRANSFER',
    'CHEQUE', 'WALLET', 'INSTALLMENT', 'BLACKLIST', 'WHITELIST', 'PRODUCT_CODE',
    'PREFIX', 'SUPPLIER', 'CUSTOMER', 'PURCHASE', 'SALE', 'RESERVATION', 'RETURN',
    'NEW_SEALED', 'USED', 'OTHER', 'CREATED', 'UPDATED', 'DELETED', 'RESTORED',
    'STATUS_CHANGED', 'PAYMENT_STATUS_CHANGED', 'DISCOUNT_APPROVED', 'PAYMENT_ADDED',
    'STOCK_ADJUSTED', 'UPLOAD', 'ANALYZE', 'IMPORT', 'COMPARE', 'TEXT', 'NUMBER',
    'NUMERIC', 'MEMO', 'UNKNOWN', 'REFERENCE_GEOGRAPHY', 'IMEI', 'SERIAL',
    'PURCHASE_RECEIPT', 'PURCHASE_RETURN', 'CUSTOMER_RETURN',
  ]

  for (const value of requiredEnums) {
    assert.ok(english.server.enum[value], `Missing English enum label for ${value}`)
    assert.ok(persian.server.enum[value], `Missing Persian enum label for ${value}`)
    assert.notEqual(persian.server.enum[value], value, `Persian enum label is still raw for ${value}`)
  }

  for (const file of [
    'src/components/SaleActionPanel.vue', 'src/components/PurchaseDetailDialog.vue',
    'src/components/SupplierDetailDialog.vue', 'src/components/UserAuditDialog.vue',
    'src/components/inventory/InventoryMonitoringPanel.vue', 'src/views/DashboardReportsView.vue',
    'src/views/LegacyImportsView.vue', 'src/views/MarketSyncView.vue',
  ]) assert.match(read(file), /enumLabel/, `${file} bypasses the shared enum translator`)
})
