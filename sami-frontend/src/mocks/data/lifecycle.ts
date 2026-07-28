import type { ModuleStatus } from '@/types/models'

/**
 * The configurable lifecycle stages, mirroring the `module_statuses` rows
 * seeded by V25. Flags — not codes — drive behaviour, so Mock Mode exercises
 * the same data-driven paths as the real backend.
 */
export const mockLifecycleStatuses: ModuleStatus[] = [
  {
    code: 'PLANNED',
    name: 'برنامه‌ریزی‌شده',
    description: 'برای توسعه در آینده زمان‌بندی شده است؛ کاری آغاز نشده',
    color: '#9E9E9E',
    icon: 'mdi-calendar-clock',
    lifecycleRank: 10,
    appliesToBackend: true,
    appliesToFrontend: true,
    navigable: true,
    showsPlaceholder: true,
    productionReady: false,
    terminal: false,
  },
  {
    code: 'IN_DEVELOPMENT',
    name: 'در حال توسعه',
    description: 'در حال ساخت است',
    color: '#FB8C00',
    icon: 'mdi-hammer-wrench',
    lifecycleRank: 20,
    appliesToBackend: true,
    appliesToFrontend: true,
    navigable: true,
    showsPlaceholder: true,
    productionReady: false,
    terminal: false,
  },
  {
    code: 'BACKEND_READY',
    name: 'بک‌اند آماده',
    description: 'پیاده‌سازی سمت سرور کامل است؛ رابط کاربری در انتظار',
    color: '#00897B',
    icon: 'mdi-server-network',
    lifecycleRank: 40,
    appliesToBackend: true,
    appliesToFrontend: false,
    navigable: true,
    showsPlaceholder: true,
    productionReady: false,
    terminal: false,
  },
  {
    code: 'FRONTEND_READY',
    name: 'رابط کاربری آماده',
    description: 'رابط کاربری کامل است؛ پیاده‌سازی سمت سرور در انتظار',
    color: '#3949AB',
    icon: 'mdi-monitor-dashboard',
    lifecycleRank: 40,
    appliesToBackend: false,
    appliesToFrontend: true,
    navigable: true,
    showsPlaceholder: false,
    productionReady: false,
    terminal: false,
  },
  {
    code: 'BETA',
    name: 'بتا',
    description: 'قابل استفاده و کامل، در حال اعتبارسنجی',
    color: '#8E24AA',
    icon: 'mdi-test-tube',
    lifecycleRank: 60,
    appliesToBackend: true,
    appliesToFrontend: true,
    navigable: true,
    showsPlaceholder: false,
    productionReady: false,
    terminal: false,
  },
  {
    code: 'ACTIVE',
    name: 'فعال',
    description: 'منتشرشده و پشتیبانی‌شده',
    color: '#2E7D32',
    icon: 'mdi-check-circle',
    lifecycleRank: 80,
    appliesToBackend: true,
    appliesToFrontend: true,
    navigable: true,
    showsPlaceholder: false,
    productionReady: true,
    terminal: false,
  },
  {
    code: 'DEPRECATED',
    name: 'منسوخ‌شده',
    description: 'هنوز در دسترس است اما برای حذف زمان‌بندی شده',
    color: '#E53935',
    icon: 'mdi-alert-circle',
    lifecycleRank: 90,
    appliesToBackend: true,
    appliesToFrontend: true,
    navigable: true,
    showsPlaceholder: false,
    productionReady: true,
    terminal: true,
  },
  {
    code: 'RETIRED',
    name: 'بازنشسته',
    description: 'از سرویس خارج شده است',
    color: '#607D8B',
    icon: 'mdi-archive-off',
    lifecycleRank: 100,
    appliesToBackend: true,
    appliesToFrontend: true,
    navigable: false,
    showsPlaceholder: true,
    productionReady: false,
    terminal: true,
  },
]

const byCode = (code: string): ModuleStatus =>
  mockLifecycleStatuses.find((s) => s.code === code) ?? mockLifecycleStatuses[0]

/** Overall = the least advanced axis, unless one is terminal (mirrors ModuleLifecycle). */
export function deriveOverall(backend: ModuleStatus, frontend: ModuleStatus): ModuleStatus {
  if (backend.terminal || frontend.terminal) {
    if (backend.terminal && frontend.terminal) {
      return backend.lifecycleRank >= frontend.lifecycleRank ? backend : frontend
    }
    return backend.terminal ? backend : frontend
  }
  return backend.lifecycleRank <= frontend.lifecycleRank ? backend : frontend
}

/** Builds the lifecycle block the API attaches to menu items and modules. */
export function lifecycleOf(backendCode: string, frontendCode: string, progress: number) {
  const backendStatus = byCode(backendCode)
  const frontendStatus = byCode(frontendCode)
  const overallStatus = deriveOverall(backendStatus, frontendStatus)
  return {
    backendStatus,
    frontendStatus,
    overallStatus,
    overallStatusDerived: true,
    progressPercentage: progress,
    releaseVersion: null,
    showPlaceholder: frontendStatus.showsPlaceholder || overallStatus.showsPlaceholder,
    navigable: overallStatus.navigable,
  }
}
