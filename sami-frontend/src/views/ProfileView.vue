<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import AppPageHeader from '@/components/AppPageHeader.vue'
import ChangePasswordDialog from '@/components/ChangePasswordDialog.vue'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { useUserExperienceStore } from '@/stores/userExperience'
import { useFormat } from '@/composables/useFormat'
import { useThemeMode, type ThemeMode } from '@/composables/useThemeMode'
import { useServerLabel } from '@/composables/useServerLabel'
import { usePwa } from '@/composables/usePwa'
import { useNotifications } from '@/composables/useNotifications'
import { useApiError } from '@/composables/useApiError'
import type { MenuItem } from '@/types/models'

const { t } = useI18n()
const auth = useAuthStore()
const menu = useMenuStore()
const userExperience = useUserExperienceStore()
const { formatDate } = useFormat()
const { mode, apply } = useThemeMode()
const { roleLabel, moduleLabel } = useServerLabel()
const pwa = usePwa()
const { isInstalled, canPromptInstall, installBusy, installError } = pwa
const notifications = useNotifications()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()
const passwordOpen = ref(false)
const selectedCodes = ref<string[]>([])
const demoEnabled = ref(false)
const keyboardShortcutsEnabled = ref(true)
const user = computed(() => auth.user)
const themeItems = computed(() => [
  { title: t('profile.light'), value: 'light' },
  { title: t('profile.dark'), value: 'dark' },
  { title: t('profile.system'), value: 'system' },
])

const mobileCandidates = computed(() => menu.items.filter((item) =>
  menu.isNavigable(item) && !menu.showsPlaceholder(item) && menu.hasFrontendRoute(item),
))
const selectedItems = computed(() => selectedCodes.value
  .map((code) => mobileCandidates.value.find((item) => item.code === code))
  .filter((item): item is MenuItem => !!item))
const availableItems = computed(() => mobileCandidates.value
  .filter((item) => !selectedCodes.value.includes(item.code)))
const installInstructionKey = computed(() => `settings.application.instructions.${pwa.getInstallInstructions()}`)

watch(() => userExperience.preferences, (preferences) => {
  selectedCodes.value = preferences.mobileNavigationConfigured
    ? [...preferences.mobileNavigationCodes]
    : mobileCandidates.value.slice(0, 4).map((item) => item.code)
  demoEnabled.value = preferences.demoNotificationsEnabled
  keyboardShortcutsEnabled.value = preferences.keyboardShortcutsEnabled
}, { deep: true })

function addMobileItem(code: string): void {
  if (selectedCodes.value.length >= 4 || selectedCodes.value.includes(code)) return
  selectedCodes.value.push(code)
}

function removeMobileItem(code: string): void {
  selectedCodes.value = selectedCodes.value.filter((item) => item !== code)
}

function moveMobileItem(index: number, direction: -1 | 1): void {
  const target = index + direction
  if (target < 0 || target >= selectedCodes.value.length) return
  const next = [...selectedCodes.value]
  ;[next[index], next[target]] = [next[target], next[index]]
  selectedCodes.value = next
}

function restoreDefaults(): void {
  selectedCodes.value = mobileCandidates.value.slice(0, 4).map((item) => item.code)
}

async function savePreferences(): Promise<void> {
  clearError()
  try {
    await userExperience.save({
      mobileNavigationCodes: selectedCodes.value,
      demoNotificationsEnabled: demoEnabled.value,
      keyboardShortcutsEnabled: keyboardShortcutsEnabled.value,
    })
    notifications.success(t('settings.saved'))
  } catch (cause) {
    setError(cause)
  }
}

async function installApplication(): Promise<void> {
  const installed = await pwa.promptInstall()
  if (installed) notifications.success(t('settings.application.installAccepted'))
  else if (installError.value) notifications.error(t('settings.application.installFailed'))
}

onMounted(async () => {
  try {
    await userExperience.load()
    const preferences = userExperience.preferences
    selectedCodes.value = preferences.mobileNavigationConfigured
      ? preferences.mobileNavigationCodes.filter((code) => mobileCandidates.value.some((item) => item.code === code))
      : mobileCandidates.value.slice(0, 4).map((item) => item.code)
    demoEnabled.value = preferences.demoNotificationsEnabled
    keyboardShortcutsEnabled.value = preferences.keyboardShortcutsEnabled
  } catch (cause) {
    setError(cause)
  }
})
</script>

<template>
  <div>
    <AppPageHeader icon="mdi-cog-outline" :title="t('profile.title')" :subtitle="t('profile.subtitle')" />
    <v-alert v-if="errorMessage" type="error" variant="tonal" closable class="mb-4" @click:close="clearError">{{ errorMessage }}</v-alert>
    <v-row>
      <v-col cols="12" lg="4">
        <v-card rounded="xl" class="h-100">
          <v-card-text class="pa-5 pa-sm-6">
            <div class="d-flex align-center ga-4 mb-6">
              <v-avatar color="primary" size="64">{{ user?.fullName?.slice(0, 2).toUpperCase() }}</v-avatar>
              <div class="min-width-0"><h2 class="text-h6 text-truncate">{{ user?.fullName }}</h2><div class="text-body-2 text-medium-emphasis text-truncate">{{ user?.email }}</div></div>
            </div>
            <v-list bg-color="transparent">
              <v-list-item prepend-icon="mdi-shield-account-outline" :title="t('profile.role')" :subtitle="roleLabel(user?.role.name)" />
              <v-list-item prepend-icon="mdi-calendar-outline" :title="t('profile.memberSince')" :subtitle="user ? formatDate(user.createdAt) : '—'" />
              <v-list-item prepend-icon="mdi-key-chain" :title="t('profile.permissions')" :subtitle="user?.isSuperAdmin ? t('profile.fullAccess') : t('profile.permissionCount', { count: user?.permissions.length ?? 0 })" />
            </v-list>
            <v-btn block color="primary" variant="tonal" prepend-icon="mdi-lock-reset" class="mt-4" @click="passwordOpen = true">{{ t('profile.changePassword') }}</v-btn>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" lg="8">
        <v-card rounded="xl" class="mb-4">
          <v-card-title class="d-flex align-center ga-2"><v-icon icon="mdi-cellphone-arrow-down" />{{ t('settings.application.title') }}</v-card-title>
          <v-card-text>
            <v-alert v-if="isInstalled" type="success" variant="tonal" icon="mdi-check-circle-outline">{{ t('settings.application.installed') }}</v-alert>
            <template v-else>
              <p class="text-body-2 text-medium-emphasis mb-4">{{ t('settings.application.description') }}</p>
              <v-btn
                color="primary"
                prepend-icon="mdi-download"
                :loading="installBusy"
                :disabled="!canPromptInstall"
                @click="installApplication"
              >{{ t('settings.application.install') }}</v-btn>
              <v-alert v-if="!canPromptInstall" type="info" variant="tonal" class="mt-4">{{ t(installInstructionKey) }}</v-alert>
            </template>
          </v-card-text>
        </v-card>

        <v-card rounded="xl" class="mb-4">
          <v-card-title class="d-flex align-center ga-2"><v-icon icon="mdi-dock-bottom" />{{ t('settings.mobileNav.title') }}</v-card-title>
          <v-card-text>
            <p class="text-body-2 text-medium-emphasis mb-4">{{ t('settings.mobileNav.description', { count: 4 }) }}</p>
            <div v-if="userExperience.loading" class="py-4"><v-skeleton-loader type="list-item@4" /></div>
            <template v-else>
              <div class="mobile-nav-preview mb-4" role="list" :aria-label="t('settings.mobileNav.preview')">
                <div v-for="item in selectedItems" :key="item.code" class="mobile-nav-preview__item" role="listitem">
                  <v-icon :icon="item.icon" size="22" /><span>{{ moduleLabel(item.code, item.name) }}</span>
                </div>
                <div class="mobile-nav-preview__item"><v-icon icon="mdi-dots-grid" size="22" /><span>{{ t('shell.navigation') }}</span></div>
              </div>

              <v-list v-if="selectedItems.length" lines="one" class="pa-0 mb-3" border rounded="lg">
                <v-list-item v-for="(item, index) in selectedItems" :key="item.code" :prepend-icon="item.icon" :title="moduleLabel(item.code, item.name)">
                  <template #append>
                    <v-btn icon="mdi-arrow-up" variant="text" size="small" :disabled="index === 0" :aria-label="t('settings.mobileNav.moveUp')" @click="moveMobileItem(index, -1)" />
                    <v-btn icon="mdi-arrow-down" variant="text" size="small" :disabled="index === selectedItems.length - 1" :aria-label="t('settings.mobileNav.moveDown')" @click="moveMobileItem(index, 1)" />
                    <v-btn icon="mdi-close" variant="text" size="small" color="error" :aria-label="t('settings.mobileNav.remove')" @click="removeMobileItem(item.code)" />
                  </template>
                </v-list-item>
              </v-list>

              <v-select
                v-if="availableItems.length"
                :items="availableItems"
                item-value="code"
                :item-title="(item: MenuItem) => moduleLabel(item.code, item.name)"
                :label="t('settings.mobileNav.add')"
                prepend-inner-icon="mdi-plus"
                variant="outlined"
                density="comfortable"
                :disabled="selectedCodes.length >= 4"
                hide-details
                @update:model-value="addMobileItem"
              />
              <v-btn variant="text" prepend-icon="mdi-restore" class="mt-3" @click="restoreDefaults">{{ t('settings.mobileNav.restore') }}</v-btn>
            </template>
          </v-card-text>
        </v-card>

        <v-card rounded="xl" class="mb-4">
          <v-card-title class="d-flex align-center ga-2"><v-icon icon="mdi-bell-outline" />{{ t('settings.notifications.title') }}</v-card-title>
          <v-card-text>
            <v-switch v-model="demoEnabled" color="primary" inset hide-details :label="t('settings.notifications.demo')" />
            <p class="text-body-2 text-medium-emphasis mt-2">{{ t('settings.notifications.demoHint') }}</p>
          </v-card-text>
        </v-card>

        <v-card rounded="xl" class="mb-4">
          <v-card-title class="d-flex align-center ga-2"><v-icon icon="mdi-keyboard-outline" />{{ t('settings.keyboard.title') }}</v-card-title>
          <v-card-text>
            <v-switch v-model="keyboardShortcutsEnabled" color="primary" inset hide-details :label="t('settings.keyboard.enabled')" />
            <p class="text-body-2 text-medium-emphasis mt-2 mb-0">{{ t('settings.keyboard.hint') }}</p>
          </v-card-text>
        </v-card>

        <v-card rounded="xl" class="mb-4">
          <v-card-title>{{ t('profile.appearance') }}</v-card-title>
          <v-card-text><v-btn-toggle :model-value="mode" color="primary" mandatory divided class="profile-theme-toggle" @update:model-value="apply($event as ThemeMode)"><v-btn v-for="item in themeItems" :key="item.value" :value="item.value">{{ item.title }}</v-btn></v-btn-toggle></v-card-text>
        </v-card>
        <v-card rounded="xl" class="mb-4">
          <v-card-title>{{ t('profile.language') }}</v-card-title>
          <v-card-text><p class="text-body-2 text-medium-emphasis mb-3">{{ t('profile.languageHint') }}</p><LanguageSwitcher /></v-card-text>
        </v-card>
        <v-btn color="primary" size="large" prepend-icon="mdi-content-save-outline" :loading="userExperience.saving" @click="savePreferences">{{ t('common.save') }}</v-btn>
      </v-col>
    </v-row>
    <ChangePasswordDialog v-model="passwordOpen" />
  </div>
</template>

<style scoped>
.min-width-0 { min-width: 0; }
.profile-theme-toggle { width: 100%; }
.profile-theme-toggle :deep(.v-btn) { flex: 1; min-height: 44px; }
.mobile-nav-preview { display: flex; min-height: 68px; border: 1px solid rgba(var(--v-border-color), .16); border-radius: 16px; overflow: hidden; }
.mobile-nav-preview__item { flex: 1; min-width: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; color: rgb(var(--v-theme-primary)); font-size: .68rem; }
.mobile-nav-preview__item span { max-width: 100%; overflow: hidden; padding-inline: 4px; text-overflow: ellipsis; white-space: nowrap; }
@media (max-width: 599px) {
  .profile-theme-toggle { flex-direction: column; height: auto !important; }
  :deep(.v-card-title) { white-space: normal; font-size: 1rem; }
  :deep(.v-list-item__append) { gap: 0; }
}
</style>
