<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import { inventoryApi } from '@/api/inventory'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import { useNotifications } from '@/composables/useNotifications'
import { usePermission } from '@/composables/usePermission'
import AppEmptyState from '@/components/AppEmptyState.vue'
import type { InventoryMovement, InventoryReservation, InventorySerial, Warehouse } from '@/types/inventory'

defineProps<{ warehouses: Warehouse[] }>()
const emit = defineEmits<{ changed: [] }>()
const { t } = useI18n()
const { smAndUp } = useDisplay()
const { formatDateTime, formatNumber } = useFormat()
const { can } = usePermission()
const error = useApiError()
const notifications = useNotifications()
const tab = ref<'movements' | 'serials' | 'reservations'>('movements')
const loading = ref(false)
const search = ref('')
const warehouseId = ref<number | null>(null)
const status = ref('')
const movements = ref<InventoryMovement[]>([])
const serials = ref<InventorySerial[]>([])
const reservations = ref<InventoryReservation[]>([])
const total = ref(0)
const page = ref(1)
const releaseTarget = ref<InventoryReservation | null>(null)
const serialTarget = ref<InventorySerial | null>(null)
const reason = ref('')

const movementHeaders = computed(() => [{ title: t('inventory.product'), key: 'productName' }, { title: t('inventory.movementType'), key: 'movementType' }, { title: t('inventory.quantity'), key: 'quantity' }, { title: t('inventory.route'), key: 'route' }, { title: t('inventory.source'), key: 'sourceType' }, { title: t('inventory.occurredAt'), key: 'occurredAt' }])
const serialHeaders = computed(() => [{ title: t('inventory.product'), key: 'productName' }, { title: t('inventory.serialNumber'), key: 'serialNumber' }, { title: t('inventory.imei'), key: 'imei' }, { title: t('inventory.warehouse'), key: 'warehouseName' }, { title: t('inventory.status'), key: 'status' }, { title: '', key: 'actions', sortable: false }])
const reservationHeaders = computed(() => [{ title: t('inventory.product'), key: 'productName' }, { title: t('inventory.source'), key: 'sourceType' }, { title: t('inventory.warehouse'), key: 'warehouseName' }, { title: t('inventory.quantity'), key: 'quantity' }, { title: t('inventory.status'), key: 'status' }, { title: t('inventory.createdAt'), key: 'createdAt' }, { title: '', key: 'actions', sortable: false }])

async function load() {
  loading.value = true; error.clear()
  try {
    if (tab.value === 'movements') { const result = await inventoryApi.movements({ page: page.value - 1, size: 20, search: search.value || undefined, warehouseId: warehouseId.value ?? undefined }); movements.value = result.content; total.value = result.totalElements }
    else if (tab.value === 'serials') { const result = await inventoryApi.serials({ page: page.value - 1, size: 20, search: search.value || undefined, warehouseId: warehouseId.value ?? undefined, status: status.value || undefined }); serials.value = result.content; total.value = result.totalElements }
    else { const result = await inventoryApi.reservations({ page: page.value - 1, size: 20, status: status.value || undefined }); reservations.value = result.content; total.value = result.totalElements }
  } catch (value) { error.set(value) } finally { loading.value = false }
}
const reload = useDebounceFn(() => { page.value = 1; void load() }, 250)
watch([search, warehouseId, status], reload)
watch(tab, () => { search.value = ''; status.value = ''; warehouseId.value = null; page.value = 1; void load() })

async function release() {
  if (!releaseTarget.value) return
  loading.value = true
  try { await inventoryApi.releaseReservation(releaseTarget.value.id, reason.value || undefined); releaseTarget.value = null; reason.value = ''; notifications.success(t('inventory.reservationReleased')); await load(); emit('changed') }
  catch (value) { error.set(value) } finally { loading.value = false }
}

async function updateSerial(statusValue: 'AVAILABLE' | 'QUARANTINED') {
  if (!serialTarget.value) return
  loading.value = true
  try { await inventoryApi.updateSerialStatus(serialTarget.value.id, statusValue, reason.value || undefined); serialTarget.value = null; reason.value = ''; notifications.success(t('inventory.serialUpdated')); await load() }
  catch (value) { error.set(value) } finally { loading.value = false }
}

function statusColor(value: string) { return ({ AVAILABLE: 'success', ACTIVE: 'info', RESERVED: 'warning', IN_TRANSIT: 'info', ISSUED: 'default', QUARANTINED: 'error', RELEASED: 'default', FULFILLED: 'success', EXPIRED: 'error', RETURNED_TO_SUPPLIER: 'secondary' } as Record<string, string>)[value] ?? 'default' }
onMounted(load)
</script>

<template>
  <section aria-labelledby="inventory-monitoring-title">
    <div class="mb-4"><h2 id="inventory-monitoring-title" class="text-h6">{{ t('inventory.monitoring') }}</h2><p class="text-body-2 text-medium-emphasis mb-0">{{ t('inventory.monitoringSubtitle') }}</p></div>
    <v-tabs v-model="tab" show-arrows class="mb-4"><v-tab value="movements" prepend-icon="mdi-swap-vertical">{{ t('inventory.movements') }}</v-tab><v-tab value="serials" prepend-icon="mdi-barcode-scan">{{ t('inventory.serials') }}</v-tab><v-tab value="reservations" prepend-icon="mdi-lock-clock">{{ t('inventory.reservations') }}</v-tab></v-tabs>
    <v-alert v-if="error.message.value" type="error" variant="tonal" closable class="mb-4" @click:close="error.clear()">{{ error.message.value }}</v-alert>
    <v-card class="app-data-surface" rounded="xl">
      <v-card-text><v-row dense><v-col v-if="tab !== 'reservations'" cols="12" sm="5"><v-text-field v-model="search" :label="t('common.search')" prepend-inner-icon="mdi-magnify" clearable hide-details /></v-col><v-col v-if="tab !== 'reservations'" cols="12" sm="4"><v-select v-model="warehouseId" :items="warehouses" item-title="name" item-value="id" :label="t('inventory.warehouse')" clearable hide-details /></v-col><v-col v-if="tab !== 'movements'" cols="12" sm="3"><v-select v-model="status" :items="tab === 'serials' ? ['AVAILABLE','RESERVED','IN_TRANSIT','ISSUED','QUARANTINED','RETURNED_TO_SUPPLIER'] : ['ACTIVE','RELEASED','FULFILLED','EXPIRED']" :item-title="(value: string) => t(`inventory.values.${value}`, value)" :label="t('inventory.status')" clearable hide-details /></v-col></v-row></v-card-text>

      <v-data-table-server v-if="smAndUp && tab === 'movements'" v-model:page="page" :headers="movementHeaders" :items="movements" :items-length="total" :loading="loading" @update:options="load"><template #[`item.productName`]="{ item }"><strong>{{ item.productName }}</strong><div class="text-caption">{{ item.sku }}</div></template><template #[`item.movementType`]="{ item }"><v-chip size="small" variant="tonal">{{ t(`inventory.values.${item.movementType}`, item.movementType) }}</v-chip></template><template #[`item.quantity`]="{ item }">{{ formatNumber(item.quantity) }}</template><template #[`item.route`]="{ item }">{{ item.fromWarehouseName || '—' }} → {{ item.toWarehouseName || '—' }}</template><template #[`item.sourceType`]="{ item }">{{ item.sourceType }}<span v-if="item.sourceId"> #{{ item.sourceId }}</span></template><template #[`item.occurredAt`]="{ item }">{{ formatDateTime(item.occurredAt) }}</template></v-data-table-server>
      <v-data-table-server v-else-if="smAndUp && tab === 'serials'" v-model:page="page" :headers="serialHeaders" :items="serials" :items-length="total" :loading="loading" @update:options="load"><template #[`item.productName`]="{ item }"><strong>{{ item.productName }}</strong><div class="text-caption">{{ item.sku }}</div></template><template #[`item.status`]="{ item }"><v-chip :color="statusColor(item.status)" size="small" variant="tonal">{{ t(`inventory.values.${item.status}`, item.status) }}</v-chip></template><template #[`item.actions`]="{ item }"><v-btn v-if="can('inventory:adjust') && ['AVAILABLE','QUARANTINED'].includes(item.status)" icon="mdi-shield-edit-outline" variant="text" @click="serialTarget = item" /></template></v-data-table-server>
      <v-data-table-server v-else-if="smAndUp && tab === 'reservations'" v-model:page="page" :headers="reservationHeaders" :items="reservations" :items-length="total" :loading="loading" @update:options="load"><template #[`item.productName`]="{ item }"><strong>{{ item.productName }}</strong><div class="text-caption">{{ item.sku }}</div></template><template #[`item.sourceType`]="{ item }">{{ item.sourceType }} #{{ item.sourceId }}</template><template #[`item.quantity`]="{ item }">{{ formatNumber(item.quantity) }}</template><template #[`item.status`]="{ item }"><v-chip :color="statusColor(item.status)" size="small" variant="tonal">{{ t(`inventory.values.${item.status}`, item.status) }}</v-chip></template><template #[`item.createdAt`]="{ item }">{{ formatDateTime(item.createdAt) }}</template><template #[`item.actions`]="{ item }"><v-btn v-if="item.status === 'ACTIVE' && can('inventory:reserve')" icon="mdi-lock-open-outline" variant="text" color="warning" @click="releaseTarget = item" /></template></v-data-table-server>

      <div v-else class="pa-3"><v-skeleton-loader v-if="loading" type="article@3" /><template v-if="tab === 'movements'"><v-card v-for="item in movements" :key="item.id" variant="outlined" rounded="lg" class="mb-3"><v-card-text><div class="d-flex"><div><strong>{{ item.productName }}</strong><div class="text-caption">{{ item.sku }}</div></div><v-spacer /><v-chip size="small">{{ t(`inventory.values.${item.movementType}`, item.movementType) }}</v-chip></div><div class="d-flex mt-3"><span>{{ item.fromWarehouseName || '—' }} → {{ item.toWarehouseName || '—' }}</span><v-spacer /><strong>{{ formatNumber(item.quantity) }}</strong></div><div class="text-caption text-medium-emphasis mt-2">{{ formatDateTime(item.occurredAt) }}</div></v-card-text></v-card><AppEmptyState v-if="!loading && !movements.length" icon="mdi-swap-vertical" :title="t('inventory.noMovements')" /></template><template v-else-if="tab === 'serials'"><v-card v-for="item in serials" :key="item.id" variant="outlined" rounded="lg" class="mb-3"><v-card-text><div class="d-flex"><div><strong>{{ item.productName }}</strong><div class="text-caption">{{ item.serialNumber || item.imei }}</div></div><v-spacer /><v-chip :color="statusColor(item.status)" size="small">{{ t(`inventory.values.${item.status}`, item.status) }}</v-chip></div><div class="mt-3">{{ item.warehouseName }} · {{ item.locationName }}</div><v-btn v-if="can('inventory:adjust') && ['AVAILABLE','QUARANTINED'].includes(item.status)" class="mt-2" variant="tonal" prepend-icon="mdi-shield-edit-outline" @click="serialTarget = item">{{ t('inventory.changeStatus') }}</v-btn></v-card-text></v-card><AppEmptyState v-if="!loading && !serials.length" icon="mdi-barcode-scan" :title="t('inventory.noSerials')" /></template><template v-else><v-card v-for="item in reservations" :key="item.id" variant="outlined" rounded="lg" class="mb-3"><v-card-text><div class="d-flex"><div><strong>{{ item.productName }}</strong><div class="text-caption">{{ item.sourceType }} #{{ item.sourceId }}</div></div><v-spacer /><v-chip :color="statusColor(item.status)" size="small">{{ t(`inventory.values.${item.status}`, item.status) }}</v-chip></div><div class="d-flex mt-3"><span>{{ item.warehouseName }}</span><v-spacer /><strong>{{ formatNumber(item.quantity) }}</strong></div><v-btn v-if="item.status === 'ACTIVE' && can('inventory:reserve')" class="mt-2" color="warning" variant="tonal" prepend-icon="mdi-lock-open-outline" @click="releaseTarget = item">{{ t('inventory.release') }}</v-btn></v-card-text></v-card><AppEmptyState v-if="!loading && !reservations.length" icon="mdi-lock-clock" :title="t('inventory.noReservations')" /></template></div>
    </v-card>

    <v-dialog :model-value="!!releaseTarget" max-width="500" @update:model-value="releaseTarget = null"><v-card rounded="xl"><v-card-title>{{ t('inventory.releaseReservation') }}</v-card-title><v-card-text><p>{{ t('inventory.releaseReservationConfirm', { product: releaseTarget?.productName }) }}</p><v-textarea v-model="reason" :label="t('inventory.reason')" rows="2" /></v-card-text><v-card-actions><v-spacer /><v-btn variant="text" @click="releaseTarget = null">{{ t('common.cancel') }}</v-btn><v-btn color="warning" :loading="loading" @click="release">{{ t('inventory.release') }}</v-btn></v-card-actions></v-card></v-dialog>
    <v-dialog :model-value="!!serialTarget" max-width="500" @update:model-value="serialTarget = null"><v-card rounded="xl"><v-card-title>{{ t('inventory.changeSerialStatus') }}</v-card-title><v-card-text><p>{{ serialTarget?.serialNumber || serialTarget?.imei }}</p><v-textarea v-model="reason" :label="t('inventory.reason')" rows="2" /></v-card-text><v-card-actions class="flex-wrap"><v-btn v-if="serialTarget?.status === 'QUARANTINED'" color="success" :loading="loading" @click="updateSerial('AVAILABLE')">{{ t('inventory.makeAvailable') }}</v-btn><v-btn v-else color="error" variant="tonal" :loading="loading" @click="updateSerial('QUARANTINED')">{{ t('inventory.quarantine') }}</v-btn><v-spacer /><v-btn variant="text" @click="serialTarget = null">{{ t('common.cancel') }}</v-btn></v-card-actions></v-card></v-dialog>
  </section>
</template>

<style scoped>@media (max-width: 599px) { :deep(.v-btn) { min-height: 44px; } }</style>
