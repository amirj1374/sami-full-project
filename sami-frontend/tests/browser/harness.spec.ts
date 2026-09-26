import { test, expect } from '@playwright/test'

test('real browser harness authenticates, renders, refreshes and captures runtime evidence', async ({ page }) => {
  const consoleErrors: string[] = []
  const failedRequests: string[] = []
  page.on('console', message => { if (message.type() === 'error') consoleErrors.push(message.text()) })
  page.on('requestfailed', request => failedRequests.push(`${request.method()} ${request.url()} ${request.failure()?.errorText ?? ''}`))

  await page.goto('/')
  await expect(page).toHaveTitle(/SAMI|ERP/i)
  await expect(page.getByRole('heading').first()).toBeVisible()
  await page.screenshot({ path: 'test-results/harness-login.png', fullPage: true })

  const email = process.env.SAMI_E2E_EMAIL
  const password = process.env.SAMI_E2E_PASSWORD
  test.skip(!email || !password, 'SAMI_E2E_EMAIL and SAMI_E2E_PASSWORD are required; never commit credentials')
  await page.getByLabel(/email/i).fill(email!)
  await page.getByLabel(/password/i).fill(password!)
  await page.getByRole('button', { name: /login|sign in|ورود/i }).click()
  await expect(page).not.toHaveURL(/login|auth/i)
  await page.screenshot({ path: 'test-results/harness-authenticated.png', fullPage: true })
  const routeBefore = page.url()
  await page.reload()
  await expect(page).toHaveURL(routeBefore)
  expect(consoleErrors, consoleErrors.join('\n')).toEqual([])
  expect(failedRequests, failedRequests.join('\n')).toEqual([])
})
