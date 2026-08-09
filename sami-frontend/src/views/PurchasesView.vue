<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import { purchasesApi, purchasingConfigApi, type PurchaseListParams } from '@/api/purchases'
import AppEmptyState from '@/components/AppEmptyState.vue'
import AppErrorState from '@/components/AppErrorState.vue'
import AppLoadingState from '@/components/AppLoadingState.vue'
import AppPageHeader from '@/components/AppPageHeader.vue'
import PurchaseDetailDialog from '@/components/PurchaseDetailDialog.vue'
import PurchaseFormDialog from '@/components/PurchaseFormDialog.vue'
import PurchasingConfigPanel from '@/components/purchasing/PurchasingConfigPanel.vue'
import PurchasingReportsPanel from '@/components/purchasing/PurchasingReportsPanel.vue'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import { useNotifications } from '@/composables/useNotifications'
import { usePermission } from '@/composables/usePermission'
import { useServerLabel } from '@/composables/useServerLabel'
import type {
  PurCancelReason,
  PurIdentifierType,
  PurStatus,
  PurType,
  PurWarehouse,
  PurchaseDashboard,
  PurchaseDetail,
  PurchaseRow,
} from '@/types/models'

interface SortItem { key: string; order?: 'asc' | 'desc' }
interface TableOptions { page: number; itemsPerPage: number; sortBy: SortItem[] }

const { t } = useI18n()
const { smAndDown } = useDisplay()
const { can } = usePermission()
const { text: srvLabel } = useServerLabel()
const { formatDate, formatNumber } = useFormat()
const notifications = useNotifications()
const error = useApiError()
const tab = ref('orders')
const loading = ref(false)
const dashboardLoading = ref(false)
const dashboard = ref<PurchaseDashboard | null>(null)
const statuses = ref<PurStatus[]>([])
const types = ref<PurType[]>([])
const warehouses = ref<PurWarehouse[]>([])
const cancelReasons = ref<PurCancelReason[]>([])
const identifierTypes = ref<PurIdentifierType[]>([])
const items = ref<PurchaseRow[]>([])
const totalItems = ref(0)
const lastOptions = ref<TableOptions>({ page: 1, itemsPerPage: 10, sortBy: [] })
const importInput = ref<HTMLInputElement | null>(null)
const importing = ref(false)
const exporting = ref(false)

const tabs = computed(() => [
  { value: 'orders', label: t('purchases.tabs.orders'), icon: 'mdi-cart-outline', allowed: true },
  { value: 'reports', label: t('purchases.tabs.reports'), icon: 'mdi-chart-box-outline', allowed: can('purchasing:report') },
  { value: 'config', label: t('purchases.tabs.config'), icon: 'mdi-tune-variant', allowed: can('purchasing:manage-config') || can('purchasing:edit') },
].filter((item) => item.allowed))

const metrics = computed(() => dashboard.value ? [
  { key: 'totalPurchases', value: dashboard.value.totalPurchases, icon: 'mdi-file-document-multiple-outline', color: 'primary' },
  { key: 'pendingApproval', value: dashboard.value.pendingApproval, icon: 'mdi-account-clock-outline', color: 'warning' },
  { key: 'openReceipts', value: dashboard.value.openReceipts, icon: 'mdi-package-down', color: 'info' },
  { key: 'overdueLines', value: dashboard.value.overdueLines, icon: 'mdi-calendar-alert', color: 'error' },
] : [])

const headers = computed(() => [
  { title: t('purchases.headers.number'), key: 'purchaseNumber' },
  { title: t('purchases.headers.supplier'), key: 'supplier', sortable: false },
  { title: t('purchases.headers.type'), key: 'type', sortable: false },
  { title: t('purchases.headers.status'), key: 'status', sortable: false },
  { title: t('purchases.headers.total'), key: 'totalAmount', align: 'end' as const },
  { title: t('purchases.headers.created'), key: 'createdAt' },
  { title: '', key: 'actions', sortable: false, align: 'end' as const },
])

const searchFilter = ref('')
const statusFilter = ref<number | null>(null)
const typeFilter = ref<number | null>(null)
const warehouseFilter = ref<number | null>(null)
const fromFilter = ref('')
const toFilter = ref('')
const statusItems = computed(() => [{ title: t('purchases.filters.allStatuses'), value: null }, ...statuses.value.map((item) => ({ title: srvLabel(item.name), value: item.id }))])
const typeItems = computed(() => [{ title: t('purchases.filters.allTypes'), value: null }, ...types.value.map((item) => ({ title: srvLabel(item.name), value: item.id }))])
const warehouseItems = computed(() => [{ title: t('purchases.filters.allWarehouses'), value: null }, ...warehouses.value.map((item) => ({ title: srvLabel(item.name), value: item.id }))])
const pageCount = computed(() => Math.max(1, Math.ceil(totalItems.value / lastOptions.value.itemsPerPage)))

function filterParams(): PurchaseListParams {
  const sort = lastOptions.value.sortBy[0]
  return {
    page: lastOptions.value.page - 1,
    size: lastOptions.value.itemsPerPage,
    sort: sort ? `${sort.key},${sort.order ?? 'asc'}` : undefined,
    search: searchFilter.value || undefined,
    statusId: statusFilter.value ?? undefined,
    typeId: typeFilter.value ?? undefined,
    warehouseId: warehouseFilter.value ?? undefined,
    createdFrom: fromFilter.value || undefined,
    createdTo: toFilter.value || undefined,
  }
}

async function loadReferenceData() {
  ;[statuses.value, types.value, warehouses.value, cancelReasons.value, identifierTypes.value] = await Promise.all([
    purchasingConfigApi.statuses(), purchasingConfigApi.types(), purchasingConfigApi.warehouses(),
    purchasingConfigApi.cancelReasons(), purchasingConfigApi.identifierTypes(),
  ])
}

async function loadDashboard() {
  dashboardLoading.value = true
  try { dashboard.value = await purchasesApi.dashboard() }
  catch (reason) { error.set(reason) }
  finally { dashboardLoading.value = false }
}

async function loadItems(options: TableOptions = lastOptions.value) {
  lastOptions.value = options
  loading.value = true
  error.clear()
  try {
    const result = await purchasesApi.list(filterParams())
    items.value = result.content
    totalItems.value = result.totalElements
  } catch (reason) {
    error.set(reason)
  } finally {
    loading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadItems(), loadDashboard()])
}

const reload = useDebounceFn(() => {
  void loadItems({ ...lastOptions.value, page: 1 })
}, 300)
watch([searchFilter, statusFilter, typeFilter, warehouseFilter, fromFilter, toFilter], reload)

const formOpen = ref(false)
const editingDetail = ref<PurchaseDetail | null>(null)
const detailOpen = ref(false)
const detailId = ref<number | null>(null)
const deleteTarget = ref<PurchaseRow | null>(null)
const deleting = ref(false)

function openCreate() { editingDetail.value = null; formOpen.value = true }
function openDetailId(id: number) { detailId.value = id; detailOpen.value = true }
function openDetail(row: PurchaseRow) { openDetailId(row.id) }

async function openEdit(row: PurchaseRow) {
  error.clear()
  try { editingDetail.value = await purchasesApi.get(row.id); formOpen.value = true }
  catch (reason) { error.set(reason) }
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    await purchasesApi.deleteDraft(deleteTarget.value.id)
    deleteTarget.value = null
    notifications.success(t('purchases.notifications.deleted'))
    await refreshAll()
  } catch (reason) { error.set(reason) }
  finally { deleting.value = false }
}

async function exportCsv() {
  exporting.value = true
  error.clear()
  try {
    const { page: _page, size: _size, sort: _sort, ...filters } = filterParams()
    await purchasesApi.exportCsv(filters)
    notifications.success(t('purchases.notifications.exported'))
  } catch (reason) { error.set(reason) }
  finally { exporting.value = false }
}

async function importCsv(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  importing.value = true
  error.clear()
  try {
    const result = await purchasesApi.importCsv(file)
    notifications.success(t('purchases.notifications.imported', { created: result.created, skipped: result.skipped }))
    await refreshAll()
  } catch (reason) { error.set(reason) }
  finally { importing.value = false }
}

function statusColor(status: PurStatus) {
  if (status.isCompletedState) return 'success'
  if (status.isCancelledState || status.isRejectedState) return 'error'
  if (status.isPendingState) return 'warning'
  if (status.isDraftState) return 'default'
  return 'info'
}

onMounted(async () => {
  try { await Promise.all([loadReferenceData(), loadDashboard(), loadItems()]) }
  catch (reason) { error.set(reason) }
})
</script>

<template>
  <div class="purchasing-page">
    <AppPageHeader
      icon="mdi-cart-arrow-down"
      :eyebrow="t('purchases.eyebrow')"
      :title="t('purchases.title')"
      :subtitle="t('purchases.subtitle')"
    >
      <template #actions>
        <input ref="importInput" type="file" accept=".csv,text/csv" class="d-none" @change="importCsv" />
        <v-btn v-if="can('purchasing:import')" variant="tonal" prepend-icon="mdi-upload" :loading="importing" @click="importInput?.click()">
          {{ t('purchases.import') }}
        </v-btn>
        <v-btn v-if="can('purchasing:export')" variant="tonal" prepend-icon="mdi-download" :loading="exporting" @click="exportCsv">
          {{ t('purchases.export') }}
        </v-btn>
        <v-btn v-if="can('purchasing:create')" color="primary" prepend-icon="mdi-plus" @click="openCreate">
          {{ t('purchases.newPurchase') }}
        </v-btn>
      </template>
    </AppPageHeader>

    <AppErrorState v-if="error.message.value" class="mb-4" :title="error.message.value" :retry-label="t('common.retry')" @retry="refreshAll" />

    <AppLoadingState v-if="dashboardLoading && !dashboard" variant="cards" :rows="4" :label="t('common.loading')" />
    <v-row v-else-if="dashboard" class="mb-2">
      <v-col v-for="metric in metrics" :key="metric.key" cols="6" lg="3">
        <v-card rounded="xl" class="purchasing-metric h-100">
          <v-card-text>
            <div class="d-flex align-start ga-3">
              <v-avatar :color="metric.color" variant="tonal" rounded="lg"><v-icon :icon="metric.icon" /></v-avatar>
              <div class="min-width-0">
                <div class="text-caption text-medium-emphasis">{{ t(`purchases.metrics.${metric.key}`) }}</div>
                <div class="text-h6 text-sm-h5 font-weight-bold mt-1">{{ formatNumber(metric.value) }}</div>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-card rounded="xl" class="purchasing-tabs mb-5">
      <v-tabs v-model="tab" color="primary" show-arrows density="comfortable">
        <v-tab v-for="item in tabs" :key="item.value" :value="item.value" :prepend-icon="item.icon">{{ item.label }}</v-tab>
      </v-tabs>
    </v-card>

    <template v-if="tab === 'orders'">
      <v-card rounded="xl" class="app-data-surface mb-4">
        <v-card-text>
          <v-row dense align="center">
            <v-col cols="12" md="4"><v-text-field v-model="searchFilter" :label="t('purchases.filters.searchPlaceholder')" prepend-inner-icon="mdi-magnify" clearable hide-details /></v-col>
            <v-col cols="6" md="2"><v-select v-model="statusFilter" :label="t('purchases.headers.status')" :items="statusItems" hide-details /></v-col>
            <v-col cols="6" md="2"><v-select v-model="typeFilter" :label="t('purchases.headers.type')" :items="typeItems" hide-details /></v-col>
            <v-col cols="12" sm="6" md="2"><v-select v-model="warehouseFilter" :label="t('purchases.filters.warehouse')" :items="warehouseItems" hide-details /></v-col>
            <v-col cols="6" sm="3" md="1"><AppPersianDatePicker v-model="fromFilter" :label="t('purchases.filters.from')" hide-details clearable /></v-col>
            <v-col cols="6" sm="3" md="1"><AppPersianDatePicker v-model="toFilter" :label="t('purchases.filters.to')" hide-details clearable /></v-col>
          </v-row>
        </v-card-text>
      </v-card>

      <AppLoadingState v-if="loading && !items.length" variant="table" :rows="6" :label="t('common.loading')" />
      <template v-else>
        <v-card v-if="!smAndDown" rounded="xl" class="app-data-surface">
          <v-data-table-server
            :headers="headers" :items="items" :items-length="totalItems" :loading="loading"
            :items-per-page="lastOptions.itemsPerPage" item-value="id" @update:options="loadItems"
          >
            <template #[`item.purchaseNumber`]="{ item }"><a class="text-primary cursor-pointer font-weight-medium" @click="openDetail(item)">{{ item.purchaseNumber }}</a></template>
            <template #[`item.supplier`]="{ item }">{{ item.supplierName }}<div class="text-caption text-medium-emphasis">{{ item.supplierCode }}</div></template>
            <template #[`item.type`]="{ item }"><v-chip size="small" variant="tonal">{{ srvLabel(item.type.name) }}</v-chip></template>
            <template #[`item.status`]="{ item }"><v-chip :color="statusColor(item.status)" size="small" variant="tonal">{{ srvLabel(item.status.name) }}</v-chip></template>
            <template #[`item.totalAmount`]="{ item }">{{ formatNumber(item.totalAmount) }}</template>
            <template #[`item.createdAt`]="{ item }">{{ formatDate(item.createdAt) }}</template>
            <template #[`item.actions`]="{ item }">
              <v-btn :aria-label="t('purchases.actions.view')" icon="mdi-eye" size="small" variant="text" @click="openDetail(item)" />
              <v-btn v-if="item.status.allowsEditing && can('purchasing:edit')" :aria-label="t('common.edit')" icon="mdi-pencil" size="small" variant="text" @click="openEdit(item)" />
              <v-btn v-if="item.status.isDraftState && can('purchasing:delete')" :aria-label="t('common.delete')" icon="mdi-delete" size="small" variant="text" color="error" @click="deleteTarget = item" />
            </template>
          </v-data-table-server>
        </v-card>

        <div v-else-if="items.length" class="d-grid ga-3">
          <v-card v-for="item in items" :key="item.id" rounded="xl" class="app-data-surface" @click="openDetail(item)">
            <v-card-text>
              <div class="d-flex align-start ga-3">
                <v-avatar color="primary" variant="tonal" rounded="lg"><v-icon icon="mdi-file-document-outline" /></v-avatar>
                <div class="flex-grow-1 min-width-0">
                  <div class="d-flex align-center flex-wrap ga-2"><strong>{{ item.purchaseNumber }}</strong><v-chip :color="statusColor(item.status)" size="x-small" variant="tonal">{{ srvLabel(item.status.name) }}</v-chip></div>
                  <div class="text-body-2 mt-1 text-truncate">{{ item.supplierName }}</div>
                  <div class="text-caption text-medium-emphasis">{{ srvLabel(item.type.name) }} · {{ formatDate(item.createdAt) }}</div>
                </div>
                <strong>{{ formatNumber(item.totalAmount) }}</strong>
              </div>
              <div class="d-flex justify-end ga-1 mt-2" @click.stop>
                <v-btn v-if="item.status.allowsEditing && can('purchasing:edit')" icon="mdi-pencil" size="small" variant="text" @click="openEdit(item)" />
                <v-btn v-if="item.status.isDraftState && can('purchasing:delete')" icon="mdi-delete" size="small" variant="text" color="error" @click="deleteTarget = item" />
              </div>
            </v-card-text>
          </v-card>
          <v-pagination v-if="pageCount > 1" v-model="lastOptions.page" :length="pageCount" :total-visible="5" @update:model-value="() => loadItems()" />
        </div>
        <AppEmptyState v-else icon="mdi-cart-outline" :title="t('purchases.empty.title')" :description="t('purchases.empty.description')" />
      </template>
    </template>

    <PurchasingReportsPanel v-else-if="tab === 'reports' && can('purchasing:report')" @open="openDetailId" />
    <PurchasingConfigPanel v-else-if="tab === 'config' && (can('purchasing:manage-config') || can('purchasing:edit'))" @changed="loadReferenceData" />

    <PurchaseFormDialog v-model="formOpen" :purchase="editingDetail" :types="types" :warehouses="warehouses" @saved="refreshAll" />
    <PurchaseDetailDialog v-model="detailOpen" :purchase-id="detailId" :cancel-reasons="cancelReasons" :identifier-types="identifierTypes" @changed="refreshAll" />

    <v-dialog :model-value="deleteTarget !== null" max-width="420" @update:model-value="deleteTarget = null">
      <v-card rounded="lg">
        <v-card-title>{{ t('purchases.delete.title') }}</v-card-title>
        <v-card-text>{{ t('purchases.delete.body') }} <strong>{{ deleteTarget?.purchaseNumber }}</strong></v-card-text>
        <v-card-actions><v-spacer /><v-btn variant="text" @click="deleteTarget = null">{{ t('common.cancel') }}</v-btn><v-btn color="error" :loading="deleting" @click="confirmDelete">{{ t('common.delete') }}</v-btn></v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.purchasing-page { min-width: 0; }
.purchasing-metric, .purchasing-tabs { border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity)); }
.purchasing-tabs { overflow: hidden; }
.d-grid { display: grid; }
@media (max-width: 599px) {
  .purchasing-metric :deep(.v-card-text) { padding: 14px; }
  .purchasing-metric :deep(.v-avatar) { width: 36px !important; height: 36px !important; }
  .purchasing-tabs :deep(.v-tab) { min-width: 112px; }
  :deep(.v-btn) { min-height: 44px; }
}
</style>
