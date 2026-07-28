<script setup lang="ts">
// 403 view: the route guard redirects here when a permission check fails.
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <v-row justify="center" align="center" class="mt-16">
    <v-col cols="12" class="text-center">
      <v-icon icon="mdi-shield-lock-outline" size="64" color="warning" class="mb-4" />
      <div class="text-h2 font-weight-bold mb-2">403</div>
      <p class="text-body-1 mb-6">{{ t('errors.forbiddenMessage') }}</p>
      <v-btn color="primary" :to="{ name: 'dashboard' }">{{ t('common.goHome') }}</v-btn>
    </v-col>
  </v-row>
</template>
