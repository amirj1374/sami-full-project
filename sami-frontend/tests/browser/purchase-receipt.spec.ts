import { test, expect } from '@playwright/test'

test('purchases and receives the UI-created phone variant with one IMEI', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Mutation runs once')
  const productName = process.env.SAMI_E2E_PRODUCT_NAME!
  const variantName = process.env.SAMI_E2E_VARIANT_NAME!
  const warehouseName = process.env.SAMI_E2E_WAREHOUSE_NAME!
  expect(productName && variantName && warehouseName).toBeTruthy()
  const marker = Date.now().toString(36).toUpperCase()
  const supplierName = `Browser Supplier ${marker}`
  const imei = `35${Date.now().toString().slice(-13).padStart(13, '0')}`

  await page.goto('/suppliers', { waitUntil: 'domcontentloaded' })
  const languageButton = page.getByRole('button', { name: /English|فارسی/i })
  if ((await languageButton.textContent())?.trim() !== 'English') {
    await languageButton.click()
    await page.getByText('English', { exact: true }).last().click()
  }
  await page.getByRole('button', { name: 'New supplier', exact: true }).click()
  const supplierDialog = page.getByRole('dialog').filter({ hasText: 'New supplier' })
  await supplierDialog.getByRole('textbox', { name: 'Company name', exact: true }).fill(supplierName)
  await supplierDialog.getByRole('textbox', { name: 'Display name', exact: true }).fill(supplierName)
  const supplierResponse = page.waitForResponse((response) => /\/api\/v1\/suppliers$/.test(response.url()) && response.request().method() === 'POST')
  await supplierDialog.getByRole('button', { name: /Save|ذخیره/i }).click()
  expect((await supplierResponse).ok()).toBeTruthy()
  await expect(page.getByText(supplierName, { exact: true }).first()).toBeVisible()
  await page.reload({ waitUntil: 'domcontentloaded' })
  await expect(page.getByText(supplierName, { exact: true }).first()).toBeVisible()

  await page.goto('/purchases', { waitUntil: 'domcontentloaded' })
  await page.getByRole('tab', { name: 'Purchase Orders', exact: true }).click()
  await page.getByRole('button', { name: 'Create', exact: true }).click()
  const dialog = page.getByRole('dialog').filter({ hasText: 'Create Purchase Order' })
  const select = async (name: string | RegExp, option: string) => {
    const control = dialog.getByRole('combobox', { name })
    await control.focus(); await control.press('ArrowDown')
    await page.getByRole('option', { name: option, exact: true }).click()
  }
  await select('Supplier', supplierName)
  await select('Product', productName)
  await expect(dialog.getByRole('combobox', { name: 'Variant' })).toBeVisible()
  await select('Variant', variantName)
  await dialog.getByRole('spinbutton', { name: 'Quantity' }).fill('1')
  await dialog.getByRole('spinbutton', { name: 'Unit price' }).fill('50000000')
  const createResponse = page.waitForResponse((response) => /\/api\/v1\/purchase-orders(?:\?|$)/.test(response.url()) && response.request().method() === 'POST')
  await dialog.getByRole('button', { name: 'Create', exact: true }).click()
  const created = await createResponse
  const createdBody = await created.json()
  expect(created.ok(), `Purchase create failed (${created.status()}): ${JSON.stringify(createdBody)}`).toBeTruthy()
  const order = createdBody.data
  const orderItem = page.getByRole('listitem').filter({ hasText: order.order_number })
  await expect(orderItem).toBeVisible()
  const submitResponse = page.waitForResponse((response) => /\/api\/v1\/purchase-orders\/\d+\/submit$/.test(response.url()))
  await orderItem.getByRole('button', { name: 'Submit' }).click()
  expect((await submitResponse).ok()).toBeTruthy()
  const submittedItem = page.getByRole('listitem').filter({ hasText: order.order_number })
  await expect(submittedItem.getByRole('button', { name: 'Approve' })).toBeVisible()
  const approveResponse = page.waitForResponse((response) => /\/api\/v1\/purchase-orders\/\d+\/approve$/.test(response.url()))
  await submittedItem.getByRole('button', { name: 'Approve' }).click()
  expect((await approveResponse).ok()).toBeTruthy()

  await page.getByRole('tab', { name: 'Goods Receipts' }).click()
  await page.getByRole('button', { name: 'Receive Approved PO' }).click()
  const receipt = page.getByRole('dialog').filter({ hasText: 'Receive Purchase Order' })
  const po = receipt.getByRole('combobox', { name: 'Purchase Order' })
  await po.focus(); await po.press('ArrowDown')
  await page.getByRole('option', { name: order.order_number, exact: true }).click()
  const wh = receipt.getByRole('combobox', { name: 'Warehouse' })
  await wh.focus(); await wh.press('ArrowDown')
  await page.getByRole('option', { name: warehouseName, exact: true }).click()
  await receipt.getByRole('spinbutton', { name: 'Received' }).fill('1')
  await receipt.getByRole('textbox', { name: 'IMEI' }).fill(imei)
  const receiptResponse = page.waitForResponse((response) => /\/api\/v1\/goods-receipts(?:\?|$)/.test(response.url()) && response.request().method() === 'POST')
  await receipt.getByRole('button', { name: 'Confirm receipt' }).click()
  expect((await receiptResponse).ok()).toBeTruthy()
})
