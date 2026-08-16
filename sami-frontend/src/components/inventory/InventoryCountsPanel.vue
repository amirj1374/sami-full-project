<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import { inventoryApi } from '@/api/inventory'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import { useNotifications } from '@/composables/useNotifications'
import { usePermission } from '@/composables/usePermission'
import AppEmptyState from '@/components/AppEmptyState.vue'
import type { InventoryCount, InventoryLocation, Warehouse } from '@/types/inventory'

const props = defineProps<{ warehouses: Warehouse[] }>()
const emit = defineEmits<{ changed: [] }>()
const { t } = useI18n()
const { smAndUp } = useDisplay()
const { formatDateTime, formatNumber } = useFormat()
const { can } = usePermission()
const error = useApiError()
const notifications = useNotifications()
const rows = ref<InventoryCount[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const saving = ref(false)
const status = ref('')
const createOpen = ref(false)
const detailOpen = ref(false)
const selected = ref<InventoryCount | null>(null)
const locations = ref<InventoryLocation[]>([])
const form = reactive({ warehouseId: null as number | null, locationId: null as number | null, notes: '' })
const counted = reactive<Record<number, number>>({})
const statusItems = computed(() => ['', 'DRAFT', 'COUNTED', 'POSTED', 'CANCELLED'].map((value) => ({ title: value ? t(`inventory.values.${value}`, value) : t('common.all'), value })))
const headers = computed(() => [
  { title: t('inventory.countNumber'), key: 'countNumber' }, { title: t('inventory.warehouse'), key: 'warehouseName' },
  { title: t('inventory.status'), key: 'status' }, { title: t('inventory.createdAt'), key: 'createdAt' },
  { title: '', key: 'actions', sortable: false },
])

async function load() {
  loading.value = true; error.clear()
  try { const result = await inventoryApi.counts({ page: page.value - 1, size: 20, status: status.value || undefined }); rows.value = result.content; total.value = result.totalElements }
  catch (reason) { error.set(reason) } finally { loading.value = false }
}

async function warehouseChanged(warehouseId: number | null) {
  form.locationId = null
  try { locations.value = warehouseId ? await inventoryApi.locations(warehouseId) : [] } catch (reason) { error.set(reason) }
}

function openCreate() {
  Object.assign(form, { warehouseId: props.warehouses.find((item) => item.defaultWarehouse)?.id ?? props.warehouses[0]?.id ?? null, locationId: null, notes: '' })
  createOpen.value = true
  void warehouseChanged(form.warehouseId)
}

async function create() {
  if (!form.warehouseId) return
  saving.value = true
  try { const result = await inventoryApi.createCount({ warehouseId: form.warehouseId, locationId: form.locationId ?? undefined, notes: form.notes || undefined }); createOpen.value = false; await show(result); notifications.success(t('inventory.countCreated')); await load(); emit('changed') }
  catch (reason) { error.set(reason) } finally { saving.value = false }
}

async function show(item: InventoryCount) {
  loading.value = true
  try {
    selected.value = await inventoryApi.count(item.id)
    Object.keys(counted).forEach((key) => delete counted[Number(key)])
    selected.value.lines.forEach((line) => { counted[line.productId] = line.countedQuantity ?? line.expectedQuantity })
    detailOpen.value = true
  } catch (reason) { error.set(reason) } finally { loading.value = false }
}

async function submit() {
  if (!selected.value) return
  saving.value = true
  try { selected.value = await inventoryApi.submitCount(selected.value.id, selected.value.lines.map((line) => ({ productId: line.productId, countedQuantity: counted[line.productId] ?? 0 }))); notifications.success(t('inventory.countSubmitted')); await load() }
  catch (reason) { error.set(reason) } finally { saving.value = false }
}

async function act(action: 'post' | 'cancel') {
  if (!selected.value) return
  saving.value = true
  try { selected.value = action === 'post' ? await inventoryApi.postCount(selected.value.id) : await inventoryApi.cancelCount(selected.value.id); notifications.success(t('inventory.countUpdated')); await load(); emit('changed') }
  catch (reason) { error.set(reason) } finally { saving.value = false }
}

function color(value: string) { return ({ DRAFT: 'default', COUNTED: 'warning', POSTED: 'success', CANCELLED: 'error' } as Record<string, string>)[value] ?? 'default' }
onMounted(load)
</script>

<template>
  <section aria-labelledby="inventory-counts-title">
    <div class="d-flex align-center flex-wrap ga-2 mb-4"><div><h2 id="inventory-counts-title" class="text-h6">{{ t('inventory.stockCounts') }}</h2><p class="text-body-2 text-medium-emphasis mb-0">{{ t('inventory.countsSubtitle') }}</p></div><v-spacer /><v-select v-model="status" :items="statusItems" density="compact" hide-details class="count-filter" @update:model-value="page = 1; load()" /><v-btn v-if="can('inventory:count')" color="primary" prepend-icon="mdi-clipboard-list-outline" @click="openCreate">{{ t('inventory.newCount') }}</v-btn></div>
    <v-alert v-if="error.message.value" type="error" variant="tonal" closable class="mb-4" @click:close="error.clear()">{{ error.message.value }}</v-alert>
    <v-card class="app-data-surface" rounded="xl">
      <v-data-table-server v-if="smAndUp" v-model:page="page" :headers="headers" :items="rows" :items-length="total" :loading="loading" @update:options="load"><template #[`item.countNumber`]="{ item }"><button type="button" class="app-inline-action text-primary" @click="show(item)">{{ item.countNumber }}</button></template><template #[`item.status`]="{ item }"><v-chip :color="color(item.status)" size="small" variant="tonal">{{ t(`inventory.values.${item.status}`, item.status) }}</v-chip></template><template #[`item.createdAt`]="{ item }">{{ formatDateTime(item.createdAt) }}</template><template #[`item.actions`]="{ item }"><v-btn icon="mdi-eye-outline" variant="text" @click="show(item)" /></template></v-data-table-server>
      <div v-else class="pa-3"><v-skeleton-loader v-if="loading" type="article@3" /><v-card v-for="item in rows" :key="item.id" variant="outlined" rounded="lg" class="mb-3" role="button" tabindex="0" :aria-label="item.countNumber" @click="show(item)" @keydown.enter.prevent="show(item)" @keydown.space.prevent="show(item)"><v-card-text><div class="d-flex align-center"><strong>{{ item.countNumber }}</strong><v-spacer /><v-chip :color="color(item.status)" size="small">{{ t(`inventory.values.${item.status}`, item.status) }}</v-chip></div><div class="mt-3">{{ item.warehouseName }}<span v-if="item.locationName"> · {{ item.locationName }}</span></div><div class="text-caption text-medium-emphasis mt-2">{{ formatDateTime(item.createdAt) }}</div></v-card-text></v-card><AppEmptyState v-if="!loading && !rows.length" icon="mdi-clipboard-list-outline" :title="t('inventory.noCounts')" /></div>
    </v-card>

    <v-dialog v-model="createOpen" :fullscreen="!smAndUp" max-width="620"><v-card rounded="xl"><v-toolbar color="transparent"><v-toolbar-title>{{ t('inventory.newCount') }}</v-toolbar-title><v-btn icon="mdi-close" @click="createOpen = false" /></v-toolbar><v-card-text><v-select v-model="form.warehouseId" :items="warehouses.filter((item) => item.active)" item-title="name" item-value="id" :label="t('inventory.warehouse')" @update:model-value="warehouseChanged" /><v-select v-model="form.locationId" :items="locations" item-title="name" item-value="id" :label="t('inventory.locationOptional')" clearable /><v-textarea v-model="form.notes" :label="t('inventory.notes')" rows="2" /></v-card-text><v-card-actions class="pa-4"><v-spacer /><v-btn variant="text" @click="createOpen = false">{{ t('common.cancel') }}</v-btn><v-btn color="primary" :loading="saving" :disabled="!form.warehouseId" @click="create">{{ t('common.create') }}</v-btn></v-card-actions></v-card></v-dialog>

    <v-dialog v-model="detailOpen" :fullscreen="!smAndUp" max-width="900"><v-card v-if="selected" rounded="xl"><v-toolbar color="transparent"><v-toolbar-title>{{ selected.countNumber }}</v-toolbar-title><v-chip :color="color(selected.status)">{{ t(`inventory.values.${selected.status}`, selected.status) }}</v-chip><v-btn icon="mdi-close" @click="detailOpen = false" /></v-toolbar><v-card-text><div class="text-body-2 text-medium-emphasis mb-4">{{ selected.warehouseName }}<span v-if="selected.locationName"> · {{ selected.locationName }}</span></div><v-table v-if="smAndUp" density="comfortable"><thead><tr><th>{{ t('inventory.product') }}</th><th>{{ t('inventory.expected') }}</th><th>{{ t('inventory.counted') }}</th><th>{{ t('inventory.variance') }}</th></tr></thead><tbody><tr v-for="line in selected.lines" :key="line.id"><td><strong>{{ line.productName }}</strong><div class="text-caption">{{ line.sku }}</div></td><td>{{ formatNumber(line.expectedQuantity) }}</td><td><v-text-field v-if="selected.status === 'DRAFT'" v-model.number="counted[line.productId]" type="number" min="0" step="0.001" density="compact" hide-details /><span v-else>{{ formatNumber(line.countedQuantity) }}</span></td><td><span :class="(line.variance ?? 0) !== 0 ? 'text-warning' : ''">{{ formatNumber(line.variance ?? 0) }}</span></td></tr></tbody></v-table><div v-else><v-card v-for="line in selected.lines" :key="line.id" variant="outlined" rounded="lg" class="mb-3"><v-card-text><strong>{{ line.productName }}</strong><div class="text-caption text-medium-emphasis">{{ line.sku }}</div><div class="count-values mt-3"><span>{{ t('inventory.expected') }}<strong>{{ formatNumber(line.expectedQuantity) }}</strong></span><span>{{ t('inventory.variance') }}<strong :class="(line.variance ?? 0) !== 0 ? 'text-warning' : ''">{{ formatNumber(line.variance ?? 0) }}</strong></span></div><v-text-field v-if="selected.status === 'DRAFT'" v-model.number="counted[line.productId]" class="mt-3" type="number" min="0" step="0.001" :label="t('inventory.counted')" hide-details /><div v-else class="text-body-2 mt-3">{{ t('inventory.counted') }}: <strong>{{ formatNumber(line.countedQuantity) }}</strong></div></v-card-text></v-card></div><AppEmptyState v-if="!selected.lines.length" icon="mdi-package-variant" :title="t('inventory.emptyCount')" /></v-card-text><v-card-actions class="pa-4 flex-wrap"><v-btn v-if="selected.status === 'DRAFT' && can('inventory:count')" color="primary" :loading="saving" @click="submit">{{ t('inventory.submitCount') }}</v-btn><v-btn v-if="selected.status === 'COUNTED' && can('inventory:count')" color="success" :loading="saving" @click="act('post')">{{ t('inventory.postCount') }}</v-btn><v-btn v-if="['DRAFT', 'COUNTED'].includes(selected.status) && can('inventory:count')" color="error" variant="tonal" :loading="saving" @click="act('cancel')">{{ t('common.cancel') }}</v-btn><v-spacer /><v-btn variant="text" @click="detailOpen = false">{{ t('common.close') }}</v-btn></v-card-actions></v-card></v-dialog>
  </section>
</template>

<style scoped>
.count-filter { max-width: 190px; }
.count-values { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.count-values span { display: flex; flex-direction: column; padding: 8px; border-radius: 10px; background: rgba(var(--v-theme-on-surface), .04); }
@media (max-width: 599px) { .count-filter { width: 100%; max-width: none; } :deep(.v-btn) { min-height: 44px; } }
</style>
