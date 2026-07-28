import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { CurrentUser } from '@/types/models'
import { authApi, type LoginPayload, type RegisterPayload } from '@/api/auth'
import { tokenStorage } from '@/api/tokenStorage'
import { useMenuStore } from '@/stores/menu'
import { MOCK_MODE } from '@/mocks/config'

/**
 * Authentication state.
 *
 * Tokens live in {@link tokenStorage} (so the HTTP layer can reach them); this
 * store owns the user profile and derived auth state for the UI. All permission
 * checks funnel through {@link can} — super admins bypass every check.
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref<CurrentUser | null>(null)
  const initialized = ref(false)

  const isAuthenticated = computed(() => user.value !== null)
  const isSuperAdmin = computed(() => user.value?.isSuperAdmin ?? false)

  /** True when the user holds the permission code (always true for super admins). */
  function can(code: string): boolean {
    if (!user.value) {
      return false
    }
    return user.value.isSuperAdmin || user.value.permissions.includes(code)
  }

  /** True when the user holds at least one of the codes. */
  function canAny(codes: string[]): boolean {
    if (isSuperAdmin.value) {
      return true
    }
    return codes.some((code) => can(code))
  }

  /** True when the user holds every one of the codes. */
  function canAll(codes: string[]): boolean {
    if (isSuperAdmin.value) {
      return true
    }
    return codes.every((code) => can(code))
  }

  /** Replaces the profile (used when a token refresh returns fresh permissions). */
  function setUser(next: CurrentUser): void {
    user.value = next
  }

  async function login(payload: LoginPayload): Promise<void> {
    const result = await authApi.login(payload)
    tokenStorage.set(result.accessToken, result.refreshToken)
    user.value = result.user
  }

  async function register(payload: RegisterPayload): Promise<void> {
    const result = await authApi.register(payload)
    tokenStorage.set(result.accessToken, result.refreshToken)
    user.value = result.user
  }

  /**
   * Revokes the refresh token server-side (best effort — a dead backend must
   * not trap the user in a session), then clears local auth and menu state.
   */
  async function logout(): Promise<void> {
    const refreshToken = tokenStorage.getRefreshToken()
    if (refreshToken) {
      try {
        await authApi.logout(refreshToken)
      } catch {
        // Best effort: the local session is cleared regardless.
      }
    }
    tokenStorage.clear()
    user.value = null
    useMenuStore().reset()
  }

  /**
   * Restores the session on app start: if a token exists, fetch the profile.
   * Called once from a router guard / app bootstrap. Safe to call repeatedly.
   */
  async function initialize(): Promise<void> {
    if (initialized.value) {
      return
    }
    // Development Mock Mode: authenticate as the mock user with no backend
    // round-trip (no login / token validation / refresh request). The dynamic
    // import keeps mock data out of the production bundle; the whole branch is
    // dead code when VITE_ENABLE_MOCK_MODE is off.
    if (MOCK_MODE) {
      const { mockUser } = await import('@/mocks/data/auth')
      user.value = mockUser
      initialized.value = true
      return
    }
    if (tokenStorage.getAccessToken()) {
      try {
        user.value = await authApi.me()
      } catch {
        await logout()
      }
    }
    initialized.value = true
  }

  return {
    user,
    initialized,
    isAuthenticated,
    isSuperAdmin,
    can,
    canAny,
    canAll,
    setUser,
    login,
    register,
    logout,
    initialize,
  }
})
