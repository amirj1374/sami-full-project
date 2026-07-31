<script setup lang="ts">
// 403 view: the route guard redirects here when a permission check fails.
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <div class="forbidden-view">
    <v-card rounded="xl" class="forbidden-card text-center pa-6 pa-sm-10">
      <div class="forbidden-icon mx-auto mb-5">
        <v-icon icon="mdi-shield-lock-outline" size="48" />
      </div>
      <div class="text-overline text-warning font-weight-bold">403</div>
      <h1 class="text-h5 text-sm-h4 font-weight-bold mb-3">{{ t('errors.forbiddenMessage') }}</h1>
      <v-btn color="primary" size="large" prepend-icon="mdi-home-outline" :to="{ name: 'dashboard' }">
        {{ t('common.goHome') }}
      </v-btn>
    </v-card>
  </div>
</template>

<style scoped>
.forbidden-view {
  min-height: calc(100dvh - 150px);
  display: grid;
  place-items: center;
}
.forbidden-card {
  width: min(100%, 520px);
  border: 1px solid rgba(var(--v-theme-on-surface), 0.1);
  box-shadow: var(--app-shadow-md);
}
.forbidden-icon {
  width: 92px;
  height: 92px;
  display: grid;
  place-items: center;
  border-radius: 28px;
  color: rgb(var(--v-theme-warning));
  background: rgba(var(--v-theme-warning), 0.1);
}
</style>
