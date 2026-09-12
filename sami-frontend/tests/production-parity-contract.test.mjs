import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const frontendRoot = path.resolve(process.cwd())
const workspaceRoot = path.resolve(frontendRoot, '..')

function read(relativePath) {
  return fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')
}

function readWorkspace(relativePath) {
  return fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')
}

function filesUnder(relativePath, extensions) {
  const directory = path.join(frontendRoot, relativePath)
  const result = []

  function visit(current) {
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      const fullPath = path.join(current, entry.name)
      if (entry.isDirectory()) {
        visit(fullPath)
      } else if (extensions.some((extension) => entry.name.endsWith(extension))) {
        result.push(fullPath)
      }
    }
  }

  visit(directory)
  return result
}

test('production API clients remain relative to the configured /api base URL', () => {
  const apiFiles = filesUnder('src/api', ['.ts'])

  for (const file of apiFiles) {
    const source = fs.readFileSync(file, 'utf8')
    assert.doesNotMatch(
      source,
      /http\.(?:get|post|put|patch|delete)[\s\S]{0,100}['"`]\/api\/v1\//,
      `${path.relative(frontendRoot, file)} contains a double-/api client path`,
    )
  }
})

test('restricted browser storage cannot abort bootstrap, requests, or persistence', () => {
  const sourceFiles = filesUnder('src', ['.ts', '.vue'])

  for (const file of sourceFiles) {
    if (file.endsWith(path.join('services', 'browserStorage.ts'))) continue
    const source = fs.readFileSync(file, 'utf8')
    assert.doesNotMatch(
      source,
      /\blocalStorage\.(?:getItem|setItem|removeItem)\s*\(/,
      `${path.relative(frontendRoot, file)} accesses localStorage without the compatibility adapter`,
    )
    assert.doesNotMatch(
      source,
      /\bsessionStorage\.(?:getItem|setItem|removeItem)\s*\(/,
      `${path.relative(frontendRoot, file)} accesses sessionStorage without a compatibility adapter`,
    )
  }

  const adapter = read('src/services/browserStorage.ts')
  assert.match(adapter, /try \{[\s\S]*window\.localStorage/)
  assert.match(adapter, /fallback\.set/)
})

test('secure-context-only capabilities fail closed without affecting business forms', () => {
  const idempotency = read('src/services/idempotencyKey.ts')
  const notification = read('src/services/notificationPermissionService.ts')
  const pwa = read('src/composables/usePwa.ts')
  const push = read('src/services/pushNotificationService.ts')
  const shortcuts = read('src/components/AppKeyboardShortcuts.vue')

  assert.match(idempotency, /getRandomValues/)
  assert.match(idempotency, /fallbackKey/)
  assert.doesNotMatch(idempotency, /Math\.random/)
  assert.match(notification, /!window\.isSecureContext/)
  assert.match(pwa, /typeof window\.matchMedia === 'function'/)
  assert.match(push, /typeof window\.matchMedia === 'function'/)
  assert.match(shortcuts, /typeof MutationObserver === 'function'/)
})

test('production topology and build-time defaults are explicit and aligned', () => {
  const http = read('src/api/http.ts')
  const frontendDockerfile = read('Dockerfile')
  const nginx = read('nginx.conf')
  const compose = readWorkspace('sami-backend/docker-compose.prod.yml')
  const backendDockerfile = readWorkspace('sami-backend/Dockerfile')
  const deploy = readWorkspace('scripts/deploy.ps1')
  const buildInfo = read('src/buildInfo.ts')

  assert.match(http, /VITE_API_BASE_URL \?\? '\/api'/)
  assert.match(nginx, /listen 80/)
  assert.match(nginx, /proxy_pass http:\/\/backend:8080\/api\//)
  assert.match(compose, /SPRING_PROFILES_ACTIVE: \$\{SPRING_PROFILES_ACTIVE:-prod\}/)
  assert.match(compose, /VITE_API_BASE_URL: \$\{VITE_API_BASE_URL:-\/api\}/)
  assert.doesNotMatch(compose, /VITE_ENABLE_MOCK_MODE\s*:/)
  assert.match(frontendDockerfile, /ARG VITE_APP_VERSION=0\.5\.0/)
  assert.match(backendDockerfile, /ARG APP_VERSION=0\.5\.0/)
  assert.match(deploy, /\[string\]\$ApplicationVersion = '0\.5\.0'/)
  assert.match(deploy, /audit', '--audit-level=high/)
  assert.match(buildInfo, /VITE_APP_VERSION \|\| '0\.5\.0'/)
})

test('purchasing clients use the same contract path as their backend controllers', () => {
  const backendPaths = [
    readWorkspace('sami-backend/src/main/java/com/sami/app/purchasing/web/PurchaseOrderController.java'),
    readWorkspace('sami-backend/src/main/java/com/sami/app/purchasing/web/GoodsReceiptController.java'),
    readWorkspace('sami-backend/src/main/java/com/sami/app/purchasing/web/SupplierInvoiceController.java'),
  ]
  const clients = [
    read('src/api/purchaseOrders.ts'),
    read('src/api/goodsReceipts.ts'),
    read('src/api/supplierInvoices.ts'),
  ]

  for (const source of backendPaths) assert.match(source, /\/api\/v1\//)
  for (const source of clients) {
    assert.doesNotMatch(source, /['"`]\/api\/v1\//)
    assert.match(source, /['"`]\/v1\//)
  }
})
