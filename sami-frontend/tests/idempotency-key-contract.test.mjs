import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = path.resolve(process.cwd())
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('Sales Save is independent of secure-context UUID support', () => {
  const sales = read('src/views/SalesView.vue')
  const inventory = read('src/components/inventory/InventoryBalancesPanel.vue')
  const receipts = read('src/components/SalesReceiptsPanel.vue')
  const salesApi = read('src/api/sales.ts')
  const helper = read('src/services/idempotencyKey.ts')
  const backendDto = fs.readFileSync(path.resolve(root, '../sami-backend/src/main/java/com/sami/app/sales/dto/SalesDtos.java'), 'utf8')

  assert.doesNotMatch(sales, /crypto\.randomUUID\(\)/)
  assert.doesNotMatch(inventory, /crypto\.randomUUID\(\)/)
  assert.match(receipts, /createClientIdempotencyKey\(\)/)
  assert.match(sales, /salesApi\.create\(payload\)/)
  assert.match(salesApi, /http\.post[\s\S]*['"]\/v1\/sales['"]|['"]\/v1\/sales['"][\s\S]*http\.post/)
  assert.match(backendDto, /String idempotencyKey/)
  assert.match(helper, /getRandomValues/)
  assert.doesNotMatch(helper, /Math\.random/)
})
