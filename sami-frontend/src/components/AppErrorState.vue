<script setup lang="ts">
withDefaults(defineProps<{ title: string; description?: string; retryLabel?: string; compact?: boolean }>(), {})
defineEmits<{ (e: 'retry'): void }>()
</script>

<template>
  <v-alert type="error" variant="tonal" :class="{ 'app-error--compact': compact }">
    <div class="font-weight-bold">{{ title }}</div>
    <div v-if="description" class="text-body-2 mt-1">{{ description }}</div>
    <v-btn v-if="retryLabel" class="mt-3" size="small" variant="tonal" prepend-icon="mdi-refresh" @click="$emit('retry')">
      {{ retryLabel }}
    </v-btn>
  </v-alert>
</template>
