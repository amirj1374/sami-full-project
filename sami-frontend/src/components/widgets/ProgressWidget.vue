<script setup lang="ts">
import { computed } from 'vue'
import { useFormat } from '@/composables/useFormat'
import type { DashboardWidget, WidgetData } from '@/types/models'

const props = defineProps<{ widget: DashboardWidget; data?: WidgetData }>()
const { formatNumber } = useFormat()

const color = computed(() => String(props.widget.config?.color ?? 'primary'))
const target = computed(() =>
  Number(props.data?.meta?.target ?? props.widget.config?.target ?? 100),
)
const percent = computed(() => {
  const value = Number(props.data?.value ?? 0)
  return target.value ? Math.max(0, Math.min(100, (value / target.value) * 100)) : 0
})
</script>

<template>
  <div>
    <div class="d-flex justify-space-between text-body-2 mb-1">
      <span>{{ formatNumber(data?.value ?? null) }}</span>
      <span class="text-medium-emphasis">/ {{ formatNumber(target) }}</span>
    </div>
    <v-progress-linear :model-value="percent" :color="color" height="12" rounded />
  </div>
</template>
