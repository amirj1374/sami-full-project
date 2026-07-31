<script setup lang="ts">
withDefaults(defineProps<{ variant?: 'page' | 'table' | 'cards' | 'detail'; rows?: number; label?: string }>(), {
  variant: 'page',
  rows: 4,
})
</script>

<template>
  <div class="app-loading" role="status" :aria-label="label">
    <template v-if="variant === 'table'">
      <div class="app-loading__toolbar app-skeleton mb-3" />
      <div v-for="row in rows" :key="row" class="app-loading__row d-flex ga-3 py-3">
        <div class="app-skeleton flex-grow-1" /><div class="app-skeleton app-loading__short" />
      </div>
    </template>
    <template v-else-if="variant === 'cards'">
      <div class="app-loading__grid">
        <div v-for="row in rows" :key="row" class="app-loading__card app-skeleton" />
      </div>
    </template>
    <template v-else>
      <div class="app-skeleton app-loading__title mb-4" />
      <div class="app-skeleton app-loading__line mb-2" />
      <div class="app-skeleton app-loading__line app-loading__line--short mb-5" />
      <div class="app-loading__panel app-skeleton" />
    </template>
    <span class="sr-only">{{ label }}</span>
  </div>
</template>

<style scoped>
.app-loading { padding: var(--app-space-4); }
.app-loading__toolbar { height: 48px; }
.app-loading__row { border-bottom: 1px solid var(--app-border); }
.app-loading__row .app-skeleton { height: 22px; }
.app-loading__short { max-width: 112px; }
.app-loading__title { width: min(280px, 70%); height: 32px; }
.app-loading__line { width: 100%; height: 16px; }
.app-loading__line--short { width: 62%; }
.app-loading__panel { height: 220px; }
.app-loading__grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 260px), 1fr)); gap: 14px; }
.app-loading__card { height: 150px; }
</style>
