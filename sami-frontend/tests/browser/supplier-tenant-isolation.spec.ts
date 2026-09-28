import { test, expect } from '@playwright/test'

test('supplier list excludes cross-tenant records and survives reload', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Tenant isolation is proven once against the authenticated tenant')
  const legitimateSupplier = process.env.SAMI_E2E_SUPPLIER_NAME
  const crossTenantSupplier = process.env.SAMI_E2E_CROSS_TENANT_SUPPLIER_NAME
  expect(legitimateSupplier && crossTenantSupplier).toBeTruthy()

  const unexpectedFailures: string[] = []
  page.on('response', (response) => {
    if (response.status() >= 400) {
      unexpectedFailures.push(`${response.status()} ${response.request().method()} ${response.url()}`)
    }
  })

  await page.goto('/suppliers', { waitUntil: 'networkidle' })
  const languageButton = page.getByRole('button', { name: /English|فارسی/i })
  if ((await languageButton.textContent())?.trim() !== 'English') {
    await languageButton.click()
    await page.getByText('English', { exact: true }).last().click()
  }

  await expect(page.getByText(legitimateSupplier!, { exact: true }).first()).toBeVisible()
  await expect(page.getByText(crossTenantSupplier!, { exact: true })).toHaveCount(0)

  await page.reload({ waitUntil: 'networkidle' })
  await expect(page.getByText(legitimateSupplier!, { exact: true }).first()).toBeVisible()
  await expect(page.getByText(crossTenantSupplier!, { exact: true })).toHaveCount(0)
  expect(unexpectedFailures).toEqual([])
})
