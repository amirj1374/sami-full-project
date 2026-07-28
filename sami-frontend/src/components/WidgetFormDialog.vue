<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { dashboardsApi, kpisApi } from '@/api/dashboards'
import { useApiError } from '@/composables/useApiError'
import type {
  DashboardChartType,
  DashboardDataSource,
  DashboardRefreshPolicy,
  DashboardWidget,
  DashboardWidgetPayload,
  DashboardWidgetType,
  Kpi,
} from '@/types/models'

/**
 * Add/edit widget dialog. Fields adapt to the selected widget type (chart type
 * for chart-capable types, KPI binding, data source + metric, refresh policy,
 * permission gate, free-form JSON config) — one dialog for every widget type.
 */
const props = defineProps<{
  modelValue: boolean
  dashboardId: number
  widget: DashboardWidget | null
  widgetTypes: DashboardWidgetType[]
  chartTypes: DashboardChartType[]
  dataSources: DashboardDataSource[]
  refreshPolicies: DashboardRefreshPolicy[]
}>()

const emit = defineEmits<{ 'update:modelValue': [boolean]; saved: [] }>()
const { t } = useI18n()
const { message: formError, set: setFormError, clear: clearFormError } = useApiError()

const open = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})
const isEdit = computed(() => props.widget !== null)
const saving = ref(false)

const code = ref('')
const title = ref('')
const description = ref('')
const widgetTypeId = ref<number | null>(null)
const chartTypeId = ref<number | null>(null)
const kpiId = ref<number | null>(null)
const dataSourceId = ref<number | null>(null)
const refreshPolicyId = ref<number | null>(null)
const metric = ref('')
const requiredPermission = ref('')
const configJson = ref('{}')
const width = ref(3)
const height = ref(2)

const kpis = ref<Kpi[]>([])

const selectedType = computed(() =>
  props.widgetTypes.find((wt) => wt.id === widgetTypeId.value) ?? null,
)

watch(open, async (isOpen) => {
  if (!isOpen) return
  clearFormError()
  const w = props.widget
  code.value = w?.code ?? ''
  title.value = w?.title ?? ''
  description.value = w?.description ?? ''
  widgetTypeId.value = w?.widgetTypeId ?? props.widgetTypes[0]?.id ?? null
  chartTypeId.value = w?.chartTypeId ?? null
  kpiId.value = w?.kpiId ?? null
  dataSourceId.value = w?.dataSourceId ?? null
  refreshPolicyId.value = w?.refreshPolicyId ?? null
  requiredPermission.value = w?.requiredPermission ?? ''
  const config = { ...(w?.config ?? {}) }
  metric.value = config.metric !== undefined ? String(config.metric) : ''
  delete config.metric
  configJson.value = JSON.stringify(config, null, 2)
  width.value = w?.width ?? 3
  height.value = w?.height ?? 2
  if (kpis.value.length === 0) {
    try {
      kpis.value = (await kpisApi.list({ size: 200 })).content
    } catch {
      /* KPI list is optional here */
    }
  }
})

async function save(): Promise<void> {
  clearFormError()
  let config: Record<string, unknown>
  try {
    config = configJson.value.trim() ? JSON.parse(configJson.value) : {}
  } catch {
    setFormError(new Error(t('dash.invalidConfigJson')))
    return
  }
  if (metric.value.trim()) config.metric = metric.value.trim()

  const payload: DashboardWidgetPayload = {
    code: code.value.trim(),
    title: title.value.trim(),
    description: description.value.trim() || undefined,
    widgetTypeId: widgetTypeId.value as number,
    chartTypeId: chartTypeId.value ?? undefined,
    kpiId: kpiId.value ?? undefined,
    dataSourceId: dataSourceId.value ?? undefined,
    refreshPolicyId: refreshPolicyId.value ?? undefined,
    positionX: props.widget?.positionX ?? 0,
    positionY: props.widget?.positionY ?? 999,
    width: width.value,
    height: height.value,
    requiredPermission: requiredPermission.value.trim() || undefined,
    config,
    expectedVersion: props.widget?.version,
  }

  saving.value = true
  try {
    if (props.widget) {
      await dashboardsApi.updateWidget(props.widget.id, payload)
    } else {
      await dashboardsApi.addWidget(props.dashboardId, payload)
    }
    open.value = false
    emit('saved')
  } catch (err) {
    setFormError(err)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <v-dialog v-model="open" max-width="640">
    <v-card rounded="lg">
      <v-card-title class="text-h6 pt-4 px-6">
        {{ isEdit ? t('dash.editWidget') : t('dash.addWidget') }}
      </v-card-title>

      <v-card-text class="px-6" style="max-height: 62vh; overflow-y: auto">
        <v-alert v-if="formError" type="error" variant="tonal" density="compact" class="mb-4">
          {{ formError }}
        </v-alert>

        <v-row dense>
          <v-col cols="12" sm="6">
            <v-text-field v-model="code" :label="t('common.code') + ' *'" :disabled="isEdit" />
          </v-col>
          <v-col cols="12" sm="6">
            <v-text-field v-model="title" :label="t('common.name') + ' *'" />
          </v-col>
          <v-col cols="12" sm="6">
            <v-select
              v-model="widgetTypeId"
              :items="widgetTypes.filter((wt) => wt.active)"
              item-title="name"
              item-value="id"
              :label="t('dash.widgetType') + ' *'"
            />
          </v-col>
          <v-col v-if="selectedType?.chartCapable" cols="12" sm="6">
            <v-select
              v-model="chartTypeId"
              :items="chartTypes.filter((ct) => ct.active)"
              item-title="name"
              item-value="id"
              :label="t('dash.chartType')"
              clearable
            />
          </v-col>
          <v-col cols="12" sm="6">
            <v-select
              v-model="kpiId"
              :items="kpis"
              item-title="name"
              item-value="id"
              :label="t('dash.kpiBinding')"
              clearable
              :hint="t('dash.kpiBindingHint')"
              persistent-hint
            />
          </v-col>
          <v-col cols="12" sm="6">
            <v-select
              v-model="dataSourceId"
              :items="dataSources.filter((ds) => ds.active)"
              item-title="name"
              item-value="id"
              :label="t('dash.dataSource')"
              clearable
            >
              <template #item="{ props: itemProps, item }">
                <v-list-item v-bind="itemProps">
                  <template #append>
                    <v-chip size="x-small" :color="item.raw.available ? 'success' : 'warning'" variant="tonal">
                      {{ item.raw.available ? t('dash.available') : t('dash.unavailable') }}
                    </v-chip>
                  </template>
                </v-list-item>
              </template>
            </v-select>
          </v-col>
          <v-col cols="12" sm="6">
            <v-text-field v-model="metric" :label="t('dash.metric')" :hint="t('dash.metricHint')" persistent-hint />
          </v-col>
          <v-col cols="12" sm="6">
            <v-select
              v-model="refreshPolicyId"
              :items="refreshPolicies"
              item-title="name"
              item-value="id"
              :label="t('dash.refreshPolicy')"
              clearable
            />
          </v-col>
          <v-col cols="6" sm="3">
            <v-text-field v-model.number="width" type="number" min="1" max="12" :label="t('dash.width')" />
          </v-col>
          <v-col cols="6" sm="3">
            <v-text-field v-model.number="height" type="number" min="1" max="8" :label="t('dash.height')" />
          </v-col>
          <v-col cols="12" sm="6">
            <v-text-field
              v-model="requiredPermission"
              :label="t('dash.requiredPermission')"
              :hint="t('dash.requiredPermissionHint')"
              persistent-hint
            />
          </v-col>
          <v-col cols="12">
            <v-textarea
              v-model="configJson"
              :label="t('dash.configJson')"
              rows="4"
              class="font-mono"
            />
          </v-col>
        </v-row>
      </v-card-text>

      <v-card-actions class="px-6 pb-4">
        <v-spacer />
        <v-btn variant="text" @click="open = false">{{ t('common.cancel') }}</v-btn>
        <v-btn color="primary" :loading="saving" @click="save">{{ t('common.save') }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
