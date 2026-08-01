<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import { inventoryApi } from '@/api/inventory'
import { useApiError } from '@/composables/useApiError'
import { useNotifications } from '@/composables/useNotifications'
import { usePermission } from '@/composables/usePermission'
import AppEmptyState from '@/components/AppEmptyState.vue'
import type { InventoryLocation, LocationType, Warehouse, WarehousePayload, WarehouseType } from '@/types/inventory'

const props = defineProps<{ warehouses: Warehouse[] }>()
const emit = defineEmits<{ changed: [] }>()
const { t } = useI18n()
const { smAndUp } = useDisplay()
const { can } = usePermission()
const error = useApiError()
const notifications = useNotifications()
const saving = ref(false)
const warehouseDialog = ref(false)
const locationDialog = ref(false)
const locationsOpen = ref(false)
const editingWarehouse = ref<Warehouse | null>(null)
const editingLocation = ref<InventoryLocation | null>(null)
const selectedWarehouse = ref<Warehouse | null>(null)
const locations = ref<InventoryLocation[]>([])
const deleteWarehouseTarget = ref<Warehouse | null>(null)
const deleteLocationTarget = ref<InventoryLocation | null>(null)
const warehouseForm = reactive<WarehousePayload>({ code: '', name: '', description: '', warehouseType: 'STANDARD', defaultWarehouse: false, active: true, displayOrder: 10 })
const locationForm = reactive({ code: '', name: '', description: '', locationType: 'STORAGE' as LocationType, defaultLocation: false, active: true })
const warehouseTypes: WarehouseType[] = ['STANDARD', 'RETAIL', 'TRANSIT', 'QUARANTINE', 'RETURNS']
const locationTypes: LocationType[] = ['RECEIVING', 'STORAGE', 'PICKING', 'TRANSIT', 'QUARANTINE', 'RETURNS']

function openWarehouse(item?: Warehouse) {
  editingWarehouse.value = item ?? null
  Object.assign(warehouseForm, item ? { companyId: item.companyId, branchId: item.branchId, code: item.code, name: item.name, description: item.description ?? '', warehouseType: item.warehouseType, defaultWarehouse: item.defaultWarehouse, active: item.active, displayOrder: item.displayOrder } : { companyId: undefined, branchId: undefined, code: '', name: '', description: '', warehouseType: 'STANDARD', defaultWarehouse: props.warehouses.length === 0, active: true, displayOrder: (props.warehouses.length + 1) * 10 })
  warehouseDialog.value = true
}

async function saveWarehouse() {
  if (!warehouseForm.code.trim() || !warehouseForm.name.trim()) return
  saving.value = true
  try {
    const payload = { ...warehouseForm, companyId: warehouseForm.companyId || undefined, branchId: warehouseForm.branchId || undefined, code: warehouseForm.code.trim(), name: warehouseForm.name.trim(), description: warehouseForm.description?.trim() || undefined }
    if (editingWarehouse.value) await inventoryApi.updateWarehouse(editingWarehouse.value.id, payload)
    else await inventoryApi.createWarehouse(payload)
    warehouseDialog.value = false; notifications.success(t('inventory.warehouseSaved')); emit('changed')
  } catch (reason) { error.set(reason) } finally { saving.value = false }
}

async function removeWarehouse() {
  if (!deleteWarehouseTarget.value) return
  saving.value = true
  try { await inventoryApi.deleteWarehouse(deleteWarehouseTarget.value.id); deleteWarehouseTarget.value = null; notifications.success(t('inventory.warehouseDeleted')); emit('changed') }
  catch (reason) { error.set(reason) } finally { saving.value = false }
}

async function showLocations(warehouse: Warehouse) {
  selectedWarehouse.value = warehouse
  try { locations.value = await inventoryApi.locations(warehouse.id); locationsOpen.value = true } catch (reason) { error.set(reason) }
}

function openLocation(item?: InventoryLocation) {
  editingLocation.value = item ?? null
  Object.assign(locationForm, item ? { code: item.code, name: item.name, description: item.description ?? '', locationType: item.locationType, defaultLocation: item.defaultLocation, active: item.active } : { code: '', name: '', description: '', locationType: 'STORAGE', defaultLocation: locations.value.length === 0, active: true })
  locationDialog.value = true
}

async function saveLocation() {
  if (!selectedWarehouse.value || !locationForm.code.trim() || !locationForm.name.trim()) return
  saving.value = true
  try {
    const payload = { ...locationForm, code: locationForm.code.trim(), name: locationForm.name.trim(), description: locationForm.description.trim() || undefined }
    if (editingLocation.value) await inventoryApi.updateLocation(selectedWarehouse.value.id, editingLocation.value.id, payload)
    else await inventoryApi.createLocation(selectedWarehouse.value.id, payload)
    locations.value = await inventoryApi.locations(selectedWarehouse.value.id); locationDialog.value = false; notifications.success(t('inventory.locationSaved'))
  } catch (reason) { error.set(reason) } finally { saving.value = false }
}

async function removeLocation() {
  if (!selectedWarehouse.value || !deleteLocationTarget.value) return
  saving.value = true
  try { await inventoryApi.deleteLocation(selectedWarehouse.value.id, deleteLocationTarget.value.id); locations.value = await inventoryApi.locations(selectedWarehouse.value.id); deleteLocationTarget.value = null; notifications.success(t('inventory.locationDeleted')) }
  catch (reason) { error.set(reason) } finally { saving.value = false }
}
</script>

<template>
  <section aria-labelledby="inventory-warehouses-title">
    <div class="d-flex align-center flex-wrap ga-2 mb-4"><div><h2 id="inventory-warehouses-title" class="text-h6">{{ t('inventory.warehouses') }}</h2><p class="text-body-2 text-medium-emphasis mb-0">{{ t('inventory.warehousesSubtitle') }}</p></div><v-spacer /><v-btn v-if="can('inventory:manage-warehouses')" color="primary" prepend-icon="mdi-plus" @click="openWarehouse()">{{ t('inventory.newWarehouse') }}</v-btn></div>
    <v-alert v-if="error.message.value" type="error" variant="tonal" closable class="mb-4" @click:close="error.clear()">{{ error.message.value }}</v-alert>
    <v-row><v-col v-for="warehouse in warehouses" :key="warehouse.id" cols="12" md="6" xl="4"><v-card rounded="xl" class="h-100 app-data-surface"><v-card-text><div class="d-flex align-start"><v-avatar color="primary" variant="tonal" rounded="lg"><v-icon icon="mdi-warehouse" /></v-avatar><div class="ms-3"><div class="d-flex align-center ga-2"><strong>{{ warehouse.name }}</strong><v-chip v-if="warehouse.defaultWarehouse" size="x-small" color="primary">{{ t('inventory.default') }}</v-chip></div><div class="text-caption text-medium-emphasis">{{ warehouse.code }} · {{ t(`inventory.values.${warehouse.warehouseType}`, warehouse.warehouseType) }}</div></div><v-spacer /><v-chip :color="warehouse.active ? 'success' : 'default'" size="small" variant="tonal">{{ warehouse.active ? t('common.active') : t('common.inactive') }}</v-chip></div><p v-if="warehouse.description" class="text-body-2 text-medium-emphasis mt-4 mb-0">{{ warehouse.description }}</p></v-card-text><v-card-actions><v-btn prepend-icon="mdi-view-grid-outline" variant="text" @click="showLocations(warehouse)">{{ t('inventory.locations') }}</v-btn><v-spacer /><v-btn v-if="can('inventory:manage-warehouses')" icon="mdi-pencil-outline" variant="text" @click="openWarehouse(warehouse)" /><v-btn v-if="can('inventory:manage-warehouses') && !warehouse.defaultWarehouse" icon="mdi-delete-outline" color="error" variant="text" @click="deleteWarehouseTarget = warehouse" /></v-card-actions></v-card></v-col></v-row>
    <AppEmptyState v-if="!warehouses.length" icon="mdi-warehouse" :title="t('inventory.noWarehouses')" :description="t('inventory.noWarehousesDescription')" />

    <v-dialog v-model="warehouseDialog" :fullscreen="!smAndUp" max-width="720" persistent><v-card rounded="xl"><v-toolbar color="transparent"><v-toolbar-title>{{ editingWarehouse ? t('inventory.editWarehouse') : t('inventory.newWarehouse') }}</v-toolbar-title><v-btn icon="mdi-close" @click="warehouseDialog = false" /></v-toolbar><v-card-text><v-row><v-col cols="12" sm="4"><v-text-field v-model="warehouseForm.code" :label="t('inventory.code')" /></v-col><v-col cols="12" sm="8"><v-text-field v-model="warehouseForm.name" :label="t('inventory.name')" /></v-col><v-col cols="12" sm="6"><v-select v-model="warehouseForm.warehouseType" :items="warehouseTypes" :item-title="(value: string) => t(`inventory.values.${value}`, value)" :label="t('inventory.warehouseType')" /></v-col><v-col cols="6" sm="3"><v-text-field v-model.number="warehouseForm.companyId" type="number" min="1" :label="t('inventory.companyId')" clearable /></v-col><v-col cols="6" sm="3"><v-text-field v-model.number="warehouseForm.branchId" type="number" min="1" :label="t('inventory.branchId')" clearable /></v-col><v-col cols="12"><v-textarea v-model="warehouseForm.description" :label="t('inventory.description')" rows="2" /></v-col><v-col cols="6"><v-switch v-model="warehouseForm.defaultWarehouse" color="primary" :label="t('inventory.defaultWarehouse')" :disabled="editingWarehouse?.defaultWarehouse" /></v-col><v-col cols="6"><v-switch v-model="warehouseForm.active" color="success" :label="t('common.active')" :disabled="editingWarehouse?.defaultWarehouse" /></v-col></v-row></v-card-text><v-card-actions class="pa-4"><v-spacer /><v-btn variant="text" @click="warehouseDialog = false">{{ t('common.cancel') }}</v-btn><v-btn color="primary" :loading="saving" :disabled="!warehouseForm.code || !warehouseForm.name" @click="saveWarehouse">{{ t('common.save') }}</v-btn></v-card-actions></v-card></v-dialog>

    <v-dialog v-model="locationsOpen" :fullscreen="!smAndUp" max-width="820"><v-card rounded="xl"><v-toolbar color="transparent"><v-toolbar-title>{{ t('inventory.locationsFor', { name: selectedWarehouse?.name }) }}</v-toolbar-title><v-btn v-if="can('inventory:manage-warehouses')" prepend-icon="mdi-plus" variant="tonal" class="me-2" @click="openLocation()">{{ t('inventory.newLocation') }}</v-btn><v-btn icon="mdi-close" @click="locationsOpen = false" /></v-toolbar><v-card-text><v-list lines="two"><v-list-item v-for="location in locations" :key="location.id" :title="location.name" :subtitle="`${location.code} · ${t(`inventory.values.${location.locationType}`, location.locationType)}`"><template #prepend><v-icon :icon="location.defaultLocation ? 'mdi-star' : 'mdi-map-marker-outline'" :color="location.defaultLocation ? 'primary' : undefined" /></template><template #append><v-chip :color="location.active ? 'success' : 'default'" size="x-small" class="me-2">{{ location.active ? t('common.active') : t('common.inactive') }}</v-chip><v-btn v-if="can('inventory:manage-warehouses')" icon="mdi-pencil-outline" variant="text" @click="openLocation(location)" /><v-btn v-if="can('inventory:manage-warehouses') && !location.defaultLocation" icon="mdi-delete-outline" color="error" variant="text" @click="deleteLocationTarget = location" /></template></v-list-item></v-list><AppEmptyState v-if="!locations.length" icon="mdi-map-marker-outline" :title="t('inventory.noLocations')" /></v-card-text></v-card></v-dialog>

    <v-dialog v-model="locationDialog" :fullscreen="!smAndUp" max-width="620"><v-card rounded="xl"><v-toolbar color="transparent"><v-toolbar-title>{{ editingLocation ? t('inventory.editLocation') : t('inventory.newLocation') }}</v-toolbar-title><v-btn icon="mdi-close" @click="locationDialog = false" /></v-toolbar><v-card-text><v-row><v-col cols="12" sm="4"><v-text-field v-model="locationForm.code" :label="t('inventory.code')" /></v-col><v-col cols="12" sm="8"><v-text-field v-model="locationForm.name" :label="t('inventory.name')" /></v-col><v-col cols="12"><v-select v-model="locationForm.locationType" :items="locationTypes" :item-title="(value: string) => t(`inventory.values.${value}`, value)" :label="t('inventory.locationType')" /></v-col><v-col cols="12"><v-textarea v-model="locationForm.description" :label="t('inventory.description')" rows="2" /></v-col><v-col cols="6"><v-switch v-model="locationForm.defaultLocation" color="primary" :label="t('inventory.defaultLocation')" :disabled="editingLocation?.defaultLocation" /></v-col><v-col cols="6"><v-switch v-model="locationForm.active" color="success" :label="t('common.active')" :disabled="editingLocation?.defaultLocation" /></v-col></v-row></v-card-text><v-card-actions class="pa-4"><v-spacer /><v-btn variant="text" @click="locationDialog = false">{{ t('common.cancel') }}</v-btn><v-btn color="primary" :loading="saving" :disabled="!locationForm.code || !locationForm.name" @click="saveLocation">{{ t('common.save') }}</v-btn></v-card-actions></v-card></v-dialog>

    <v-dialog :model-value="!!deleteWarehouseTarget" max-width="440" @update:model-value="deleteWarehouseTarget = null"><v-card><v-card-title>{{ t('inventory.deleteWarehouse') }}</v-card-title><v-card-text>{{ t('inventory.deleteWarehouseConfirm', { name: deleteWarehouseTarget?.name }) }}</v-card-text><v-card-actions><v-spacer /><v-btn variant="text" @click="deleteWarehouseTarget = null">{{ t('common.cancel') }}</v-btn><v-btn color="error" :loading="saving" @click="removeWarehouse">{{ t('common.delete') }}</v-btn></v-card-actions></v-card></v-dialog>
    <v-dialog :model-value="!!deleteLocationTarget" max-width="440" @update:model-value="deleteLocationTarget = null"><v-card><v-card-title>{{ t('inventory.deleteLocation') }}</v-card-title><v-card-text>{{ t('inventory.deleteLocationConfirm', { name: deleteLocationTarget?.name }) }}</v-card-text><v-card-actions><v-spacer /><v-btn variant="text" @click="deleteLocationTarget = null">{{ t('common.cancel') }}</v-btn><v-btn color="error" :loading="saving" @click="removeLocation">{{ t('common.delete') }}</v-btn></v-card-actions></v-card></v-dialog>
  </section>
</template>

<style scoped>@media (max-width: 599px) { :deep(.v-btn) { min-height: 44px; } }</style>
