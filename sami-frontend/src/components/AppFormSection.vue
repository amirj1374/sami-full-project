<script setup lang="ts">
/**
 * A titled form section: an icon badge, a title, an optional description, and a
 * padded body for the fields (drop a `v-row`/`v-col` grid inside). Gives every
 * Create/Edit form the same "grouped cards" rhythm so long forms stay scannable.
 */
withDefaults(
  defineProps<{
    title: string
    description?: string
    icon?: string
  }>(),
  {},
)
</script>

<template>
  <section class="app-form-section">
    <header class="app-form-section__head">
      <span v-if="icon" class="app-form-section__icon">
        <v-icon :icon="icon" size="20" />
      </span>
      <div class="app-form-section__titles">
        <h3 class="app-form-section__title">{{ title }}</h3>
        <p v-if="description" class="app-form-section__desc">{{ description }}</p>
      </div>
      <div class="app-form-section__aside">
        <slot name="aside" />
      </div>
    </header>
    <div class="app-form-section__body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.app-form-section {
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: var(--app-radius);
  background: rgb(var(--v-theme-surface));
  padding: var(--app-space-4) var(--app-space-5) var(--app-space-5);
  box-shadow: var(--app-shadow-sm);
}
.app-form-section + .app-form-section {
  margin-top: var(--app-space-4);
}
.app-form-section__head {
  display: flex;
  align-items: flex-start;
  gap: var(--app-space-3);
  margin-bottom: var(--app-space-4);
}
.app-form-section__icon {
  flex: none;
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  color: rgb(var(--v-theme-primary));
  background: linear-gradient(135deg, rgba(var(--v-theme-primary), 0.16), rgba(var(--v-theme-primary), 0.06));
}
.app-form-section__titles {
  flex: 1 1 auto;
  min-width: 0;
}
.app-form-section__title {
  font-size: 0.95rem;
  font-weight: 700;
  letter-spacing: -0.1px;
  line-height: 1.5;
}
.app-form-section__desc {
  font-size: 0.8rem;
  color: rgba(var(--v-theme-on-surface), 0.6);
  margin: 2px 0 0;
}
.app-form-section__aside {
  flex: none;
}
</style>
