import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
const api=fs.readFileSync('src/api/salesReceipts.ts','utf8'); const ui=fs.readFileSync('src/components/SalesReceiptConfirmPanel.vue','utf8')
test('receipt confirmation contract carries treasury account and references',()=>{assert.match(api,/treasuryAccountId/);assert.match(api,/confirm-now/);assert.match(ui,/Treasury account/);assert.match(ui,/accountingPostingReference/)})
