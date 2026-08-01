<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AppEmptyState from '@/components/AppEmptyState.vue'
import AppLoadingState from '@/components/AppLoadingState.vue'
import { useServerLabel } from '@/composables/useServerLabel'
import type { CrmStats, CrmStatsBucket } from '@/types/models'

const props = defineProps<{ stats: CrmStats | null; loading: boolean }>()
const emit = defineEmits<{ refresh: [] }>()
const { t } = useI18n()
const { text: serverLabel } = useServerLabel()

const reports = computed(() => props.stats ? [
  { key: 'byStatus', icon: 'mdi-state-machine', color: 'primary', rows: props.stats.byStatus },
  { key: 'byType', icon: 'mdi-shape-outline', color: 'info', rows: props.stats.byType },
  { key: 'bySource', icon: 'mdi-source-branch', color: 'success', rows: props.stats.bySource },
] : [])

function percent(row: CrmStatsBucket) {
  if (!props.stats?.total) return 0
  return Math.round((row.count / props.stats.total) * 100)
}
</script>

<template>
  <section aria-labelledby="crm-reports-title">
    <div class="d-flex align-center flex-wrap ga-2 mb-4">
      <div>
        <h2 id="crm-reports-title" class="text-h6">{{ t('customers.reports.title') }}</h2>
        <p class="text-body-2 text-medium-emphasis mb-0">{{ t('customers.reports.subtitle') }}</p>
      </div>
      <v-spacer />
      <v-btn variant="tonal" prepend-icon="mdi-refresh" :loading="loading" @click="emit('refresh')">{{ t('common.refresh') }}</v-btn>
    </div>
    <AppLoadingState v-if="loading && !stats" variant="cards" :rows="3" :label="t('common.loading')" />
    <v-row v-else-if="stats">
      <v-col v-for="report in reports" :key="report.key" cols="12" lg="4">
        <v-card rounded="xl" class="app-data-surface h-100">
          <v-card-title class="d-flex align-center ga-2">
            <v-avatar :color="report.color" size="36" variant="tonal" rounded="lg"><v-icon :icon="report.icon" size="small" /></v-avatar>
            <span>{{ t(`customers.reports.${report.key}`) }}</span>
          </v-card-title>
          <v-card-text>
            <div v-for="row in report.rows" :key="row.name" class="mb-4">
              <div class="d-flex align-center mb-1">
                <span class="text-body-2 text-truncate">{{ serverLabel(row.name) }}</span>
                <v-spacer />
                <strong>{{ row.count }}</strong>
                <span class="text-caption text-medium-emphasis ms-2">{{ percent(row) }}%</span>
              </div>
              <v-progress-linear :model-value="percent(row)" :color="report.color" height="7" rounded />
            </div>
            <AppEmptyState v-if="!report.rows.length" icon="mdi-chart-box-outline" :title="t('customers.reports.empty')" />
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </section>
</template>

<style scoped>
@media (max-width: 599px) {
  :deep(.v-btn) { min-height: 44px; }
}
</style>
