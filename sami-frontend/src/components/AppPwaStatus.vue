<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { usePwa } from '@/composables/usePwa'

const { t } = useI18n()
const { online, canInstall, updateAvailable, install, applyUpdate } = usePwa()

function retryConnection() {
  window.location.reload()
}
</script>

<template>
  <div class="app-pwa-status">
    <v-snackbar :model-value="!online" color="warning" :timeout="-1" location="top">
      <div class="d-flex align-center ga-3">
        <v-icon icon="mdi-wifi-off" />
        <span class="flex-grow-1">{{ t('pwa.offline') }}</span>
        <v-btn variant="text" @click="retryConnection">{{ t('pwa.retry') }}</v-btn>
      </div>
    </v-snackbar>
    <v-snackbar :model-value="updateAvailable" color="info" :timeout="-1">
      <div class="d-flex align-center ga-3"><v-icon icon="mdi-update" /><span class="flex-grow-1">{{ t('pwa.updateAvailable') }}</span><v-btn variant="text" @click="applyUpdate">{{ t('pwa.updateNow') }}</v-btn></div>
    </v-snackbar>
    <v-snackbar :model-value="canInstall && online" color="primary" :timeout="-1">
      <div class="d-flex align-center ga-3"><v-icon icon="mdi-cellphone-arrow-down" /><span class="flex-grow-1">{{ t('pwa.installDescription') }}</span><v-btn variant="text" @click="install">{{ t('pwa.install') }}</v-btn></div>
    </v-snackbar>
  </div>
</template>
