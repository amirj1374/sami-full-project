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
  <header class="app-page-header d-flex flex-wrap align-center ga-4 mb-6">
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
  </header>
</template>

<style scoped>
.app-page-header__badge {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  color: rgb(var(--v-theme-accent));
  background: rgb(var(--v-theme-primary));
  box-shadow: 0 8px 20px rgba(var(--v-theme-primary), 0.18);
  flex: none;
}
.app-eyebrow {
  color: rgb(var(--v-theme-primary)) !important;
  font-weight: 700;
  letter-spacing: 1.2px;
  line-height: 1.4;
}
@media (max-width: 599px) {
  .app-page-header {
    gap: 12px !important;
    margin-bottom: 18px !important;
  }
  .app-page-header__badge {
    width: 40px;
    height: 40px;
  }
  .app-page-header__badge :deep(.v-icon) {
    font-size: 22px !important;
  }
  .app-page-header__text {
    min-width: calc(100% - 56px);
  }
  .app-page-header__text h1 {
    font-size: 1.35rem !important;
    line-height: 1.35;
  }
  .app-page-header__actions {
    width: 100%;
    padding-top: 4px;
  }
  .app-page-header__actions :deep(.v-btn) {
    flex: 1;
  }
}
</style>
