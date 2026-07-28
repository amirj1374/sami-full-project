import { defineStore } from 'pinia'
import { ref } from 'vue'
import { menuApi } from '@/api/menu'
import { router } from '@/router'
import type { MenuItem } from '@/types/models'
import { MOCK_MODE } from '@/mocks/config'

/**
 * Navigation menu state.
 *
 * `load()` fetches the permission-filtered menu and, for each item whose path
 * the router does not already know, registers a dynamic child route of the
 * layout pointing at the placeholder view — a module created in the admin
 * panel becomes navigable without a code change. `reset()` (called on logout)
 * clears the items and removes those dynamic routes again.
 *
 * ## Where module status comes from
 *
 * Route registration answers a PHYSICAL question — does a Vue screen exist in
 * this bundle? It no longer decides what the user is TOLD. That is the
 * module's lifecycle status, supplied by the backend on each menu item and
 * rendered by `PlaceholderView`.
 *
 * The distinction matters because the two disagree constantly: modules with
 * complete, tested backends have no screens yet, and a module created at
 * runtime has neither. Both used to render the identical "ready for
 * development" message.
 *
 * Registration therefore stays exactly as it was — it is the fallback that
 * guarantees a module can never 404 — while the message became data.
 */
export const useMenuStore = defineStore('menu', () => {
  const items = ref<MenuItem[]>([])
  const loaded = ref(false)

  /** Names of dynamically registered routes, so reset() can remove them. */
  const dynamicRouteNames: string[] = []

  // Single-flight: concurrent guard runs share one in-flight load.
  let loadPromise: Promise<void> | null = null

  async function load(): Promise<void> {
    loadPromise ??= fetchAndRegister().finally(() => {
      loadPromise = null
    })
    return loadPromise
  }

  async function fetchAndRegister(): Promise<void> {
    // In Mock Mode the guard must not depend on the backend (or on MSW having
    // taken control yet), so the menu is loaded offline from mock data. Dead
    // code / tree-shaken when VITE_ENABLE_MOCK_MODE is off.
    const menuItems = MOCK_MODE
      ? (await import('@/mocks/data/menu')).mockMenu
      : await menuApi.list()
    items.value = menuItems
    for (const item of menuItems) {
      registerDynamicRoute(item)
    }
    loaded.value = true
  }

  /**
   * Adds a placeholder route for a menu path no static route covers.
   *
   * Deliberately does NOT consult the lifecycle status. A static route means
   * the screen physically exists in this bundle, which is a stronger fact than
   * a status column that may be stale; overriding it would let an out-of-date
   * row hide working UI. Conversely a module the backend calls ACTIVE still
   * gets the placeholder when no screen exists, because there is nothing else
   * to render.
   */
  function registerDynamicRoute(item: MenuItem): void {
    const resolved = router.resolve(item.path)
    if (resolved.name && resolved.name !== 'not-found') {
      return // A static (or previously registered) route already covers it.
    }
    const name = `module:${item.code}`
    router.addRoute('layout', {
      path: item.path.replace(/^\//, ''),
      name,
      component: () => import('@/views/PlaceholderView.vue'),
      meta: { requiresAuth: true, permission: `${item.code}:view` },
    })
    dynamicRouteNames.push(name)
  }

  function reset(): void {
    items.value = []
    loaded.value = false
    for (const name of dynamicRouteNames) {
      if (router.hasRoute(name)) {
        router.removeRoute(name)
      }
    }
    dynamicRouteNames.length = 0
  }

  /**
   * The menu entry for a path, used by `PlaceholderView` to read the module's
   * lifecycle status.
   */
  function byPath(path: string): MenuItem | null {
    return items.value.find((item) => item.path === path) ?? null
  }

  /** True when this bundle physically contains a screen for the path. */
  function hasFrontendRoute(item: MenuItem): boolean {
    const resolved = router.resolve(item.path)
    return !!resolved.name && resolved.name !== 'not-found' && !dynamicRouteNames.includes(String(resolved.name))
  }

  /**
   * Whether a module should be presented as not-yet-usable.
   *
   * Resolution order — the backend is the single source of truth and route
   * existence is only a safety net:
   *
   *   1. backend says showPlaceholder      -> placeholder
   *   2. backend says not navigable        -> placeholder
   *   3. backend says navigable + a screen exists -> real view
   *   4. no screen in this bundle          -> placeholder (safe fallback)
   *
   * Step 4 also covers a backend that predates the lifecycle fields, where
   * every flag is undefined and behaviour reverts to the original rule.
   */
  function showsPlaceholder(item: MenuItem): boolean {
    if (item.showPlaceholder === true) {
      return true
    }
    if (item.navigable === false) {
      return true
    }
    // The backend says the module is usable (or predates the fields). Render
    // the real screen only if one is actually shipped in this bundle.
    return !hasFrontendRoute(item)
  }

  /** Whether the user may open the module at all (backend verdict wins). */
  function isNavigable(item: MenuItem): boolean {
    return item.navigable !== false
  }

  return { items, loaded, load, reset, byPath, showsPlaceholder, isNavigable, hasFrontendRoute }
})
