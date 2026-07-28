<script setup lang="ts">
/**
 * One read-only label/value pair for View pages. Renders an uppercase caption
 * label above the value (em-dash fallback when empty) — the elegant alternative
 * to disabled inputs. Lives inside a `v-row` (see AppDetailList) and takes the
 * standard responsive column spans.
 */
withDefaults(
  defineProps<{
    label: string
    value?: string | number | null
    icon?: string
    cols?: number | string
    sm?: number | string
    md?: number | string
  }>(),
  { cols: 12, sm: 6, md: 4 },
)
</script>

<template>
  <v-col :cols="cols" :sm="sm" :md="md">
    <div class="app-detail-item">
      <span class="app-detail-item__label">{{ label }}</span>
      <span class="app-detail-item__value">
        <v-icon v-if="icon && (value ?? '') !== ''" :icon="icon" size="16" class="me-1 text-medium-emphasis" />
        <template v-if="(value ?? '') !== ''">{{ value }}</template>
        <span v-else class="text-disabled">—</span>
        <slot />
      </span>
    </div>
  </v-col>
</template>

<style scoped>
.app-detail-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 2px 0;
}
.app-detail-item__label {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  color: rgba(var(--v-theme-on-surface), 0.5);
}
.app-detail-item__value {
  font-size: 0.9rem;
  font-weight: 500;
  color: rgb(var(--v-theme-on-surface));
  overflow-wrap: anywhere;
}
</style>
