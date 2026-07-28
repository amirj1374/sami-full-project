<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { useFormat } from '@/composables/useFormat'
import { useServerLabel } from '@/composables/useServerLabel'
import { productsApi } from '@/api/products'
import { customersApi } from '@/api/customers'
import { suppliersApi } from '@/api/suppliers'
import { purchasesApi } from '@/api/purchases'
import type { Product, PurchaseRow } from '@/types/models'
import AppPageHeader from '@/components/AppPageHeader.vue'
import AppEmptyState from '@/components/AppEmptyState.vue'

const { t } = useI18n()
const auth = useAuthStore()
const { formatNumber, formatDate } = useFormat()
const { statusLabel } = useServerLabel()

const loading = ref(true)
const counts = ref({ products: 0, activeProducts: 0, customers: 0, suppliers: 0, purchases: 0 })
const purchaseValue = ref(0)
const recentPurchases = ref<PurchaseRow[]>([])
const lowStock = ref<Product[]>([])
const monthly = ref<number[]>([])
const statusDist = ref<{ name: string; value: number; color: string }[]>([])

const DONUT_COLORS = ['primary', 'secondary', 'accent', 'info', 'success', 'warning']

const kpis = computed(() => [
  {
    key: 'products',
    icon: 'mdi-package-variant',
    value: counts.value.products,
    sub: t('dashboard.activeCount', { n: formatNumber(counts.value.activeProducts) }),
  },
  {
    key: 'customers',
    icon: 'mdi-account-group-outline',
    value: counts.value.customers,
    sub: t('dashboard.registered'),
  },
  {
    key: 'suppliers',
    icon: 'mdi-truck-outline',
    value: counts.value.suppliers,
    sub: t('dashboard.activePartners'),
    accent: true,
  },
  {
    key: 'purchases',
    icon: 'mdi-cart-outline',
    value: counts.value.purchases,
    sub: `${formatNumber(purchaseValue.value)} ${t('dashboard.currencyUnit')}`,
  },
])

// --- Purchase-status donut --------------------------------------------------
const R = 42
const C = 2 * Math.PI * R
const donutTotal = computed(() => statusDist.value.reduce((s, d) => s + d.value, 0))
const donutSegments = computed(() => {
  const total = donutTotal.value || 1
  let acc = 0
  return statusDist.value.map((d) => {
    const len = (d.value / total) * C
    const seg = { ...d, dash: `${len} ${C - len}`, offset: -(acc / total) * C }
    acc += d.value
    return seg
  })
})

function buildMonthly(rows: PurchaseRow[]): number[] {
  const now = new Date()
  const buckets: number[] = Array(12).fill(0)
  const keys: string[] = []
  for (let i = 11; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    keys.push(`${d.getFullYear()}-${d.getMonth()}`)
  }
  for (const r of rows) {
    const d = new Date(r.createdAt)
    const idx = keys.indexOf(`${d.getFullYear()}-${d.getMonth()}`)
    if (idx >= 0) buckets[idx] += 1
  }
  return buckets
}

function buildStatusDist(rows: PurchaseRow[]): { name: string; value: number; color: string }[] {
  const map = new Map<string, number>()
  for (const r of rows) map.set(r.status.name, (map.get(r.status.name) ?? 0) + 1)
  return [...map.entries()]
    .sort((a, b) => b[1] - a[1])
    .map(([name, value], i) => ({ name, value, color: DONUT_COLORS[i % DONUT_COLORS.length] }))
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const [products, active, customers, suppliers, purchasePage, low] = await Promise.all([
      productsApi.list({ size: 1 }),
      productsApi.list({ size: 1, active: true }),
      customersApi.list({ size: 1 }),
      suppliersApi.list({ size: 1 }),
      purchasesApi.list({ size: 400, sort: 'createdAt,desc' }),
      productsApi.list({ size: 5, active: true, sort: 'stockQuantity,asc' }),
    ])
    const rows = purchasePage.content
    counts.value = {
      products: products.totalElements,
      activeProducts: active.totalElements,
      customers: customers.totalElements,
      suppliers: suppliers.totalElements,
      purchases: purchasePage.totalElements,
    }
    purchaseValue.value = rows.reduce((s, r) => s + Number(r.totalAmount), 0)
    recentPurchases.value = rows.slice(0, 6)
    monthly.value = buildMonthly(rows)
    statusDist.value = buildStatusDist(rows)
    lowStock.value = low.content
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <AppPageHeader
      icon="mdi-view-dashboard-outline"
      :eyebrow="t('dashboard.overview')"
      :title="t('dashboard.title')"
    >
      <template #actions>
        <v-chip variant="tonal" color="success" size="small" class="font-weight-medium">
          <span class="live-dot me-2" /> {{ t('dashboard.live') }}
        </v-chip>
        <v-btn variant="tonal" prepend-icon="mdi-refresh" :loading="loading" @click="load">
          {{ t('common.refresh') }}
        </v-btn>
      </template>
    </AppPageHeader>

    <p class="text-body-2 text-medium-emphasis mb-5 mt-n3">
      <i18n-t keypath="dashboard.welcome" tag="span">
        <template #name>
          <strong class="text-high-emphasis">{{ auth.user?.fullName }}</strong>
        </template>
      </i18n-t>
    </p>

    <!-- KPI cards -->
    <v-row>
      <v-col v-for="kpi in kpis" :key="kpi.key" cols="12" sm="6" lg="3">
        <v-card rounded="lg" border flat class="kpi-card app-elevate h-100">
          <v-card-text class="d-flex align-center ga-4">
            <span :class="['kpi-icon', { 'kpi-icon--accent': kpi.accent }]">
              <v-icon :icon="kpi.icon" size="26" />
            </span>
            <div class="flex-grow-1 overflow-hidden">
              <div v-if="loading" class="app-skeleton mb-2" style="height: 26px; width: 60%" />
              <div v-else class="text-h5 font-weight-bold">{{ formatNumber(kpi.value) }}</div>
              <div class="text-body-2 text-medium-emphasis">{{ t(`dashboard.kpi.${kpi.key}`) }}</div>
              <div v-if="!loading" class="text-caption text-disabled mt-1 text-truncate">{{ kpi.sub }}</div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Purchases trend + status donut -->
    <v-row class="mt-2">
      <v-col cols="12" lg="8">
        <v-card rounded="lg" border flat class="h-100">
          <v-card-item>
            <v-card-title class="text-subtitle-1 font-weight-bold">{{ t('dashboard.charts.purchasesByMonth') }}</v-card-title>
            <v-card-subtitle>{{ t('dashboard.last12Months') }}</v-card-subtitle>
          </v-card-item>
          <v-card-text>
            <div v-if="loading" class="app-skeleton" style="height: 120px" />
            <v-sparkline
              v-else
              :model-value="monthly"
              type="bar"
              color="primary"
              height="120"
              :padding="6"
              auto-draw
              :auto-draw-duration="900"
            />
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" lg="4">
        <v-card rounded="lg" border flat class="h-100">
          <v-card-item>
            <v-card-title class="text-subtitle-1 font-weight-bold">{{ t('dashboard.charts.purchaseStatus') }}</v-card-title>
          </v-card-item>
          <v-card-text class="d-flex flex-column align-center">
            <div v-if="loading" class="app-skeleton" style="height: 160px; width: 160px; border-radius: 50%" />
            <template v-else>
              <div class="donut-wrap">
                <svg viewBox="0 0 100 100" class="donut">
                  <g transform="rotate(-90 50 50)">
                    <circle cx="50" cy="50" :r="R" fill="none" class="donut-track" stroke-width="11" />
                    <circle
                      v-for="seg in donutSegments"
                      :key="seg.name"
                      cx="50"
                      cy="50"
                      :r="R"
                      fill="none"
                      stroke-width="11"
                      :stroke-dasharray="seg.dash"
                      :stroke-dashoffset="seg.offset"
                      :style="{ stroke: `rgb(var(--v-theme-${seg.color}))` }"
                    />
                  </g>
                </svg>
                <div class="donut-center">
                  <div class="text-h6 font-weight-bold">{{ formatNumber(donutTotal) }}</div>
                  <div class="text-caption text-medium-emphasis">{{ t('dashboard.purchasesLabel') }}</div>
                </div>
              </div>
              <div class="mt-4 w-100 d-flex flex-column ga-2">
                <div v-for="seg in donutSegments" :key="seg.name" class="d-flex align-center justify-space-between">
                  <span class="d-inline-flex align-center text-body-2">
                    <span class="donut-dot" :style="{ background: `rgb(var(--v-theme-${seg.color}))` }" />
                    {{ statusLabel(seg.name) }}
                  </span>
                  <span class="text-body-2 font-weight-medium">{{ formatNumber(seg.value) }}</span>
                </div>
              </div>
            </template>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Recent purchases + low stock -->
    <v-row class="mt-2">
      <v-col cols="12" lg="8">
        <v-card rounded="lg" border flat class="h-100">
          <v-card-item>
            <v-card-title class="text-subtitle-1 font-weight-bold">{{ t('dashboard.recentPurchases') }}</v-card-title>
            <template #append>
              <v-btn variant="text" size="small" append-icon="mdi-arrow-right" :to="'/purchases'">
                {{ t('dashboard.viewAll') }}
              </v-btn>
            </template>
          </v-card-item>
          <v-divider />
          <div v-if="loading" class="pa-4 d-flex flex-column ga-3">
            <div v-for="n in 5" :key="n" class="app-skeleton" style="height: 40px" />
          </div>
          <template v-else-if="recentPurchases.length">
            <v-table density="comfortable" class="dash-table">
              <thead>
                <tr>
                  <th>{{ t('dashboard.colNumber') }}</th>
                  <th>{{ t('dashboard.colSupplier') }}</th>
                  <th>{{ t('dashboard.colStatus') }}</th>
                  <th class="text-end">{{ t('dashboard.colTotal') }}</th>
                  <th class="text-end">{{ t('dashboard.date') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in recentPurchases" :key="p.id">
                  <td class="font-weight-medium">{{ p.purchaseNumber }}</td>
                  <td class="text-truncate" style="max-width: 180px">{{ p.supplierName }}</td>
                  <td><v-chip size="x-small" variant="tonal" label>{{ statusLabel(p.status.name) }}</v-chip></td>
                  <td class="text-end font-weight-medium">{{ formatNumber(p.totalAmount) }}</td>
                  <td class="text-end text-medium-emphasis">{{ formatDate(p.createdAt) }}</td>
                </tr>
              </tbody>
            </v-table>
          </template>
          <AppEmptyState
            v-else
            dense
            icon="mdi-cart-outline"
            :title="t('dashboard.noPurchases')"
          />
        </v-card>
      </v-col>

      <v-col cols="12" lg="4">
        <v-card rounded="lg" border flat class="h-100">
          <v-card-item>
            <v-card-title class="text-subtitle-1 font-weight-bold">{{ t('dashboard.lowStock') }}</v-card-title>
            <template #append>
              <v-icon icon="mdi-alert-outline" color="warning" />
            </template>
          </v-card-item>
          <v-divider />
          <div v-if="loading" class="pa-4 d-flex flex-column ga-3">
            <div v-for="n in 4" :key="n" class="app-skeleton" style="height: 44px" />
          </div>
          <template v-else-if="lowStock.length">
            <v-list class="py-0">
              <template v-for="(p, i) in lowStock" :key="p.id">
                <v-list-item>
                  <template #prepend>
                    <v-avatar rounded="lg" size="36" color="surface-variant" class="me-1">
                      <v-icon icon="mdi-package-variant-closed" size="18" />
                    </v-avatar>
                  </template>
                  <v-list-item-title class="text-body-2 font-weight-medium text-truncate">{{ p.name }}</v-list-item-title>
                  <v-list-item-subtitle class="text-caption">{{ p.sku }}</v-list-item-subtitle>
                  <template #append>
                    <v-chip
                      size="x-small"
                      label
                      :color="p.stockQuantity === 0 ? 'error' : 'warning'"
                      variant="tonal"
                    >
                      {{ formatNumber(p.stockQuantity) }}
                    </v-chip>
                  </template>
                </v-list-item>
                <v-divider v-if="i < lowStock.length - 1" />
              </template>
            </v-list>
          </template>
          <AppEmptyState v-else dense icon="mdi-check-circle-outline" :title="t('dashboard.stockHealthy')" />
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<style scoped>
.kpi-icon {
  width: 52px;
  height: 52px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--app-radius);
  color: rgb(var(--v-theme-primary));
  background: linear-gradient(135deg, rgba(var(--v-theme-primary), 0.16), rgba(var(--v-theme-primary), 0.06));
  flex: none;
}
.kpi-icon--accent {
  color: rgb(var(--v-theme-accent));
  background: linear-gradient(135deg, rgba(var(--v-theme-accent), 0.18), rgba(var(--v-theme-accent), 0.06));
}
.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgb(var(--v-theme-success));
  box-shadow: 0 0 0 0 rgba(var(--v-theme-success), 0.5);
  /* A few attention pulses on load, then settle — no perpetual repaint for an
     all-day dashboard. */
  animation: live-pulse 1.8s ease-out 4;
}
@keyframes live-pulse {
  70% {
    box-shadow: 0 0 0 6px rgba(var(--v-theme-success), 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(var(--v-theme-success), 0);
  }
}
.donut-wrap {
  position: relative;
  width: 160px;
  height: 160px;
}
.donut {
  width: 160px;
  height: 160px;
}
.donut-track {
  stroke: rgba(var(--v-theme-on-surface), 0.08);
}
.donut-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.donut-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 3px;
  margin-inline-end: 8px;
}
.dash-table :deep(th) {
  font-size: 0.72rem !important;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: rgba(var(--v-theme-on-surface), 0.55) !important;
}
</style>
