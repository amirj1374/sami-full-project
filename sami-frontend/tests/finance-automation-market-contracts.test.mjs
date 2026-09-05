import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

test('treasury and purchase-payment routes are real and permission guarded', () => {
  const router = read('src/router/index.ts')
  assert.match(router, /path: 'treasury'/)
  assert.match(router, /permission: 'treasury:view'/)
  assert.match(router, /path: 'purchase-payments'/)
  assert.match(router, /purchase-payments:view-own/)
})

test('purchase-payment UI uses real APIs and numeric mobile inputs', () => {
  const view = read('src/views/PurchasePaymentsView.vue')
  const api = read('src/api/purchasePayments.ts')
  assert.match(view, /inputmode="numeric"/)
  assert.match(view, /purchasePaymentsApi\.pay/)
  assert.match(api, /\/v1\/purchase-payment-requests/)
})

test('market source credentials are configured by environment reference', () => {
  const view = read('src/views/MarketSyncView.vue')
  assert.match(view, /authEnv/)
  assert.doesNotMatch(view, /password|apiKey|privateKey/i)
})
