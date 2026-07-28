<script setup lang="ts">
import { computed } from 'vue'
import { useFormat } from '@/composables/useFormat'
import type { DashboardWidget, WidgetData } from '@/types/models'

const props = defineProps<{ widget: DashboardWidget; data?: WidgetData }>()
const { formatNumber } = useFormat()

const meta = computed(() => props.data?.meta ?? {})
const color = computed(() => String(props.widget.config?.color ?? 'primary'))
const target = computed(() => Number(meta.value.target ?? props.widget.config?.target ?? 0))
</script>

<template>
  <div>
    <div class="d-flex align-baseline">
      <span class="text-h4 font-weight-bold" :class="`text-${color}`">
        {{ formatNumber(data?.value ?? null) }}
      </span>
      <span v-if="meta.unit" class="text-body-2 text-medium-emphasis ml-1">{{ meta.unit }}</span>
    </div>
    <div v-if="widget.config?.label" class="text-caption text-medium-emphasis">
      {{ widget.config.label }}
    </div>
    <div v-if="target" class="text-caption text-medium-emphasis mt-1">
      / {{ formatNumber(target) }}
    </div>
  </div>
</template>
