import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import ts from 'typescript'

const root = path.resolve(process.cwd())
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const mapperSource = read('src/services/apiErrorMapper.ts')
const mapperJavaScript = ts.transpileModule(mapperSource, {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
}).outputText
const mapperModuleUrl = `data:text/javascript;base64,${Buffer.from(mapperJavaScript).toString('base64')}`
const { mapApiErrorMessage } = await import(mapperModuleUrl)

const fa = JSON.parse(read('src/locales/fa.json'))
const translateFa = (key) => key.split('.').reduce((value, part) => value?.[part], fa) ?? key
const mapSalesError = (code, message = '') => mapApiErrorMessage({ code, message }, translateFa, 'sales')

test('Sales Complete payment mismatch has exact user-facing Persian copy', () => {
  const message = mapSalesError(
    'OPERATION_NOT_ALLOWED',
    'Captured payments must equal the final amount',
  )

  assert.equal(
    message,
    'امکان تکمیل فروش وجود ندارد؛ مبلغ پرداخت‌شده باید با مبلغ نهایی فروش برابر باشد.',
  )
  assert.doesNotMatch(message, /OPERATION_NOT_ALLOWED|409|Captured payments/i)
})

test('Sales API error categories never expose backend technical copy', () => {
  const cases = [
    ['OPERATION_NOT_ALLOWED', 'Unmapped backend operation detail', fa.salesError.operationNotAllowed],
    ['RESOURCE_NOT_FOUND', 'Technical resource detail', fa.apiError.notFound],
    ['VALIDATION_ERROR', 'Technical validation detail', fa.apiError.validation],
    ['VALIDATION_FAILED', 'Technical validation detail', fa.apiError.validation],
    ['CONFLICT', 'Technical conflict detail', fa.apiError.conflict],
    ['RESOURCE_CONFLICT', 'Technical conflict detail', fa.apiError.conflict],
    ['FORBIDDEN', 'Technical permission detail', fa.apiError.forbidden],
    ['ACCESS_DENIED', 'Technical permission detail', fa.apiError.forbidden],
    ['UNAUTHORIZED', 'Technical authentication detail', fa.apiError.unauthorized],
    ['UNAUTHENTICATED', 'Technical authentication detail', fa.apiError.unauthorized],
    ['401', 'HTTP status detail', fa.apiError.unauthorized],
    ['403', 'HTTP status detail', fa.apiError.forbidden],
    ['409', 'HTTP status detail', fa.apiError.conflict],
    ['NETWORK_ERROR', 'Network Error', fa.apiError.network],
    ['INTERNAL_ERROR', 'Stack trace detail', fa.apiError.server],
    ['503', 'Service unavailable detail', fa.apiError.server],
    ['UNMAPPED_CODE', 'Sensitive internal detail', fa.apiError.generic],
  ]

  for (const [code, backendMessage, expected] of cases) {
    const actual = mapSalesError(code, backendMessage)
    assert.equal(actual, expected, code)
    assert.notEqual(actual, backendMessage, code)
  }
})

test('Payment, Delivery, Invoice, and Receipt business failures have precise Sales messages', () => {
  const cases = [
    ['BAD_REQUEST', 'Payments cannot exceed the final amount', fa.salesError.paymentsCannotExceedFinal],
    ['OPERATION_NOT_ALLOWED', 'Sale cannot accept payments', fa.salesError.saleCannotAcceptPayments],
    ['OPERATION_NOT_ALLOWED', 'Only confirmed orders can be delivered', fa.salesError.onlyConfirmedOrdersCanBeDelivered],
    ['RESOURCE_CONFLICT', 'Delivery quantity exceeds the currently reserved quantity', fa.salesError.deliveryQuantityExceedsReserved],
    ['OPERATION_NOT_ALLOWED', 'Only confirmed orders can be invoiced', fa.salesError.onlyConfirmedOrdersCanBeInvoiced],
    ['RESOURCE_CONFLICT', 'Invoice quantity exceeds invoiceable delivered quantity', fa.salesError.invoiceQuantityExceedsInvoiceable],
    ['OPERATION_NOT_ALLOWED', 'Accounting receivable posting is not available', fa.salesError.accountingReceivableUnavailable],
    ['VALIDATION_FAILED', 'Allocation exceeds outstanding receipt or invoice', fa.salesError.allocationExceedsOutstanding],
    ['VALIDATION_FAILED', 'Receipt allocations are invalid', fa.salesError.receiptAllocationsInvalid],
    ['OPERATION_NOT_ALLOWED', 'Accounting settlement provider is unavailable', fa.salesError.accountingSettlementUnavailable],
  ]

  for (const [code, backendMessage, expected] of cases) {
    assert.equal(mapSalesError(code, backendMessage), expected)
  }
})

test('Sales action surfaces use the shared scoped error owner', () => {
  const scopedComponents = [
    'src/views/SalesView.vue',
    'src/components/SalesDocumentsPanel.vue',
    'src/components/SalesOrdersPanel.vue',
    'src/components/SalesDeliveriesPanel.vue',
    'src/components/SalesInvoicesPanel.vue',
    'src/components/SalesReceiptsPanel.vue',
    'src/components/SalesReceiptConfirmPanel.vue',
    'src/components/SalesReportsPanel.vue',
    'src/components/LostSalesPanel.vue',
  ]

  for (const component of scopedComponents) {
    assert.match(read(component), /useApiError\s*\(\s*\{\s*scope:\s*['"]sales['"]\s*\}\s*\)/, component)
  }

  const salesView = read('src/views/SalesView.vue')
  const http = read('src/api/http.ts')
  const errorOwner = read('src/composables/useApiError.ts')
  assert.match(http, /const apiError = \(err\.response\?\.data as ApiResponse<unknown> \| undefined\)\?\.error/)
  assert.match(http, /throw apiError \?\? \{ code: 'NETWORK_ERROR', message: err\.message \}/)
  assert.match(errorOwner, /mapApiErrorMessage\(err, translate, options\.scope\)/)
  assert.match(salesView, /@failed="error\.set"/)
  for (const component of scopedComponents) {
    assert.doesNotMatch(read(component), /\?\.message\s*\?\?/, component)
  }
})
