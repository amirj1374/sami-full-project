import { test, expect } from '@playwright/test'

const order = process.env.SAMI_E2E_ORDER_NUMBER!
const product = process.env.SAMI_E2E_PRODUCT_NAME!
const variant = process.env.SAMI_E2E_VARIANT_NAME!
const imei = process.env.SAMI_E2E_IMEI!

test.describe('responsive organization context diagnostic', () => {
  test.setTimeout(120_000)
  for (const project of ['tablet', 'mobile']) {
    test(`${project} context → sales → certified order`, async ({ page }, testInfo) => {
      test.skip(testInfo.project.name !== project)
      expect(order && product && variant && imei).toBeTruthy()
      const events: string[] = []
      const errors: string[] = []
      page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()) })
      page.on('response', async (response) => {
        if (/organization\/context|sales-orders/.test(response.url())) {
          events.push(`${response.status()} ${response.request().method()} ${response.url()}`)
        }
      })

      await page.goto('/sales', { waitUntil: 'networkidle' })
      const contextButton = page.getByRole('button').filter({ hasText: /Branch|شعبه|Company|شرکت|Browser/i }).first()
      await expect(contextButton).toBeVisible()
      await contextButton.click()
      const branch = page.getByRole('menuitem', { name: /Browser Branch MUJSXNRG-0/i }).first()
      await expect(branch).toBeVisible()
      const contextPut = page.waitForResponse((r) => r.request().method() === 'PUT' && /organization\/context$/.test(r.url()) && r.ok())
      await branch.click()
      await contextPut

      await page.goto('/sales', { waitUntil: 'networkidle' })
      await page.getByRole('tab', { name: /Sales Orders|سفارش‌های فروش/i }).click()
      const salesResponse = await page.waitForResponse((r) => r.request().method() === 'GET' && /sales-orders\?/.test(r.url()) && r.ok())
      expect(salesResponse.url()).toContain('companyId=5')
      expect(salesResponse.url()).toContain('branchId=12')
      await expect(page.getByRole('button', { name: order, exact: true })).toBeVisible()
      await page.getByRole('button', { name: order, exact: true }).click()
      const main = page.locator('main')
      await expect(main).toContainText(product)
      await expect(main).toContainText(variant)
      await expect(main).toContainText(imei)
      await page.reload({ waitUntil: 'networkidle' })
      await page.getByRole('tab', { name: /Sales Orders|سفارش‌های فروش/i }).click()
      await page.getByRole('button', { name: order, exact: true }).click()
      await expect(page.locator('main')).toContainText(imei)
      await testInfo.attach(`${project}-context-events`, { body: events.join('\n'), contentType: 'text/plain' })
      expect(errors).toEqual([])
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy()
    })
  }
})
