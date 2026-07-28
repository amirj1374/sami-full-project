/**
 * Small persistence layer for auth tokens.
 *
 * Kept separate from the Pinia store so the Axios interceptors can read/write
 * tokens without importing the store (which would create a circular dependency).
 * localStorage is used for simplicity; swap for httpOnly cookies if the threat
 * model requires XSS-resistant storage.
 */
const ACCESS_KEY = 'sami.accessToken'
const REFRESH_KEY = 'sami.refreshToken'

export const tokenStorage = {
  getAccessToken: (): string | null => localStorage.getItem(ACCESS_KEY),
  getRefreshToken: (): string | null => localStorage.getItem(REFRESH_KEY),

  set(accessToken: string, refreshToken: string): void {
    localStorage.setItem(ACCESS_KEY, accessToken)
    localStorage.setItem(REFRESH_KEY, refreshToken)
  },

  clear(): void {
    localStorage.removeItem(ACCESS_KEY)
    localStorage.removeItem(REFRESH_KEY)
  },
}
