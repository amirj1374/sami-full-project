<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
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
import type {
  BlacklistReason,
  Customer,
  CustomerDetail,
  CustomerImportResult,
  CustomerSource,
  CustomerStatus,
  CustomerTag,
  CustomerType,
  PreferenceDefinition,
} from '@/types/models'

const { t } = useI18n()
const { formatDate } = useFormat()
const { can } = usePermission()
const { text: srvLabel } = useServerLabel()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()

// --- Reference data ---------------------------------------------------------
const types = ref<CustomerType[]>([])
const statuses = ref<CustomerStatus[]>([])
const sources = ref<CustomerSource[]>([])
const tags = ref<CustomerTag[]>([])
const preferenceDefs = ref<PreferenceDefinition[]>([])
const blacklistReasons = ref<BlacklistReason[]>([])

onMounted(async () => {
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
  <div>
    <div class="d-flex align-center mb-4">
      <h1 class="text-h4">{{ t('customers.title') }}</h1>
      <v-spacer />
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
        class="mr-2"
        :loading="importing"
        @click="importInput?.click()"
      >
        {{ t('customers.actions.importCsv') }}
      </v-btn>
      <v-btn
        v-can="'customers:export'"
        variant="tonal"
        prepend-icon="mdi-download"
        class="mr-2"
        :loading="exporting"
        @click="exportCsv"
      >
        {{ t('customers.actions.exportCsv') }}
      </v-btn>
      <v-btn v-can="'customers:create'" color="primary" prepend-icon="mdi-plus" @click="openCreate">
        {{ t('customers.actions.newCustomer') }}
      </v-btn>
    </div>

    <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
      {{ errorMessage }}
    </v-alert>

    <v-card rounded="lg" border flat>
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
        :headers="headers"
        :items="items"
        :items-length="totalItems"
        :loading="loading"
        :items-per-page="10"
        item-value="id"
        @update:options="loadItems"
      >
        <template #[`item.displayName`]="{ item }">
          <a class="text-primary cursor-pointer" @click="openProfile(item)">{{ item.displayName }}</a>
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
            class="mr-1"
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
    </v-card>

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
            <template #created><strong>{{ importResult?.created }}</strong></template>
            <template #skipped><strong>{{ importResult?.skipped.length }}</strong></template>
          </i18n-t>
          <v-list v-if="importResult?.skipped.length" density="compact" class="border rounded" max-height="240" style="overflow-y: auto">
            <v-list-item v-for="row in importResult.skipped" :key="row.line">
              <v-list-item-title class="text-body-2">
                {{ t('customers.import.skippedRow', { line: row.line, reason: row.reason }) }}
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
