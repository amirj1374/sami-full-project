import { test as setup, expect } from '@playwright/test'

const authFile = 'test-results/.auth/user.json'

setup('authenticate once for mutation journeys', async ({ page }) => {
  const email = process.env.SAMI_E2E_EMAIL
  const password = process.env.SAMI_E2E_PASSWORD
  setup.skip(!email || !password, 'SAMI_E2E_EMAIL and SAMI_E2E_PASSWORD are required; never commit credentials')

  await page.goto('/', { waitUntil: 'domcontentloaded', timeout: 60_000 })
  await expect(page.getByRole('textbox', { name: /email|ایمیل/i })).toBeVisible({ timeout: 60_000 })
  await page.getByRole('textbox', { name: /email|ایمیل/i }).fill(email!)
  await page.getByRole('textbox', { name: /password|رمز عبور/i }).fill(password!)
  await Promise.all([
    page.waitForResponse((response) => /\/api\/v1\/auth\/login$/.test(response.url()) && response.ok()),
    page.getByRole('button', { name: /login|sign in|ورود/i }).click(),
  ])
  await expect(page).not.toHaveURL(/login|auth/i)
  await page.context().storageState({ path: authFile })
})
