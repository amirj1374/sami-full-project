import { test, expect } from '@playwright/test'

test('creates an active warehouse in the selected Company and Branch context', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Mutation runs once; responsive verification reuses persisted state')
  const marker = Date.now().toString(36).toUpperCase()
  const code = `WH-${marker}`
  const name = `Browser Warehouse ${marker}`
  const consoleErrors: string[] = []
  const failedRequests: string[] = []
  page.on('console', (message) => { if (message.type() === 'error') consoleErrors.push(message.text()) })
  page.on('requestfailed', (request) => { const reason = request.failure()?.errorText ?? ''; if (!/ERR_ABORTED|Load request cancelled/i.test(reason)) failedRequests.push(`${request.method()} ${request.url()} ${reason}`) })

  await page.goto('/inventory', { waitUntil: 'domcontentloaded' })
  await expect(page).toHaveURL(/\/inventory$/)
  await page.getByRole('tab', { name: /Warehouses|انبارها/i }).click()
  await page.getByRole('button', { name: /New warehouse|انبار جدید/i }).click()
  const dialog = page.getByRole('dialog', { name: /New warehouse|انبار جدید/i })
  await expect(dialog).toBeVisible()
  await expect(dialog.getByRole('spinbutton', { name: /Company ID|شناسه شرکت/i })).not.toHaveValue('')
  await expect(dialog.getByRole('spinbutton', { name: /Branch ID|شناسه شعبه/i })).not.toHaveValue('')
  await dialog.getByRole('textbox', { name: /Code|کد/i }).fill(code)
  await dialog.getByRole('textbox', { name: /Name|نام/i }).fill(name)
  const save = dialog.getByRole('button', { name: /Save|ذخیره/i })
  await expect(save).toBeEnabled()
  const created = page.waitForResponse((response) => /\/api\/v1\/inventory\/warehouses$/.test(response.url()) && response.request().method() === 'POST')
  await save.click()
  expect((await created).status()).toBe(201)
  await expect(page.getByText(/Warehouse saved successfully|انبار با موفقیت ذخیره شد/i)).toBeVisible()
  await expect(page.getByText(name, { exact: true })).toBeVisible()
  await page.reload({ waitUntil: 'domcontentloaded' })
  await page.getByRole('tab', { name: /Warehouses|انبارها/i }).click()
  await expect(page.getByText(name, { exact: true })).toBeVisible()
  expect(consoleErrors, consoleErrors.join('\n')).toEqual([])
  expect(failedRequests, failedRequests.join('\n')).toEqual([])
})
