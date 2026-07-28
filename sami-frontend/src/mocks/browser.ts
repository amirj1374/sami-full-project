import { setupWorker } from 'msw/browser'
import { handlers } from './handlers'

/** The MSW browser worker (Mock Mode only). */
export const worker = setupWorker(...handlers)

/** Starts the service worker; unhandled requests pass through to the network. */
export async function startMockWorker(): Promise<void> {
  await worker.start({ onUnhandledRequest: 'bypass' })
}
