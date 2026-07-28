<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { dashboardsApi } from '@/api/dashboards'
import { usePermission } from '@/composables/usePermission'
import { useApiError } from '@/composables/useApiError'
import DynamicDashboard from '@/components/DynamicDashboard.vue'
import WidgetFormDialog from '@/components/WidgetFormDialog.vue'
import type {
  DashboardChartType,
  DashboardDataSource,
  DashboardDetail,
  DashboardRefreshPolicy,
  DashboardSavedFilter,
  DashboardWidget,
  DashboardWidgetType,
  WidgetDataResponse,
} from '@/types/models'

/**
 * The dashboard viewer + builder. View mode: live widget data with the filter
 * engine (date range + saved filters) and refresh-policy polling; a threshold
 * band change raises a live alert. Edit mode (with permission): drag to
 * reorder, resize, add/edit/remove widgets, save layout.
 */
const { t } = useI18n()
const route = useRoute()
const { can } = usePermission()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()

const dashboardId = computed(() => Number(route.params.id))
const detail = ref<DashboardDetail | null>(null)
const widgets = ref<DashboardWidget[]>([])
const widgetData = ref<Record<number, WidgetDataResponse>>({})
const loading = ref(false)
const refreshing = ref(false)
const editMode = ref(false)
const layoutDirty = ref(false)
const savingLayout = ref(false)

// lookups for the widget dialog
const widgetTypes = ref<DashboardWidgetType[]>([])
const chartTypes = ref<DashboardChartType[]>([])
const dataSources = ref<DashboardDataSource[]>([])
const refreshPolicies = ref<DashboardRefreshPolicy[]>([])

// filter engine
const fromFilter = ref('')
const toFilter = ref('')
const savedFilters = ref<DashboardSavedFilter[]>([])
const filterName = ref('')

// live alerts
const alert = ref<string | null>(null)

let timer: ReturnType<typeof setInterval> | undefined

const canEditDashboard = computed(
  () => (detail.value?.dashboard.canEdit ?? false) && can('dashboards:manage-widgets'),
)

async function loadAll(): Promise<void> {
  loading.value = true
  clearError()
  try {
    detail.value = await dashboardsApi.get(dashboardId.value)
    widgets.value = [...detail.value.widgets]
    await refresh()
    scheduleRefresh()
    if (widgetTypes.value.length === 0) {
      ;[widgetTypes.value, chartTypes.value, dataSources.value, refreshPolicies.value, savedFilters.value] =
        await Promise.all([
          dashboardsApi.widgetTypes(),
          dashboardsApi.chartTypes(),
          dashboardsApi.dataSources(),
          dashboardsApi.refreshPolicies(),
          dashboardsApi.savedFilters(),
        ])
    }
  } catch (err) {
    setError(err)
  } finally {
    loading.value = false
  }
}

async function refresh(): Promise<void> {
  refreshing.value = true
  try {
    const results = await dashboardsApi.refresh(dashboardId.value, {
      from: fromFilter.value || undefined,
      to: toFilter.value || undefined,
    })
    const previous = widgetData.value
    const map: Record<number, WidgetDataResponse> = {}
    for (const r of results) map[r.widgetId] = r
    detectThresholdChanges(previous, map)
    widgetData.value = map
  } catch (err) {
    setError(err)
  } finally {
    refreshing.value = false
  }
}

/** Live alert when any widget's threshold band changes between refreshes. */
function detectThresholdChanges(
  previous: Record<number, WidgetDataResponse>,
  next: Record<number, WidgetDataResponse>,
): void {
  for (const r of Object.values(next)) {
    const before = previous[r.widgetId]?.data?.meta?.thresholdLevel as string | undefined
    const after = r.data?.meta?.thresholdLevel as string | undefined
    if (before && after && before !== after) {
      alert.value = t('dash.thresholdChanged', { widget: r.title, from: before, to: after })
    }
  }
}

function scheduleRefresh(): void {
  if (timer) clearInterval(timer)
  const intervals = widgets.value.map((w) => w.refreshIntervalSeconds).filter((s) => s > 0)
  if (intervals.length === 0) return
  timer = setInterval(() => void refresh(), Math.max(15, Math.min(...intervals)) * 1000)
}

// --- builder ----------------------------------------------------------------
const widgetFormOpen = ref(false)
const editingWidget = ref<DashboardWidget | null>(null)

function addWidget(): void {
  editingWidget.value = null
  widgetFormOpen.value = true
}

function editWidget(widget: DashboardWidget): void {
  editingWidget.value = widget
  widgetFormOpen.value = true
}

async function removeWidget(widget: DashboardWidget): Promise<void> {
  clearError()
  try {
    await dashboardsApi.removeWidget(widget.id)
    await loadAll()
  } catch (err) {
    setError(err)
  }
}

function reorder(fromId: number, toId: number): void {
  const fromIndex = widgets.value.findIndex((w) => w.id === fromId)
  const toIndex = widgets.value.findIndex((w) => w.id === toId)
  if (fromIndex < 0 || toIndex < 0) return
  const [moved] = widgets.value.splice(fromIndex, 1)
  widgets.value.splice(toIndex, 0, moved)
  layoutDirty.value = true
}

function resize(widgetId: number, dw: number, dh: number): void {
  const widget = widgets.value.find((w) => w.id === widgetId)
  if (!widget) return
  widget.width = Math.max(1, Math.min(12, widget.width + dw))
  widget.height = Math.max(1, Math.min(8, widget.height + dh))
  layoutDirty.value = true
}

async function saveLayout(): Promise<void> {
  savingLayout.value = true
  clearError()
  try {
    await dashboardsApi.saveLayout(
      dashboardId.value,
      widgets.value.map((w, index) => ({
        widgetId: w.id,
        positionX: 0,
        positionY: index,
        width: w.width,
        height: w.height,
      })),
    )
    layoutDirty.value = false
  } catch (err) {
    setError(err)
  } finally {
    savingLayout.value = false
  }
}

// --- saved filters ----------------------------------------------------------
async function saveCurrentFilter(): Promise<void> {
  if (!filterName.value.trim()) return
  clearError()
  try {
    await dashboardsApi.saveFilter({
      name: filterName.value.trim(),
      filter: { from: fromFilter.value || null, to: toFilter.value || null },
    })
    filterName.value = ''
    savedFilters.value = await dashboardsApi.savedFilters()
  } catch (err) {
    setError(err)
  }
}

function applySavedFilter(filter: DashboardSavedFilter): void {
  fromFilter.value = (filter.filter.from as string | null) ?? ''
  toFilter.value = (filter.filter.to as string | null) ?? ''
}

async function deleteSavedFilter(filter: DashboardSavedFilter): Promise<void> {
  try {
    await dashboardsApi.deleteSavedFilter(filter.id)
    savedFilters.value = await dashboardsApi.savedFilters()
  } catch (err) {
    setError(err)
  }
}

watch([fromFilter, toFilter], () => void refresh())
watch(dashboardId, () => void loadAll())

onMounted(loadAll)
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div>
    <div class="d-flex align-center flex-wrap mb-4 ga-2">
      <v-btn icon="mdi-arrow-left" variant="text" :to="{ name: 'dashboards' }" />
      <div>
        <h1 class="text-h5">{{ detail?.dashboard.name ?? '…' }}</h1>
        <span class="text-caption text-medium-emphasis">{{ detail?.dashboard.description }}</span>
      </div>
      <v-spacer />

      <v-text-field v-model="fromFilter" :label="t('dash.from')" type="date" density="compact" hide-details clearable style="max-width: 155px" />
      <v-text-field v-model="toFilter" :label="t('dash.to')" type="date" density="compact" hide-details clearable style="max-width: 155px" />

      <v-menu :close-on-content-click="false">
        <template #activator="{ props: menuProps }">
          <v-btn variant="tonal" prepend-icon="mdi-filter" v-bind="menuProps">
            {{ t('dash.savedFilters') }}
          </v-btn>
        </template>
        <v-card min-width="280" class="pa-3">
          <div class="d-flex ga-2 mb-2">
            <v-text-field v-model="filterName" :label="t('dash.filterName')" density="compact" hide-details />
            <v-btn size="small" color="primary" :disabled="!filterName.trim()" @click="saveCurrentFilter">
              {{ t('common.save') }}
            </v-btn>
          </div>
          <v-list density="compact">
            <v-list-item
              v-for="filter in savedFilters"
              :key="filter.id"
              :title="filter.name"
              @click="applySavedFilter(filter)"
            >
              <template #append>
                <v-btn icon="mdi-close" size="x-small" variant="text" @click.stop="deleteSavedFilter(filter)" />
              </template>
            </v-list-item>
          </v-list>
        </v-card>
      </v-menu>

      <v-btn variant="tonal" prepend-icon="mdi-refresh" :loading="refreshing" @click="refresh">
        {{ t('common.refresh') }}
      </v-btn>

      <template v-if="canEditDashboard">
        <v-btn
          :variant="editMode ? 'flat' : 'tonal'"
          :color="editMode ? 'primary' : undefined"
          prepend-icon="mdi-pencil-ruler"
          @click="editMode = !editMode"
        >
          {{ editMode ? t('dash.doneEditing') : t('dash.editLayout') }}
        </v-btn>
        <template v-if="editMode">
          <v-btn color="primary" variant="tonal" prepend-icon="mdi-plus" @click="addWidget">
            {{ t('dash.addWidget') }}
          </v-btn>
          <v-btn color="success" :disabled="!layoutDirty" :loading="savingLayout" prepend-icon="mdi-content-save" @click="saveLayout">
            {{ t('dash.saveLayout') }}
          </v-btn>
        </template>
      </template>
    </div>

    <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
      {{ errorMessage }}
    </v-alert>

    <v-progress-linear v-if="loading" indeterminate class="mb-4" />

    <DynamicDashboard
      v-if="detail"
      :widgets="widgets"
      :data="widgetData"
      :refreshing="refreshing"
      :edit-mode="editMode"
      @reorder="reorder"
      @resize="resize"
      @edit="editWidget"
      @remove="removeWidget"
    />

    <v-alert v-if="detail && widgets.length === 0 && !loading" type="info" variant="tonal" density="compact" class="mt-2">
      {{ t('dash.noWidgets') }}
    </v-alert>

    <WidgetFormDialog
      v-model="widgetFormOpen"
      :dashboard-id="dashboardId"
      :widget="editingWidget"
      :widget-types="widgetTypes"
      :chart-types="chartTypes"
      :data-sources="dataSources"
      :refresh-policies="refreshPolicies"
      @saved="loadAll"
    />

    <v-snackbar :model-value="!!alert" color="warning" timeout="6000" @update:model-value="alert = null">
      {{ alert }}
    </v-snackbar>
  </div>
</template>
