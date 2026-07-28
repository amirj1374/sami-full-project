<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { dashboardsApi, kpisApi } from '@/api/dashboards'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import type { DashboardAuditEntry, DashboardRow, Kpi, KpiValueEntry } from '@/types/models'

/**
 * Reports: Executive KPI Summary (all KPIs, latest values vs targets, CSV
 * export), KPI Trend analysis (historical chart), and Dashboard Usage (the
 * audit trail of who changed what and when).
 */
const { t } = useI18n()
const { formatNumber, formatDateTime } = useFormat()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()

const tab = ref<'summary' | 'trends' | 'usage'>('summary')
const kpis = ref<Kpi[]>([])
const dashboards = ref<DashboardRow[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  clearError()
  try {
    ;[kpis.value, dashboards.value] = await Promise.all([
      kpisApi.list({ size: 200 }).then((p) => p.content),
      dashboardsApi.list({ size: 100 }).then((p) => p.content),
    ])
    trendKpiId.value = kpis.value[0]?.id ?? null
    usageDashboardId.value = dashboards.value[0]?.id ?? null
  } catch (err) {
    setError(err)
  } finally {
    loading.value = false
  }
})

function attainment(kpi: Kpi): number | null {
  if (kpi.latestValue === null || !kpi.targetValue) return null
  return Math.round((kpi.latestValue / kpi.targetValue) * 100)
}

async function exportCsv(): Promise<void> {
  clearError()
  try {
    const csv = await kpisApi.exportCsv()
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
    const link = document.createElement('a')
    link.href = url
    link.download = 'executive-kpi-summary.csv'
    link.click()
    URL.revokeObjectURL(url)
  } catch (err) {
    setError(err)
  }
}

// --- trends -----------------------------------------------------------------
const trendKpiId = ref<number | null>(null)
const trendEntries = ref<KpiValueEntry[]>([])

watch(trendKpiId, async (id) => {
  if (id === null) return
  try {
    trendEntries.value = await kpisApi.history(id, 60)
  } catch (err) {
    setError(err)
  }
})

const trendMax = computed(() => Math.max(1, ...trendEntries.value.map((e) => Number(e.value) || 0)))
const trendPoints = computed(() => {
  const entries = [...trendEntries.value].reverse()
  return entries
    .map((e, i) => {
      const x = entries.length > 1 ? (i / (entries.length - 1)) * 100 : 50
      const y = 40 - (Number(e.value) / trendMax.value) * 36
      return `${x},${y}`
    })
    .join(' ')
})

// --- usage ------------------------------------------------------------------
const usageDashboardId = ref<number | null>(null)
const usageEntries = ref<DashboardAuditEntry[]>([])

watch(usageDashboardId, async (id) => {
  if (id === null) return
  try {
    usageEntries.value = (await dashboardsApi.audit(id, { size: 50 })).content
  } catch (err) {
    setError(err)
  }
})
</script>

<template>
  <div>
    <div class="d-flex align-center mb-4 ga-2">
      <v-btn icon="mdi-arrow-left" variant="text" :to="{ name: 'dashboards' }" />
      <h1 class="text-h4">{{ t('dash.reports') }}</h1>
      <v-spacer />
      <v-btn v-can="'dashboards:export'" variant="tonal" prepend-icon="mdi-download" @click="exportCsv">
        {{ t('common.export') }}
      </v-btn>
    </div>

    <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
      {{ errorMessage }}
    </v-alert>

    <v-tabs v-model="tab" class="mb-4">
      <v-tab value="summary">{{ t('dash.executiveSummary') }}</v-tab>
      <v-tab value="trends">{{ t('dash.kpiTrends') }}</v-tab>
      <v-tab value="usage">{{ t('dash.dashboardUsage') }}</v-tab>
    </v-tabs>

    <v-progress-linear v-if="loading" indeterminate class="mb-4" />

    <v-window v-model="tab">
      <!-- Executive KPI summary -->
      <v-window-item value="summary">
        <v-row>
          <v-col v-for="kpi in kpis" :key="kpi.id" cols="12" sm="6" md="3">
            <v-card rounded="lg" border flat>
              <v-card-text>
                <div class="text-caption text-medium-emphasis">{{ kpi.name }}</div>
                <div class="text-h5 font-weight-bold">
                  {{ kpi.latestValue !== null ? formatNumber(kpi.latestValue) : '—' }}
                  <span class="text-body-2 text-medium-emphasis">{{ kpi.unit ?? '' }}</span>
                </div>
                <div class="d-flex align-center mt-1 ga-1">
                  <v-chip v-if="kpi.latestThresholdLevel" size="x-small" variant="tonal" color="info">
                    {{ kpi.latestThresholdLevel }}
                  </v-chip>
                  <span v-if="attainment(kpi) !== null" class="text-caption text-medium-emphasis">
                    {{ attainment(kpi) }}% {{ t('dash.ofTarget') }}
                  </span>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
        <v-alert v-if="!loading && kpis.length === 0" type="info" variant="tonal" density="compact">
          {{ t('dash.noKpis') }}
        </v-alert>
      </v-window-item>

      <!-- KPI trends -->
      <v-window-item value="trends">
        <v-card rounded="lg" border flat>
          <v-card-text>
            <v-select
              v-model="trendKpiId"
              :items="kpis"
              item-title="name"
              item-value="id"
              :label="t('dash.kpis')"
              density="comfortable"
              style="max-width: 320px"
            />
            <div v-if="trendEntries.length === 0" class="text-body-2 text-medium-emphasis">—</div>
            <svg v-else viewBox="0 0 100 44" width="100%" height="220" preserveAspectRatio="none">
              <polyline :points="trendPoints" fill="none" stroke="#1867C0" stroke-width="1.2" />
            </svg>
          </v-card-text>
        </v-card>
      </v-window-item>

      <!-- Dashboard usage (audit) -->
      <v-window-item value="usage">
        <v-card rounded="lg" border flat>
          <v-card-text>
            <v-select
              v-model="usageDashboardId"
              :items="dashboards"
              item-title="name"
              item-value="id"
              :label="t('dash.dashboards')"
              density="comfortable"
              style="max-width: 320px"
            />
            <v-table density="compact" class="text-caption">
              <thead>
                <tr>
                  <th>{{ t('common.actions') }}</th>
                  <th>{{ t('dash.actor') }}</th>
                  <th>{{ t('dash.when') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="entry in usageEntries" :key="entry.id">
                  <td>
                    <v-chip size="x-small" variant="tonal">{{ entry.action }}</v-chip>
                    <span v-if="entry.newValues" class="text-medium-emphasis ml-1">
                      {{ JSON.stringify(entry.newValues) }}
                    </span>
                  </td>
                  <td>{{ entry.actorEmail ?? 'system' }}</td>
                  <td>{{ formatDateTime(entry.createdAt) }}</td>
                </tr>
              </tbody>
            </v-table>
          </v-card-text>
        </v-card>
      </v-window-item>
    </v-window>
  </div>
</template>
