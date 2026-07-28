import type { AppModule } from '@/types/models'
import { lifecycleOf } from './lifecycle'

/**
 * Modules for the admin screen in Mock Mode, carrying the same lifecycle block
 * the real API returns so the lifecycle column, the axis-filtered dropdowns and
 * the validation rules are all exercised without a backend.
 */
let seq = 0
const mod = (
  code: string,
  name: string,
  icon: string,
  path: string,
  backend: string,
  frontend: string,
  progress: number,
  isSystem = false,
): AppModule => {
  const lc = lifecycleOf(backend, frontend, progress)
  return {
    id: ++seq,
    code,
    name,
    description: null,
    icon,
    path,
    displayOrder: seq * 10,
    enabled: true,
    isSystem,
    permissionCount: 6,
    ...lc,
    developmentNotes: null,
    available: true,
    productionReady: lc.overallStatus.productionReady,
  } as AppModule
}

export const mockModules: AppModule[] = [
  mod('dashboard', 'داشبورد', 'mdi-view-dashboard', '/', 'ACTIVE', 'ACTIVE', 100, true),
  mod('users', 'کاربران', 'mdi-account-group', '/users', 'ACTIVE', 'ACTIVE', 100, true),
  mod('roles', 'نقش‌ها', 'mdi-shield-account', '/roles', 'ACTIVE', 'ACTIVE', 100, true),
  mod('modules', 'ماژول‌ها', 'mdi-view-module', '/modules', 'ACTIVE', 'ACTIVE', 100, true),
  mod('products', 'محصولات', 'mdi-package-variant-closed', '/products', 'ACTIVE', 'ACTIVE', 100),
  mod('customers', 'مشتریان', 'mdi-account-heart', '/customers', 'ACTIVE', 'ACTIVE', 100),
  // Backend complete, screens pending.
  mod('knowledge', 'پایگاه دانش', 'mdi-book-open-variant', '/knowledge', 'ACTIVE', 'PLANNED', 60),
  // Part-built on both axes.
  mod('portal', 'پورتال مشتریان', 'mdi-account-box-outline', '/portal', 'IN_DEVELOPMENT', 'PLANNED', 35),
  // Schema only — matches the V26 correction.
  mod('organization', 'سازمان', 'mdi-domain', '/organization', 'IN_DEVELOPMENT', 'PLANNED', 15),
]
