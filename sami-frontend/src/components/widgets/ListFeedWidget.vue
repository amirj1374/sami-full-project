<script setup lang="ts">
import { computed } from 'vue'
import type { DashboardWidget, WidgetData } from '@/types/models'

/**
 * Renders ROWS data as a feed/list (activity feeds, notification panels,
 * timelines, calendars). Recognized row keys: title/label/message, subtitle,
 * time/date, icon, color — anything else is shown as the row's text.
 */
const props = defineProps<{ widget: DashboardWidget; data?: WidgetData }>()

interface FeedRow {
  title: string
  subtitle?: string
  time?: string
  icon?: string
  color?: string
}

const rows = computed<FeedRow[]>(() =>
  (props.data?.rows ?? []).map((r) => ({
    title: String(r.title ?? r.label ?? r.message ?? Object.values(r)[0] ?? ''),
    subtitle: r.subtitle !== undefined ? String(r.subtitle) : undefined,
    time: r.time !== undefined ? String(r.time) : r.date !== undefined ? String(r.date) : undefined,
    icon: r.icon !== undefined ? String(r.icon) : undefined,
    color: r.color !== undefined ? String(r.color) : undefined,
  })),
)
</script>

<template>
  <div v-if="rows.length === 0" class="text-caption text-medium-emphasis text-center">—</div>
  <v-list v-else density="compact" class="py-0">
    <v-list-item v-for="(row, i) in rows" :key="i" class="px-0">
      <template v-if="row.icon" #prepend>
        <v-icon :icon="row.icon" :color="row.color ?? 'primary'" size="small" />
      </template>
      <v-list-item-title class="text-body-2">{{ row.title }}</v-list-item-title>
      <v-list-item-subtitle v-if="row.subtitle" class="text-caption">
        {{ row.subtitle }}
      </v-list-item-subtitle>
      <template v-if="row.time" #append>
        <span class="text-caption text-medium-emphasis">{{ row.time }}</span>
      </template>
    </v-list-item>
  </v-list>
</template>
