<script setup lang="ts">
import { computed } from 'vue'
import { useNotifications } from '@/composables/useNotifications'

const { queue, dismiss } = useNotifications()
const current = computed(() => queue.value[0])
const colors = { success: 'success', error: 'error', warning: 'warning', info: 'info', loading: 'primary' } as const
</script>

<template>
  <v-snackbar
    :model-value="!!current"
    :color="current ? colors[current.kind] : 'info'"
    :timeout="current?.kind === 'loading' ? -1 : (current?.timeout ?? 4500)"
    @update:model-value="value => { if (!value && current) dismiss(current.id) }"
  >
    <div class="d-flex align-center ga-3">
      <v-progress-circular v-if="current?.kind === 'loading'" indeterminate size="20" width="2" />
      <span class="flex-grow-1">{{ current?.message }}</span>
      <v-btn v-if="current?.actionLabel" variant="text" size="small" @click="current.action?.(); dismiss(current.id)">{{ current.actionLabel }}</v-btn>
      <v-btn icon="mdi-close" variant="text" size="small" :aria-label="$t('common.close')" @click="current && dismiss(current.id)" />
    </div>
  </v-snackbar>
</template>
