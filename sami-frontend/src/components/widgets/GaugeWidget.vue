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
  <div class="text-center">
    <v-progress-circular :model-value="percent" :size="92" :width="10" :color="color">
      <span class="text-body-1 font-weight-bold">{{ formatNumber(data?.value ?? null) }}</span>
    </v-progress-circular>
    <div class="text-caption text-medium-emphasis mt-2">
      {{ Math.round(percent) }}%
    </div>
  </div>
</template>
