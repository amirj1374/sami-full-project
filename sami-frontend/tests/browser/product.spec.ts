import { test, expect } from '@playwright/test'

test('creates a phone product and initial variant after Warehouse setup', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Mutation runs once')
  const marker = Date.now().toString(36).toUpperCase()
  const name = `Browser Phone ${marker}`
  const sku = `PHONE-${marker}`
  await page.goto('/products', { waitUntil: 'domcontentloaded' })
  await page.getByRole('button', { name: /New product|محصول جدید/i }).click()
  const dialog = page.getByRole('dialog', { name: /New product|محصول جدید/i })
  await dialog.getByRole('textbox', { name: /^(Name|نام) \*/i }).fill(name)
  await dialog.getByRole('textbox', { name: /^(SKU|کد کالا) \*/i }).fill(sku)
  await dialog.getByRole('textbox', { name: /Variant code|کد گونه/i }).fill(`V-${marker}`)
  await dialog.getByRole('textbox', { name: /Variant name|نام گونه/i }).fill(`Browser Phone 256GB ${marker}`)
  await dialog.getByRole('textbox', { name: /Variant SKU|شناسه گونه/i }).fill(`VSKU-${marker}`)
  const productResponse = page.waitForResponse((response) => /\/api\/v1\/products$/.test(response.url()) && response.request().method() === 'POST')
  const variantResponse = page.waitForResponse((response) => /\/api\/v1\/products\/\d+\/variants$/.test(response.url()) && response.request().method() === 'POST')
  await dialog.getByRole('button', { name: /Create|ایجاد/i }).click()
  expect((await productResponse).ok()).toBeTruthy()
  expect((await variantResponse).ok()).toBeTruthy()
  await expect(page.getByText(/No active Inventory warehouse is configured for this branch/i)).toHaveCount(0)
  await expect(page.getByText(name, { exact: true })).toBeVisible()
  await page.reload({ waitUntil: 'domcontentloaded' })
  await expect(page.getByText(name, { exact: true })).toBeVisible()
})
