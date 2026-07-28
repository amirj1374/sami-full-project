<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDebounceFn } from '@vueuse/core'
import { suppliersApi, supplierConfigApi, type SupplierListParams } from '@/api/suppliers'
import { usePermission } from '@/composables/usePermission'
import { useServerLabel } from '@/composables/useServerLabel'
import { useApiError } from '@/composables/useApiError'
import SupplierFormDialog from '@/components/SupplierFormDialog.vue'
import SupplierDetailDialog from '@/components/SupplierDetailDialog.vue'
import type {
  SupCategory,
  SupDocumentType,
  SupPaymentTerm,
  SupRatingCriterion,
  SupStatus,
  SupTag,
  SupType,
  SupplierDetail,
  SupplierRow,
} from '@/types/models'

const { t } = useI18n()
const { can } = usePermission()
const { text: srvLabel } = useServerLabel()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()

// --- Reference data ---------------------------------------------------------
const types = ref<SupType[]>([])
const statuses = ref<SupStatus[]>([])
const categories = ref<SupCategory[]>([])
const tags = ref<SupTag[]>([])
const paymentTerms = ref<SupPaymentTerm[]>([])
const criteria = ref<SupRatingCriterion[]>([])
const documentTypes = ref<SupDocumentType[]>([])

onMounted(async () => {
  try {
    ;[types.value, statuses.value, categories.value, tags.value, paymentTerms.value,
      criteria.value, documentTypes.value] = await Promise.all([
      supplierConfigApi.types(),
      supplierConfigApi.statuses(),
      supplierConfigApi.categories(),
      supplierConfigApi.tags(),
      supplierConfigApi.paymentTerms(),
      supplierConfigApi.ratingCriteria(),
      supplierConfigApi.documentTypes(),
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

const items = ref<SupplierRow[]>([])
const totalItems = ref(0)
const loading = ref(false)
const lastOptions = ref<TableOptions>({ page: 1, itemsPerPage: 10, sortBy: [] })

const headers = computed(() => [
  { title: t('suppliers.headers.code'), key: 'supplierCode' },
  { title: t('suppliers.headers.supplier'), key: 'displayName' },
  { title: t('suppliers.headers.type'), key: 'type', sortable: false },
  { title: t('suppliers.headers.status'), key: 'status', sortable: false },
  { title: t('suppliers.headers.rating'), key: 'ratingAvg' },
  { title: t('suppliers.headers.city'), key: 'city' },
  { title: '', key: 'actions', sortable: false, align: 'end' as const },
])

// --- Filters ----------------------------------------------------------------
const searchFilter = ref('')
const statusFilter = ref<number | null>(null)
const typeFilter = ref<number | null>(null)
const categoryFilter = ref<number[]>([])
const tagFilter = ref<number[]>([])
const minRatingFilter = ref<number | null>(null)
const includeHidden = ref(false)

function currentFilter() {
  return {
    search: searchFilter.value || undefined,
    statusId: statusFilter.value ?? undefined,
    typeId: typeFilter.value ?? undefined,
    categoryIds: categoryFilter.value.length ? categoryFilter.value : undefined,
    tagIds: tagFilter.value.length ? tagFilter.value : undefined,
    minRating: minRatingFilter.value ?? undefined,
    includeHidden: includeHidden.value || undefined,
  }
}

async function loadItems(options: TableOptions) {
  lastOptions.value = options
  loading.value = true
  clearError()

  const sort = options.sortBy[0]
  const params: SupplierListParams = {
    page: options.page - 1,
    size: options.itemsPerPage,
    sort: sort ? `${sort.key},${sort.order ?? 'asc'}` : undefined,
    ...currentFilter(),
  }

  try {
    const pageResult = await suppliersApi.list(params)
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
watch(
  [searchFilter, statusFilter, typeFilter, categoryFilter, tagFilter, minRatingFilter, includeHidden],
  reload,
)

const statusItems = computed(() => [
  { title: t('suppliers.filters.allStatuses'), value: null as number | null },
  ...statuses.value.map((s) => ({ title: s.name, value: s.id as number | null })),
])
const typeItems = computed(() => [
  { title: t('suppliers.filters.allTypes'), value: null as number | null },
  ...types.value.map((ty) => ({ title: ty.name, value: ty.id as number | null })),
])

// --- Dialogs / actions -------------------------------------------------------
const formOpen = ref(false)
const editingDetail = ref<SupplierDetail | null>(null)
const detailOpen = ref(false)
const detailId = ref<number | null>(null)
const actionBusy = ref<number | null>(null)
const deleteTarget = ref<SupplierRow | null>(null)
const purgeTarget = ref<SupplierRow | null>(null)
const confirming = ref(false)
const exporting = ref(false)

function openCreate() {
  editingDetail.value = null
  formOpen.value = true
}

async function openEdit(row: SupplierRow) {
  clearError()
  try {
    editingDetail.value = await suppliersApi.get(row.id)
    formOpen.value = true
  } catch (err) {
    setError(err)
  }
}

function openDetail(row: SupplierRow) {
  detailId.value = row.id
  detailOpen.value = true
}

async function rowAction(row: SupplierRow, action: () => Promise<unknown>) {
  actionBusy.value = row.id
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
    await suppliersApi.softDelete(deleteTarget.value.id)
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
    await suppliersApi.purge(purgeTarget.value.id)
    purgeTarget.value = null
    refresh()
  } catch (err) {
    setError(err)
  } finally {
    confirming.value = false
  }
}

async function exportCsv() {
  exporting.value = true
  clearError()
  try {
    const csv = await suppliersApi.exportCsv(currentFilter())
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
    const link = document.createElement('a')
    link.href = url
    link.download = 'suppliers.csv'
    link.click()
    URL.revokeObjectURL(url)
  } catch (err) {
    setError(err)
  } finally {
    exporting.value = false
  }
}

function statusColor(status: SupStatus): string {
  if (status.isDeletedState) return 'error'
  if (status.isArchivedState) return 'warning'
  if (status.isBlocking) return 'error'
  return 'success'
}
</script>

<template>
  <div>
    <div class="d-flex align-center mb-4">
      <h1 class="text-h4">{{ t('suppliers.title') }}</h1>
      <v-spacer />
      <v-btn
        v-can="'suppliers:export'"
        variant="tonal"
        prepend-icon="mdi-download"
        class="mr-2"
        :loading="exporting"
        @click="exportCsv"
      >
        {{ t('suppliers.exportCsv') }}
      </v-btn>
      <v-btn v-can="'suppliers:create'" color="primary" prepend-icon="mdi-plus" @click="openCreate">
        {{ t('suppliers.newSupplier') }}
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
              :label="t('suppliers.filters.searchPlaceholder')"
              prepend-inner-icon="mdi-magnify"
              clearable
              hide-details
              density="comfortable"
            />
          </v-col>
          <v-col cols="6" sm="2">
            <v-select v-model="statusFilter" :label="t('suppliers.headers.status')" :items="statusItems" hide-details density="comfortable" />
          </v-col>
          <v-col cols="6" sm="2">
            <v-select v-model="typeFilter" :label="t('suppliers.headers.type')" :items="typeItems" hide-details density="comfortable" />
          </v-col>
          <v-col cols="6" sm="2">
            <v-select
              v-model="minRatingFilter"
              :label="t('suppliers.filters.minRating')"
              :items="[{ title: t('suppliers.filters.any'), value: null }, 2, 3, 4, 4.5]"
              hide-details
              density="comfortable"
            />
          </v-col>
          <v-col cols="6" sm="2">
            <v-switch v-model="includeHidden" :label="t('suppliers.filters.archivedDeleted')" color="primary" hide-details density="compact" />
          </v-col>
          <v-col cols="12" sm="6">
            <v-autocomplete
              v-model="categoryFilter"
              :label="t('suppliers.filters.categories')"
              :items="categories"
              item-title="name"
              item-value="id"
              multiple
              chips
              closable-chips
              hide-details
              density="comfortable"
            />
          </v-col>
          <v-col cols="12" sm="6">
            <v-autocomplete
              v-model="tagFilter"
              :label="t('suppliers.filters.tags')"
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
          <a class="text-primary cursor-pointer" @click="openDetail(item)">{{ item.displayName }}</a>
          <div class="text-caption text-medium-emphasis">{{ item.companyName }}</div>
        </template>
        <template #[`item.type`]="{ item }">
          <v-chip size="small" variant="tonal">{{ srvLabel(item.type.name) }}</v-chip>
        </template>
        <template #[`item.status`]="{ item }">
          <v-chip :color="statusColor(item.status)" size="small" variant="tonal">
            {{ srvLabel(item.status.name) }}
          </v-chip>
        </template>
        <template #[`item.ratingAvg`]="{ item }">
          <v-rating :model-value="item.ratingAvg ?? 0" density="compact" size="x-small" half-increments readonly />
        </template>
        <template #[`item.actions`]="{ item }">
          <v-btn icon="mdi-eye" size="small" variant="text" @click="openDetail(item)" />
          <v-btn
            v-can="'suppliers:edit'"
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
                v-if="can('suppliers:archive') && !item.status.isArchivedState && !item.status.isDeletedState"
                prepend-icon="mdi-archive"
                :title="t('suppliers.actions.archive')"
                @click="rowAction(item, () => suppliersApi.archive(item.id))"
              />
              <v-list-item
                v-if="can('suppliers:restore') && (item.status.isArchivedState || item.status.isDeletedState)"
                prepend-icon="mdi-restore"
                :title="t('suppliers.actions.restore')"
                @click="rowAction(item, () => suppliersApi.restore(item.id))"
              />
              <v-list-item
                v-if="can('suppliers:delete') && !item.status.isDeletedState"
                prepend-icon="mdi-delete"
                :title="t('suppliers.actions.deleteSoft')"
                @click="deleteTarget = item"
              />
              <v-list-item
                v-if="can('suppliers:purge') && item.status.isDeletedState"
                prepend-icon="mdi-delete-forever"
                base-color="error"
                :title="t('suppliers.actions.deletePermanently')"
                @click="purgeTarget = item"
              />
            </v-list>
          </v-menu>
        </template>
      </v-data-table-server>
    </v-card>

    <SupplierFormDialog
      v-model="formOpen"
      :supplier="editingDetail"
      :types="types"
      :statuses="statuses"
      :categories="categories"
      :tags="tags"
      :payment-terms="paymentTerms"
      @saved="refresh"
    />

    <SupplierDetailDialog
      v-model="detailOpen"
      :supplier-id="detailId"
      :criteria="criteria"
      :document-types="documentTypes"
      @changed="refresh"
    />

    <!-- Soft delete confirmation -->
    <v-dialog :model-value="!!deleteTarget" max-width="440" @update:model-value="deleteTarget = null">
      <v-card rounded="lg">
        <v-card-title class="text-h6">{{ t('suppliers.deleteDialog.title') }}</v-card-title>
        <v-card-text>
          <i18n-t keypath="suppliers.deleteDialog.body" tag="span">
            <template #name><strong>{{ deleteTarget?.displayName }}</strong></template>
            <template #code>{{ deleteTarget?.supplierCode }}</template>
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
        <v-card-title class="text-h6">{{ t('suppliers.purgeDialog.title') }}</v-card-title>
        <v-card-text>
          <i18n-t keypath="suppliers.purgeDialog.body" tag="span">
            <template #name><strong>{{ purgeTarget?.displayName }}</strong></template>
            <template #code>{{ purgeTarget?.supplierCode }}</template>
            <template #warning><strong>{{ t('suppliers.purgeDialog.cannotUndo') }}</strong></template>
          </i18n-t>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="purgeTarget = null">{{ t('common.cancel') }}</v-btn>
          <v-btn color="error" :loading="confirming" @click="confirmPurge">{{ t('suppliers.purgeDialog.deleteForever') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
