<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useDisplay } from 'vuetify'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { useNavHistory } from '@/composables/useNavHistory'
import { useServerLabel } from '@/composables/useServerLabel'
import { useThemeMode, type ThemeMode } from '@/composables/useThemeMode'
import type { MenuItem } from '@/types/models'
import ChangePasswordDialog from '@/components/ChangePasswordDialog.vue'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import CommandPalette from '@/components/CommandPalette.vue'

const { t } = useI18n()
const { mobile } = useDisplay()
const auth = useAuthStore()
const menu = useMenuStore()
const router = useRouter()
const route = useRoute()
const { favorites, recent, isFavorite, toggleFavorite, recordVisit } = useNavHistory()
const { moduleLabel, roleLabel } = useServerLabel()
const { mode: themeMode, isDark, apply: applyTheme } = useThemeMode()

const RAIL_KEY = 'sami.sidebar.rail'
const drawer = ref(!mobile.value)
const rail = ref(localStorage.getItem(RAIL_KEY) === '1')
const navQuery = ref('')
const paletteOpen = ref(false)
const changePasswordOpen = ref(false)

const themeOptions: { value: ThemeMode; icon: string; label: string }[] = [
  { value: 'light', icon: 'mdi-white-balance-sunny', label: 'shell.themeLight' },
  { value: 'dark', icon: 'mdi-weather-night', label: 'shell.themeDark' },
  { value: 'system', icon: 'mdi-monitor', label: 'shell.themeSystem' },
]

const TITLE_KEYS: Record<string, string> = {
  dashboard: 'dashboard.title',
  products: 'products.title',
  suppliers: 'suppliers.title',
  purchases: 'purchases.title',
  customers: 'customers.title',
  users: 'users.title',
  roles: 'roles.title',
  permissions: 'permissions.title',
  modules: 'modules.title',
}

const isDashboard = computed(() => route.name === 'dashboard')

/** Prefer the (backend-supplied) menu label for the active path; fall back to i18n. */
const activeMenuItem = computed(() => menu.items.find((m) => m.path === route.path))
const pageTitle = computed(() => {
  if (activeMenuItem.value) return moduleLabel(activeMenuItem.value.code, activeMenuItem.value.name)
  const key = TITLE_KEYS[String(route.name ?? '')]
  return key ? t(key) : t('common.appName')
})

const breadcrumbs = computed(() => {
  const home = { title: t('dashboard.title'), to: { name: 'dashboard' }, disabled: false }
  if (isDashboard.value) return [{ ...home, disabled: true }]
  return [home, { title: pageTitle.value, disabled: true }]
})

// --- Sidebar item groups ----------------------------------------------------
const filteredItems = computed<MenuItem[]>(() => {
  const q = navQuery.value.trim().toLowerCase()
  if (!q) return menu.items
  return menu.items.filter(
    (m) => m.name.toLowerCase().includes(q) || m.code.toLowerCase().includes(q),
  )
})
const favoriteItems = computed<MenuItem[]>(() =>
  menu.items.filter((m) => favorites.value.includes(m.path)),
)
const recentItems = computed<MenuItem[]>(() =>
  recent.value
    .map((p) => menu.items.find((m) => m.path === p))
    .filter((m): m is MenuItem => !!m && m.path !== route.path)
    .slice(0, 4),
)

const initials = computed(() => {
  const parts = (auth.user?.fullName ?? '').split(/\s+/).filter(Boolean)
  return parts.slice(0, 2).map((p) => p.charAt(0).toUpperCase()).join('') || '?'
})

function toggleRail(): void {
  rail.value = !rail.value
  localStorage.setItem(RAIL_KEY, rail.value ? '1' : '0')
}

function onGlobalKey(e: KeyboardEvent): void {
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    paletteOpen.value = true
  }
}

watch(
  () => route.path,
  (path) => recordVisit(path),
  { immediate: true },
)

onMounted(() => window.addEventListener('keydown', onGlobalKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onGlobalKey))

async function logout(): Promise<void> {
  await auth.logout()
  await router.push({ name: 'login' })
}
</script>

<template>
  <v-navigation-drawer
    v-model="drawer"
    :temporary="mobile"
    :rail="rail && !mobile"
    :rail-width="76"
    width="264"
    class="app-drawer"
  >
    <!-- Brand -->
    <div class="app-brand d-flex align-center px-4" :class="rail && !mobile ? 'justify-center' : 'ga-3'">
      <v-avatar rounded="lg" color="primary" size="38" class="app-brand__logo">
        <span class="text-subtitle-1 font-weight-bold">S</span>
      </v-avatar>
      <div v-if="!(rail && !mobile)" class="d-flex flex-column">
        <span class="text-subtitle-1 font-weight-bold app-brand__name">{{ t('common.appName') }}</span>
        <span class="text-caption text-medium-emphasis">{{ t('shell.tagline') }}</span>
      </div>
    </div>

    <!-- Sidebar search -->
    <div v-if="!(rail && !mobile)" class="px-3 pb-2">
      <v-text-field
        v-model="navQuery"
        density="compact"
        variant="solo-filled"
        flat
        rounded="lg"
        hide-details
        prepend-inner-icon="mdi-magnify"
        :placeholder="t('shell.searchNav')"
      />
    </div>

    <div class="app-nav-scroll px-3">
      <!-- Favorites -->
      <template v-if="!(rail && !mobile) && !navQuery && favoriteItems.length">
        <div class="app-nav-label">{{ t('shell.favorites') }}</div>
        <v-list nav color="primary" density="comfortable" class="pa-0 mb-2">
          <v-list-item
            v-for="item in favoriteItems"
            :key="`fav-${item.code}`"
            :to="item.path"
            :prepend-icon="item.icon"
            :title="moduleLabel(item.code, item.name)"
            rounded="lg"
            class="app-nav-item"
          >
            <template #append>
              <v-icon icon="mdi-star" size="16" color="accent" />
            </template>
          </v-list-item>
        </v-list>
      </template>

      <!-- Recently visited -->
      <template v-if="!(rail && !mobile) && !navQuery && recentItems.length">
        <div class="app-nav-label">{{ t('shell.recent') }}</div>
        <v-list nav density="comfortable" class="pa-0 mb-2">
          <v-list-item
            v-for="item in recentItems"
            :key="`recent-${item.code}`"
            :to="item.path"
            :prepend-icon="item.icon"
            :title="moduleLabel(item.code, item.name)"
            rounded="lg"
            class="app-nav-item"
          />
        </v-list>
      </template>

      <!-- Main navigation -->
      <div v-if="!(rail && !mobile)" class="app-nav-label">{{ t('shell.navigation') }}</div>
      <v-list nav color="primary" density="comfortable" class="pa-0">
        <template v-for="item in filteredItems" :key="item.code">
          <v-tooltip v-if="rail && !mobile" :text="moduleLabel(item.code, item.name)" location="end">
            <template #activator="{ props: tip }">
              <v-list-item
                v-bind="tip"
                :to="item.path"
                :prepend-icon="item.icon"
                rounded="lg"
                class="app-nav-item app-nav-item--rail"
              />
            </template>
          </v-tooltip>
          <v-list-item
            v-else
            :to="item.path"
            :prepend-icon="item.icon"
            :title="moduleLabel(item.code, item.name)"
            rounded="lg"
            class="app-nav-item"
          >
            <template #append>
              <v-btn
                :icon="isFavorite(item.path) ? 'mdi-star' : 'mdi-star-outline'"
                :color="isFavorite(item.path) ? 'accent' : undefined"
                size="x-small"
                variant="text"
                class="app-nav-item__fav"
                :aria-label="t('shell.pinToFavorites')"
                @click.prevent.stop="toggleFavorite(item.path)"
              />
            </template>
          </v-list-item>
        </template>

        <div v-if="navQuery && !filteredItems.length" class="text-caption text-medium-emphasis text-center py-4">
          {{ t('shell.noNavResults') }}
        </div>
      </v-list>
    </div>

    <!-- Collapse toggle (desktop) -->
    <template #append>
      <v-divider />
      <div class="pa-2" :class="rail && !mobile ? 'text-center' : ''">
        <v-btn
          v-if="!mobile"
          :block="!rail"
          variant="text"
          size="small"
          :prepend-icon="rail ? undefined : 'mdi-chevron-double-left'"
          :icon="rail ? 'mdi-chevron-double-right' : undefined"
          :aria-label="t('shell.collapse')"
          @click="toggleRail"
        >
          <template v-if="!rail">{{ t('shell.collapse') }}</template>
        </v-btn>
      </div>
    </template>
  </v-navigation-drawer>

  <v-app-bar flat border height="64" class="app-topbar">
    <v-app-bar-nav-icon
      v-if="mobile"
      :aria-label="t('shell.toggleMenu')"
      @click="drawer = !drawer"
    />

    <div class="d-none d-sm-flex flex-column justify-center ms-2">
      <v-breadcrumbs :items="breadcrumbs" density="compact" divider="/" class="pa-0 app-breadcrumbs">
        <template #title="{ item }">
          <span :class="item.disabled ? 'text-high-emphasis font-weight-medium' : 'text-medium-emphasis'">
            {{ item.title }}
          </span>
        </template>
      </v-breadcrumbs>
      <span class="text-subtitle-1 font-weight-bold app-page-title">{{ pageTitle }}</span>
    </div>

    <v-spacer />

    <!-- Global search / command palette trigger -->
    <v-btn
      variant="tonal"
      class="app-search-trigger text-none d-none d-md-flex me-1"
      color="on-surface"
      @click="paletteOpen = true"
    >
      <v-icon icon="mdi-magnify" size="18" class="me-2 text-medium-emphasis" />
      <span class="text-medium-emphasis">{{ t('shell.searchEverything') }}</span>
      <v-chip size="x-small" variant="tonal" label class="ms-3">⌘K</v-chip>
    </v-btn>
    <v-btn
      icon="mdi-magnify"
      variant="text"
      class="d-md-none"
      :aria-label="t('shell.searchEverything')"
      @click="paletteOpen = true"
    />

    <!-- Branch selector (single branch today) -->
    <v-menu location="bottom end">
      <template #activator="{ props: p }">
        <v-btn variant="text" class="text-none d-none d-sm-flex" v-bind="p">
          <v-icon icon="mdi-store-outline" size="18" class="me-2" />
          {{ t('shell.mainBranch') }}
          <v-icon icon="mdi-chevron-down" size="16" class="ms-1" />
        </v-btn>
      </template>
      <v-card min-width="220">
        <div class="px-4 py-2 text-caption text-medium-emphasis">{{ t('shell.branch') }}</div>
        <v-divider />
        <v-list density="compact" nav class="pa-2">
          <v-list-item rounded="lg" active prepend-icon="mdi-store-marker-outline" :title="t('shell.mainBranch')">
            <template #append><v-icon icon="mdi-check" size="18" color="primary" /></template>
          </v-list-item>
        </v-list>
      </v-card>
    </v-menu>

    <!-- Notifications -->
    <v-menu location="bottom end" :close-on-content-click="false">
      <template #activator="{ props: p }">
        <v-btn icon="mdi-bell-outline" variant="text" v-bind="p" :aria-label="t('layout.notifications')" />
      </template>
      <v-card min-width="320" max-width="360">
        <div class="px-4 py-3 text-subtitle-2 font-weight-bold">{{ t('layout.notifications') }}</div>
        <v-divider />
        <div class="pa-6">
          <div class="text-center">
            <v-icon icon="mdi-bell-sleep-outline" size="36" class="mb-2 text-disabled" />
            <div class="text-body-2 text-medium-emphasis">{{ t('layout.noNotifications') }}</div>
          </div>
        </div>
      </v-card>
    </v-menu>

    <!-- Theme switch -->
    <v-menu location="bottom end">
      <template #activator="{ props: p }">
        <v-btn
          :icon="isDark ? 'mdi-weather-night' : 'mdi-white-balance-sunny'"
          variant="text"
          v-bind="p"
          :aria-label="t('shell.theme')"
        />
      </template>
      <v-list density="compact" nav min-width="180" class="pa-2">
        <v-list-item
          v-for="opt in themeOptions"
          :key="opt.value"
          :active="themeMode === opt.value"
          :prepend-icon="opt.icon"
          :title="t(opt.label)"
          rounded="lg"
          @click="applyTheme(opt.value)"
        />
      </v-list>
    </v-menu>

    <LanguageSwitcher />

    <!-- Profile -->
    <v-menu location="bottom end">
      <template #activator="{ props: p }">
        <v-btn icon variant="text" v-bind="p" class="ms-1" :aria-label="t('shell.account')">
          <v-avatar color="primary" size="34">
            <span class="text-body-2 font-weight-medium">{{ initials }}</span>
          </v-avatar>
        </v-btn>
      </template>
      <v-card min-width="260">
        <div class="d-flex align-center ga-3 px-4 py-3">
          <v-avatar color="primary" size="40">
            <span class="text-body-2 font-weight-medium">{{ initials }}</span>
          </v-avatar>
          <div class="d-flex flex-column overflow-hidden">
            <span class="text-body-2 font-weight-medium text-truncate">{{ auth.user?.fullName }}</span>
            <span class="text-caption text-medium-emphasis text-truncate">{{ auth.user?.email }}</span>
          </div>
        </div>
        <div class="px-4 pb-3">
          <v-chip size="x-small" color="primary" variant="tonal" class="font-weight-medium">
            {{ roleLabel(auth.user?.role.name) }}
          </v-chip>
        </div>
        <v-divider />
        <v-list density="compact" nav class="pa-2">
          <v-list-item
            rounded="lg"
            prepend-icon="mdi-lock-reset"
            :title="t('shell.changePassword')"
            @click="changePasswordOpen = true"
          />
          <v-list-item
            rounded="lg"
            base-color="error"
            prepend-icon="mdi-logout"
            :title="t('shell.logout')"
            @click="logout"
          />
        </v-list>
      </v-card>
    </v-menu>
  </v-app-bar>

  <v-main class="app-main">
    <v-container class="py-6">
      <router-view v-slot="{ Component }">
        <transition name="route-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </v-container>
  </v-main>

  <CommandPalette v-model="paletteOpen" />
  <ChangePasswordDialog v-model="changePasswordOpen" />
</template>

<style scoped>
.app-brand {
  height: 64px;
}
.app-brand__logo {
  box-shadow: 0 4px 12px rgba(var(--v-theme-primary), 0.35);
}
.app-nav-scroll {
  overflow-y: auto;
}
.app-nav-label {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.8px;
  text-transform: uppercase;
  color: rgba(var(--v-theme-on-surface), 0.45);
  padding: 10px 12px 4px;
}
.app-nav-item {
  margin-bottom: 2px;
  transition: background var(--app-dur-fast) var(--app-ease);
}
.app-nav-item__fav {
  opacity: 0;
  transition: opacity var(--app-dur-fast) var(--app-ease);
}
.app-nav-item:hover .app-nav-item__fav {
  opacity: 1;
}
.app-nav-item--rail {
  justify-content: center;
}
.app-search-trigger {
  min-width: 240px;
  justify-content: flex-start;
  border-radius: var(--app-radius) !important;
  background: rgba(var(--v-theme-on-surface), 0.05);
}
.app-page-title {
  line-height: 1.2;
}
.app-breadcrumbs :deep(.v-breadcrumbs-item) {
  font-size: 0.72rem;
  padding: 0;
}
</style>
