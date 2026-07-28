<script setup lang="ts">
import { computed } from 'vue'
import type { DashboardWidget, WidgetData } from '@/types/models'

const props = defineProps<{ widget: DashboardWidget; data?: WidgetData }>()

const rows = computed(() => props.data?.rows ?? [])
const columns = computed(() => {
  if (props.data?.columns?.length) return props.data.columns
  return rows.value.length > 0 ? Object.keys(rows.value[0]) : []
})
</script>

<template>
  <div v-if="rows.length === 0" class="text-caption text-medium-emphasis text-center">—</div>
  <v-table v-else density="compact" class="text-caption">
    <thead>
      <tr>
        <th v-for="c in columns" :key="c">{{ c }}</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="(row, i) in rows" :key="i">
        <td v-for="c in columns" :key="c">{{ row[c] }}</td>
      </tr>
    </tbody>
  </v-table>
</template>
