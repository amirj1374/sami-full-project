<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AppPageHeader from '@/components/AppPageHeader.vue'
import ChangePasswordDialog from '@/components/ChangePasswordDialog.vue'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import { useAuthStore } from '@/stores/auth'
import { useFormat } from '@/composables/useFormat'
import { useThemeMode, type ThemeMode } from '@/composables/useThemeMode'

const { t } = useI18n()
const auth = useAuthStore()
const { formatDate } = useFormat()
const { mode, apply } = useThemeMode()
const passwordOpen = ref(false)
const user = computed(() => auth.user)
const themeItems = computed(() => [
  { title: t('profile.light'), value: 'light' },
  { title: t('profile.dark'), value: 'dark' },
  { title: t('profile.system'), value: 'system' },
])
</script>

<template>
  <div>
    <AppPageHeader icon="mdi-account-circle-outline" :title="t('profile.title')" :subtitle="t('profile.subtitle')" />
    <v-row>
      <v-col cols="12" md="5">
        <v-card rounded="xl" class="h-100">
          <v-card-text class="pa-5 pa-sm-6">
            <div class="d-flex align-center ga-4 mb-6">
              <v-avatar color="primary" size="64">{{ user?.fullName?.slice(0, 2).toUpperCase() }}</v-avatar>
              <div class="min-width-0"><h2 class="text-h6 text-truncate">{{ user?.fullName }}</h2><div class="text-body-2 text-medium-emphasis text-truncate">{{ user?.email }}</div></div>
            </div>
            <v-list bg-color="transparent">
              <v-list-item prepend-icon="mdi-shield-account-outline" :title="t('profile.role')" :subtitle="user?.role.name" />
              <v-list-item prepend-icon="mdi-calendar-outline" :title="t('profile.memberSince')" :subtitle="user ? formatDate(user.createdAt) : '—'" />
              <v-list-item prepend-icon="mdi-key-chain" :title="t('profile.permissions')" :subtitle="user?.isSuperAdmin ? t('profile.fullAccess') : t('profile.permissionCount', { count: user?.permissions.length ?? 0 })" />
            </v-list>
            <v-btn block color="primary" variant="tonal" prepend-icon="mdi-lock-reset" class="mt-4" @click="passwordOpen = true">{{ t('profile.changePassword') }}</v-btn>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="7">
        <v-card rounded="xl" class="mb-4">
          <v-card-title>{{ t('profile.appearance') }}</v-card-title>
          <v-card-text><v-btn-toggle :model-value="mode" color="primary" mandatory divided class="profile-theme-toggle" @update:model-value="apply($event as ThemeMode)"><v-btn v-for="item in themeItems" :key="item.value" :value="item.value">{{ item.title }}</v-btn></v-btn-toggle></v-card-text>
        </v-card>
        <v-card rounded="xl">
          <v-card-title>{{ t('profile.language') }}</v-card-title>
          <v-card-text><p class="text-body-2 text-medium-emphasis mb-3">{{ t('profile.languageHint') }}</p><LanguageSwitcher /></v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <ChangePasswordDialog v-model="passwordOpen" />
  </div>
</template>

<style scoped>
.min-width-0 { min-width: 0; }
.profile-theme-toggle { width: 100%; }
.profile-theme-toggle :deep(.v-btn) { flex: 1; min-height: 44px; }
@media (max-width: 599px) { .profile-theme-toggle { flex-direction: column; height: auto !important; } }
</style>
