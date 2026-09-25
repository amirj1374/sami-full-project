import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (path) => readFileSync(join(root, path), 'utf8')

test('Sales lifecycle clients map to the implemented backend endpoints', () => {
  const sales = read('src/api/sales.ts')
  const documents = read('src/api/salesDocuments.ts')
  const orders = read('src/api/salesOrders.ts')
  const deliveries = read('src/api/deliveries.ts')
  const invoices = read('src/api/salesInvoices.ts')
  const receipts = read('src/api/salesReceipts.ts')

  assert.match(sales, /\/v1\/sales\/\$\{id\}\/confirm/)
  assert.match(sales, /\/v1\/sales\/\$\{id\}\/complete/)
  assert.match(documents, /\/v1\/sales-documents\/\$\{id\}\/issue/)
  assert.match(orders, /\/v1\/sales-orders\/from-quotation\/\$\{sourceId\}/)
  assert.match(orders, /\/v1\/sales-orders\/\$\{id\}\/confirm/)
  assert.match(deliveries, /\/v1\/sales-deliveries\/from-order\/\$\{orderId\}/)
  assert.match(deliveries, /\/v1\/sales-deliveries\/\$\{id\}\/confirm/)
  assert.match(invoices, /\/v1\/sales-invoices\/\$\{id\}\/issue/)
  assert.match(receipts, /sales-receipts\/\$\{id\}\/confirm-now/)
})

test('Invoiceable line type matches the backend invoiceable response fields', () => {
  const invoiceableType = read('src/types/salesInvoices.ts')
  assert.match(invoiceableType, /deliveryId:number/)
  assert.match(invoiceableType, /previouslyInvoiced:number/)
  assert.doesNotMatch(invoiceableType, /issuedQuantity:number/)
})

test('Sales mutation state cannot reveal the next action before the mutation lock is released', () => {
  const view = read('src/views/SalesView.vue')
  const documents = read('src/components/SalesDocumentsPanel.vue')
  const orders = read('src/components/SalesOrdersPanel.vue')
  const deliveries = read('src/components/SalesDeliveriesPanel.vue')
  const invoices = read('src/components/SalesInvoicesPanel.vue')

  assert.match(view, /updated =\s*action === "cancel"/)
  assert.match(view, /saving\.value = false;\s*if \(updated\) selected\.value = updated/)
  assert.match(view, /v-if="selected\.status === 'CONFIRMED' && canAction\('complete'\)"[\s\S]*:disabled="saving"/)
  assert.match(documents, /let updated:SalesDocument\|undefined/)
  assert.match(orders, /let updated\s*=|const updated\s*=|const updated\s*:/)
  assert.match(deliveries, /let updated:Delivery\|undefined/)
  assert.match(invoices, /let updated:\s*SalesInvoice\s*\|\s*undefined/)
  assert.match(orders, /@click="open\(\)"/)
  assert.match(deliveries, /saving\.value\|\|!canSave\.value/)
  assert.match(invoices, /salesInvoicesApi\.invoiceable/)
  assert.match(read('src/components/SalesReceiptsPanel.vue'), /if \(busy\.value\) return/)
})

test('Sales mutation owners retain rapid duplicate-submission guards', () => {
  assert.match(read('src/views/SalesView.vue'), /async function save\(\) \{\s*if \(saving\.value\) return/)
  assert.match(read('src/components/SaleActionPanel.vue'), /async function submit\(\)\{if\(busy\.value\)return/)
  assert.match(read('src/components/SalesDocumentsPanel.vue'), /async function save\(\)\{if\(saving\.value\)return/)
  assert.match(read('src/components/SalesOrdersPanel.vue'), /async function save\(\) \{\s*if \(saving\.value\) return/)
  assert.match(read('src/components/SalesDeliveriesPanel.vue'), /async function save\(\)\{if\(!source\.value\|\|saving\.value\|\|!canSave\.value\)return/)
  assert.match(read('src/components/SalesInvoicesPanel.vue'), /async function save\(\) \{\s*if \(saving\.value \|\|/)
  assert.match(read('src/components/SalesReceiptsPanel.vue'), /async function create\(\) \{\s*if \(busy\.value\) return/)
})
