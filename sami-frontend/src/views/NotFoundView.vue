<script setup lang="ts">
// Fallback 404 view.
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AppEmptyState from '@/components/AppEmptyState.vue'

const { t } = useI18n()
const router = useRouter()
</script>

<template>
  <v-main>
    <v-container class="fill-height" fluid>
      <v-card class="mx-auto" rounded="xl" width="min(100%, 560px)">
        <AppEmptyState
          kind="unavailable"
          icon="mdi-map-marker-question-outline"
          title="404"
          :description="t('errors.notFoundMessage')"
          :action-label="t('common.goHome')"
          action-icon="mdi-home-outline"
          :secondary-action-label="t('common.back')"
          secondary-action-icon="mdi-arrow-left"
          @action="router.push({ name: 'dashboard' })"
          @secondary-action="router.back()"
        />
      </v-card>
    </v-container>
  </v-main>
</template>
