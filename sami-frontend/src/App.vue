<script setup lang="ts">
// Root component: Vuetify application shell hosting the routed layouts.
// Re-apply the persisted theme (light/dark/system) once at startup.
import { useThemeMode } from '@/composables/useThemeMode'

const { mode, apply } = useThemeMode()
apply(mode.value)
</script>

<template>
  <v-app>
    <router-view />
  </v-app>
</template>
