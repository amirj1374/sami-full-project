<script setup lang="ts">
import { computed } from 'vue'
import { useFormat } from '@/composables/useFormat'
import type { DashboardWidget, WidgetData } from '@/types/models'

const props = defineProps<{ widget: DashboardWidget; data?: WidgetData }>()
const { formatNumber } = useFormat()

const ranked = computed(() =>
  [...(props.data?.series ?? [])].sort((a, b) => Number(b.value) - Number(a.value)),
)
const max = computed(() => Math.max(1, ...ranked.value.map((p) => Number(p.value) || 0)))
const MEDALS = ['🥇', '🥈', '🥉']
</script>

<template>
  <div v-if="ranked.length === 0" class="text-caption text-medium-emphasis text-center">—</div>
  <div v-else>
    <div v-for="(p, i) in ranked" :key="i" class="mb-2">
      <div class="d-flex justify-space-between text-body-2">
        <span>{{ MEDALS[i] ?? `${i + 1}.` }} {{ p.label }}</span>
        <span class="font-weight-medium">{{ formatNumber(p.value) }}</span>
      </div>
      <v-progress-linear :model-value="(Number(p.value) / max) * 100" color="primary" height="6" rounded />
    </div>
  </div>
</template>
