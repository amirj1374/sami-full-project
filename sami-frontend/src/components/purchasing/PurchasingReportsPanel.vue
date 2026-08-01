<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { purchasesApi } from '@/api/purchases'
import AppEmptyState from '@/components/AppEmptyState.vue'
import AppErrorState from '@/components/AppErrorState.vue'
import AppLoadingState from '@/components/AppLoadingState.vue'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import type { PurchaseReport, PurchaseReportGroup } from '@/types/models'

const emit = defineEmits<{ open: [purchaseId: number] }>()
const { t } = useI18n()
const { formatDate, formatNumber } = useFormat()
const error = useApiError()
const loading = ref(false)
const report = ref<PurchaseReport | null>(null)
const filters = reactive({ createdFrom: '', createdTo: '' })

async function load() {
  loading.value = true
  error.clear()
  try {
    report.value = await purchasesApi.report({
      createdFrom: filters.createdFrom || undefined,
      createdTo: filters.createdTo || undefined,
    })
  } catch (reason) {
    error.set(reason)
  } finally {
    loading.value = false
  }
}

function amount(group: PurchaseReportGroup) {
  return formatNumber(group.amount)
}

onMounted(load)
</script>

<template>
  <section aria-labelledby="purchasing-reports-title">
    <div class="d-flex align-center flex-wrap ga-2 mb-4">
      <div>
        <h2 id="purchasing-reports-title" class="text-h6">{{ t('purchases.reports.title') }}</h2>
        <p class="text-body-2 text-medium-emphasis mb-0">{{ t('purchases.reports.subtitle') }}</p>
      </div>
      <v-spacer />
      <v-btn variant="tonal" prepend-icon="mdi-refresh" :loading="loading" @click="load">
        {{ t('common.refresh') }}
      </v-btn>
    </div>

    <AppErrorState
      v-if="error.message.value"
      class="mb-4"
      :title="error.message.value"
      :retry-label="t('common.retry')"
      @retry="load"
    />

    <v-card rounded="xl" class="app-data-surface pa-4 mb-4">
      <v-row dense align="center">
        <v-col cols="6" md="3">
          <v-text-field v-model="filters.createdFrom" type="date" :label="t('purchases.filters.from')" hide-details />
        </v-col>
        <v-col cols="6" md="3">
          <v-text-field v-model="filters.createdTo" type="date" :label="t('purchases.filters.to')" hide-details />
        </v-col>
        <v-col cols="12" md="3">
          <v-btn block color="primary" :loading="loading" @click="load">{{ t('common.apply') }}</v-btn>
        </v-col>
      </v-row>
    </v-card>

    <AppLoadingState v-if="loading && !report" variant="cards" :rows="3" :label="t('common.loading')" />
    <template v-else-if="report">
      <v-row>
        <v-col
          v-for="section in [
            { key: 'status', title: t('purchases.reports.byStatus'), rows: report.byStatus },
            { key: 'type', title: t('purchases.reports.byType'), rows: report.byType },
            { key: 'supplier', title: t('purchases.reports.bySupplier'), rows: report.bySupplier },
          ]"
          :key="section.key"
          cols="12"
          lg="4"
        >
          <v-card rounded="xl" class="app-data-surface h-100">
            <v-card-title class="text-subtitle-1">{{ section.title }}</v-card-title>
            <v-list v-if="section.rows.length" density="comfortable">
              <v-list-item v-for="row in section.rows" :key="row.key" :title="row.label">
                <v-list-item-subtitle>{{ t('purchases.reports.purchaseCount', { count: row.count }) }}</v-list-item-subtitle>
                <template #append><strong>{{ amount(row) }}</strong></template>
              </v-list-item>
            </v-list>
            <AppEmptyState v-else icon="mdi-chart-box-outline" :title="t('purchases.reports.noData')" />
          </v-card>
        </v-col>
      </v-row>

      <v-card rounded="xl" class="app-data-surface mt-4">
        <v-card-title class="d-flex align-center">
          <span>{{ t('purchases.reports.overdue') }}</span>
          <v-spacer />
          <v-chip color="warning" size="small" variant="tonal">{{ report.overdueLines.length }}</v-chip>
        </v-card-title>
        <v-list v-if="report.overdueLines.length" lines="two">
          <v-list-item
            v-for="line in report.overdueLines"
            :key="line.itemId"
            :title="line.productName"
            :subtitle="`${line.purchaseNumber} · ${line.supplierName} · ${formatDate(line.expectedDelivery)}`"
            @click="emit('open', line.purchaseId)"
          >
            <template #append>
              <v-chip color="warning" size="small" variant="tonal">
                {{ t('purchases.reports.remainingValue', { value: formatNumber(line.remainingQuantity) }) }}
              </v-chip>
            </template>
          </v-list-item>
        </v-list>
        <AppEmptyState v-else icon="mdi-calendar-check-outline" :title="t('purchases.reports.noOverdue')" />
      </v-card>
    </template>
  </section>
</template>

<style scoped>
@media (max-width: 599px) {
  :deep(.v-btn) { min-height: 44px; }
  :deep(.v-list-item__append) { align-self: end; margin-inline-start: 8px; }
}
</style>
