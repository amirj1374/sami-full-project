import { test, expect, type Locator, type Page } from '@playwright/test'

async function choose(page: Page, control: Locator, option: string) {
  await control.focus()
  await control.press('ArrowDown')
  await page.getByRole('option', { name: option, exact: true }).click()
}

test('issues the delivered phone order invoice and proves final inventory and IMEI state', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Continuation mutation runs once')
  test.setTimeout(90_000)
  const orderNumber = process.env.SAMI_E2E_ORDER_NUMBER!
  const productName = process.env.SAMI_E2E_PRODUCT_NAME!
  const imei = process.env.SAMI_E2E_IMEI!
  expect(orderNumber && productName && imei).toBeTruthy()

  await page.goto('/sales', { waitUntil: 'domcontentloaded' })
  const languageButton = page.getByRole('button', { name: /English|فارسی/i })
  if ((await languageButton.textContent())?.trim() !== 'English') {
    await languageButton.click()
    await page.getByText('English', { exact: true }).last().click()
  }
  await page.getByRole('tab', { name: 'Sales Invoices', exact: true }).click()
  await page.getByRole('button', { name: 'New invoice', exact: true }).click()
  const invoiceDialog = page.getByRole('dialog').filter({ hasText: 'New invoice' })
  await choose(page, invoiceDialog.getByRole('combobox', { name: 'Sales order', exact: true }), orderNumber)
  await expect(invoiceDialog.getByRole('spinbutton', { name: 'Quantity', exact: true })).toHaveValue('1')
  const invoiceResponse = page.waitForResponse((response) => /\/api\/v1\/sales-invoices$/.test(response.url()) && response.request().method() === 'POST')
  await invoiceDialog.getByRole('button', { name: 'Save', exact: true }).click()
  const invoiceCreated = await invoiceResponse
  expect(invoiceCreated.ok()).toBeTruthy()
  const invoice = (await invoiceCreated.json()).data
  const issueResponse = page.waitForResponse((response) => new RegExp(`/api/v1/sales-invoices/${invoice.id}/issue$`).test(response.url()))
  await page.getByRole('button', { name: 'Issue', exact: true }).click()
  expect((await issueResponse).ok()).toBeTruthy()
  await expect(page.getByText('ISSUED', { exact: true })).toBeVisible()
  await page.keyboard.press('Escape')
  await page.reload({ waitUntil: 'domcontentloaded' })
  await page.getByRole('tab', { name: 'Sales Invoices', exact: true }).click()
  await expect(page.getByRole('row').filter({ hasText: invoice.number })).toContainText('ISSUED')

  await page.goto('/inventory', { waitUntil: 'domcontentloaded' })
  await page.getByRole('textbox', { name: 'Search', exact: true }).fill(productName)
  const finalBalance = page.getByRole('row').filter({ hasText: productName })
  await expect(finalBalance.getByRole('cell').nth(2)).toHaveText('0')
  await expect(finalBalance.getByRole('cell').nth(3)).toHaveText('0')
  await expect(finalBalance.getByRole('cell').nth(4)).toHaveText('0')
  await page.getByRole('tab', { name: 'Monitoring', exact: true }).click()
  await page.getByRole('tab', { name: 'Serials / IMEI', exact: true }).click()
  await page.getByRole('textbox', { name: 'Search', exact: true }).fill(imei)
  await expect(page.getByRole('row').filter({ hasText: imei })).toContainText('Issued')
  await page.reload({ waitUntil: 'domcontentloaded' })
  await page.getByRole('tab', { name: 'Monitoring', exact: true }).click()
  await page.getByRole('tab', { name: 'Serials / IMEI', exact: true }).click()
  await page.getByRole('textbox', { name: 'Search', exact: true }).fill(imei)
  await expect(page.getByRole('row').filter({ hasText: imei })).toContainText('Issued')
})
