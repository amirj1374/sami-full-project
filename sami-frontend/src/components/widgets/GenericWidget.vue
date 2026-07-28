<script setup lang="ts">
import { computed } from 'vue'
import { useFormat } from '@/composables/useFormat'
import type { DashboardWidget, WidgetData } from '@/types/models'

/**
 * Fallback renderer for unknown/unsupported widget types: shows the scalar
 * value when present, otherwise the raw data — so a newly configured widget
 * type is always visible, never broken.
 */
const props = defineProps<{ widget: DashboardWidget; data?: WidgetData }>()
const { formatNumber } = useFormat()

const hasScalar = computed(() => props.data?.value !== null && props.data?.value !== undefined)
</script>

<template>
  <div v-if="hasScalar" class="text-h4 font-weight-bold text-primary">
    {{ formatNumber(data?.value ?? null) }}
  </div>
  <pre v-else-if="data && data.kind !== 'EMPTY'" class="text-caption" style="white-space: pre-wrap">{{
    JSON.stringify(data.series ?? data.rows ?? {}, null, 1)
  }}</pre>
  <div v-else class="text-caption text-medium-emphasis text-center">—</div>
</template>
