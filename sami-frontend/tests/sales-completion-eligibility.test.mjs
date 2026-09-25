import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import ts from 'typescript'

const root = path.resolve(process.cwd())
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const helperSource = read('src/services/saleCompletionEligibility.ts')
const helperJavaScript = ts.transpileModule(helperSource, {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
}).outputText
const helperModuleUrl = `data:text/javascript;base64,${Buffer.from(helperJavaScript).toString('base64')}`
const { getSaleCompletionEligibility } = await import(helperModuleUrl)

test('Complete eligibility mirrors the backend captured-payment contract', () => {
  const backend = fs.readFileSync(
    path.resolve(root, '../sami-backend/src/main/java/com/sami/app/sales/service/SalesService.java'),
    'utf8',
  )
  assert.match(backend, /filter\(p->"CAPTURED"\.equals\(p\.getStatus\(\)\)\)/)
  assert.match(backend, /paid\.compareTo\(s\.getFinalAmount\(\)\)!=0/)

  assert.deepEqual(
    getSaleCompletionEligibility({ status: 'CONFIRMED', finalAmount: 100, payments: [] }),
    { canComplete: false, paymentRequired: true },
  )
  assert.deepEqual(
    getSaleCompletionEligibility({
      status: 'CONFIRMED',
      finalAmount: 100,
      payments: [{ status: 'CAPTURED', amount: 100 }],
    }),
    { canComplete: true, paymentRequired: false },
  )
  assert.deepEqual(
    getSaleCompletionEligibility({
      status: 'CONFIRMED',
      finalAmount: 0.3,
      payments: [
        { status: 'CAPTURED', amount: 0.1 },
        { status: 'CAPTURED', amount: 0.2 },
        { status: 'PENDING', amount: 10 },
      ],
    }),
    { canComplete: true, paymentRequired: false },
  )
})

test('Complete UI blocks unpaid requests and retains one-request mutation guard', () => {
  const view = read('src/views/SalesView.vue')
  const api = read('src/api/sales.ts')
  const fa = JSON.parse(read('src/locales/fa.json'))

  assert.equal(fa.sales.completePaymentRequired, 'برای تکمیل فروش، ابتدا مبلغ باقی‌مانده را دریافت و ثبت کنید.')
  assert.match(view, /action === "complete"[\s\S]*!getSaleCompletionEligibility\(selected\.value\)\.canComplete[\s\S]*return/)
  assert.match(view, /:disabled="saving \|\| !completionEligibility\?\.canComplete"/)
  assert.match(view, /completionEligibility\?\.paymentRequired/)
  assert.match(view, /sales\.completePaymentRequired/)
  assert.match(view, /if \(!selected\.value \|\| saving\.value\) return;[\s\S]*saving\.value = true;[\s\S]*await salesApi\[action\]\(saleId\)/)
  assert.match(api, /http\.post<ApiResponse<Sale>>\(`\/v1\/sales\/\$\{id\}\/complete`\)/)
})
