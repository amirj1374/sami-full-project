<script setup lang="ts">
import { computed } from 'vue'
import type { DashboardWidget, WidgetData } from '@/types/models'

/** Renders a series as intensity-colored cells (label + shaded value). */
const props = defineProps<{ widget: DashboardWidget; data?: WidgetData }>()

const series = computed(() => props.data?.series ?? [])
const max = computed(() => Math.max(1, ...series.value.map((p) => Number(p.value) || 0)))

function cellStyle(value: number): Record<string, string> {
  const intensity = Math.max(0.08, Math.min(1, value / max.value))
  return { backgroundColor: `rgba(24, 103, 192, ${intensity})` }
}
</script>

<template>
  <div v-if="series.length === 0" class="text-caption text-medium-emphasis text-center">—</div>
  <div v-else class="d-flex flex-wrap ga-1">
    <div
      v-for="(p, i) in series"
      :key="i"
      class="rounded pa-2 text-center text-caption"
      :style="{ ...cellStyle(Number(p.value)), minWidth: '56px', color: 'white' }"
    >
      <div class="font-weight-medium">{{ p.value }}</div>
      <div class="text-truncate" style="max-width: 72px">{{ p.label }}</div>
    </div>
  </div>
</template>
