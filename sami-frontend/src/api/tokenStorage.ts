/**
 * Small persistence layer for auth tokens.
 *
 * Kept separate from the Pinia store so the Axios interceptors can read/write
 * tokens without importing the store (which would create a circular dependency).
 * localStorage is used for simplicity; swap for httpOnly cookies if the threat
 * model requires XSS-resistant storage.
 */
import { browserStorage } from '@/services/browserStorage'

const ACCESS_KEY = 'sami.accessToken'
const REFRESH_KEY = 'sami.refreshToken'

export const tokenStorage = {
  getAccessToken: (): string | null => browserStorage.getItem(ACCESS_KEY),
  getRefreshToken: (): string | null => browserStorage.getItem(REFRESH_KEY),

  set(accessToken: string, refreshToken: string): void {
    browserStorage.setItem(ACCESS_KEY, accessToken)
    browserStorage.setItem(REFRESH_KEY, refreshToken)
  },

  clear(): void {
    browserStorage.removeItem(ACCESS_KEY)
    browserStorage.removeItem(REFRESH_KEY)
  },
}
