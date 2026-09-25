import { ref } from 'vue'
import type { ApiError } from '@/types/api'
import { i18n } from '@/i18n'
import { mapApiErrorMessage, type ApiErrorScope } from '@/services/apiErrorMapper'

/** Type guard for the backend's ApiError shape. */
function isApiError(value: unknown): value is ApiError {
  return typeof value === 'object' && value !== null && 'code' in value && 'message' in value
}

/**
 * Normalizes thrown values (ApiError, Error, unknown) into a single reactive
 * message for display, so views don't repeat error-shaping logic.
 */
export function useApiError(options: { scope?: ApiErrorScope } = {}) {
  const message = ref<string | null>(null)
  const code = ref<string | null>(null)
  const fieldErrors = ref<ApiError['fieldErrors']>(null)
  const translate = (key: string) => i18n.global.t(key)

  function set(err: unknown): void {
    if (isApiError(err)) {
      code.value = err.code
      fieldErrors.value = err.fieldErrors
      message.value = mapApiErrorMessage(err, translate, options.scope)
    } else {
      code.value = err instanceof Error && err.name === 'TimeoutError' ? 'TIMEOUT' : 'UNKNOWN'
      message.value = mapApiErrorMessage(
        { code: code.value, message: '' },
        translate,
        options.scope,
      )
    }
  }

  function clear(): void {
    message.value = null
    code.value = null
    fieldErrors.value = null
  }

  return { message, code, fieldErrors, set, clear }
}
