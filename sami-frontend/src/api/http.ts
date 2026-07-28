import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import type { ApiResponse } from '@/types/api'
import type { AuthResult, CurrentUser } from '@/types/models'
import { tokenStorage } from './tokenStorage'

/**
 * Central Axios instance.
 *
 * Responsibilities:
 *  - attach the Bearer access token on every request;
 *  - transparently refresh the access token once on a 401 and replay the request;
 *  - surface a normalized `ApiError` so callers get consistent error handling.
 */
const baseURL = import.meta.env.VITE_API_BASE_URL ?? '/api'

export const http: AxiosInstance = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStorage.getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// --- Single-flight refresh handling ---------------------------------------
// If several requests fail with 401 at once, we refresh only once and let the
// others wait for that single refresh to resolve.
let refreshPromise: Promise<string> | null = null

/** Callback invoked when refresh definitively fails (e.g. to log the user out). */
let onAuthFailure: (() => void) | null = null
export function registerAuthFailureHandler(handler: () => void): void {
  onAuthFailure = handler
}

/**
 * Callback invoked with the refreshed profile after a successful token refresh,
 * so the auth store can keep the user's permissions current.
 */
let onAuthRefresh: ((user: CurrentUser) => void) | null = null
export function registerAuthRefreshHandler(handler: (user: CurrentUser) => void): void {
  onAuthRefresh = handler
}

async function refreshAccessToken(): Promise<string> {
  const refreshToken = tokenStorage.getRefreshToken()
  if (!refreshToken) {
    throw new Error('No refresh token available')
  }

  // Use a bare axios call so we don't recurse through this instance's interceptors.
  const { data } = await axios.post<ApiResponse<AuthResult>>(
    `${baseURL}/v1/auth/refresh`,
    { refreshToken },
    { headers: { 'Content-Type': 'application/json' } },
  )
  const result = data.data
  if (!result) {
    throw new Error('Empty refresh response')
  }
  tokenStorage.set(result.accessToken, result.refreshToken)
  onAuthRefresh?.(result.user)
  return result.accessToken
}

http.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined
    const status = error.response?.status
    const isRefreshCall = original?.url?.includes('/auth/refresh')

    if (status === 401 && original && !original._retry && !isRefreshCall) {
      original._retry = true
      try {
        refreshPromise ??= refreshAccessToken().finally(() => {
          refreshPromise = null
        })
        const newToken = await refreshPromise
        original.headers.Authorization = `Bearer ${newToken}`
        return http(original)
      } catch {
        tokenStorage.clear()
        onAuthFailure?.()
      }
    }

    return Promise.reject(error)
  },
)

/**
 * Unwraps the standard envelope, returning `data` on success and throwing the
 * `ApiError` (with a fallback) otherwise. Keeps call sites clean.
 */
export async function unwrap<T>(promise: Promise<AxiosResponse<ApiResponse<T>>>): Promise<T> {
  try {
    const { data } = await promise
    if (data.success && data.data !== null) {
      return data.data
    }
    throw data.error ?? { code: 'UNKNOWN', message: 'Unexpected empty response' }
  } catch (err) {
    if (axios.isAxiosError(err)) {
      const apiError = (err.response?.data as ApiResponse<unknown> | undefined)?.error
      throw apiError ?? { code: 'NETWORK_ERROR', message: err.message }
    }
    throw err
  }
}

/**
 * Like {@link unwrap}, but for endpoints whose success payload is empty
 * (`ApiResponse.ok()` with `data: null`): resolves on success, throws the
 * normalized `ApiError` otherwise.
 */
export async function unwrapVoid(
  promise: Promise<AxiosResponse<ApiResponse<unknown>>>,
): Promise<void> {
  try {
    const { data } = await promise
    if (data.success) {
      return
    }
    throw data.error ?? { code: 'UNKNOWN', message: 'Unexpected error response' }
  } catch (err) {
    if (axios.isAxiosError(err)) {
      const apiError = (err.response?.data as ApiResponse<unknown> | undefined)?.error
      throw apiError ?? { code: 'NETWORK_ERROR', message: err.message }
    }
    throw err
  }
}
