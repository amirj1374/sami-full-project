import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'

/**
 * Route meta contract:
 *  - `requiresAuth`: user must be authenticated.
 *  - `permission`: permission code required to enter (checked via the auth store).
 *  - `guestOnly`: only reachable when logged out (login/register/password reset).
 */
declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    permission?: string
    guestOnly?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'layout',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      {
        path: '',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'dashboards',
        name: 'dashboards',
        component: () => import('@/views/DashboardsView.vue'),
        meta: { requiresAuth: true, permission: 'dashboards:view' },
      },
      {
        path: 'dashboards/reports',
        name: 'dashboard-reports',
        component: () => import('@/views/DashboardReportsView.vue'),
        meta: { requiresAuth: true, permission: 'dashboards:view' },
      },
      {
        path: 'dashboards/:id(\\d+)',
        name: 'dashboard-viewer',
        component: () => import('@/views/DashboardViewerView.vue'),
        meta: { requiresAuth: true, permission: 'dashboards:view' },
      },
      {
        path: 'kpis',
        name: 'kpis',
        component: () => import('@/views/KpisView.vue'),
        meta: { requiresAuth: true, permission: 'dashboards:view' },
      },
      {
        path: 'products',
        name: 'products',
        component: () => import('@/views/ProductsView.vue'),
        meta: { requiresAuth: true, permission: 'products:view' },
      },
      {
        path: 'suppliers',
        name: 'suppliers',
        component: () => import('@/views/SuppliersView.vue'),
        meta: { requiresAuth: true, permission: 'suppliers:view' },
      },
      {
        path: 'purchases',
        name: 'purchases',
        component: () => import('@/views/PurchasesView.vue'),
        meta: { requiresAuth: true, permission: 'purchasing:view' },
      },
      {
        path: 'inventory',
        name: 'inventory',
        component: () => import('@/views/InventoryView.vue'),
        meta: { requiresAuth: true, permission: 'inventory:view' },
      },
      {
        path: 'sales',
        name: 'sales',
        component: () => import('@/views/SalesView.vue'),
        meta: { requiresAuth: true, permission: 'sales:view' },
      },
      {
        path: 'customers',
        name: 'customers',
        component: () => import('@/views/CustomersView.vue'),
        meta: { requiresAuth: true, permission: 'customers:view' },
      },
      {
        path: 'automation',
        alias: 'automations',
        name: 'automations',
        component: () => import('@/views/AutomationsView.vue'),
        meta: { requiresAuth: true, permission: 'automation:view' },
      },
      {
        path: 'scheduler',
        name: 'scheduler',
        component: () => import('@/views/SchedulerView.vue'),
        meta: { requiresAuth: true, permission: 'scheduler:view' },
      },
      {
        path: 'data-quality',
        name: 'data-quality',
        component: () => import('@/views/DataQualityView.vue'),
        meta: { requiresAuth: true, permission: 'data-quality:view' },
      },
      {
        path: 'legacy-imports',
        name: 'legacy-imports',
        component: () => import('@/views/LegacyImportsView.vue'),
        meta: { requiresAuth: true, permission: 'legacy-import:view' },
      },
      {
        path: 'hamta',
        name: 'hamta',
        component: () => import('@/views/HamtaView.vue'),
        meta: { requiresAuth: true, permission: 'hamta:view' },
      },
      {
        path: 'market-sync',
        name: 'market-sync',
        component: () => import('@/views/MarketSyncView.vue'),
        meta: { requiresAuth: true, permission: 'market-sync:view' },
      },
      {
        path: 'sim-investment',
        name: 'sim-investment',
        component: () => import('@/views/SimInvestmentView.vue'),
        meta: { requiresAuth: true, permission: 'sim-investment:view' },
      },
      {
        path: 'attendance',
        name: 'attendance',
        component: () => import('@/views/AttendanceView.vue'),
        meta: { requiresAuth: true, permission: 'attendance:view' },
      },
      {
        path: 'licensing',
        name: 'licensing',
        component: () => import('@/views/LicensingView.vue'),
        meta: { requiresAuth: true, permission: 'licensing:view' },
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/admin/UsersView.vue'),
        meta: { requiresAuth: true, permission: 'users:view' },
      },
      {
        path: 'roles',
        name: 'roles',
        component: () => import('@/views/admin/RolesView.vue'),
        meta: { requiresAuth: true, permission: 'roles:view' },
      },
      {
        path: 'permissions',
        name: 'permissions',
        component: () => import('@/views/admin/PermissionsView.vue'),
        meta: { requiresAuth: true, permission: 'permissions:view' },
      },
      {
        path: 'modules',
        name: 'modules',
        component: () => import('@/views/admin/ModulesView.vue'),
        meta: { requiresAuth: true, permission: 'modules:view' },
      },
      {
        path: 'forbidden',
        name: 'forbidden',
        component: () => import('@/views/ForbiddenView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { requiresAuth: true },
      },
    ],
  },
  {
    path: '/auth',
    component: () => import('@/layouts/AuthLayout.vue'),
    children: [
      {
        path: 'login',
        name: 'login',
        component: () => import('@/views/LoginView.vue'),
        meta: { guestOnly: true },
      },
      {
        path: 'register',
        name: 'register',
        component: () => import('@/views/RegisterView.vue'),
        meta: { guestOnly: true },
      },
      {
        path: 'forgot-password',
        name: 'forgot-password',
        component: () => import('@/views/ForgotPasswordView.vue'),
        meta: { guestOnly: true },
      },
      {
        path: 'reset-password',
        name: 'reset-password',
        component: () => import('@/views/ResetPasswordView.vue'),
        meta: { guestOnly: true },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
  },
]

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

// Global guard: restore session once, load the menu once, then enforce route meta.
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.initialize()

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: 'dashboard' }
  }

  if (auth.isAuthenticated) {
    const menu = useMenuStore()
    if (!menu.loaded) {
      try {
        await menu.load()
      } catch {
        // A failed menu load must not block navigation; a later one retries.
      }
      // Loading may have registered dynamic routes: if this navigation fell
      // through to the 404 catch-all, re-resolve it against the updated table.
      if (to.name === 'not-found' && router.resolve(to.fullPath).name !== 'not-found') {
        return to.fullPath
      }
    }
  }

  if (to.meta.permission && !auth.can(to.meta.permission)) {
    return { name: 'forbidden' }
  }
  return true
})
