import { defineConfig, devices } from '@playwright/test'
import path from 'node:path'
import fs from 'node:fs'

const fixtureFile = path.join(import.meta.dirname, 'tests/browser/readiness-fixtures.env')
try {
  const fixtureText = fs.readFileSync(fixtureFile, 'utf8')
  for (const line of fixtureText.split(/\r?\n/)) {
    const match = line.match(/^([A-Z0-9_]+)=(.*)$/)
    if (match && !process.env[match[1]]) process.env[match[1]] = match[2]
  }
} catch {
  // Fixture handoff is optional for unrelated browser tests.
}

const baseURL = process.env.SAMI_E2E_BASE_URL ?? 'http://127.0.0.1:7474'
const authState = path.join(import.meta.dirname, 'test-results/.auth/user.json')

export default defineConfig({
  testDir: './tests/browser',
  timeout: 45_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  reporter: [['list'], ['json', { outputFile: 'test-results/real-user-acceptance.json' }]],
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    locale: 'en-US',
    viewport: { width: 1440, height: 900 },
  },
  projects: [
    { name: 'cold-auth', testMatch: /harness\.spec\.ts/, use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } } },
    { name: 'auth-setup', testMatch: /auth\.setup\.ts/ },
    { name: 'desktop', testIgnore: /harness\.spec\.ts/, dependencies: ['auth-setup'], use: { ...devices['Desktop Chrome'], storageState: authState, viewport: { width: 1440, height: 900 } } },
    { name: 'tablet', testIgnore: /harness\.spec\.ts/, dependencies: ['auth-setup'], use: { ...devices['iPad Mini'], storageState: authState, viewport: { width: 768, height: 1024 } } },
    { name: 'mobile', testIgnore: /harness\.spec\.ts/, dependencies: ['auth-setup'], use: { ...devices['Pixel 5'], storageState: authState, viewport: { width: 393, height: 851 } } },
  ],
})
