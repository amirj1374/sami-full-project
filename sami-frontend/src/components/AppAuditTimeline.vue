<script setup lang="ts">
import AppEmptyState from '@/components/AppEmptyState.vue'

export interface AuditTimelineItem {
  id: string | number
  title: string
  description?: string | null
  actor?: string | null
  occurredAt: string
  type?: 'created' | 'updated' | 'status' | 'approved' | 'permission' | 'default'
}
withDefaults(defineProps<{ items: AuditTimelineItem[]; emptyTitle: string; formatDate: (value: string) => string }>(), {})
const colors = { created: 'success', updated: 'info', status: 'warning', approved: 'success', permission: 'primary', default: 'secondary' }
</script>

<template>
  <v-timeline v-if="items.length" side="end" density="compact" truncate-line="both">
    <v-timeline-item v-for="item in items" :key="item.id" :dot-color="colors[item.type ?? 'default']" size="small">
      <div class="font-weight-bold">{{ item.title }}</div>
      <div v-if="item.description" class="text-body-2 text-medium-emphasis mt-1">{{ item.description }}</div>
      <div class="text-caption text-medium-emphasis mt-1">{{ formatDate(item.occurredAt) }}<span v-if="item.actor"> · {{ item.actor }}</span></div>
    </v-timeline-item>
  </v-timeline>
  <AppEmptyState v-else dense icon="mdi-timeline-clock-outline" :title="emptyTitle" />
</template>
