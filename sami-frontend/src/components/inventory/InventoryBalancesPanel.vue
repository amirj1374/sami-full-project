<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import { inventoryApi } from '@/api/inventory'
import { productsApi } from '@/api/products'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import { useNotifications } from '@/composables/useNotifications'
import { usePermission } from '@/composables/usePermission'
import AppEmptyState from '@/components/AppEmptyState.vue'
import AppMoneyField from '@/components/AppMoneyField.vue'
import AppQuickCreateButton from '@/components/AppQuickCreateButton.vue'
import QuickProductCreateDialog from '@/components/QuickProductCreateDialog.vue'
import type { Product } from '@/types/models'
import type { InventoryBalance, InventoryLocation, Warehouse } from '@/types/inventory'

const props = defineProps<{ warehouses: Warehouse[] }>()
const emit = defineEmits<{ changed: [] }>()
const { t } = useI18n()
const { smAndUp } = useDisplay()
const { formatNumber } = useFormat()
const error = useApiError()
const notifications = useNotifications()
const { can } = usePermission()
const loading = ref(false)
const rows = ref<InventoryBalance[]>([])
const total = ref(0)
const page = ref(1)
const filters = reactive({ search: '', warehouseId: null as number | null, lowStock: false })
const adjustmentOpen = ref(false)
const saving = ref(false)
const products = ref<Product[]>([])
const quickProductOpen = ref(false)
const quickProductLine = ref<{ productId: number | null } | null>(null)
function openQuickProduct(line: { productId: number | null }) { quickProductLine.value = line; quickProductOpen.value = true }
function selectCreatedProduct(product: Product) { if (!products.value.some((item) => item.id === product.id)) products.value.push(product); if (quickProductLine.value) quickProductLine.value.productId = product.id; quickProductLine.value = null }
const locations = ref<InventoryLocation[]>([])
const importInput = ref<HTMLInputElement | null>(null)
const form = reactive({ warehouseId: null as number | null, locationId: null as number | null, reason: '', lines: [] as Array<{ productId: number | null; quantity: number; unitCost: number }> })

const headers = computed(() => [
  { title: t('inventory.product'), key: 'productName' },
  { title: t('inventory.warehouse'), key: 'warehouseName' },
  { title: t('inventory.onHand'), key: 'onHand', align: 'end' as const },
  { title: t('inventory.reserved'), key: 'reserved', align: 'end' as const },
  { title: t('inventory.available'), key: 'available', align: 'end' as const },
  { title: t('inventory.value'), key: 'inventoryValue', align: 'end' as const },
  { title: t('inventory.status'), key: 'lowStock' },
])

async function load() {
  loading.value = true
  error.clear()
  try {
    const result = await inventoryApi.balances({
      page: page.value - 1,
      size: 20,
      search: filters.search || undefined,
      warehouseId: filters.warehouseId ?? undefined,
      lowStock: filters.lowStock || undefined,
    })
    rows.value = result.content
    total.value = result.totalElements
  } catch (reason) {
    error.set(reason)
  } finally {
    loading.value = false
  }
}

const reload = useDebounceFn(() => { page.value = 1; void load() }, 250)
watch(() => [filters.search, filters.warehouseId, filters.lowStock], reload)
watch(() => form.warehouseId, async (warehouseId) => {
  form.locationId = null
  locations.value = warehouseId ? await inventoryApi.locations(warehouseId) : []
  form.locationId = locations.value.find((item) => item.defaultLocation)?.id ?? null
})

function addLine() {
  form.lines.push({ productId: null, quantity: 1, unitCost: 0 })
}

async function openAdjustment() {
  Object.assign(form, { warehouseId: props.warehouses.find((item) => item.defaultWarehouse)?.id ?? props.warehouses[0]?.id ?? null, locationId: null, reason: '', lines: [] })
  addLine()
  adjustmentOpen.value = true
  try {
    products.value = (await productsApi.list({ size: 200, active: true })).content
  } catch (reason) {
    error.set(reason)
  }
}

async function saveAdjustment() {
  if (!form.warehouseId || !form.reason.trim() || form.lines.some((line) => !line.productId || line.quantity === 0)) return
  saving.value = true
  try {
    await inventoryApi.adjust({
      warehouseId: form.warehouseId,
      locationId: form.locationId ?? undefined,
      reason: form.reason.trim(),
      idempotencyKey: crypto.randomUUID(),
      lines: form.lines.map((line) => ({ productId: line.productId!, quantity: line.quantity, unitCost: line.unitCost })),
    })
    adjustmentOpen.value = false
    notifications.success(t('inventory.adjustmentSaved'))
    await load()
    emit('changed')
  } catch (reason) {
    error.set(reason)
  } finally {
    saving.value = false
  }
}

async function importCsv(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  loading.value = true
  try {
    const result = await inventoryApi.importCsv(file)
    if (result.errors.length) {
      error.set({ message: result.errors.join('\n') })
    } else {
      notifications.success(t('inventory.importDone', { count: result.successfulRows }))
      await load()
      emit('changed')
    }
  } catch (reason) {
    error.set(reason)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section aria-labelledby="inventory-balances-title">
    <div class="d-flex align-center flex-wrap ga-2 mb-4">
      <div>
        <h2 id="inventory-balances-title" class="text-h6">{{ t('inventory.balances') }}</h2>
        <p class="text-body-2 text-medium-emphasis mb-0">{{ t('inventory.balancesSubtitle') }}</p>
      </div>
      <v-spacer />
      <input ref="importInput" class="d-none" type="file" accept=".csv,text/csv" @change="importCsv">
      <v-btn v-if="can('inventory:import')" variant="tonal" prepend-icon="mdi-upload" @click="importInput?.click()">{{ t('inventory.importCsv') }}</v-btn>
      <v-btn v-if="can('inventory:export')" variant="tonal" prepend-icon="mdi-download" @click="inventoryApi.exportCsv({ search: filters.search || undefined, warehouseId: filters.warehouseId ?? undefined, lowStock: filters.lowStock || undefined }).catch(error.set)">{{ t('inventory.exportCsv') }}</v-btn>
      <v-btn v-if="can('inventory:adjust')" color="primary" prepend-icon="mdi-tune-variant" @click="openAdjustment">{{ t('inventory.adjustStock') }}</v-btn>
    </div>

    <v-alert v-if="error.message.value" type="error" variant="tonal" closable class="mb-4" @click:close="error.clear()">{{ error.message.value }}</v-alert>
    <v-card class="app-data-surface" rounded="xl">
      <v-card-text>
        <v-row dense>
          <v-col cols="12" sm="5"><v-text-field v-model="filters.search" :label="t('common.search')" prepend-inner-icon="mdi-magnify" clearable hide-details /></v-col>
          <v-col cols="12" sm="4"><v-select v-model="filters.warehouseId" :items="warehouses" item-title="name" item-value="id" :label="t('inventory.warehouse')" clearable hide-details /></v-col>
          <v-col cols="12" sm="3"><v-switch v-model="filters.lowStock" color="warning" :label="t('inventory.lowStockOnly')" hide-details /></v-col>
        </v-row>
      </v-card-text>

      <v-data-table-server v-if="smAndUp" v-model:page="page" :headers="headers" :items="rows" :items-length="total" :loading="loading" @update:options="load">
        <template #[`item.productName`]="{ item }"><strong>{{ item.productName }}</strong><div class="text-caption text-medium-emphasis">{{ item.sku }}</div></template>
        <template #[`item.warehouseName`]="{ item }">{{ item.warehouseName }}<div class="text-caption text-medium-emphasis">{{ item.locationName }}</div></template>
        <template #[`item.onHand`]="{ item }">{{ formatNumber(item.onHand) }}</template>
        <template #[`item.reserved`]="{ item }">{{ formatNumber(item.reserved) }}</template>
        <template #[`item.available`]="{ item }"><strong>{{ formatNumber(item.available) }}</strong></template>
        <template #[`item.inventoryValue`]="{ item }">{{ formatNumber(item.inventoryValue) }}</template>
        <template #[`item.lowStock`]="{ item }"><v-chip :color="item.lowStock ? 'warning' : 'success'" size="small" variant="tonal">{{ item.lowStock ? t('inventory.lowStock') : t('inventory.healthy') }}</v-chip></template>
      </v-data-table-server>
      <div v-else class="pa-3">
        <v-skeleton-loader v-if="loading" type="article@3" />
        <v-card v-for="item in rows" :key="item.id" variant="outlined" rounded="lg" class="mb-3">
          <v-card-text><div class="d-flex align-center"><div><strong>{{ item.productName }}</strong><div class="text-caption">{{ item.sku }}</div></div><v-spacer /><v-chip :color="item.lowStock ? 'warning' : 'success'" size="small">{{ item.lowStock ? t('inventory.lowStock') : t('inventory.healthy') }}</v-chip></div><div class="text-body-2 text-medium-emphasis mt-3">{{ item.warehouseName }} · {{ item.locationName }}</div><div class="inventory-quantities mt-3"><span>{{ t('inventory.onHand') }} <strong>{{ formatNumber(item.onHand) }}</strong></span><span>{{ t('inventory.reserved') }} <strong>{{ formatNumber(item.reserved) }}</strong></span><span>{{ t('inventory.available') }} <strong>{{ formatNumber(item.available) }}</strong></span></div></v-card-text>
        </v-card>
        <AppEmptyState v-if="!loading && !rows.length" icon="mdi-package-variant" :title="t('inventory.noBalances')" />
      </div>
    </v-card>

    <v-dialog v-model="adjustmentOpen" :fullscreen="!smAndUp" max-width="900" persistent>
      <v-card rounded="xl">
        <v-toolbar color="transparent"><v-toolbar-title>{{ t('inventory.adjustStock') }}</v-toolbar-title><v-btn icon="mdi-close" :aria-label="t('common.close')" @click="adjustmentOpen = false" /></v-toolbar>
        <v-card-text>
          <v-row><v-col cols="12" md="6"><v-select v-model="form.warehouseId" :items="warehouses.filter((item) => item.active)" item-title="name" item-value="id" :label="t('inventory.warehouse')" /></v-col><v-col cols="12" md="6"><v-select v-model="form.locationId" :items="locations" item-title="name" item-value="id" :label="t('inventory.location')" /></v-col><v-col cols="12"><v-textarea v-model="form.reason" :label="t('inventory.reason')" rows="2" counter="500" /></v-col></v-row>
          <div class="d-flex align-center mb-3"><h3 class="text-subtitle-1">{{ t('inventory.lines') }}</h3><v-spacer /><v-btn variant="tonal" prepend-icon="mdi-plus" @click="addLine">{{ t('inventory.addLine') }}</v-btn></div>
          <v-card v-for="(line, index) in form.lines" :key="index" variant="outlined" class="pa-3 mb-3"><v-row dense><v-col cols="12" md="5"><v-autocomplete v-model="line.productId" :items="products" item-title="name" item-value="id" :label="t('inventory.product')"><template v-if="can('products:create')" #append-inner><AppQuickCreateButton :label="t('common.createNew', { entity: t('inventory.product') })" @click="openQuickProduct(line)" /></template></v-autocomplete></v-col><v-col cols="6" md="3"><v-text-field v-model.number="line.quantity" type="number" step="0.001" :label="t('inventory.quantitySigned')" /></v-col><v-col cols="5" md="3"><AppMoneyField v-model="line.unitCost" min="0" :label="t('inventory.unitCost')" /></v-col><v-col cols="1"><v-btn icon="mdi-delete-outline" color="error" variant="text" :disabled="form.lines.length === 1" @click="form.lines.splice(index, 1)" /></v-col></v-row></v-card>
        </v-card-text>
        <v-card-actions class="pa-4"><v-spacer /><v-btn variant="text" @click="adjustmentOpen = false">{{ t('common.cancel') }}</v-btn><v-btn color="primary" :loading="saving" :disabled="!form.warehouseId || !form.reason || form.lines.some((line) => !line.productId || line.quantity === 0)" @click="saveAdjustment">{{ t('inventory.postAdjustment') }}</v-btn></v-card-actions>
      </v-card>
    </v-dialog>
    <QuickProductCreateDialog v-model="quickProductOpen" @created="selectCreatedProduct" />
  </section>
</template>

<style scoped>
.inventory-quantities { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.inventory-quantities span { display: flex; flex-direction: column; padding: 8px; border-radius: 10px; background: rgba(var(--v-theme-on-surface), .04); }
@media (max-width: 599px) { :deep(.v-btn) { min-height: 44px; } }
</style>
