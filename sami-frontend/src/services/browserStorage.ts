/**
 * Browser storage access that is safe in restricted/privacy-mode contexts.
 *
 * Some browsers expose localStorage but throw when it is read or written
 * (for example, when storage is disabled or blocked for the current origin).
 * A small process-local fallback keeps the current tab usable while avoiding
 * an exception during application bootstrap or an API request interceptor.
 */
const fallback = new Map<string, string>()

function storage(): Storage | null {
  if (typeof window === 'undefined') return null
  try {
    return window.localStorage
  } catch {
    return null
  }
}

export const browserStorage = {
  getItem(key: string): string | null {
    const persistent = storage()
    if (persistent) {
      try {
        const value = persistent.getItem(key)
        if (value !== null) {
          fallback.delete(key)
          return value
        }
      } catch {
        // Fall through to the tab-local fallback.
      }
    }
    return fallback.get(key) ?? null
  },

  setItem(key: string, value: string): void {
    const persistent = storage()
    if (persistent) {
      try {
        persistent.setItem(key, value)
        fallback.delete(key)
        return
      } catch {
        // Fall through to the tab-local fallback.
      }
    }
    fallback.set(key, value)
  },

  removeItem(key: string): void {
    const persistent = storage()
    if (persistent) {
      try {
        persistent.removeItem(key)
      } catch {
        // The fallback still needs to be cleared below.
      }
    }
    fallback.delete(key)
  },
}
