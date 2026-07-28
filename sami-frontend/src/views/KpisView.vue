<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDebounceFn } from '@vueuse/core'
import { dashboardsApi, kpisApi } from '@/api/dashboards'
import { usePermission } from '@/composables/usePermission'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import type {
  DashboardDataSource,
  DashboardKpiStatus,
  DashboardRefreshPolicy,
  Kpi,
  KpiThreshold,
  KpiValueEntry,
} from '@/types/models'

/**
 * KPI management: list with latest values/bands, create/edit with formula,
 * thresholds and refresh policy, formula validation, calculation preview and
 * value history.
 */
const { t } = useI18n()
const { can } = usePermission()
const { formatNumber, formatDateTime } = useFormat()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()

const CALC_METHODS = ['PROVIDER', 'FORMULA', 'MANUAL']
const AGGREGATIONS = ['LAST', 'SUM', 'AVG', 'COUNT', 'MIN', 'MAX']

const kpiStatuses = ref<DashboardKpiStatus[]>([])
const dataSources = ref<DashboardDataSource[]>([])
const refreshPolicies = ref<DashboardRefreshPolicy[]>([])

onMounted(async () => {
  try {
    ;[kpiStatuses.value, dataSources.value, refreshPolicies.value] = await Promise.all([
      dashboardsApi.kpiStatuses(),
      dashboardsApi.dataSources(),
      dashboardsApi.refreshPolicies(),
    ])
  } catch (err) {
    setError(err)
  }
})

// --- table ------------------------------------------------------------------
interface TableOptions {
  page: number
  itemsPerPage: number
  sortBy: { key: string; order?: 'asc' | 'desc' }[]
}

const items = ref<Kpi[]>([])
const totalItems = ref(0)
const loading = ref(false)
const lastOptions = ref<TableOptions>({ page: 1, itemsPerPage: 10, sortBy: [] })
const searchFilter = ref('')

const headers = computed(() => [
  { title: t('common.code'), key: 'code' },
  { title: t('common.name'), key: 'name' },
  { title: t('dash.method'), key: 'calculationMethod', sortable: false },
  { title: t('dash.latestValue'), key: 'latestValue', sortable: false },
  { title: t('dash.band'), key: 'band', sortable: false },
  { title: t('common.status'), key: 'status', sortable: false },
  { title: '', key: 'actions', sortable: false, align: 'end' as const },
])

async function loadItems(options: TableOptions): Promise<void> {
  lastOptions.value = options
  loading.value = true
  clearError()
  const sort = options.sortBy[0]
  try {
    const page = await kpisApi.list({
      page: options.page - 1,
      size: options.itemsPerPage,
      sort: sort ? `${sort.key},${sort.order ?? 'asc'}` : undefined,
      search: searchFilter.value || undefined,
    })
    items.value = page.content
    totalItems.value = page.totalElements
  } catch (err) {
    setError(err)
  } finally {
    loading.value = false
  }
}

function refresh(): void {
  void loadItems(lastOptions.value)
}

const reload = useDebounceFn(() => void loadItems({ ...lastOptions.value, page: 1 }), 300)
watch(searchFilter, reload)

// --- form -------------------------------------------------------------------
const formOpen = ref(false)
const editing = ref<Kpi | null>(null)
const saving = ref(false)
const { message: formError, set: setFormError, clear: clearFormError } = useApiError()

const code = ref('')
const name = ref('')
const description = ref('')
const method = ref('PROVIDER')
const formula = ref('')
const aggregation = ref('LAST')
const dataSourceId = ref<number | null>(null)
const refreshPolicyId = ref<number | null>(null)
const targetValue = ref<number | null>(null)
const unit = ref('')
const statusId = ref<number | null>(null)
const thresholds = ref<Omit<KpiThreshold, 'id'>[]>([])

function openCreate(): void {
  editing.value = null
  code.value = ''
  name.value = ''
  description.value = ''
  method.value = 'PROVIDER'
  formula.value = ''
  aggregation.value = 'LAST'
  dataSourceId.value = null
  refreshPolicyId.value = null
  targetValue.value = null
  unit.value = ''
  statusId.value = kpiStatuses.value.find((s) => s.isDefault)?.id ?? null
  thresholds.value = []
  clearFormError()
  formOpen.value = true
}

function openEdit(kpi: Kpi): void {
  editing.value = kpi
  code.value = kpi.code
  name.value = kpi.name
  description.value = kpi.description ?? ''
  method.value = kpi.calculationMethod
  formula.value = kpi.formula ?? ''
  aggregation.value = kpi.aggregation
  dataSourceId.value = kpi.dataSourceId
  refreshPolicyId.value = kpi.refreshPolicyId
  targetValue.value = kpi.targetValue
  unit.value = kpi.unit ?? ''
  statusId.value = kpi.statusId
  thresholds.value = kpi.thresholds.map(({ levelName, color, minValue, maxValue, sortOrder }) => ({
    levelName,
    color,
    minValue,
    maxValue,
    sortOrder,
  }))
  clearFormError()
  formOpen.value = true
}

function addThreshold(): void {
  thresholds.value.push({
    levelName: '',
    color: 'green',
    minValue: null,
    maxValue: null,
    sortOrder: thresholds.value.length,
  })
}

async function save(): Promise<void> {
  clearFormError()
  saving.value = true
  const payload = {
    code: code.value.trim(),
    name: name.value.trim(),
    description: description.value.trim() || undefined,
    calculationMethod: method.value,
    formula: formula.value.trim() || undefined,
    aggregation: aggregation.value,
    dataSourceId: dataSourceId.value ?? undefined,
    refreshPolicyId: refreshPolicyId.value ?? undefined,
    targetValue: targetValue.value ?? undefined,
    unit: unit.value.trim() || undefined,
    statusId: statusId.value ?? undefined,
    thresholds: thresholds.value
      .filter((th) => th.levelName.trim())
      .map((th, index) => ({ ...th, sortOrder: index })),
    expectedVersion: editing.value?.version,
  }
  try {
    if (editing.value) await kpisApi.update(editing.value.id, payload)
    else await kpisApi.create(payload)
    formOpen.value = false
    refresh()
  } catch (err) {
    setFormError(err)
  } finally {
    saving.value = false
  }
}

// --- actions: validate / preview / history / delete -------------------------
const preview = ref<{ kpi: Kpi; value: number; level: string | null } | null>(null)
const historyOpen = ref(false)
const historyKpi = ref<Kpi | null>(null)
const historyEntries = ref<KpiValueEntry[]>([])
const deleteTarget = ref<Kpi | null>(null)
const deleting = ref(false)
const actionBusy = ref<number | null>(null)

async function calculate(kpi: Kpi): Promise<void> {
  actionBusy.value = kpi.id
  clearError()
  try {
    const result = await kpisApi.calculate(kpi.id)
    preview.value = { kpi, value: result.value, level: result.thresholdLevel }
    refresh()
  } catch (err) {
    setError(err)
  } finally {
    actionBusy.value = null
  }
}

async function validate(kpi: Kpi): Promise<void> {
  actionBusy.value = kpi.id
  clearError()
  try {
    const result = await kpisApi.validate(kpi.id)
    preview.value = result.valid
      ? { kpi, value: NaN, level: t('dash.formulaValid') }
      : { kpi, value: NaN, level: result.message }
  } catch (err) {
    setError(err)
  } finally {
    actionBusy.value = null
  }
}

async function openHistory(kpi: Kpi): Promise<void> {
  historyKpi.value = kpi
  historyOpen.value = true
  try {
    historyEntries.value = await kpisApi.history(kpi.id, 50)
  } catch (err) {
    setError(err)
  }
}

async function confirmDelete(): Promise<void> {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    await kpisApi.remove(deleteTarget.value.id)
    deleteTarget.value = null
    refresh()
  } catch (err) {
    setError(err)
  } finally {
    deleting.value = false
  }
}

async function exportCsv(): Promise<void> {
  clearError()
  try {
    const csv = await kpisApi.exportCsv()
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
    const link = document.createElement('a')
    link.href = url
    link.download = 'kpi-report.csv'
    link.click()
    URL.revokeObjectURL(url)
  } catch (err) {
    setError(err)
  }
}

const historyMax = computed(() =>
  Math.max(1, ...historyEntries.value.map((e) => Number(e.value) || 0)),
)
</script>

<template>
  <div>
    <div class="d-flex align-center mb-4 flex-wrap ga-2">
      <v-btn icon="mdi-arrow-left" variant="text" :to="{ name: 'dashboards' }" />
      <h1 class="text-h4">{{ t('dash.kpis') }}</h1>
      <v-spacer />
      <v-btn v-can="'dashboards:export'" variant="tonal" prepend-icon="mdi-download" @click="exportCsv">
        {{ t('common.export') }}
      </v-btn>
      <v-btn v-can="'dashboards:manage-kpis'" color="primary" prepend-icon="mdi-plus" @click="openCreate">
        {{ t('dash.newKpi') }}
      </v-btn>
    </div>

    <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
      {{ errorMessage }}
    </v-alert>

    <v-card rounded="lg" border flat>
      <v-card-text>
        <v-text-field
          v-model="searchFilter"
          :label="t('common.search')"
          prepend-inner-icon="mdi-magnify"
          clearable
          hide-details
          density="comfortable"
          style="max-width: 380px"
        />
      </v-card-text>

      <v-data-table-server
        :headers="headers"
        :items="items"
        :items-length="totalItems"
        :loading="loading"
        :items-per-page="10"
        item-value="id"
        @update:options="loadItems"
      >
        <template #[`item.calculationMethod`]="{ item }">
          <v-chip size="small" variant="tonal">{{ item.calculationMethod }}</v-chip>
        </template>
        <template #[`item.latestValue`]="{ item }">
          <span v-if="item.latestValue !== null" class="font-weight-medium">
            {{ formatNumber(item.latestValue) }} {{ item.unit ?? '' }}
          </span>
          <span v-else class="text-medium-emphasis">—</span>
        </template>
        <template #[`item.band`]="{ item }">
          <v-chip v-if="item.latestThresholdLevel" size="small" variant="tonal" color="info">
            {{ item.latestThresholdLevel }}
          </v-chip>
        </template>
        <template #[`item.status`]="{ item }">
          <v-chip size="small" variant="tonal">{{ item.statusName }}</v-chip>
        </template>
        <template #[`item.actions`]="{ item }">
          <v-btn
            icon="mdi-play"
            size="small"
            variant="text"
            :loading="actionBusy === item.id"
            :title="t('dash.calculate')"
            @click="calculate(item)"
          />
          <v-btn icon="mdi-history" size="small" variant="text" @click="openHistory(item)" />
          <v-btn
            v-if="can('dashboards:manage-kpis')"
            icon="mdi-check-decagram"
            size="small"
            variant="text"
            :title="t('dash.validateFormula')"
            @click="validate(item)"
          />
          <v-btn
            v-if="can('dashboards:manage-kpis')"
            icon="mdi-pencil"
            size="small"
            variant="text"
            @click="openEdit(item)"
          />
          <v-btn
            v-if="can('dashboards:manage-kpis')"
            icon="mdi-delete"
            size="small"
            variant="text"
            color="error"
            @click="deleteTarget = item"
          />
        </template>
      </v-data-table-server>
    </v-card>

    <!-- KPI form -->
    <v-dialog v-model="formOpen" max-width="720">
      <v-card rounded="lg">
        <v-card-title class="text-h6 pt-4 px-6">
          {{ editing ? t('dash.editKpi') : t('dash.newKpi') }}
        </v-card-title>
        <v-card-text class="px-6" style="max-height: 62vh; overflow-y: auto">
          <v-alert v-if="formError" type="error" variant="tonal" density="compact" class="mb-4">
            {{ formError }}
          </v-alert>
          <v-row dense>
            <v-col cols="12" sm="6"><v-text-field v-model="code" :label="t('common.code') + ' *'" :disabled="!!editing" /></v-col>
            <v-col cols="12" sm="6"><v-text-field v-model="name" :label="t('common.name') + ' *'" /></v-col>
            <v-col cols="12"><v-textarea v-model="description" :label="t('common.description')" rows="2" /></v-col>
            <v-col cols="12" sm="4">
              <v-select v-model="method" :items="CALC_METHODS" :label="t('dash.method') + ' *'" />
            </v-col>
            <v-col cols="12" sm="8">
              <v-text-field
                v-model="formula"
                :label="t('dash.formula')"
                :hint="method === 'FORMULA' ? t('dash.formulaHint') : t('dash.metricHint')"
                persistent-hint
                class="font-mono"
              />
            </v-col>
            <v-col cols="12" sm="4">
              <v-select v-model="aggregation" :items="AGGREGATIONS" :label="t('dash.aggregation')" />
            </v-col>
            <v-col cols="12" sm="4">
              <v-select
                v-model="dataSourceId"
                :items="dataSources"
                item-title="name"
                item-value="id"
                :label="t('dash.dataSource')"
                clearable
              />
            </v-col>
            <v-col cols="12" sm="4">
              <v-select
                v-model="refreshPolicyId"
                :items="refreshPolicies"
                item-title="name"
                item-value="id"
                :label="t('dash.refreshPolicy')"
                clearable
              />
            </v-col>
            <v-col cols="6" sm="4"><v-text-field v-model.number="targetValue" type="number" :label="t('dash.target')" /></v-col>
            <v-col cols="6" sm="4"><v-text-field v-model="unit" :label="t('dash.unit')" /></v-col>
            <v-col cols="12" sm="4">
              <v-select v-model="statusId" :items="kpiStatuses" item-title="name" item-value="id" :label="t('common.status')" />
            </v-col>
          </v-row>

          <div class="d-flex align-center mt-2 mb-1">
            <span class="text-subtitle-2">{{ t('dash.thresholds') }}</span>
            <v-spacer />
            <v-btn size="small" variant="tonal" prepend-icon="mdi-plus" @click="addThreshold">
              {{ t('common.add') }}
            </v-btn>
          </div>
          <v-row v-for="(th, i) in thresholds" :key="i" dense align="center">
            <v-col cols="3"><v-text-field v-model="th.levelName" :label="t('dash.level')" density="compact" hide-details /></v-col>
            <v-col cols="3">
              <v-select
                v-model="th.color"
                :items="['green', 'light-green', 'amber', 'orange', 'red', 'blue', 'grey']"
                :label="t('dash.color')"
                density="compact"
                hide-details
              />
            </v-col>
            <v-col cols="2"><v-text-field v-model.number="th.minValue" type="number" :label="t('dash.min')" density="compact" hide-details /></v-col>
            <v-col cols="2"><v-text-field v-model.number="th.maxValue" type="number" :label="t('dash.max')" density="compact" hide-details /></v-col>
            <v-col cols="2" class="text-end">
              <v-btn icon="mdi-close" size="x-small" variant="text" @click="thresholds.splice(i, 1)" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions class="px-6 pb-4">
          <v-spacer />
          <v-btn variant="text" @click="formOpen = false">{{ t('common.cancel') }}</v-btn>
          <v-btn color="primary" :loading="saving" @click="save">{{ t('common.save') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- calculation / validation preview -->
    <v-snackbar :model-value="!!preview" timeout="6000" @update:model-value="preview = null">
      <template v-if="preview">
        <strong>{{ preview.kpi.name }}</strong>
        <template v-if="!Number.isNaN(preview.value)">
          : {{ formatNumber(preview.value) }}
          <v-chip v-if="preview.level" size="x-small" class="ml-1">{{ preview.level }}</v-chip>
        </template>
        <template v-else> — {{ preview.level }}</template>
      </template>
    </v-snackbar>

    <!-- history -->
    <v-dialog v-model="historyOpen" max-width="560">
      <v-card rounded="lg">
        <v-card-title class="text-h6 pt-4 px-6">
          {{ t('dash.history') }} — {{ historyKpi?.name }}
        </v-card-title>
        <v-card-text class="px-6">
          <div v-if="historyEntries.length === 0" class="text-body-2 text-medium-emphasis">—</div>
          <template v-else>
            <svg viewBox="0 0 100 40" width="100%" height="80" preserveAspectRatio="none" class="mb-3">
              <polyline
                :points="historyEntries.slice().reverse().map((e, i, arr) =>
                  `${arr.length > 1 ? (i / (arr.length - 1)) * 100 : 50},${40 - (Number(e.value) / historyMax) * 36}`).join(' ')"
                fill="none"
                stroke="#1867C0"
                stroke-width="1.5"
              />
            </svg>
            <v-table density="compact" class="text-caption">
              <thead>
                <tr>
                  <th>{{ t('dash.value') }}</th>
                  <th>{{ t('dash.band') }}</th>
                  <th>{{ t('dash.computedAt') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(e, i) in historyEntries" :key="i">
                  <td>{{ formatNumber(e.value) }}</td>
                  <td>{{ e.thresholdLevel ?? '—' }}</td>
                  <td>{{ formatDateTime(e.computedAt) }}</td>
                </tr>
              </tbody>
            </v-table>
          </template>
        </v-card-text>
        <v-card-actions class="px-6 pb-4">
          <v-spacer />
          <v-btn variant="text" @click="historyOpen = false">{{ t('common.close') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- delete confirm -->
    <v-dialog :model-value="!!deleteTarget" max-width="420" @update:model-value="deleteTarget = null">
      <v-card rounded="lg">
        <v-card-title class="text-h6">{{ t('common.delete') }}</v-card-title>
        <v-card-text>{{ t('dash.deleteConfirm') }} <strong>{{ deleteTarget?.name }}</strong></v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="deleteTarget = null">{{ t('common.cancel') }}</v-btn>
          <v-btn color="error" :loading="deleting" @click="confirmDelete">{{ t('common.delete') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
