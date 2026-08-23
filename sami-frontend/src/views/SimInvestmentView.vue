<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDisplay } from 'vuetify'
import { simInvestmentApi } from '@/api/simInvestment'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import { useNotifications } from '@/composables/useNotifications'
import { usePermission } from '@/composables/usePermission'
import AppMobileRecordCard from '@/components/AppMobileRecordCard.vue'
import AppPageHeader from '@/components/AppPageHeader.vue'
import AppPersianDatePicker from '@/components/AppPersianDatePicker.vue'
import type { SimAnalysis, SimImportBatch, SimImportMessage, SimInvestmentOverview } from '@/types/simInvestment'

const { t } = useI18n()
const { smAndUp } = useDisplay()
const { can } = usePermission()
const { formatDate, formatDateTime, formatMoney, formatNumber } = useFormat()
const apiError = useApiError()
const notifications = useNotifications()
const tab = ref('overview')
const loading = ref(false)
const importing = ref(false)
const recalculating = ref(false)
const importDialog = ref(false)
const detailDialog = ref(false)
const messagesDialog = ref(false)
const overview = ref<SimInvestmentOverview>()
const numbers = ref<SimAnalysis[]>([])
const opportunities = ref<SimAnalysis[]>([])
const imports = ref<SimImportBatch[]>([])
const messages = ref<SimImportMessage[]>([])
const selected = ref<Record<string, any>>()
const page = ref(1)
const totalPages = ref(0)
const totalElements = ref(0)
const filters = reactive({ search: '', numberClass: '', confidence: '', liquidity: '' })
const importForm = reactive<{ file: File | null; sourceCode: string; observedOn: string | null; fullSnapshot: boolean }>({ file: null, sourceCode: '0912_MARKET', observedOn: null, fullSnapshot: true })
const classes = ['ORDINARY', 'SEMI_ROUND', 'ROUND', 'SPECIAL', 'VIP']
const levels = ['LOW', 'MEDIUM', 'HIGH']
const headers = computed(() => [
  { title: t('simInvestment.phone'), key: 'normalized_phone' }, { title: t('simInvestment.class'), key: 'number_class' },
  { title: t('simInvestment.rondiScore'), key: 'rondi_score' }, { title: t('simInvestment.askingPrice'), key: 'current_asking_price' },
  { title: t('simInvestment.marketValue'), key: 'market_median' }, { title: t('simInvestment.confidence'), key: 'confidence' },
  { title: t('simInvestment.investmentScore'), key: 'investment_score' }, { title: t('common.actions'), key: 'actions', sortable: false },
])
const metrics = computed(() => overview.value ? [
  { key: 'analyzed', value: formatNumber(overview.value.analyzed_numbers), icon: 'mdi-phone-search', color: 'primary' },
  { key: 'activeListings', value: formatNumber(overview.value.activeListings), icon: 'mdi-store-search', color: 'info' },
  { key: 'portfolioValue', value: formatMoney(overview.value.estimated_portfolio_value), icon: 'mdi-wallet', color: 'success' },
  { key: 'potentialProfit', value: formatMoney(overview.value.potential_portfolio_profit), icon: 'mdi-trending-up', color: 'warning' },
] : [])

function label(value?: string | null) { return value ? t(`simInvestment.values.${value}`) : '—' }
function levelColor(value?: string) { return value === 'HIGH' ? 'success' : value === 'MEDIUM' ? 'warning' : 'error' }
function scoreColor(value: number) { return value >= 70 ? 'success' : value >= 45 ? 'warning' : 'secondary' }

async function loadOverview() {
  overview.value = await simInvestmentApi.overview()
  opportunities.value = await simInvestmentApi.opportunities(20)
}
async function loadNumbers() {
  const result = await simInvestmentApi.numbers({ page: page.value - 1, size: 25, search: filters.search || undefined, numberClass: filters.numberClass || undefined, confidence: filters.confidence || undefined, liquidity: filters.liquidity || undefined })
  numbers.value = result.content; totalPages.value = result.totalPages; totalElements.value = result.totalElements
}
async function loadImports() { if (can('sim-investment:view-history')) imports.value = await simInvestmentApi.imports() }
async function loadAll() {
  loading.value = true; apiError.clear()
  try { await Promise.all([loadOverview(), loadNumbers(), loadImports()]) }
  catch (error) { apiError.set(error) }
  finally { loading.value = false }
}
async function applyFilters() { page.value = 1; loading.value = true; apiError.clear(); try { await loadNumbers() } catch (error) { apiError.set(error) } finally { loading.value = false } }
async function changePage() { loading.value = true; try { await loadNumbers() } catch (error) { apiError.set(error) } finally { loading.value = false } }
async function openDetail(item: SimAnalysis) { apiError.clear(); try { selected.value = await simInvestmentApi.detail(item.normalized_phone); detailDialog.value = true } catch (error) { apiError.set(error) } }
async function runImport() {
  if (!importForm.file) return
  importing.value = true; apiError.clear()
  try {
    const result = await simInvestmentApi.importFile(importForm.file, importForm.sourceCode, importForm.observedOn, importForm.fullSnapshot)
    importDialog.value = false; importForm.file = null; await loadAll()
    notifications.success(t('simInvestment.importSuccess', { count: result.imported_count }))
  } catch (error) { apiError.set(error) }
  finally { importing.value = false }
}
async function recalculate() {
  recalculating.value = true; apiError.clear()
  try { await simInvestmentApi.recalculate(); await loadAll(); notifications.success(t('simInvestment.recalculated')) }
  catch (error) { apiError.set(error) }
  finally { recalculating.value = false }
}
async function showMessages(batch: SimImportBatch) { try { messages.value = await simInvestmentApi.importMessages(batch.id); messagesDialog.value = true } catch (error) { apiError.set(error) } }
onMounted(loadAll)
</script>

<template>
  <div class="sim-investment-page">
    <AppPageHeader icon="mdi-chart-line-variant" :eyebrow="t('simInvestment.eyebrow')" :title="t('simInvestment.title')" :subtitle="t('simInvestment.subtitle')">
      <template #actions>
        <v-btn v-if="can('sim-investment:recalculate')" variant="tonal" prepend-icon="mdi-calculator-variant" :loading="recalculating" @click="recalculate">{{ t('simInvestment.recalculate') }}</v-btn>
        <v-btn v-if="can('sim-investment:import')" color="primary" prepend-icon="mdi-file-upload-outline" @click="importDialog = true">{{ t('simInvestment.importSnapshot') }}</v-btn>
      </template>
    </AppPageHeader>
    <v-alert v-if="apiError.message.value" type="error" variant="tonal" closable class="mb-4" @click:close="apiError.clear()">{{ apiError.message.value }}</v-alert>
    <v-alert v-if="overview?.low_confidence" type="warning" variant="tonal" class="mb-4">{{ t('simInvestment.lowConfidenceWarning', { count: formatNumber(overview.low_confidence) }) }}</v-alert>

    <v-tabs v-model="tab" show-arrows class="mb-4">
      <v-tab value="overview">{{ t('simInvestment.tabs.overview') }}</v-tab><v-tab value="numbers">{{ t('simInvestment.tabs.numbers') }}</v-tab>
      <v-tab value="opportunities">{{ t('simInvestment.tabs.opportunities') }}</v-tab><v-tab v-if="can('sim-investment:view-history')" value="imports">{{ t('simInvestment.tabs.imports') }}</v-tab>
    </v-tabs>
    <v-window v-model="tab">
      <v-window-item value="overview">
        <v-row><v-col v-for="metric in metrics" :key="metric.key" cols="6" lg="3"><v-card rounded="xl" class="metric-card h-100"><v-card-text><v-avatar :color="metric.color" variant="tonal" rounded="lg"><v-icon :icon="metric.icon" /></v-avatar><div class="text-h5 mt-3 text-break">{{ metric.value }}</div><div class="text-caption text-medium-emphasis">{{ t(`simInvestment.metrics.${metric.key}`) }}</div></v-card-text></v-card></v-col></v-row>
        <v-row class="mt-1"><v-col cols="12" md="6"><v-card rounded="xl" class="h-100"><v-card-title>{{ t('simInvestment.classDistribution') }}</v-card-title><v-card-text><div v-for="item in overview?.classDistribution" :key="item.label" class="distribution-row"><span>{{ label(item.label) }}</span><strong>{{ formatNumber(item.value) }}</strong></div></v-card-text></v-card></v-col><v-col cols="12" md="6"><v-card rounded="xl" class="h-100"><v-card-title>{{ t('simInvestment.marketHealth') }}</v-card-title><v-card-text><div class="distribution-row"><span>{{ t('simInvestment.strongOpportunities') }}</span><strong>{{ formatNumber(overview?.strong_opportunities ?? 0) }}</strong></div><div class="distribution-row"><span>{{ t('simInvestment.highLiquidity') }}</span><strong>{{ formatNumber(overview?.high_liquidity ?? 0) }}</strong></div><div class="distribution-row"><span>{{ t('simInvestment.lastCalculation') }}</span><strong>{{ formatDateTime(overview?.calculated_at) || '—' }}</strong></div></v-card-text></v-card></v-col></v-row>
      </v-window-item>

      <v-window-item value="numbers">
        <v-card rounded="xl" class="mb-4">
          <v-card-text>
            <v-form @submit.prevent="applyFilters">
              <v-row>
                <v-col class="sim-investment-filter-field" cols="12" md="4"><v-text-field v-model="filters.search" :label="t('simInvestment.searchPhone')" prepend-inner-icon="mdi-magnify" clearable /></v-col>
                <v-col class="sim-investment-filter-field" cols="12" sm="4" md="2"><v-select v-model="filters.numberClass" :items="classes" :item-title="label" :label="t('simInvestment.class')" clearable /></v-col>
                <v-col class="sim-investment-filter-field" cols="12" sm="4" md="2"><v-select v-model="filters.confidence" :items="levels" :item-title="label" :label="t('simInvestment.confidence')" clearable /></v-col>
                <v-col class="sim-investment-filter-field" cols="12" sm="4" md="2"><v-select v-model="filters.liquidity" :items="levels" :item-title="label" :label="t('simInvestment.liquidity')" clearable /></v-col>
                <v-col class="sim-investment-filter-field d-flex align-center" cols="12" md="2"><v-btn block color="primary" type="submit">{{ t('common.apply') }}</v-btn></v-col>
              </v-row>
            </v-form>
          </v-card-text>
        </v-card>
        <v-card v-if="smAndUp" rounded="xl"><v-data-table :headers="headers" :items="numbers" :loading="loading" hide-default-footer><template #item.number_class="{ item }"><v-chip size="small" variant="tonal">{{ label(item.number_class) }}</v-chip></template><template #item.rondi_score="{ item }"><v-progress-linear :model-value="item.rondi_score" :color="scoreColor(item.rondi_score)" height="20" rounded><strong>{{ item.rondi_score }}</strong></v-progress-linear></template><template #item.current_asking_price="{ item }">{{ formatMoney(item.current_asking_price) || '—' }}</template><template #item.market_median="{ item }">{{ item.confidence === 'LOW' ? t('simInvestment.insufficientData') : formatMoney(item.market_median) }}</template><template #item.confidence="{ item }"><v-chip :color="levelColor(item.confidence)" size="small">{{ label(item.confidence) }} · {{ formatNumber(item.sample_count) }}</v-chip></template><template #item.investment_score="{ item }"><v-chip :color="scoreColor(item.investment_score)">{{ item.investment_score }}</v-chip></template><template #item.actions="{ item }"><v-btn icon="mdi-eye-outline" variant="text" :aria-label="t('common.showDetails')" @click="openDetail(item)" /></template></v-data-table></v-card>
        <div v-else class="d-grid ga-3"><AppMobileRecordCard v-for="item in numbers" :key="item.id" :label="item.normalized_phone" @open="openDetail(item)"><div class="d-flex align-center ga-3"><v-avatar color="primary" variant="tonal"><v-icon>mdi-sim</v-icon></v-avatar><div class="min-width-0"><strong dir="ltr">{{ item.normalized_phone }}</strong><div class="text-caption">{{ label(item.number_class) }} · {{ t('simInvestment.scoreShort', { score: item.investment_score }) }}</div></div></div><template #details><div>{{ t('simInvestment.askingPrice') }}: {{ formatMoney(item.current_asking_price) || '—' }}</div><div>{{ t('simInvestment.marketValue') }}: {{ item.confidence === 'LOW' ? t('simInvestment.insufficientData') : formatMoney(item.market_median) }}</div><div>{{ t('simInvestment.confidence') }}: {{ label(item.confidence) }} ({{ formatNumber(item.sample_count) }})</div></template><template #actions><v-btn icon="mdi-eye-outline" color="primary" variant="tonal" :aria-label="t('common.showDetails')" @click.stop="openDetail(item)" /></template></AppMobileRecordCard></div>
        <div class="d-flex flex-wrap align-center justify-space-between ga-3 mt-4"><span class="text-caption">{{ t('simInvestment.resultCount', { count: formatNumber(totalElements) }) }}</span><v-pagination v-if="totalPages > 1" v-model="page" :length="totalPages" :total-visible="smAndUp ? 7 : 3" @update:model-value="changePage" /></div>
      </v-window-item>

      <v-window-item value="opportunities"><v-alert type="info" variant="tonal" class="mb-4">{{ t('simInvestment.opportunityPolicy') }}</v-alert><v-row><v-col v-for="item in opportunities" :key="item.id" cols="12" md="6" lg="4"><v-card rounded="xl" class="h-100 opportunity-card" @click="openDetail(item)"><v-card-text><div class="d-flex justify-space-between align-center"><strong class="text-h6" dir="ltr">{{ item.normalized_phone }}</strong><v-chip :color="scoreColor(item.investment_score)">{{ item.investment_score }}</v-chip></div><div class="mt-3">{{ label(item.number_class) }} · {{ t('simInvestment.rondiScore') }} {{ item.rondi_score }}</div><div class="mt-2">{{ t('simInvestment.askingPrice') }}: <strong>{{ formatMoney(item.current_asking_price) }}</strong></div><div>{{ t('simInvestment.potentialProfit') }}: <strong>{{ formatMoney(item.potential_profit) }}</strong></div><v-chip class="mt-3" size="small" :color="levelColor(item.confidence)">{{ label(item.confidence) }} · {{ item.sample_count }} {{ t('simInvestment.samples') }}</v-chip></v-card-text></v-card></v-col></v-row></v-window-item>

      <v-window-item value="imports"><v-card rounded="xl"><v-list lines="three"><v-list-item v-for="batch in imports" :key="batch.id" :title="batch.original_filename" :subtitle="`${formatDate(batch.observed_on)} · ${formatNumber(batch.imported_count)} ${t('simInvestment.rowsImported')}`"><template #prepend><v-avatar color="primary" variant="tonal"><v-icon>mdi-file-table-outline</v-icon></v-avatar></template><template #append><div class="text-end"><v-chip size="small" variant="tonal">{{ label(batch.status) }}</v-chip><v-btn v-if="batch.warning_count || batch.error_count" class="ms-2" size="small" variant="text" @click="showMessages(batch)">{{ t('simInvestment.messages') }}</v-btn></div></template></v-list-item><v-list-item v-if="!imports.length" :title="t('simInvestment.noImports')" /></v-list></v-card></v-window-item>
    </v-window>

    <v-dialog v-model="importDialog" :fullscreen="!smAndUp" max-width="700"><v-card><v-card-title>{{ t('simInvestment.importSnapshot') }}</v-card-title><v-card-text><div class="sim-investment-import-fields"><v-file-input v-model="importForm.file" accept=".csv,.xlsx,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" :label="t('simInvestment.chooseFile')" prepend-icon="mdi-file-table-outline" :hint="t('simInvestment.fileHint')" persistent-hint /><v-text-field v-model="importForm.sourceCode" :label="t('simInvestment.sourceCode')" /><AppPersianDatePicker v-model="importForm.observedOn" :label="t('simInvestment.observedOn')" clearable /><v-switch v-model="importForm.fullSnapshot" :label="t('simInvestment.fullSnapshot')" :hint="t('simInvestment.fullSnapshotHint')" persistent-hint /><v-alert type="info" variant="tonal">{{ t('simInvestment.importSafety') }}</v-alert></div></v-card-text><v-card-actions><v-spacer /><v-btn @click="importDialog = false">{{ t('common.cancel') }}</v-btn><v-btn color="primary" :disabled="!importForm.file" :loading="importing" @click="runImport">{{ t('common.import') }}</v-btn></v-card-actions></v-card></v-dialog>

    <v-dialog v-model="detailDialog" :fullscreen="!smAndUp" max-width="900"><v-card><v-card-title class="d-flex align-center"><span dir="ltr">{{ selected?.normalized_phone }}</span><v-spacer /><v-btn icon="mdi-close" variant="text" @click="detailDialog = false" /></v-card-title><v-card-text v-if="selected"><v-row><v-col v-for="field in ['number_class','rondi_score','investment_score','confidence','liquidity','sample_count']" :key="field" cols="6" md="4"><div class="detail-field"><span>{{ t(`simInvestment.fields.${field}`) }}</span><strong>{{ ['number_class','confidence','liquidity'].includes(field) ? label(selected[field]) : formatNumber(selected[field]) }}</strong></div></v-col></v-row><v-divider class="my-4" /><v-row><v-col v-for="field in ['best_buy_price','normal_buy_price','normal_sell_price','best_sell_price','actual_buy_price','actual_sell_price','potential_profit']" :key="field" cols="12" sm="6" md="4"><div class="detail-field"><span>{{ t(`simInvestment.fields.${field}`) }}</span><strong>{{ formatMoney(selected[field]) || '—' }}</strong></div></v-col></v-row><v-alert v-if="selected.confidence === 'LOW'" type="warning" variant="tonal" class="mt-4">{{ t('simInvestment.noCertainPrice') }}</v-alert><div class="mt-4"><strong>{{ t('simInvestment.patterns') }}:</strong> {{ (selected.rondi_patterns || []).map(label).join('، ') || '—' }}</div><div v-if="selected.block_group" class="mt-2"><strong>{{ t('simInvestment.block') }}:</strong> <span dir="ltr">{{ selected.block_group }}</span></div></v-card-text></v-card></v-dialog>
    <v-dialog v-model="messagesDialog" max-width="800"><v-card><v-card-title>{{ t('simInvestment.messages') }}</v-card-title><v-card-text><v-list><v-list-item v-for="(message, index) in messages" :key="index" :title="label(message.code)" :subtitle="message.source_row_number ? t('simInvestment.rowMessage', { row: message.source_row_number }) : message.message"><template #prepend><v-icon :color="message.severity === 'ERROR' ? 'error' : 'warning'">mdi-alert-circle-outline</v-icon></template></v-list-item></v-list></v-card-text></v-card></v-dialog>
  </div>
</template>

<style scoped>
.sim-investment-page{max-width:1600px;min-width:0;margin:auto}.metric-card,.opportunity-card{transition:transform var(--app-dur) var(--app-ease)}.opportunity-card{cursor:pointer}.opportunity-card:hover{transform:translateY(-2px)}.distribution-row,.detail-field{display:flex;align-items:center;justify-content:space-between;gap:var(--app-space-3);padding:var(--app-space-3) 0;border-bottom:1px solid var(--app-border)}.detail-field{height:100%;flex-direction:column;align-items:flex-start;border:1px solid var(--app-border);border-radius:var(--app-radius-lg);padding:var(--app-space-3)}.sim-investment-filter-field{padding-block:0}.sim-investment-import-fields{display:grid;gap:var(--app-form-field-gap)}.d-grid{display:grid}.min-width-0{min-width:0}@media(max-width:600px){.sim-investment-page{padding-bottom:calc(88px + env(safe-area-inset-bottom))}.opportunity-card:hover{transform:none}}
</style>
