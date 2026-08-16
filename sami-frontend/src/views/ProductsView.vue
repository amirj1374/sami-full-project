<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDebounceFn } from '@vueuse/core'
import { useDisplay } from 'vuetify'
import { productsApi, type ProductListParams } from '@/api/products'
import { usePermission } from '@/composables/usePermission'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import ProductFormDialog from '@/components/ProductFormDialog.vue'
import AppPageHeader from '@/components/AppPageHeader.vue'
import AppMobileRecordCard from '@/components/AppMobileRecordCard.vue'
import type { Product } from '@/types/models'
import { marketSyncApi, type MarketRow } from '@/api/marketSync'

const { t } = useI18n()
const { smAndUp } = useDisplay()
const { formatNumber } = useFormat()
const { can } = usePermission()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()

// --- Table state ----------------------------------------------------------
interface SortItem {
  key: string
  order?: 'asc' | 'desc'
}
interface TableOptions {
  page: number
  itemsPerPage: number
  sortBy: SortItem[]
}

const items = ref<Product[]>([])
const totalItems = ref(0)
const loading = ref(false)
const lastOptions = ref<TableOptions>({ page: 1, itemsPerPage: 10, sortBy: [] })
const pageCount = computed(() => Math.max(1, Math.ceil(totalItems.value / lastOptions.value.itemsPerPage)))

const headers = computed(() => [
  { title: t('products.colName'), key: 'name' },
  { title: t('products.colSku'), key: 'sku' },
  { title: t('products.colPrice'), key: 'price', align: 'end' as const },
  { title: t('products.colStock'), key: 'stockQuantity', align: 'end' as const },
  { title: t('products.colActive'), key: 'active' },
  { title: t('hamta.eligible'), key: 'hamtaEligible' },
  { title: '', key: 'actions', sortable: false, align: 'end' as const },
])

// --- Filters --------------------------------------------------------------
const nameFilter = ref('')
const activeFilter = ref<boolean | null>(null)

async function loadItems(options: TableOptions) {
  lastOptions.value = options
  loading.value = true
  clearError()

  const sort = options.sortBy[0]
  const params: ProductListParams = {
    page: options.page - 1, // Vuetify is 1-based; the API is 0-based.
    size: options.itemsPerPage,
    sort: sort ? `${sort.key},${sort.order ?? 'asc'}` : undefined,
    name: nameFilter.value || undefined,
    active: activeFilter.value ?? undefined,
  }

  try {
    const pageResult = await productsApi.list(params)
    items.value = pageResult.content
    totalItems.value = pageResult.totalElements
  } catch (err) {
    setError(err)
  } finally {
    loading.value = false
  }
}

// Reload (reset to page 1) when filters change; debounce the text input.
const reload = useDebounceFn(() => {
  loadItems({ ...lastOptions.value, page: 1 })
}, 300)
watch([nameFilter, activeFilter], reload)

// --- Create / edit dialog -------------------------------------------------
const dialogOpen = ref(false)
const editing = ref<Product | null>(null)

function openCreate() {
  editing.value = null
  dialogOpen.value = true
}
function openEdit(product: Product) {
  editing.value = product
  dialogOpen.value = true
}
function onSaved() {
  loadItems(lastOptions.value)
}

// --- Delete confirmation --------------------------------------------------
const deleteTarget = ref<Product | null>(null)
const deleting = ref(false)
const marketStatus = ref<MarketRow | null>(null)
const marketStatusOpen = ref(false)

async function showMarketStatus(product: Product) {
  clearError()
  try { marketStatus.value = await marketSyncApi.productStatus(product.id); marketStatusOpen.value = true }
  catch (err) { setError(err) }
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    await productsApi.remove(deleteTarget.value.id)
    deleteTarget.value = null
    loadItems(lastOptions.value)
  } catch (err) {
    setError(err)
  } finally {
    deleting.value = false
  }
}

function formatPrice(value: number): string {
  return `${formatNumber(value)} ${t('common.currency')}`
}
</script>

<template>
  <div>
    <AppPageHeader icon="mdi-package-variant-closed" :title="t('products.title')">
      <template #actions><v-btn v-if="can('products:create')" color="primary" prepend-icon="mdi-plus" @click="openCreate">{{ t('products.newProduct') }}</v-btn></template>
    </AppPageHeader>

    <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
      {{ errorMessage }}
    </v-alert>

    <v-card rounded="lg" border flat class="app-data-surface">
      <v-card-text>
        <v-row dense>
          <v-col cols="12" sm="6">
            <v-text-field
              v-model="nameFilter"
              :label="t('products.searchByName')"
              prepend-inner-icon="mdi-magnify"
              clearable
              hide-details
              density="comfortable"
            />
          </v-col>
          <v-col cols="12" sm="6">
            <v-select
              v-model="activeFilter"
              :label="t('products.status')"
              :items="[
                { title: t('common.all'), value: null },
                { title: t('products.active'), value: true },
                { title: t('products.inactive'), value: false },
              ]"
              hide-details
              density="comfortable"
            />
          </v-col>
        </v-row>
      </v-card-text>

      <v-data-table-server
        v-if="smAndUp"
        :headers="headers"
        :items="items"
        :items-length="totalItems"
        :loading="loading"
        :items-per-page="10"
        @update:options="loadItems"
      >
        <template #[`item.price`]="{ item }">{{ formatPrice(item.price) }}</template>
        <template #[`item.active`]="{ item }">
          <v-chip :color="item.active ? 'success' : 'default'" size="small" variant="tonal">
            {{ item.active ? t('products.active') : t('products.inactive') }}
          </v-chip>
        </template>
        <template #[`item.hamtaEligible`]="{ item }">
          <v-chip :color="item.hamtaEligible ? 'primary' : 'default'" size="small" variant="tonal">
            {{ item.hamtaEligible ? t('common.yes') : t('common.no') }}
          </v-chip>
        </template>
        <template #[`item.actions`]="{ item }">
          <v-btn v-if="can('market-sync:view') && item.sku.startsWith('SIM-')" icon="mdi-sync-circle" size="small" variant="text" :aria-label="t('marketSync.title')" @click="showMarketStatus(item)" />
          <v-btn
            v-if="can('products:edit')"
            icon="mdi-pencil"
            size="small"
            variant="text"
            @click="openEdit(item)"
          />
          <v-btn
            v-if="can('products:delete')"
            icon="mdi-delete"
            size="small"
            variant="text"
            color="error"
            @click="deleteTarget = item"
          />
        </template>
      </v-data-table-server>
      <div v-else class="pa-3 d-grid ga-3">
        <AppMobileRecordCard v-for="item in items" :key="item.id" :label="item.name">
          <div class="d-flex align-start ga-3">
            <v-avatar color="primary" variant="tonal" rounded="lg"><v-icon icon="mdi-package-variant" /></v-avatar>
            <div class="flex-grow-1 min-width-0">
              <div class="font-weight-bold text-truncate">{{ item.name }}</div>
              <div class="text-caption text-medium-emphasis"><bdi>{{ item.sku }}</bdi></div>
            </div>
            <div class="text-end"><strong>{{ formatPrice(item.price) }}</strong><div class="text-caption">{{ t('products.colStock') }}: {{ formatNumber(item.stockQuantity) }}</div></div>
          </div>
          <template #details>
            <div class="d-grid ga-2 text-body-2">
              <div class="d-flex justify-space-between"><span>{{ t('products.status') }}</span><v-chip :color="item.active ? 'success' : 'default'" size="x-small" variant="tonal">{{ item.active ? t('products.active') : t('products.inactive') }}</v-chip></div>
              <div class="d-flex justify-space-between"><span>{{ t('hamta.eligible') }}</span><strong>{{ item.hamtaEligible ? t('common.yes') : t('common.no') }}</strong></div>
              <div v-if="item.description" class="text-medium-emphasis">{{ item.description }}</div>
            </div>
          </template>
          <template #actions>
            <v-btn v-if="can('market-sync:view') && item.sku.startsWith('SIM-')" icon="mdi-sync-circle" size="small" color="primary" variant="tonal" :aria-label="t('marketSync.title')" @click="showMarketStatus(item)" />
            <v-btn v-if="can('products:edit')" icon="mdi-pencil" size="small" color="primary" variant="tonal" :aria-label="t('common.edit')" @click="openEdit(item)" />
            <v-btn v-if="can('products:delete')" icon="mdi-delete" size="small" color="error" variant="tonal" :aria-label="t('common.delete')" @click="deleteTarget = item" />
          </template>
        </AppMobileRecordCard>
        <v-pagination v-if="pageCount > 1" v-model="lastOptions.page" :length="pageCount" :total-visible="5" @update:model-value="loadItems(lastOptions)" />
      </div>
    </v-card>

    <ProductFormDialog v-model="dialogOpen" :product="editing" @saved="onSaved" />

    <v-dialog :model-value="!!deleteTarget" max-width="420" @update:model-value="deleteTarget = null">
      <v-card rounded="lg">
        <v-card-title class="text-h6">{{ t('products.deleteTitle') }}</v-card-title>
        <v-card-text>
          <i18n-t keypath="products.deleteConfirm" tag="span">
            <template #name><strong>{{ deleteTarget?.name }}</strong></template>
          </i18n-t>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="deleteTarget = null">{{ t('common.cancel') }}</v-btn>
          <v-btn color="error" :loading="deleting" @click="confirmDelete">{{ t('common.delete') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-dialog v-model="marketStatusOpen" max-width="620">
      <v-card rounded="xl"><v-card-title>{{ t('marketSync.title') }}</v-card-title><v-card-text v-if="marketStatus">
        <v-list lines="two"><v-list-item :title="t('marketSync.source')" :subtitle="marketStatus.source_name"/><v-list-item title="Product Code" :subtitle="marketStatus.normalized_product_code"/><v-list-item :title="t('marketSync.previewSourcePrice')" :subtitle="String(marketStatus.source_price)"/><v-list-item :title="t('marketSync.finalPrice')" :subtitle="String(marketStatus.final_price)"/><v-list-item :title="t('marketSync.status')" :subtitle="`${marketStatus.availability_state} · ${marketStatus.publication_state} · ${marketStatus.publication_reason}`"/></v-list>
      </v-card-text><v-card-actions><v-spacer/><v-btn @click="marketStatusOpen=false">{{t('common.close')}}</v-btn></v-card-actions></v-card>
    </v-dialog>
  </div>
</template>
