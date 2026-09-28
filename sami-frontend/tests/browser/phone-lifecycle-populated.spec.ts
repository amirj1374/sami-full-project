import { test, expect, type Page, type TestInfo } from '@playwright/test'

async function capture(page: Page, testInfo: TestInfo, name: string) {
  await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy()
  await testInfo.attach(`${testInfo.project.name}-${name}`, {
    body: await page.screenshot({ fullPage: true }),
    contentType: 'image/png',
  })
}

async function languageButton(page: Page) {
  const viewport = page.viewportSize()
  if (viewport && viewport.width < 960) {
    await page.getByRole('button', { name: /Toggle menu|نمایش منو/i }).click()
    const button = page.getByRole('navigation').getByRole('button', { name: /English|فارسی/i })
    await expect(button).toBeVisible()
    return button
  }
  const button = page.getByRole('button', { name: /English|فارسی/i })
  await expect(button).toBeVisible()
  return button
}

test('completed phone lifecycle remains populated, responsive, localized and consistent', async ({ page }, testInfo) => {
  test.setTimeout(180_000)
  const data = {
    supplier: process.env.SAMI_E2E_SUPPLIER_NAME!,
    purchase: process.env.SAMI_E2E_PURCHASE_NUMBER!,
    receipt: process.env.SAMI_E2E_RECEIPT_NUMBER!,
    product: process.env.SAMI_E2E_PRODUCT_NAME!,
    variant: process.env.SAMI_E2E_VARIANT_NAME!,
    imei: process.env.SAMI_E2E_IMEI!,
    customer: process.env.SAMI_E2E_CUSTOMER_NAME!,
    order: process.env.SAMI_E2E_ORDER_NUMBER!,
    delivery: process.env.SAMI_E2E_DELIVERY_NUMBER!,
    invoice: process.env.SAMI_E2E_INVOICE_NUMBER!,
  }
  Object.values(data).forEach((value) => expect(value).toBeTruthy())

  const consoleErrors: string[] = []
  const failedRequests: string[] = []
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })
  page.on('response', (response) => {
    if (response.status() >= 400) failedRequests.push(`${response.status()} ${response.request().method()} ${response.url()}`)
  })

  await page.goto('/suppliers', { waitUntil: 'networkidle' })
  let localeButton = testInfo.project.name === 'desktop' ? await languageButton(page) : null
  if (localeButton && (await localeButton.textContent())?.trim() !== 'English') {
    await localeButton.click()
    await page.getByText('English', { exact: true }).last().click()
  }
  await expect(page.getByText(data.supplier, { exact: true }).first()).toBeVisible()
  await capture(page, testInfo, 'supplier')

  if (testInfo.project.name === 'desktop') {
    localeButton = await languageButton(page)
    await localeButton.click()
    await page.getByText('فارسی', { exact: true }).last().click()
    await expect.poll(() => page.evaluate(() => document.documentElement.dir)).toBe('rtl')
    localeButton = await languageButton(page)
    await localeButton.click()
    await page.getByText('English', { exact: true }).last().click()
    await expect.poll(() => page.evaluate(() => document.documentElement.dir)).toBe('ltr')
  }

  await page.goto('/purchases', { waitUntil: 'networkidle' })
  await page.getByRole('tab', { name: 'Purchase Orders', exact: true }).click()
  await expect(page.getByText(data.purchase, { exact: true }).first()).toBeVisible()
  await capture(page, testInfo, 'purchase')
  await page.getByRole('tab', { name: 'Goods Receipts', exact: true }).click()
  await expect.poll(() => page.getByText(data.receipt, { exact: true }).count(), { timeout: 30_000 }).toBeGreaterThan(0)
  await expect(page.getByText(data.receipt, { exact: true }).first()).toBeVisible()
  await expect(page.getByText(/Confirmed/i).first()).toBeVisible()
  await capture(page, testInfo, 'goods-receipt')

  await page.goto('/products', { waitUntil: 'networkidle' })
  await page.getByRole('textbox', { name: /Search by name|جست‌وجو بر اساس نام/i }).fill(data.product)
  const productRow = page.getByRole('row').filter({ hasText: data.product })
  if (testInfo.project.name === 'desktop') await expect(productRow).toContainText('65,000,000')
  else await expect(page.getByText(data.product, { exact: true }).first()).toBeVisible()
  await capture(page, testInfo, 'product-variant')

  await page.goto('/inventory', { waitUntil: 'networkidle' })
  await page.getByRole('textbox', { name: /^Search$|^جست‌وجو$/i }).fill(data.product)
  if (testInfo.project.name === 'desktop') {
    const balance = page.getByRole('row').filter({ hasText: data.product })
    await expect(balance.getByRole('cell').nth(2)).toHaveText('0')
    await expect(balance.getByRole('cell').nth(3)).toHaveText('0')
    await expect(balance.getByRole('cell').nth(4)).toHaveText('0')
  } else {
    await expect(page.getByText(data.product, { exact: true }).first()).toBeVisible()
  }
  await page.getByRole('tab', { name: /Monitoring|پایش/i }).click()
  await page.getByRole('tab', { name: /Serials \/ IMEI|سریال/i }).click()
  await page.getByRole('textbox', { name: /^Search$|^جست‌وجو$/i }).fill(data.imei)
  const serialRecord = testInfo.project.name === 'mobile'
    ? page.getByText(data.imei, { exact: true }).locator('xpath=ancestor::*[contains(@class,"v-card")][1]')
    : page.getByRole('row').filter({ hasText: data.imei })
  await expect(serialRecord).toContainText(/Issued|تحویل‌شده/i)
  await capture(page, testInfo, 'inventory-imei')

  await page.goto('/sales', { waitUntil: 'networkidle' })
  await page.getByRole('tab', { name: /Sales Orders|سفارش‌های فروش/i }).click()
  const orderRecord = testInfo.project.name === 'desktop'
    ? page.getByText(data.order, { exact: true }).first()
    : page.getByRole('button', { name: data.order, exact: true })
  await expect(orderRecord).toBeVisible()
  if (testInfo.project.name === 'desktop') {
    const orderRow = page.getByRole('row').filter({ hasText: data.order })
    await expect(orderRow).toContainText('Confirmed')
    await orderRow.getByRole('button', { name: 'View', exact: true }).click()
    const detail = page.locator('main')
    await expect(detail).toContainText(data.customer)
    await expect(detail).toContainText(data.product)
    await expect(detail).toContainText(data.variant)
    await expect(detail).toContainText(data.imei)
  }
  await capture(page, testInfo, 'sales-order')
  await page.keyboard.press('Escape')

  await page.getByRole('tab', { name: /Deliveries|تحویل‌ها/i }).click()
  const deliveryRecord = testInfo.project.name === 'desktop'
    ? page.getByRole('row').filter({ hasText: data.delivery })
    : page.getByRole('button', { name: data.delivery, exact: true }).locator('xpath=ancestor::article')
  await expect(deliveryRecord).toContainText(/Confirmed|تایید|تأیید/i)
  await capture(page, testInfo, 'delivery')
  await page.getByRole('tab', { name: /Sales Invoices|فاکتورهای فروش/i }).click()
  const invoiceRecord = page.getByRole('row').filter({ hasText: data.invoice })
  await expect(invoiceRecord).toContainText(/Issued/i)
  await capture(page, testInfo, 'invoice')

  await page.reload({ waitUntil: 'networkidle' })
  await page.getByRole('tab', { name: /Sales Invoices|فاکتورهای فروش/i }).click()
  const persistedInvoice = page.getByRole('row').filter({ hasText: data.invoice })
  await expect(persistedInvoice).toContainText(/Issued/i)
  expect(consoleErrors).toEqual([])
  expect(failedRequests).toEqual([])
})
