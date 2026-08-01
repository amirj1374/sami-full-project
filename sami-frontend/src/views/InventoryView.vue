<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { inventoryApi } from '@/api/inventory'
import AppErrorState from '@/components/AppErrorState.vue'
import AppLoadingState from '@/components/AppLoadingState.vue'
import AppPageHeader from '@/components/AppPageHeader.vue'
import InventoryBalancesPanel from '@/components/inventory/InventoryBalancesPanel.vue'
import InventoryCountsPanel from '@/components/inventory/InventoryCountsPanel.vue'
import InventoryMonitoringPanel from '@/components/inventory/InventoryMonitoringPanel.vue'
import InventoryReportsPanel from '@/components/inventory/InventoryReportsPanel.vue'
import InventoryTransfersPanel from '@/components/inventory/InventoryTransfersPanel.vue'
import InventoryWarehousesPanel from '@/components/inventory/InventoryWarehousesPanel.vue'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import { usePermission } from '@/composables/usePermission'
import type { InventoryDashboard, Warehouse } from '@/types/inventory'

const { t } = useI18n()
const { formatNumber } = useFormat()
const { can } = usePermission()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()
const loading = ref(false)
const tab = ref('balances')
const warehouses = ref<Warehouse[]>([])
const summary = ref<InventoryDashboard | null>(null)

const metrics = computed(() => summary.value ? [
  { key: 'inventoryValue', value: summary.value.inventoryValue, icon: 'mdi-cash-multiple', color: 'primary' },
  { key: 'totalOnHand', value: summary.value.totalOnHand, icon: 'mdi-package-variant-closed', color: 'info' },
  { key: 'totalAvailable', value: summary.value.totalAvailable, icon: 'mdi-package-variant', color: 'success' },
  { key: 'lowStockCount', value: summary.value.lowStockCount, icon: 'mdi-alert-outline', color: 'warning' },
] : [])

const tabs = computed(() => [
  { value: 'balances', label: t('inventory.balances'), icon: 'mdi-package-variant' },
  { value: 'transfers', label: t('inventory.transfers'), icon: 'mdi-truck-fast-outline', allowed: can('inventory:transfer') },
  { value: 'counts', label: t('inventory.stockCounts'), icon: 'mdi-clipboard-list-outline', allowed: can('inventory:count') },
  { value: 'monitoring', label: t('inventory.monitoring'), icon: 'mdi-monitor-dashboard' },
  { value: 'warehouses', label: t('inventory.warehouses'), icon: 'mdi-warehouse', allowed: can('inventory:manage-warehouses') },
  { value: 'reports', label: t('inventory.reports'), icon: 'mdi-chart-box-outline', allowed: can('inventory:report') },
].filter((item) => item.allowed !== false))

async function load(): Promise<void> {
  loading.value = true
  clearError()
  try {
    ;[summary.value, warehouses.value] = await Promise.all([
      inventoryApi.dashboard(),
      inventoryApi.warehouses(),
    ])
  } catch (error) {
    setError(error)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="inventory-page">
    <AppPageHeader
      icon="mdi-warehouse"
      :eyebrow="t('inventory.eyebrow')"
      :title="t('inventory.title')"
      :subtitle="t('inventory.subtitle')"
    >
      <template #actions>
        <v-btn variant="tonal" prepend-icon="mdi-refresh" :loading="loading" @click="load">
          {{ t('common.refresh') }}
        </v-btn>
      </template>
    </AppPageHeader>

    <AppErrorState v-if="errorMessage" class="mb-4" :title="errorMessage" :retry-label="t('common.retry')" @retry="load" />
    <AppLoadingState v-if="loading && !summary" variant="cards" :rows="4" :label="t('common.loading')" />

    <template v-else>
      <v-row v-if="summary" class="mb-2">
        <v-col v-for="metric in metrics" :key="metric.key" cols="6" lg="3">
          <v-card rounded="xl" class="inventory-metric h-100">
            <v-card-text>
              <div class="d-flex align-start ga-3">
                <v-avatar :color="metric.color" variant="tonal" rounded="lg"><v-icon :icon="metric.icon" /></v-avatar>
                <div class="min-width-0">
                  <div class="text-caption text-medium-emphasis">{{ t(`inventory.${metric.key}`) }}</div>
                  <div class="text-h6 text-sm-h5 font-weight-bold mt-1">{{ formatNumber(metric.value) }}</div>
                </div>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <v-card rounded="xl" class="inventory-tabs mb-5">
        <v-tabs v-model="tab" color="primary" show-arrows density="comfortable">
          <v-tab v-for="item in tabs" :key="item.value" :value="item.value" :prepend-icon="item.icon">
            {{ item.label }}
          </v-tab>
        </v-tabs>
      </v-card>

      <InventoryBalancesPanel v-if="tab === 'balances'" :warehouses="warehouses" @changed="load" />
      <InventoryTransfersPanel v-else-if="tab === 'transfers' && can('inventory:transfer')" :warehouses="warehouses" @changed="load" />
      <InventoryCountsPanel v-else-if="tab === 'counts' && can('inventory:count')" :warehouses="warehouses" @changed="load" />
      <InventoryMonitoringPanel v-else-if="tab === 'monitoring'" :warehouses="warehouses" @changed="load" />
      <InventoryWarehousesPanel v-else-if="tab === 'warehouses' && can('inventory:manage-warehouses')" :warehouses="warehouses" @changed="load" />
      <InventoryReportsPanel v-else-if="tab === 'reports' && can('inventory:report')" />
    </template>
  </div>
</template>

<style scoped>
.inventory-page { min-width: 0; }
.inventory-metric { border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity)); }
.inventory-tabs { overflow: hidden; border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity)); }
@media (max-width: 599px) {
  .inventory-metric :deep(.v-card-text) { padding: 14px; }
  .inventory-metric :deep(.v-avatar) { width: 36px !important; height: 36px !important; }
  .inventory-tabs :deep(.v-tab) { min-width: 112px; }
}
</style>
