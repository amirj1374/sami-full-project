<script setup lang="ts">
/**
 * Reusable empty state: an illustrative icon medallion, a title, an optional
 * description and an optional call-to-action. Every list/section that can be
 * empty renders this instead of a blank area, so no screen ever looks broken.
 */
withDefaults(
  defineProps<{
    icon?: string
    title: string
    description?: string
    actionLabel?: string
    actionIcon?: string
    dense?: boolean
  }>(),
  { icon: 'mdi-inbox-outline', actionIcon: 'mdi-plus' },
)

defineEmits<{ (e: 'action'): void }>()
</script>

<template>
  <div class="app-empty text-center" :class="{ 'app-empty--dense': dense }">
    <div class="app-empty__medallion">
      <v-icon :icon="icon" :size="dense ? 30 : 40" />
    </div>
    <div class="text-subtitle-1 font-weight-bold mt-4">{{ title }}</div>
    <div v-if="description" class="text-body-2 text-medium-emphasis mt-1 mx-auto app-empty__desc">
      {{ description }}
    </div>
    <div v-if="actionLabel" class="mt-4">
      <v-btn color="primary" :prepend-icon="actionIcon" @click="$emit('action')">
        {{ actionLabel }}
      </v-btn>
    </div>
    <slot />
  </div>
</template>

<style scoped>
.app-empty {
  padding: var(--app-space-8) var(--app-space-4);
}
.app-empty--dense {
  padding: var(--app-space-6) var(--app-space-4);
}
.app-empty__medallion {
  width: 84px;
  height: 84px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: rgb(var(--v-theme-primary));
  background: radial-gradient(
    circle at 50% 40%,
    rgba(var(--v-theme-primary), 0.14),
    rgba(var(--v-theme-primary), 0.05)
  );
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-primary), 0.12);
}
.app-empty__desc {
  max-width: 420px;
}
</style>
