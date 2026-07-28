<script setup lang="ts">
import { computed } from 'vue'
import type { DashboardWidget, WidgetData, WidgetDataPoint } from '@/types/models'

/**
 * Dependency-free SVG chart renderer. The widget's chart type selects the
 * form: bar (default), line/area, pie/donut. Other configured chart types fall
 * back to bars, so newly added chart-type rows degrade gracefully.
 */
const props = defineProps<{ widget: DashboardWidget; data?: WidgetData }>()

const series = computed<WidgetDataPoint[]>(() => props.data?.series ?? [])
const max = computed(() => Math.max(1, ...series.value.map((p) => Number(p.value) || 0)))
const chart = computed(() => props.widget.chartTypeCode ?? 'bar')

const PALETTE = ['#1867C0', '#26A69A', '#EF6C00', '#8E24AA', '#43A047', '#E53935', '#FDD835', '#5E35B1']

const linePoints = computed(() =>
  series.value
    .map((p, i) => {
      const x = series.value.length > 1 ? (i / (series.value.length - 1)) * 100 : 50
      const y = 100 - (Number(p.value) / max.value) * 90
      return `${x},${y}`
    })
    .join(' '),
)

const total = computed(() => series.value.reduce((s, p) => s + (Number(p.value) || 0), 0) || 1)

const pieSlices = computed(() => {
  let angle = -Math.PI / 2
  return series.value.map((p, i) => {
    const fraction = (Number(p.value) || 0) / total.value
    const start = angle
    const end = angle + fraction * Math.PI * 2
    angle = end
    const large = fraction > 0.5 ? 1 : 0
    const sx = 50 + 45 * Math.cos(start)
    const sy = 50 + 45 * Math.sin(start)
    const ex = 50 + 45 * Math.cos(end)
    const ey = 50 + 45 * Math.sin(end)
    return {
      d: `M50,50 L${sx},${sy} A45,45 0 ${large} 1 ${ex},${ey} Z`,
      color: PALETTE[i % PALETTE.length],
      label: p.label,
    }
  })
})
</script>

<template>
  <div v-if="series.length === 0" class="text-caption text-medium-emphasis text-center">—</div>

  <!-- pie / donut -->
  <div v-else-if="chart === 'pie' || chart === 'donut'" class="d-flex align-center ga-4">
    <svg viewBox="0 0 100 100" width="110" height="110">
      <path v-for="(s, i) in pieSlices" :key="i" :d="s.d" :fill="s.color" />
      <circle v-if="chart === 'donut'" cx="50" cy="50" r="24" fill="rgb(var(--v-theme-surface))" />
    </svg>
    <div class="text-caption">
      <div v-for="(s, i) in pieSlices" :key="i" class="d-flex align-center">
        <span class="d-inline-block mr-1 rounded" :style="{ background: s.color, width: '10px', height: '10px' }" />
        {{ s.label }}
      </div>
    </div>
  </div>

  <!-- line / area -->
  <svg v-else-if="chart === 'line' || chart === 'area'" viewBox="0 0 100 100" width="100%" height="120" preserveAspectRatio="none">
    <polygon
      v-if="chart === 'area'"
      :points="`0,100 ${linePoints} 100,100`"
      fill="rgba(24,103,192,0.18)"
    />
    <polyline :points="linePoints" fill="none" stroke="#1867C0" stroke-width="2" />
  </svg>

  <!-- bar family (default / fallback for other chart types) -->
  <div v-else>
    <svg :viewBox="`0 0 ${Math.max(series.length * 40, 40)} 100`" width="100%" height="110" preserveAspectRatio="none">
      <rect
        v-for="(p, i) in series"
        :key="i"
        :x="i * 40 + 8"
        :y="100 - (Number(p.value) / max) * 92"
        width="24"
        :height="(Number(p.value) / max) * 92"
        fill="#1867C0"
        rx="3"
      />
    </svg>
    <div class="d-flex text-caption text-medium-emphasis" style="gap: 16px">
      <span v-for="(p, i) in series" :key="i" class="text-truncate" style="width: 24px">{{ p.label }}</span>
    </div>
  </div>
</template>
