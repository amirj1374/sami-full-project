<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import { inventoryApi } from '@/api/inventory'
import { productsApi } from '@/api/products'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import { useNotifications } from '@/composables/useNotifications'
import { usePermission } from '@/composables/usePermission'
import AppEmptyState from '@/components/AppEmptyState.vue'
import AppQuickCreateButton from '@/components/AppQuickCreateButton.vue'
import QuickProductCreateDialog from '@/components/QuickProductCreateDialog.vue'
import type { Product } from '@/types/models'
import type { InventoryTransfer, Warehouse } from '@/types/inventory'

const props = defineProps<{ warehouses: Warehouse[] }>()
const emit = defineEmits<{ changed: [] }>()
const { t } = useI18n()
const { smAndUp } = useDisplay()
const { formatDateTime, formatNumber } = useFormat()
const { can } = usePermission()
const error = useApiError()
const notifications = useNotifications()
const loading = ref(false)
const saving = ref(false)
const rows = ref<InventoryTransfer[]>([])
const total = ref(0)
const page = ref(1)
const status = ref('')
const editor = ref(false)
const detail = ref(false)
const selected = ref<InventoryTransfer | null>(null)
const products = ref<Product[]>([])
const quickProductOpen = ref(false)
const quickProductLine = ref<{ productId: number | null } | null>(null)
function openQuickProduct(line: { productId: number | null }) { quickProductLine.value = line; quickProductOpen.value = true }
function selectCreatedProduct(product: Product) { if (!products.value.some((item) => item.id === product.id)) products.value.push(product); if (quickProductLine.value) quickProductLine.value.productId = product.id; quickProductLine.value = null }
const form = reactive({ fromWarehouseId: null as number | null, toWarehouseId: null as number | null, notes: '', lines: [] as Array<{ productId: number | null; quantity: number; serialText: string }> })
const statusItems = computed(() => ['', 'DRAFT', 'SHIPPED', 'RECEIVED', 'CANCELLED'].map((value) => ({ title: value ? t(`inventory.values.${value}`, value) : t('common.all'), value })))
const headers = computed(() => [
  { title: t('inventory.transferNumber'), key: 'transferNumber' },
  { title: t('inventory.fromWarehouse'), key: 'fromWarehouseName' },
  { title: t('inventory.toWarehouse'), key: 'toWarehouseName' },
  { title: t('inventory.status'), key: 'status' },
  { title: t('inventory.createdAt'), key: 'createdAt' },
  { title: '', key: 'actions', sortable: false },
])

async function load() {
  loading.value = true
  error.clear()
  try {
    const result = await inventoryApi.transfers({ page: page.value - 1, size: 20, status: status.value || undefined })
    rows.value = result.content
    total.value = result.totalElements
  } catch (reason) { error.set(reason) } finally { loading.value = false }
}

function addLine() { form.lines.push({ productId: null, quantity: 1, serialText: '' }) }

async function openCreate() {
  Object.assign(form, { fromWarehouseId: props.warehouses.find((item) => item.defaultWarehouse)?.id ?? null, toWarehouseId: null, notes: '', lines: [] })
  addLine()
  editor.value = true
  try { products.value = (await productsApi.list({ size: 200, active: true })).content } catch (reason) { error.set(reason) }
}

async function save() {
  if (!form.fromWarehouseId || !form.toWarehouseId || form.fromWarehouseId === form.toWarehouseId || form.lines.some((line) => !line.productId || line.quantity <= 0)) return
  saving.value = true
  try {
    const created = await inventoryApi.createTransfer({
      fromWarehouseId: form.fromWarehouseId,
      toWarehouseId: form.toWarehouseId,
      notes: form.notes || undefined,
      lines: form.lines.map((line) => ({
        productId: line.productId!, quantity: line.quantity,
        serialNumbers: line.serialText.split(/[,\n]/).map((value) => value.trim()).filter(Boolean),
      })),
    })
    editor.value = false
    selected.value = created
    detail.value = true
    notifications.success(t('inventory.transferCreated'))
    await load(); emit('changed')
  } catch (reason) { error.set(reason) } finally { saving.value = false }
}

async function show(item: InventoryTransfer) {
  loading.value = true
  try { selected.value = await inventoryApi.transfer(item.id); detail.value = true } catch (reason) { error.set(reason) } finally { loading.value = false }
}

async function act(action: 'ship' | 'receive' | 'cancel') {
  if (!selected.value) return
  saving.value = true
  try {
    selected.value = action === 'ship' ? await inventoryApi.shipTransfer(selected.value.id) : action === 'receive' ? await inventoryApi.receiveTransfer(selected.value.id) : await inventoryApi.cancelTransfer(selected.value.id)
    notifications.success(t('inventory.transferUpdated'))
    await load(); emit('changed')
  } catch (reason) { error.set(reason) } finally { saving.value = false }
}

function color(value: string) { return ({ DRAFT: 'default', SHIPPED: 'info', RECEIVED: 'success', CANCELLED: 'error' } as Record<string, string>)[value] ?? 'default' }
onMounted(load)
</script>

<template>
  <section aria-labelledby="inventory-transfers-title">
    <div class="d-flex align-center flex-wrap ga-2 mb-4"><div><h2 id="inventory-transfers-title" class="text-h6">{{ t('inventory.transfers') }}</h2><p class="text-body-2 text-medium-emphasis mb-0">{{ t('inventory.transfersSubtitle') }}</p></div><v-spacer /><v-select v-model="status" :items="statusItems" density="compact" hide-details class="transfer-filter" @update:model-value="page = 1; load()" /><v-btn v-if="can('inventory:transfer')" color="primary" prepend-icon="mdi-swap-horizontal" @click="openCreate">{{ t('inventory.newTransfer') }}</v-btn></div>
    <v-alert v-if="error.message.value" type="error" variant="tonal" closable class="mb-4" @click:close="error.clear()">{{ error.message.value }}</v-alert>
    <v-card class="app-data-surface" rounded="xl">
      <v-data-table-server v-if="smAndUp" v-model:page="page" :headers="headers" :items="rows" :items-length="total" :loading="loading" @update:options="load"><template #[`item.transferNumber`]="{ item }"><button type="button" class="app-inline-action text-primary" @click="show(item)">{{ item.transferNumber }}</button></template><template #[`item.status`]="{ item }"><v-chip :color="color(item.status)" size="small" variant="tonal">{{ t(`inventory.values.${item.status}`, item.status) }}</v-chip></template><template #[`item.createdAt`]="{ item }">{{ formatDateTime(item.createdAt) }}</template><template #[`item.actions`]="{ item }"><v-btn icon="mdi-eye-outline" variant="text" :aria-label="t('common.view')" @click="show(item)" /></template></v-data-table-server>
      <div v-else class="pa-3"><v-skeleton-loader v-if="loading" type="article@3" /><v-card v-for="item in rows" :key="item.id" variant="outlined" class="mb-3" rounded="lg" role="button" tabindex="0" :aria-label="item.transferNumber" @click="show(item)" @keydown.enter.prevent="show(item)" @keydown.space.prevent="show(item)"><v-card-text><div class="d-flex align-center"><strong>{{ item.transferNumber }}</strong><v-spacer /><v-chip :color="color(item.status)" size="small">{{ t(`inventory.values.${item.status}`, item.status) }}</v-chip></div><div class="d-flex align-center ga-2 mt-4"><span>{{ item.fromWarehouseName }}</span><v-icon icon="mdi-arrow-right" size="18" /><span>{{ item.toWarehouseName }}</span></div><div class="text-caption text-medium-emphasis mt-2">{{ formatDateTime(item.createdAt) }}</div></v-card-text></v-card><AppEmptyState v-if="!loading && !rows.length" icon="mdi-swap-horizontal" :title="t('inventory.noTransfers')" /></div>
    </v-card>

    <v-dialog v-model="editor" :fullscreen="!smAndUp" max-width="900" persistent><v-card rounded="xl"><v-toolbar color="transparent"><v-toolbar-title>{{ t('inventory.newTransfer') }}</v-toolbar-title><v-btn icon="mdi-close" @click="editor = false" /></v-toolbar><v-card-text><v-row><v-col cols="12" md="6"><v-select v-model="form.fromWarehouseId" :items="warehouses.filter((item) => item.active)" item-title="name" item-value="id" :label="t('inventory.fromWarehouse')" /></v-col><v-col cols="12" md="6"><v-select v-model="form.toWarehouseId" :items="warehouses.filter((item) => item.active && item.id !== form.fromWarehouseId)" item-title="name" item-value="id" :label="t('inventory.toWarehouse')" /></v-col><v-col cols="12"><v-textarea v-model="form.notes" :label="t('inventory.notes')" rows="2" /></v-col></v-row><div class="d-flex align-center mb-3"><h3 class="text-subtitle-1">{{ t('inventory.lines') }}</h3><v-spacer /><v-btn variant="tonal" prepend-icon="mdi-plus" @click="addLine">{{ t('inventory.addLine') }}</v-btn></div><v-card v-for="(line, index) in form.lines" :key="index" variant="outlined" class="pa-3 mb-3"><v-row dense><v-col cols="12" md="5"><v-autocomplete v-model="line.productId" :items="products" item-title="name" item-value="id" :label="t('inventory.product')"><template v-if="can('products:create')" #append-inner><AppQuickCreateButton :label="t('common.createNew', { entity: t('inventory.product') })" @click="openQuickProduct(line)" /></template></v-autocomplete></v-col><v-col cols="5" md="2"><v-text-field v-model.number="line.quantity" type="number" min="0.001" step="0.001" :label="t('inventory.quantity')" /></v-col><v-col cols="6" md="4"><v-textarea v-model="line.serialText" rows="1" auto-grow :label="t('inventory.serialsOptional')" /></v-col><v-col cols="1"><v-btn icon="mdi-delete-outline" variant="text" color="error" :disabled="form.lines.length === 1" @click="form.lines.splice(index, 1)" /></v-col></v-row></v-card></v-card-text><v-card-actions class="pa-4"><v-spacer /><v-btn variant="text" @click="editor = false">{{ t('common.cancel') }}</v-btn><v-btn color="primary" :loading="saving" :disabled="!form.fromWarehouseId || !form.toWarehouseId || form.lines.some((line) => !line.productId || line.quantity <= 0)" @click="save">{{ t('common.create') }}</v-btn></v-card-actions></v-card></v-dialog>

    <v-dialog v-model="detail" :fullscreen="!smAndUp" max-width="800"><v-card v-if="selected" rounded="xl"><v-toolbar color="transparent"><v-toolbar-title>{{ selected.transferNumber }}</v-toolbar-title><v-chip :color="color(selected.status)">{{ t(`inventory.values.${selected.status}`, selected.status) }}</v-chip><v-btn icon="mdi-close" @click="detail = false" /></v-toolbar><v-card-text><v-row><v-col cols="6"><div class="text-caption">{{ t('inventory.fromWarehouse') }}</div><strong>{{ selected.fromWarehouseName }}</strong></v-col><v-col cols="6"><div class="text-caption">{{ t('inventory.toWarehouse') }}</div><strong>{{ selected.toWarehouseName }}</strong></v-col></v-row><v-list class="mt-4" lines="two"><v-list-item v-for="line in selected.lines" :key="line.id" :title="line.productName" :subtitle="`${line.sku}${line.serialNumbers.length ? ' · ' + line.serialNumbers.join(', ') : ''}`"><template #append><strong>{{ formatNumber(line.quantity) }}</strong></template></v-list-item></v-list><p v-if="selected.notes" class="text-body-2 mt-4">{{ selected.notes }}</p></v-card-text><v-card-actions class="pa-4 flex-wrap"><v-btn v-if="selected.status === 'DRAFT' && can('inventory:transfer')" color="info" :loading="saving" @click="act('ship')">{{ t('inventory.ship') }}</v-btn><v-btn v-if="selected.status === 'SHIPPED' && can('inventory:transfer')" color="success" :loading="saving" @click="act('receive')">{{ t('inventory.receive') }}</v-btn><v-btn v-if="selected.status === 'DRAFT' && can('inventory:transfer')" color="error" variant="tonal" :loading="saving" @click="act('cancel')">{{ t('common.cancel') }}</v-btn><v-spacer /><v-btn variant="text" @click="detail = false">{{ t('common.close') }}</v-btn></v-card-actions></v-card></v-dialog>
    <QuickProductCreateDialog v-model="quickProductOpen" @created="selectCreatedProduct" />
  </section>
</template>

<style scoped>.transfer-filter { max-width: 190px; } @media (max-width: 599px) { .transfer-filter { max-width: none; width: 100%; } :deep(.v-btn) { min-height: 44px; } }</style>
