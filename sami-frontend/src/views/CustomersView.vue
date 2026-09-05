<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import { useDebounceFn } from '@vueuse/core'
import { customersApi, type CustomerListParams } from '@/api/customers'
import { crmConfigApi } from '@/api/crmConfig'
import { usePermission } from '@/composables/usePermission'
import { useServerLabel } from '@/composables/useServerLabel'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import CustomerFormDialog from '@/components/CustomerFormDialog.vue'
import CustomerProfileDialog from '@/components/CustomerProfileDialog.vue'
import CustomerMergeDialog from '@/components/CustomerMergeDialog.vue'
import AppEmptyState from '@/components/AppEmptyState.vue'
import AppLoadingState from '@/components/AppLoadingState.vue'
import AppPageHeader from '@/components/AppPageHeader.vue'
import CrmConfigPanel from '@/components/crm/CrmConfigPanel.vue'
import CrmReportsPanel from '@/components/crm/CrmReportsPanel.vue'
import CrmSegmentsPanel from '@/components/crm/CrmSegmentsPanel.vue'
import { useNotifications } from '@/composables/useNotifications'
import type {
  BlacklistReason,
  Customer,
  CustomerDetail,
  CustomerImportResult,
  CrmStats,
  CustomerSource,
  CustomerStatus,
  CustomerTag,
  CustomerType,
  PreferenceDefinition,
} from '@/types/models'

const { t } = useI18n()
const { smAndDown } = useDisplay()
const { formatDate } = useFormat()
const { can } = usePermission()
const { text: srvLabel } = useServerLabel()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()
const notifications = useNotifications()
const tab = ref('customers')
const stats = ref<CrmStats | null>(null)
const statsLoading = ref(false)

const tabs = computed(() => [
  { value: 'customers', label: t('customers.tabs.customers'), icon: 'mdi-account-group-outline', allowed: true },
  { value: 'reports', label: t('customers.tabs.reports'), icon: 'mdi-chart-box-outline', allowed: true },
  { value: 'segments', label: t('customers.tabs.segments'), icon: 'mdi-account-filter-outline', allowed: true },
  { value: 'config', label: t('customers.tabs.config'), icon: 'mdi-tune-variant', allowed: can('customers:manage-config') || can('customers:edit') },
].filter((item) => item.allowed))

const metrics = computed(() => stats.value ? [
  { key: 'total', value: stats.value.total, icon: 'mdi-account-group-outline', color: 'primary' },
  { key: 'newLast30Days', value: stats.value.newLast30Days, icon: 'mdi-account-plus-outline', color: 'success' },
  { key: 'statuses', value: stats.value.byStatus.length, icon: 'mdi-state-machine', color: 'info' },
  { key: 'segments', value: stats.value.byType.length, icon: 'mdi-shape-outline', color: 'secondary' },
] : [])

// --- Reference data ---------------------------------------------------------
const types = ref<CustomerType[]>([])
const statuses = ref<CustomerStatus[]>([])
const sources = ref<CustomerSource[]>([])
const tags = ref<CustomerTag[]>([])
const preferenceDefs = ref<PreferenceDefinition[]>([])
const blacklistReasons = ref<BlacklistReason[]>([])

async function loadReferenceData() {
  try {
    ;[types.value, statuses.value, sources.value, tags.value, preferenceDefs.value, blacklistReasons.value] =
      await Promise.all([
        crmConfigApi.types(),
        crmConfigApi.statuses(),
        crmConfigApi.sources(),
        crmConfigApi.tags(),
        crmConfigApi.preferenceDefinitions(true),
        crmConfigApi.blacklistReasons(),
      ])
  } catch (err) {
    setError(err)
  }
}

async function loadStats() {
  statsLoading.value = true
  try {
    stats.value = await customersApi.stats()
  } catch (err) {
    setError(err)
  } finally {
    statsLoading.value = false
  }
}

onMounted(() => {
  void Promise.all([loadReferenceData(), loadStats(), loadItems(lastOptions.value)])
})

// --- Table state ------------------------------------------------------------
interface SortItem {
  key: string
  order?: 'asc' | 'desc'
}
interface TableOptions {
  page: number
  itemsPerPage: number
  sortBy: SortItem[]
}

const items = ref<Customer[]>([])
const totalItems = ref(0)
const loading = ref(false)
const lastOptions = ref<TableOptions>({ page: 1, itemsPerPage: 10, sortBy: [] })
const pageCount = computed(() => Math.max(1, Math.ceil(totalItems.value / lastOptions.value.itemsPerPage)))

const headers = computed(() => [
  { title: t('customers.headers.code'), key: 'customerCode' },
  { title: t('customers.headers.name'), key: 'displayName' },
  { title: t('customers.headers.type'), key: 'type', sortable: false },
  { title: t('customers.headers.status'), key: 'status', sortable: false },
  { title: t('customers.headers.tags'), key: 'tags', sortable: false },
  { title: t('customers.headers.created'), key: 'createdAt' },
  { title: '', key: 'actions', sortable: false, align: 'end' as const },
])

// --- Filters ----------------------------------------------------------------
const searchFilter = ref('')
const statusFilter = ref<number | null>(null)
const typeFilter = ref<number | null>(null)
const sourceFilter = ref<number | null>(null)
const tagFilter = ref<number[]>([])
const cityFilter = ref('')
const includeHidden = ref(false)

function currentFilter() {
  return {
    search: searchFilter.value || undefined,
    statusId: statusFilter.value ?? undefined,
    typeId: typeFilter.value ?? undefined,
    sourceId: sourceFilter.value ?? undefined,
    tagIds: tagFilter.value.length ? tagFilter.value : undefined,
    city: cityFilter.value || undefined,
    includeHidden: includeHidden.value || undefined,
  }
}

async function loadItems(options: TableOptions) {
  lastOptions.value = options
  loading.value = true
  clearError()

  const sort = options.sortBy[0]
  const params: CustomerListParams = {
    page: options.page - 1,
    size: options.itemsPerPage,
    sort: sort ? `${sort.key},${sort.order ?? 'asc'}` : undefined,
    ...currentFilter(),
  }

  try {
    const pageResult = await customersApi.list(params)
    items.value = pageResult.content
    totalItems.value = pageResult.totalElements
  } catch (err) {
    setError(err)
  } finally {
    loading.value = false
  }
}

function refresh() {
  void loadItems(lastOptions.value)
  void loadStats()
}

const reload = useDebounceFn(() => {
  void loadItems({ ...lastOptions.value, page: 1 })
}, 300)
watch([searchFilter, statusFilter, typeFilter, sourceFilter, tagFilter, cityFilter, includeHidden], reload)

const statusItems = computed(() => [
  { title: t('customers.filters.allStatuses'), value: null as number | null },
  ...statuses.value.map((s) => ({ title: s.name, value: s.id as number | null })),
])
const typeItems = computed(() => [
  { title: t('customers.filters.allTypes'), value: null as number | null },
  ...types.value.map((ct) => ({ title: ct.name, value: ct.id as number | null })),
])
const sourceItems = computed(() => [
  { title: t('customers.filters.allSources'), value: null as number | null },
  ...sources.value.map((s) => ({ title: s.name, value: s.id as number | null })),
])

// --- Dialogs ----------------------------------------------------------------
const formOpen = ref(false)
const editingDetail = ref<CustomerDetail | null>(null)
const profileOpen = ref(false)
const profileCustomer = ref<Customer | null>(null)
const mergeOpen = ref(false)
const mergeSource = ref<Customer | null>(null)

function openCreate() {
  editingDetail.value = null
  formOpen.value = true
}

async function openEdit(customer: Customer) {
  clearError()
  try {
    editingDetail.value = await customersApi.get(customer.id)
    formOpen.value = true
  } catch (err) {
    setError(err)
  }
}

function openProfile(customer: Customer) {
  profileCustomer.value = customer
  profileOpen.value = true
}

function openMerge(customer: Customer) {
  mergeSource.value = customer
  mergeOpen.value = true
}

// --- Row actions / confirmations --------------------------------------------
const actionBusy = ref<number | null>(null)
const deleteTarget = ref<Customer | null>(null)
const purgeTarget = ref<Customer | null>(null)
const confirming = ref(false)

async function rowAction(customer: Customer, action: () => Promise<unknown>) {
  actionBusy.value = customer.id
  clearError()
  try {
    await action()
    notifications.success(t('customers.notifications.updated'))
    refresh()
  } catch (err) {
    setError(err)
  } finally {
    actionBusy.value = null
  }
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  confirming.value = true
  try {
    await customersApi.softDelete(deleteTarget.value.id)
    deleteTarget.value = null
    refresh()
    notifications.success(t('customers.notifications.deleted'))
  } catch (err) {
    setError(err)
  } finally {
    confirming.value = false
  }
}

async function confirmPurge() {
  if (!purgeTarget.value) return
  confirming.value = true
  try {
    await customersApi.purge(purgeTarget.value.id)
    purgeTarget.value = null
    refresh()
    notifications.success(t('customers.notifications.purged'))
  } catch (err) {
    setError(err)
  } finally {
    confirming.value = false
  }
}

// --- Blacklist ----------------------------------------------------------------
const blacklistTarget = ref<Customer | null>(null)
const blacklistReasonId = ref<number | null>(null)
const blacklistNote = ref('')
const blacklistBusy = ref(false)

function openBlacklist(customer: Customer) {
  blacklistReasonId.value = null
  blacklistNote.value = ''
  blacklistTarget.value = customer
}

async function confirmBlacklist() {
  if (!blacklistTarget.value || blacklistReasonId.value === null) return
  blacklistBusy.value = true
  clearError()
  try {
    await customersApi.blacklist(blacklistTarget.value.id, {
      reasonId: blacklistReasonId.value,
      note: blacklistNote.value.trim() || undefined,
    })
    blacklistTarget.value = null
    refresh()
    notifications.success(t('customers.notifications.blacklisted'))
  } catch (err) {
    setError(err)
  } finally {
    blacklistBusy.value = false
  }
}

// --- Import -------------------------------------------------------------------
const importInput = ref<HTMLInputElement | null>(null)
const importing = ref(false)
const importResult = ref<CustomerImportResult | null>(null)

async function onImportFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  importing.value = true
  clearError()
  try {
    importResult.value = await customersApi.importCsv(file)
    refresh()
    notifications.success(t('customers.notifications.imported'))
  } catch (err) {
    setError(err)
  } finally {
    importing.value = false
  }
}

// --- Export -----------------------------------------------------------------
const exporting = ref(false)

async function exportCsv() {
  exporting.value = true
  clearError()
  try {
    const csv = await customersApi.exportCsv(currentFilter())
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
    const link = document.createElement('a')
    link.href = url
    link.download = 'customers.csv'
    link.click()
    URL.revokeObjectURL(url)
    notifications.success(t('customers.notifications.exported'))
  } catch (err) {
    setError(err)
  } finally {
    exporting.value = false
  }
}

function statusColor(status: CustomerStatus): string {
  if (status.isDeletedState) return 'error'
  if (status.isArchivedState) return 'warning'
  if (status.isBlocking) return 'error'
  return 'success'
}
</script>

<template>
  <div class="crm-page">
    <AppPageHeader
      icon="mdi-account-group-outline"
      :eyebrow="t('customers.eyebrow')"
      :title="t('customers.title')"
      :subtitle="t('customers.subtitle')"
    >
      <template #actions>
      <input
        ref="importInput"
        type="file"
        accept=".csv,text/csv"
        class="d-none"
        @change="onImportFile"
      />
      <v-btn
        v-can="'customers:import'"
        variant="tonal"
        prepend-icon="mdi-upload"
        :loading="importing"
        @click="importInput?.click()"
      >
        {{ t('customers.actions.importCsv') }}
      </v-btn>
      <v-btn
        v-can="'customers:export'"
        variant="tonal"
        prepend-icon="mdi-download"
        :loading="exporting"
        @click="exportCsv"
      >
        {{ t('customers.actions.exportCsv') }}
      </v-btn>
      <v-btn v-can="'customers:create'" color="primary" prepend-icon="mdi-plus" @click="openCreate">
        {{ t('customers.actions.newCustomer') }}
      </v-btn>
      </template>
    </AppPageHeader>

    <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
      {{ errorMessage }}
    </v-alert>

    <AppLoadingState v-if="statsLoading && !stats" variant="cards" :rows="4" :label="t('common.loading')" />
    <v-row v-else-if="stats" class="mb-2">
      <v-col v-for="metric in metrics" :key="metric.key" cols="6" lg="3">
        <v-card rounded="xl" class="crm-metric h-100">
          <v-card-text>
            <div class="d-flex align-start ga-3">
              <v-avatar :color="metric.color" variant="tonal" rounded="lg"><v-icon :icon="metric.icon" /></v-avatar>
              <div class="min-width-0">
                <div class="text-caption text-medium-emphasis">{{ t(`customers.metrics.${metric.key}`) }}</div>
                <div class="text-h6 text-sm-h5 font-weight-bold mt-1">{{ metric.value }}</div>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-card rounded="xl" class="crm-tabs mb-5">
      <v-tabs v-model="tab" color="primary" show-arrows density="comfortable">
        <v-tab v-for="item in tabs" :key="item.value" :value="item.value" :prepend-icon="item.icon">{{ item.label }}</v-tab>
      </v-tabs>
    </v-card>

    <v-card v-if="tab === 'customers'" rounded="xl" class="app-data-surface">
      <v-card-text>
        <v-row dense align="center">
          <v-col cols="12" sm="4">
            <v-text-field
              v-model="searchFilter"
              :label="t('customers.filters.searchPlaceholder')"
              prepend-inner-icon="mdi-magnify"
              clearable
              hide-details
              density="comfortable"
            />
          </v-col>
          <v-col cols="6" sm="2">
            <v-select v-model="statusFilter" :label="t('customers.filters.status')" :items="statusItems" hide-details density="comfortable" />
          </v-col>
          <v-col cols="6" sm="2">
            <v-select v-model="typeFilter" :label="t('customers.filters.type')" :items="typeItems" hide-details density="comfortable" />
          </v-col>
          <v-col cols="6" sm="2">
            <v-select v-model="sourceFilter" :label="t('customers.filters.source')" :items="sourceItems" hide-details density="comfortable" />
          </v-col>
          <v-col cols="6" sm="2">
            <v-text-field v-model="cityFilter" :label="t('customers.filters.city')" hide-details density="comfortable" clearable />
          </v-col>
          <v-col cols="12" sm="4">
            <v-autocomplete
              v-model="tagFilter"
              :label="t('customers.filters.tags')"
              :items="tags"
              item-title="name"
              item-value="id"
              multiple
              chips
              closable-chips
              hide-details
              density="comfortable"
            />
          </v-col>
          <v-col cols="12" sm="3">
            <v-switch
              v-model="includeHidden"
              :label="t('customers.filters.includeHidden')"
              color="primary"
              hide-details
              density="compact"
            />
          </v-col>
        </v-row>
      </v-card-text>

      <v-data-table-server
        v-if="!smAndDown"
        :headers="headers"
        :items="items"
        :items-length="totalItems"
        :loading="loading"
        :items-per-page="10"
        item-value="id"
        @update:options="loadItems"
      >
        <template #[`item.displayName`]="{ item }">
          <button type="button" class="app-inline-action text-primary" data-sami-shortcut-skip @click="openProfile(item)">{{ item.displayName }}</button>
          <div v-if="item.companyName" class="text-caption text-medium-emphasis">
            {{ item.companyName }}
          </div>
        </template>
        <template #[`item.type`]="{ item }">
          <v-chip size="small" variant="tonal">{{ srvLabel(item.type.name) }}</v-chip>
        </template>
        <template #[`item.status`]="{ item }">
          <v-chip :color="statusColor(item.status)" size="small" variant="tonal">
            {{ srvLabel(item.status.name) }}
          </v-chip>
        </template>
        <template #[`item.tags`]="{ item }">
          <v-chip
            v-for="tag in item.tags.slice(0, 3)"
            :key="tag.id"
            size="x-small"
            variant="tonal"
            :color="tag.color ?? undefined"
            class="me-1"
          >
            {{ srvLabel(tag.name) }}
          </v-chip>
          <span v-if="item.tags.length > 3" class="text-caption">+{{ item.tags.length - 3 }}</span>
        </template>
        <template #[`item.createdAt`]="{ item }">{{ formatDate(item.createdAt) }}</template>
        <template #[`item.actions`]="{ item }">
          <v-btn
            icon="mdi-account-details"
            size="small"
            variant="text"
            @click="openProfile(item)"
          />
          <v-btn
            v-can="'customers:edit'"
            icon="mdi-pencil"
            size="small"
            variant="text"
            @click="openEdit(item)"
          />
          <v-menu location="bottom end">
            <template #activator="{ props: menuProps }">
              <v-btn
                icon="mdi-dots-vertical"
                size="small"
                variant="text"
                v-bind="menuProps"
                :loading="actionBusy === item.id"
              />
            </template>
            <v-list density="compact" min-width="220">
              <v-list-item
                v-if="can('customers:merge')"
                prepend-icon="mdi-account-convert"
                :title="t('customers.menu.merge')"
                @click="openMerge(item)"
              />
              <v-list-item
                v-if="can('customers:edit') && !item.status.isBlacklistState && !item.status.isDeletedState"
                prepend-icon="mdi-account-cancel"
                :title="t('customers.menu.blacklist')"
                @click="openBlacklist(item)"
              />
              <v-list-item
                v-if="can('customers:edit') && item.status.isBlacklistState"
                prepend-icon="mdi-account-check"
                :title="t('customers.menu.liftBlacklist')"
                @click="rowAction(item, () => customersApi.unblacklist(item.id))"
              />
              <v-list-item
                v-if="can('customers:archive') && !item.status.isArchivedState && !item.status.isDeletedState"
                prepend-icon="mdi-archive"
                :title="t('customers.menu.archive')"
                @click="rowAction(item, () => customersApi.archive(item.id))"
              />
              <v-list-item
                v-if="can('customers:restore') && (item.status.isArchivedState || item.status.isDeletedState)"
                prepend-icon="mdi-restore"
                :title="t('customers.menu.restore')"
                @click="rowAction(item, () => customersApi.restore(item.id))"
              />
              <v-list-item
                v-if="can('customers:delete') && !item.status.isDeletedState"
                prepend-icon="mdi-delete"
                :title="t('customers.menu.softDelete')"
                @click="deleteTarget = item"
              />
              <v-list-item
                v-if="can('customers:purge') && item.status.isDeletedState"
                prepend-icon="mdi-delete-forever"
                base-color="error"
                :title="t('customers.menu.deletePermanently')"
                @click="purgeTarget = item"
              />
            </v-list>
          </v-menu>
        </template>
      </v-data-table-server>

      <AppLoadingState v-else-if="loading && !items.length" variant="table" :rows="5" :label="t('common.loading')" />
      <div v-else-if="items.length" class="d-grid ga-3 pa-3 pt-0">
        <v-card
          v-for="item in items"
          :key="item.id"
          rounded="xl"
          variant="outlined"
          role="button"
          tabindex="0"
          :aria-label="item.displayName"
          @click="openProfile(item)"
          @keydown.enter.prevent="openProfile(item)"
          @keydown.space.prevent="openProfile(item)"
        >
          <v-card-text>
            <div class="d-flex align-start ga-3">
              <v-avatar color="primary" variant="tonal" rounded="lg"><v-icon icon="mdi-account" /></v-avatar>
              <div class="flex-grow-1 min-width-0">
                <div class="d-flex align-center flex-wrap ga-2">
                  <strong class="text-truncate">{{ item.displayName }}</strong>
                  <v-chip :color="statusColor(item.status)" size="x-small" variant="tonal">{{ srvLabel(item.status.name) }}</v-chip>
                </div>
                <div class="text-caption text-medium-emphasis mt-1">{{ item.customerCode }} · {{ srvLabel(item.type.name) }}</div>
                <div v-if="item.companyName" class="text-body-2 text-truncate mt-1">{{ item.companyName }}</div>
                <div class="d-flex flex-wrap ga-1 mt-2">
                  <v-chip v-for="tag in item.tags.slice(0, 3)" :key="tag.id" size="x-small" variant="tonal" :color="tag.color ?? undefined">{{ srvLabel(tag.name) }}</v-chip>
                </div>
              </div>
            </div>
            <div class="d-flex justify-end ga-1 mt-2" @click.stop @keydown.stop>
              <v-btn :aria-label="t('customers.actions.viewProfile')" icon="mdi-account-details" size="small" variant="text" @click="openProfile(item)" />
              <v-btn v-if="can('customers:edit')" :aria-label="t('common.edit')" icon="mdi-pencil" size="small" variant="text" @click="openEdit(item)" />
              <v-menu location="bottom end">
                <template #activator="{ props: menuProps }"><v-btn :aria-label="t('customers.actions.more')" icon="mdi-dots-vertical" size="small" variant="text" v-bind="menuProps" /></template>
                <v-list density="compact" min-width="220">
                  <v-list-item v-if="can('customers:merge')" prepend-icon="mdi-account-convert" :title="t('customers.menu.merge')" @click="openMerge(item)" />
                  <v-list-item v-if="can('customers:edit') && !item.status.isBlacklistState && !item.status.isDeletedState" prepend-icon="mdi-account-cancel" :title="t('customers.menu.blacklist')" @click="openBlacklist(item)" />
                  <v-list-item v-if="can('customers:edit') && item.status.isBlacklistState" prepend-icon="mdi-account-check" :title="t('customers.menu.liftBlacklist')" @click="rowAction(item, () => customersApi.unblacklist(item.id))" />
                  <v-list-item v-if="can('customers:archive') && !item.status.isArchivedState && !item.status.isDeletedState" prepend-icon="mdi-archive" :title="t('customers.menu.archive')" @click="rowAction(item, () => customersApi.archive(item.id))" />
                  <v-list-item v-if="can('customers:restore') && (item.status.isArchivedState || item.status.isDeletedState)" prepend-icon="mdi-restore" :title="t('customers.menu.restore')" @click="rowAction(item, () => customersApi.restore(item.id))" />
                  <v-list-item v-if="can('customers:delete') && !item.status.isDeletedState" prepend-icon="mdi-delete" :title="t('customers.menu.softDelete')" @click="deleteTarget = item" />
                  <v-list-item v-if="can('customers:purge') && item.status.isDeletedState" prepend-icon="mdi-delete-forever" base-color="error" :title="t('customers.menu.deletePermanently')" @click="purgeTarget = item" />
                </v-list>
              </v-menu>
            </div>
          </v-card-text>
        </v-card>
        <v-pagination
          v-if="pageCount > 1"
          v-model="lastOptions.page"
          :length="pageCount"
          :total-visible="5"
          @update:model-value="() => loadItems(lastOptions)"
        />
      </div>
      <AppEmptyState v-else icon="mdi-account-search-outline" :title="t('customers.empty.title')" :description="t('customers.empty.description')" />
    </v-card>

    <CrmReportsPanel v-else-if="tab === 'reports'" :stats="stats" :loading="statsLoading" @refresh="loadStats" />
    <CrmSegmentsPanel v-else-if="tab === 'segments'" :types="types" :statuses="statuses" :sources="sources" :tags="tags" />
    <CrmConfigPanel v-else-if="tab === 'config'" @changed="loadReferenceData" />

    <CustomerFormDialog
      v-model="formOpen"
      :customer="editingDetail"
      :types="types"
      :statuses="statuses"
      :sources="sources"
      :tags="tags"
      :preference-defs="preferenceDefs"
      @saved="refresh"
    />

    <!-- Blacklist dialog -->
    <v-dialog :model-value="!!blacklistTarget" max-width="440" @update:model-value="blacklistTarget = null">
      <v-card rounded="lg">
        <v-card-title class="text-h6">{{ t('customers.blacklist.title') }}</v-card-title>
        <v-card-text>
          <i18n-t keypath="customers.blacklist.body" tag="p" class="text-body-2 mb-4">
            <template #name><strong>{{ blacklistTarget?.displayName }}</strong></template>
            <template #code>{{ blacklistTarget?.customerCode }}</template>
          </i18n-t>
          <v-select
            v-model="blacklistReasonId"
            :items="blacklistReasons.filter((r) => r.active)"
            item-title="name"
            item-value="id"
            :label="t('customers.blacklist.reason')"
            class="mb-2"
          />
          <v-textarea v-model="blacklistNote" :label="t('customers.blacklist.note')" rows="2" maxlength="500" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="blacklistTarget = null">{{ t('common.cancel') }}</v-btn>
          <v-btn
            color="error"
            :disabled="blacklistReasonId === null"
            :loading="blacklistBusy"
            @click="confirmBlacklist"
          >
            {{ t('customers.blacklist.confirm') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Import result dialog -->
    <v-dialog :model-value="!!importResult" max-width="520" @update:model-value="importResult = null">
      <v-card rounded="lg">
        <v-card-title class="text-h6">{{ t('customers.import.title') }}</v-card-title>
        <v-card-text>
          <i18n-t keypath="customers.import.summary" tag="p" class="text-body-1 mb-3">
            <template #ok><strong>{{ importResult?.created }}</strong></template>
            <template #failed><strong>{{ importResult?.skipped.length }}</strong></template>
          </i18n-t>
          <v-list v-if="importResult?.skipped.length" density="compact" class="border rounded" max-height="240" style="overflow-y: auto">
            <v-list-item v-for="row in importResult.skipped" :key="row.line">
              <v-list-item-title class="text-body-2">
                {{ t('customers.import.skippedRow', { row: row.line, reason: row.reason }) }}
              </v-list-item-title>
            </v-list-item>
          </v-list>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="importResult = null">{{ t('common.close') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <CustomerProfileDialog v-model="profileOpen" :customer="profileCustomer" />

    <CustomerMergeDialog v-model="mergeOpen" :source="mergeSource" @merged="refresh" />

    <!-- Soft delete confirmation -->
    <v-dialog :model-value="!!deleteTarget" max-width="440" @update:model-value="deleteTarget = null">
      <v-card rounded="lg">
        <v-card-title class="text-h6">{{ t('customers.delete.title') }}</v-card-title>
        <v-card-text>
          <i18n-t keypath="customers.delete.body" tag="span">
            <template #name><strong>{{ deleteTarget?.displayName }}</strong></template>
            <template #code>{{ deleteTarget?.customerCode }}</template>
          </i18n-t>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="deleteTarget = null">{{ t('common.cancel') }}</v-btn>
          <v-btn color="error" :loading="confirming" @click="confirmDelete">{{ t('common.delete') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Permanent delete confirmation -->
    <v-dialog :model-value="!!purgeTarget" max-width="440" @update:model-value="purgeTarget = null">
      <v-card rounded="lg">
        <v-card-title class="text-h6">{{ t('customers.purge.title') }}</v-card-title>
        <v-card-text>
          <i18n-t keypath="customers.purge.body" tag="span">
            <template #name><strong>{{ purgeTarget?.displayName }}</strong></template>
            <template #code>{{ purgeTarget?.customerCode }}</template>
            <template #warning><strong>{{ t('customers.purge.warning') }}</strong></template>
          </i18n-t>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="purgeTarget = null">{{ t('common.cancel') }}</v-btn>
          <v-btn color="error" :loading="confirming" @click="confirmPurge">{{ t('customers.purge.confirm') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.crm-page { min-width: 0; }
.crm-metric, .crm-tabs { border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity)); }
.crm-tabs { overflow: hidden; }
.d-grid { display: grid; }
.min-width-0 { min-width: 0; }
@media (max-width: 599px) {
  .crm-metric :deep(.v-card-text) { padding: 14px; }
  .crm-metric :deep(.v-avatar) { width: 36px !important; height: 36px !important; }
  .crm-tabs :deep(.v-tab) { min-width: 112px; }
  :deep(.v-btn) { min-height: 44px; }
}
</style>
