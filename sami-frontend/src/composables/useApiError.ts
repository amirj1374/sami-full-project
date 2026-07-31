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
  const code = ref<string | null>(null)
  const fieldErrors = ref<ApiError['fieldErrors']>(null)

  function friendlyMessage(errorCode?: string): string {
    const normalized = (errorCode ?? '').toUpperCase()
    if (normalized.includes('NETWORK')) return i18n.global.t('apiError.network')
    if (normalized.includes('TIMEOUT')) return i18n.global.t('apiError.timeout')
    if (normalized.includes('UNAUTHORIZED') || normalized === '401') return i18n.global.t('apiError.unauthorized')
    if (normalized.includes('FORBIDDEN') || normalized === '403') return i18n.global.t('apiError.forbidden')
    if (normalized.includes('NOT_FOUND') || normalized === '404') return i18n.global.t('apiError.notFound')
    if (normalized.includes('CONFLICT') || normalized.includes('VERSION') || normalized === '409') return i18n.global.t('apiError.conflict')
    if (normalized.includes('VALIDATION') || normalized === '422') return i18n.global.t('apiError.validation')
    if (normalized.includes('INTERNAL') || normalized === '500') return i18n.global.t('apiError.server')
    return i18n.global.t('apiError.generic')
  }

  function set(err: unknown): void {
    if (isApiError(err)) {
      code.value = err.code
      fieldErrors.value = err.fieldErrors
      message.value = friendlyMessage(err.code)
    } else {
      code.value = err instanceof Error && err.name === 'TimeoutError' ? 'TIMEOUT' : 'UNKNOWN'
      message.value = friendlyMessage(code.value)
    }
  }

  function clear(): void {
    message.value = null
    code.value = null
    fieldErrors.value = null
  }

  return { message, code, fieldErrors, set, clear }
}
