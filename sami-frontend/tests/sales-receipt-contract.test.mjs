import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
const api=fs.readFileSync('src/api/salesReceipts.ts','utf8'); const ui=fs.readFileSync('src/components/SalesReceiptConfirmPanel.vue','utf8')
const controller=fs.readFileSync('../sami-backend/src/main/java/com/sami/app/sales/receipt/ReceiptConfirmationController.java','utf8')
test('receipt confirmation contract carries treasury account and matches the backend route',()=>{assert.match(api,/treasuryAccountId/);assert.match(api,/sales-receipts\/\$\{id\}\/confirm-now/);assert.match(controller,/@PostMapping\("\/\{id\}\/confirm-now"\)/);assert.match(ui,/Treasury account/);assert.match(ui,/accountingPostingReference/)})
