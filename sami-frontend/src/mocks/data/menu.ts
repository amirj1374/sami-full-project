import type { MenuItem } from '@/types/models'
import { lifecycleOf } from './lifecycle'

/**
 * The navigation menu used in Mock Mode (mirrors the seeded backend modules).
 *
 * Each entry carries the same lifecycle block the real `/v1/menu` returns, so
 * placeholder behaviour is exercised from backend data rather than from route
 * existence. `knowledge` and `portal` are deliberately included: they have no
 * Vue screens, which is exactly the case the lifecycle feature exists to
 * describe honestly.
 */
export const mockMenu: MenuItem[] = [
  { code: 'dashboards', name: 'داشبوردها', icon: 'mdi-view-dashboard-variant', path: '/dashboards', displayOrder: 15, ...lifecycleOf('ACTIVE', 'ACTIVE', 100) },
  { code: 'customers', name: 'مشتریان', icon: 'mdi-account-heart', path: '/customers', displayOrder: 20, ...lifecycleOf('ACTIVE', 'ACTIVE', 100) },
  { code: 'suppliers', name: 'تأمین‌کنندگان', icon: 'mdi-truck', path: '/suppliers', displayOrder: 22, ...lifecycleOf('ACTIVE', 'ACTIVE', 100) },
  { code: 'products', name: 'محصولات', icon: 'mdi-package-variant-closed', path: '/products', displayOrder: 30, ...lifecycleOf('ACTIVE', 'ACTIVE', 100) },
  { code: 'purchasing', name: 'خرید', icon: 'mdi-cart-arrow-down', path: '/purchases', displayOrder: 35, ...lifecycleOf('ACTIVE', 'ACTIVE', 100) },
  { code: 'users', name: 'کاربران', icon: 'mdi-account-group', path: '/users', displayOrder: 40, ...lifecycleOf('ACTIVE', 'ACTIVE', 100) },
  { code: 'roles', name: 'نقش‌ها', icon: 'mdi-shield-account', path: '/roles', displayOrder: 50, ...lifecycleOf('ACTIVE', 'ACTIVE', 100) },
  { code: 'permissions', name: 'دسترسی‌ها', icon: 'mdi-key', path: '/permissions', displayOrder: 60, ...lifecycleOf('ACTIVE', 'ACTIVE', 100) },
  { code: 'modules', name: 'ماژول‌ها', icon: 'mdi-view-module', path: '/modules', displayOrder: 70, ...lifecycleOf('ACTIVE', 'ACTIVE', 100) },

  // Backend complete, no screens yet.
  { code: 'knowledge', name: 'پایگاه دانش', icon: 'mdi-book-open-variant', path: '/knowledge', displayOrder: 80, ...lifecycleOf('ACTIVE', 'PLANNED', 60) },
  // Part-built on both axes.
  { code: 'portal', name: 'پورتال مشتریان', icon: 'mdi-account-box-outline', path: '/portal', displayOrder: 90, ...lifecycleOf('IN_DEVELOPMENT', 'PLANNED', 35) },
]
