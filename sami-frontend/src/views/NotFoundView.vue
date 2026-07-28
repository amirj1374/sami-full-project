<script setup lang="ts">
// Fallback 404 view.
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <v-main>
    <v-container class="fill-height" fluid>
      <v-row justify="center" align="center">
        <v-col cols="12" class="text-center">
          <div class="text-h2 font-weight-bold mb-2">404</div>
          <p class="text-body-1 mb-6">{{ t('errors.notFoundMessage') }}</p>
          <v-btn color="primary" :to="{ name: 'dashboard' }">{{ t('common.goHome') }}</v-btn>
        </v-col>
      </v-row>
    </v-container>
  </v-main>
</template>
