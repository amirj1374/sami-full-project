import { computed, type ComputedRef } from 'vue'
import { useAuthStore } from '@/stores/auth'

export interface UsePermission {
  can: (code: string) => boolean
  canAny: (codes: string[]) => boolean
  canAll: (codes: string[]) => boolean
  isSuperAdmin: ComputedRef<boolean>
}

/**
 * Permission checks for components, delegating to the auth store (so super
 * admins always pass). Components use this composable or the `v-can`
 * directive — never the store's permission array directly.
 */
export function usePermission(): UsePermission {
  const auth = useAuthStore()

  return {
    can: (code) => auth.can(code),
    canAny: (codes) => auth.canAny(codes),
    canAll: (codes) => auth.canAll(codes),
    isSuperAdmin: computed(() => auth.isSuperAdmin),
  }
}
