import { ref } from 'vue'
import type { ApiError } from '@/types/api'
import { i18n } from '@/i18n'

/** Type guard for the backend's ApiError shape. */
function isApiError(value: unknown): value is ApiError {
  return typeof value === 'object' && value !== null && 'code' in value && 'message' in value
}

/**
 * Normalizes thrown values (ApiError, Error, unknown) into a single reactive
 * message for display, so views don't repeat error-shaping logic.
 */
export function useApiError() {
  const message = ref<string | null>(null)

  function set(err: unknown): void {
    if (isApiError(err)) {
      message.value = err.message
    } else if (err instanceof Error) {
      message.value = err.message
    } else {
      message.value = i18n.global.t('apiError.generic')
    }
  }

  function clear(): void {
    message.value = null
  }

  return { message, set, clear }
}
