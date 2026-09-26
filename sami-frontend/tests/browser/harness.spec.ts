import { test, expect } from '@playwright/test'

test('real browser harness authenticates, renders, refreshes and captures runtime evidence', async ({ page }) => {
  test.setTimeout(120_000)
  const consoleErrors: string[] = []
  const failedRequests: string[] = []
  page.on('console', message => { if (message.type() === 'error') consoleErrors.push(message.text()) })
  page.on('requestfailed', request => {
    const reason = request.failure()?.errorText ?? ''
    // Chromium reports requests cancelled by an intentional route reload as
    // ERR_ABORTED. They are not network failures; retain all other failures.
    if (!/ERR_ABORTED|Load request cancelled/i.test(reason)) {
      failedRequests.push(`${request.method()} ${request.url()} ${reason}`)
    }
  })

  // The dev bundle can take a few seconds to optimize on a cold, mounted
  // workspace. Wait for the actual login affordance rather than assuming a
  // semantic heading exists on the splash screen.
  await page.goto('/', { waitUntil: 'domcontentloaded', timeout: 60_000 })
  await expect(page).toHaveTitle(/SAMI|ERP/i, { timeout: 60_000 })
  await expect(page.getByRole('textbox').first()).toBeVisible({ timeout: 60_000 })
  await page.screenshot({ path: 'test-results/harness-login.png', fullPage: true })

  const email = process.env.SAMI_E2E_EMAIL
  const password = process.env.SAMI_E2E_PASSWORD
  test.skip(!email || !password, 'SAMI_E2E_EMAIL and SAMI_E2E_PASSWORD are required; never commit credentials')
  await page.getByRole('textbox', { name: /email|ایمیل/i }).fill(email!)
  await page.getByRole('textbox', { name: /password|رمز عبور/i }).fill(password!)
  await page.getByRole('button', { name: /login|sign in|ورود/i }).click()
  await expect(page).not.toHaveURL(/login|auth/i)
  await page.screenshot({ path: 'test-results/harness-authenticated.png', fullPage: true })
  const routeBefore = page.url()
  await page.reload()
  await expect(page).toHaveURL(routeBefore)
  expect(consoleErrors, consoleErrors.join('\n')).toEqual([])
  expect(failedRequests, failedRequests.join('\n')).toEqual([])
})
