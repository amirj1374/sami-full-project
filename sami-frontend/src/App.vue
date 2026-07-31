<script setup lang="ts">
// Root component: Vuetify application shell hosting the routed layouts.
// Re-apply the persisted theme (light/dark/system) once at startup.
import { useThemeMode } from '@/composables/useThemeMode'
import AppNotificationHost from '@/components/AppNotificationHost.vue'
import AppPwaStatus from '@/components/AppPwaStatus.vue'

const { mode, apply } = useThemeMode()
apply(mode.value)
</script>

<template>
  <v-app>
    <router-view />
    <AppNotificationHost />
    <AppPwaStatus />
  </v-app>
</template>
