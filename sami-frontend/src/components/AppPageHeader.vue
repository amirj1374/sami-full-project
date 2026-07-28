<script setup lang="ts">
/**
 * Consistent page header: an icon badge, title, optional subtitle/eyebrow and a
 * right-aligned actions slot. Gives every screen the same rhythm and hierarchy.
 */
withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    eyebrow?: string
    icon?: string
  }>(),
  {},
)
</script>

<template>
  <div class="app-page-header d-flex flex-wrap align-center ga-4 mb-5">
    <div v-if="icon" class="app-page-header__badge">
      <v-icon :icon="icon" size="26" />
    </div>
    <div class="app-page-header__text flex-grow-1">
      <div v-if="eyebrow" class="text-overline text-medium-emphasis app-eyebrow">{{ eyebrow }}</div>
      <h1 class="text-h5 text-sm-h4 font-weight-bold">{{ title }}</h1>
      <p v-if="subtitle" class="text-body-2 text-medium-emphasis mt-1 mb-0">{{ subtitle }}</p>
    </div>
    <div class="app-page-header__actions d-flex align-center ga-2 flex-wrap">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.app-page-header__badge {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--app-radius);
  color: rgb(var(--v-theme-primary));
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-primary), 0.16),
    rgba(var(--v-theme-primary), 0.06)
  );
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-primary), 0.1);
  flex: none;
}
.app-eyebrow {
  letter-spacing: 1.4px;
  line-height: 1.4;
}
</style>
