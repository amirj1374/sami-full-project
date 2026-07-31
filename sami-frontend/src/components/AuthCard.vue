<script setup lang="ts">
defineProps<{
  title: string
  subtitle: string
}>()
</script>

<template>
  <v-card class="auth-card" rounded="xl">
    <div class="auth-card__accent" />
    <v-card-item class="auth-card__heading">
      <v-card-title class="auth-card__title">{{ title }}</v-card-title>
      <v-card-subtitle class="auth-card__subtitle">{{ subtitle }}</v-card-subtitle>
    </v-card-item>

    <v-card-text class="auth-card__content">
      <slot />
    </v-card-text>

    <v-card-actions v-if="$slots.actions" class="auth-card__actions">
      <slot name="actions" />
    </v-card-actions>
  </v-card>
</template>

<style scoped>
.auth-card {
  position: relative;
  width: 100%;
  overflow: hidden;
  border: 1px solid rgba(var(--v-theme-on-surface), 0.1);
  background: rgba(var(--v-theme-surface), 0.96);
  box-shadow: var(--app-shadow-lg);
}
.auth-card__accent {
  height: 4px;
  background: linear-gradient(
    90deg,
    rgb(var(--v-theme-primary)),
    rgb(var(--v-theme-accent))
  );
}
.auth-card__heading {
  padding: 28px 28px 12px;
}
.auth-card__title {
  font-size: 1.5rem;
  font-weight: 750;
  line-height: 1.35;
  white-space: normal;
}
.auth-card__subtitle {
  margin-top: 6px;
  line-height: 1.65;
  white-space: normal;
}
.auth-card__content {
  padding: 16px 28px 8px;
}
.auth-card__actions {
  min-height: 64px;
  padding: 8px 28px 24px;
  justify-content: center;
  flex-wrap: wrap;
  gap: 2px;
}

@media (max-width: 599px) {
  .auth-card {
    border: 0;
    border-radius: var(--app-radius-xl) var(--app-radius-xl) 0 0 !important;
    box-shadow: 0 -8px 32px rgba(20, 28, 46, 0.09);
  }
  .auth-card__heading {
    padding: 24px 20px 10px;
  }
  .auth-card__title {
    font-size: 1.35rem;
  }
  .auth-card__content {
    padding: 14px 20px 6px;
  }
  .auth-card__actions {
    padding: 8px 20px max(20px, env(safe-area-inset-bottom));
  }
}
</style>
