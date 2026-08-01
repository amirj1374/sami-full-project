<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { inventoryApi } from '@/api/inventory'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import { usePermission } from '@/composables/usePermission'
import AppEmptyState from '@/components/AppEmptyState.vue'
import type { InventoryAudit, InventoryReport } from '@/types/inventory'

const { t } = useI18n()
const { formatDateTime, formatNumber } = useFormat()
const { can } = usePermission()
const error = useApiError()
const loading = ref(false)
const report = ref<InventoryReport | null>(null)
const audits = ref<InventoryAudit[]>([])
const filters = reactive({ from: '', to: '' })
const windowRef = globalThis.window

async function load() {
  loading.value = true; error.clear()
  try {
    const [reportResult, auditResult] = await Promise.all([
      inventoryApi.report({ from: filters.from ? new Date(`${filters.from}T00:00:00`).toISOString() : undefined, to: filters.to ? new Date(`${filters.to}T23:59:59.999`).toISOString() : undefined }),
      can('inventory:view-audit') ? inventoryApi.audit({ limit: 100 }) : Promise.resolve([]),
    ])
    report.value = reportResult; audits.value = auditResult
  } catch (reason) { error.set(reason) } finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <section aria-labelledby="inventory-reports-title">
    <div class="d-flex align-center flex-wrap ga-2 mb-4"><div><h2 id="inventory-reports-title" class="text-h6">{{ t('inventory.reports') }}</h2><p class="text-body-2 text-medium-emphasis mb-0">{{ t('inventory.reportsSubtitle') }}</p></div><v-spacer /><v-btn variant="tonal" prepend-icon="mdi-printer-outline" @click="windowRef.print()">{{ t('inventory.printReport') }}</v-btn></div>
    <v-alert v-if="error.message.value" type="error" variant="tonal" closable class="mb-4" @click:close="error.clear()">{{ error.message.value }}</v-alert>
    <v-card rounded="xl" class="app-data-surface pa-4 mb-4"><v-row dense><v-col cols="6" md="3"><v-text-field v-model="filters.from" type="date" :label="t('inventory.from')" hide-details /></v-col><v-col cols="6" md="3"><v-text-field v-model="filters.to" type="date" :label="t('inventory.to')" hide-details /></v-col><v-col cols="12" md="3"><v-btn block color="primary" :loading="loading" @click="load">{{ t('common.apply') }}</v-btn></v-col></v-row></v-card>
    <v-skeleton-loader v-if="loading && !report" type="card@3" />
    <template v-else-if="report">
      <v-row class="mb-2"><v-col v-for="metric in [{ key: 'inventoryValue', value: formatNumber(report.summary.inventoryValue) }, { key: 'totalOnHand', value: formatNumber(report.summary.totalOnHand) }, { key: 'totalAvailable', value: formatNumber(report.summary.totalAvailable) }, { key: 'lowStockCount', value: formatNumber(report.summary.lowStockCount) }]" :key="metric.key" cols="6" md="3"><v-card rounded="xl" class="pa-4 h-100"><div class="text-caption text-medium-emphasis">{{ t(`inventory.${metric.key}`) }}</div><div class="text-h6 font-weight-bold mt-2">{{ metric.value }}</div></v-card></v-col></v-row>
      <v-row><v-col cols="12" md="6"><v-card rounded="xl" class="h-100 app-data-surface"><v-card-title>{{ t('inventory.valueByWarehouse') }}</v-card-title><v-list v-if="report.warehouseValues.length"><v-list-item v-for="row in report.warehouseValues" :key="row.label" :title="row.label" :subtitle="t('inventory.productCountValue', { count: row.count })"><template #append><strong>{{ formatNumber(row.value) }}</strong></template></v-list-item></v-list><AppEmptyState v-else icon="mdi-warehouse" :title="t('inventory.noReportData')" /></v-card></v-col><v-col cols="12" md="6"><v-card rounded="xl" class="h-100 app-data-surface"><v-card-title>{{ t('inventory.movementSummary') }}</v-card-title><v-list v-if="report.movementTypes.length"><v-list-item v-for="row in report.movementTypes" :key="row.label" :title="t(`inventory.values.${row.label}`, row.label)" :subtitle="t('inventory.recordCountValue', { count: row.count })"><template #append><strong>{{ formatNumber(row.value) }}</strong></template></v-list-item></v-list><AppEmptyState v-else icon="mdi-swap-vertical" :title="t('inventory.noReportData')" /></v-card></v-col></v-row>
      <v-card rounded="xl" class="app-data-surface mt-4"><v-card-title>{{ t('inventory.lowStock') }}</v-card-title><v-list v-if="report.lowStock.length" lines="two"><v-list-item v-for="row in report.lowStock" :key="row.id" :title="row.productName" :subtitle="`${row.sku} · ${row.warehouseName} · ${row.locationName}`"><template #append><v-chip color="warning" size="small">{{ formatNumber(row.available) }}</v-chip></template></v-list-item></v-list><AppEmptyState v-else icon="mdi-check-circle-outline" :title="t('inventory.noLowStock')" /></v-card>
    </template>
    <v-card v-if="can('inventory:view-audit')" rounded="xl" class="app-data-surface mt-4"><v-card-title>{{ t('inventory.auditTrail') }}</v-card-title><v-timeline v-if="audits.length" side="end" density="compact" truncate-line="both"><v-timeline-item v-for="item in audits" :key="item.id" size="x-small" dot-color="primary"><div class="d-flex align-center flex-wrap ga-2"><strong>{{ t(`inventory.values.${item.action}`, item.action) }}</strong><v-chip size="x-small" variant="tonal">{{ item.entityType }}<span v-if="item.entityId"> #{{ item.entityId }}</span></v-chip></div><div class="text-caption text-medium-emphasis">{{ item.actorEmail || t('inventory.systemActor') }} · {{ formatDateTime(item.createdAt) }}</div></v-timeline-item></v-timeline><AppEmptyState v-else icon="mdi-history" :title="t('inventory.noAudit')" /></v-card>
  </section>
</template>

<style scoped>@media print { button, .v-card:first-of-type { display: none !important; } } @media (max-width: 599px) { :deep(.v-btn) { min-height: 44px; } }</style>
