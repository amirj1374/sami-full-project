import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = path.resolve(process.cwd())
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('critical create/edit forms guard duplicate submissions inside submit handlers', () => {
  for (const file of [
    'src/views/SalesView.vue',
    'src/components/CustomerFormDialog.vue',
    'src/components/PurchaseFormDialog.vue',
    'src/components/SupplierFormDialog.vue',
    'src/components/ProductFormDialog.vue',
  ]) {
    const source = read(file)
    assert.match(source, /if \((?:saving|loading)\.value\) return/)
  }
})

test('sales and purchasing form validation cannot fail silently before API calls', () => {
  assert.match(read('src/views/SalesView.vue'), /error\.set\(\{ code: "VALIDATION"/)
  assert.match(read('src/components/SalesDocumentsPanel.vue'), /error\.set\(\{code:'VALIDATION'/)
  assert.match(read('src/components/SalesOrdersPanel.vue'), /error\.set\(\{code:'VALIDATION'/)
  assert.match(read('src/components/purchasing/GoodsReceiptsPanel.vue'), /!warehouseId\.value/)
  assert.match(read('src/components/purchasing/SupplierInvoicesPanel.vue'), /po\.value\.id!==gr\.value\.purchase_order_id/)
})

test('payment and treasury mutations expose one shared in-flight guard', () => {
  const payments = read('src/views/PurchasePaymentsView.vue')
  const treasury = read('src/views/TreasuryView.vue')
  assert.match(payments, /saving=ref\(false\)/)
  assert.match(payments, /if\(saving\.value\)return/)
  assert.match(treasury, /if\(saving\.value\)return/)
})
