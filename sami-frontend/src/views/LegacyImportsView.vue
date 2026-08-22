<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { legacyImportsApi } from '@/api/legacyImports'
import { buildInfo } from '@/buildInfo'
import { useServerLabel } from '@/composables/useServerLabel'
import type { ApiError } from '@/types/api'
import type { LegacyBatch, LegacyDataset, LegacyMigrationGroup, LegacyReconciliation } from '@/types/legacyImports'

type LegacyStage = 'load' | 'upload' | 'analyze' | 'stage' | 'reconcile'
interface CustomerIssue { code: string; fileName?: string; stage: LegacyStage; reasonKey: string; solutionKey: string }
interface UploadResult { fileName: string; success: boolean; issue?: CustomerIssue }

const { t } = useI18n()
const { enumLabel } = useServerLabel()
const batches = ref<LegacyBatch[]>([])
const groups = ref<LegacyMigrationGroup[]>([])
const selected = ref<LegacyBatch>()
const selectedGroup = ref<LegacyMigrationGroup>()
const datasets = ref<LegacyDataset[]>([])
const reconciliation = ref<LegacyReconciliation>()
const busy = ref(false)
const issue = ref<CustomerIssue>()
const uploadResults = ref<UploadResult[]>([])
const tab = ref('overview')
const selectedFiles = ref<File[]>([])
const groupName = ref('')
const reportTypes = ['GENERAL_LEDGER', 'SUBSIDIARY_LEDGER', 'DETAILED_LEDGER']
const statusColor = (status?: string) => ({ FAILED: 'error', READY: 'info', COMPLETED: 'success', COMPLETED_WITH_WARNINGS: 'warning', PASS: 'success', FAIL: 'error', WARNING: 'warning', BLOCKED: 'error', PENDING: 'secondary' } as Record<string, string>)[status ?? ''] ?? 'secondary'
const groupBatches = computed(() => reconciliation.value?.batches ?? batches.value.filter(batch => batch.migration_group_id === selectedGroup.value?.id))
const hasLowConfidenceDataset = computed(() => datasets.value.some(dataset => dataset.metadata?.requiresTypeConfirmation === true))

function errorCode(error: unknown): string {
  if (typeof error === 'object' && error !== null && 'code' in error && typeof (error as ApiError).code === 'string') return (error as ApiError).code
  if (error instanceof Error && error.name === 'TimeoutError') return 'TIMEOUT'
  return 'UNKNOWN'
}
function customerIssue(error: unknown, stage: LegacyStage, fileName?: string): CustomerIssue {
  const code = errorCode(error)
  const key = ({
    LEGACY_IMPORT_FILE_REQUIRED: 'fileRequired', LEGACY_IMPORT_UNSUPPORTED_FORMAT: 'unsupportedFormat',
    LEGACY_IMPORT_UNSAFE_ARCHIVE: 'unsafeArchive', LEGACY_IMPORT_EMPTY: 'empty',
    LEGACY_IMPORT_MANIFEST_REQUIRED: 'manifestRequired', LEGACY_IMPORT_UNREADABLE: 'unreadable',
    LEGACY_IMPORT_TOO_LARGE: 'tooLarge', UPLOAD_TOO_LARGE: 'tooLarge', LEGACY_IMPORT_DUPLICATE: 'duplicate',
    LEGACY_IMPORT_INVALID_STATE: 'invalidState', LEGACY_IMPORT_TOOL_UNAVAILABLE: 'toolUnavailable',
    NETWORK_ERROR: 'network', TIMEOUT: 'network', ACCESS_DENIED: 'access', UNAUTHENTICATED: 'access',
  } as Record<string, string>)[code] ?? 'unexpected'
  return { code, fileName, stage, reasonKey: `legacy.customerErrors.${key}.reason`, solutionKey: `legacy.customerErrors.${key}.solution` }
}
async function load() { [batches.value, groups.value] = await Promise.all([legacyImportsApi.list(), legacyImportsApi.groups()]) }
async function refreshSelectedGroup() {
  if (!selectedGroup.value) return
  const current = groups.value.find(group => group.id === selectedGroup.value?.id) ?? selectedGroup.value
  selectedGroup.value = current
  reconciliation.value = await legacyImportsApi.reconciliation(current.id)
}
async function select(batch: LegacyBatch) {
  try { issue.value = undefined; selected.value = batch; datasets.value = await legacyImportsApi.datasets(batch.id) }
  catch (error) { issue.value = customerIssue(error, 'load', batch.original_filename) }
}
async function selectGroup(group: LegacyMigrationGroup) {
  try { issue.value = undefined; selectedGroup.value = group; reconciliation.value = await legacyImportsApi.reconciliation(group.id) }
  catch (error) { issue.value = customerIssue(error, 'load') }
}
async function action(stageName: LegacyStage, fileName: string | undefined, work: () => Promise<unknown>) {
  busy.value = true; issue.value = undefined
  try { await work(); await load(); await refreshSelectedGroup() }
  catch (error) { issue.value = customerIssue(error, stageName, fileName) }
  finally { busy.value = false }
}
async function createGroup() { await action('load', undefined, async () => { const group = await legacyImportsApi.createGroup(groupName.value); groupName.value = ''; selectedGroup.value = group; reconciliation.value = await legacyImportsApi.reconciliation(group.id) }) }
async function upload() {
  if (!selectedFiles.value.length || !selectedGroup.value) return
  busy.value = true; issue.value = undefined; uploadResults.value = []
  const failed: File[] = []
  for (const file of selectedFiles.value) {
    try { await legacyImportsApi.upload(file, selectedGroup.value.id); uploadResults.value.push({ fileName: file.name, success: true }) }
    catch (error) { const detail = customerIssue(error, 'upload', file.name); failed.push(file); uploadResults.value.push({ fileName: file.name, success: false, issue: detail }); issue.value ??= detail }
  }
  selectedFiles.value = failed
  try { await load(); await refreshSelectedGroup() }
  catch (error) { issue.value ??= customerIssue(error, 'load') }
  finally { busy.value = false }
}
async function analyze(batch: LegacyBatch) { await action('analyze', batch.original_filename, async () => { await legacyImportsApi.analyze(batch.id); await select(batch) }) }
async function stage(batch: LegacyBatch) { await action('stage', batch.original_filename, async () => { await legacyImportsApi.execute(batch.id); await select(batch) }) }
async function reconcile() { if (selectedGroup.value) await action('reconcile', undefined, async () => { reconciliation.value = await legacyImportsApi.reconcile(selectedGroup.value!.id) }) }
async function confirmType(dataset: LegacyDataset, reportType: string) { if (selected.value) await action('analyze', selected.value.original_filename, async () => { await legacyImportsApi.confirmReportType(selected.value!.id, dataset.id, reportType); await select(selected.value!) }) }
async function explain(exceptionId: number) { if (selectedGroup.value) await action('reconcile', undefined, () => legacyImportsApi.reviewException(selectedGroup.value!.id, exceptionId, 'EXPLAINED', 'Reviewed migration evidence.')) }
function downloadValidationReport() {
  if (!selectedGroup.value || !reconciliation.value) return
  const source = reconciliation.value
  const report = {
    schemaVersion: '1.0',
    generatedAt: new Date().toISOString(),
    build: buildInfo,
    safety: {
      stagingOnly: source.stagingOnly !== false,
      canonicalWrites: 0,
      finalImportAvailable: source.canonicalAccountingAvailable === true,
      rawRecordsIncluded: false,
      sourceFilenamesIncluded: false,
    },
    migrationGroup: {
      id: selectedGroup.value.id,
      name: selectedGroup.value.name,
      status: source.status,
      acceptanceStatus: source.acceptance_status,
    },
    summary: {
      journalRows: source.journalRows ?? null,
      journalDebit: source.journalDebit ?? null,
      journalCredit: source.journalCredit ?? null,
      journalDifference: source.journalDifference ?? null,
      trialBalanceRows: source.trialBalanceRows ?? null,
      chequeRows: source.chequeRows ?? null,
    },
    checks: source.checks,
    exceptions: source.exceptions.map(item => ({
      domain: item.domain,
      classification: item.classification,
      approvalStatus: item.approval_status,
      difference: item.difference_value,
      explanation: item.explanation ?? null,
    })),
    batches: source.batches.map(batch => ({
      id: batch.id,
      evidenceType: batch.evidence_type,
      status: batch.status,
      recordCount: batch.record_count,
      warningCount: batch.warning_count,
      errorCount: batch.error_count,
    })),
  }
  const url = URL.createObjectURL(new Blob([JSON.stringify(report, null, 2)], { type: 'application/json' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `asan-customer-validation-${selectedGroup.value.id}.json`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
onMounted(() => load().catch(error => { issue.value = customerIssue(error, 'load') }))
</script>

<template>
  <div class="legacy-page pa-4 pa-md-6">
    <div class="d-flex flex-wrap align-center justify-space-between ga-4 mb-6"><div><h1 class="text-h4 font-weight-bold">{{ t('legacy.title') }}</h1><p class="text-medium-emphasis mt-1">{{ t('legacy.accountingSubtitle') }}</p></div><v-chip color="primary" variant="tonal" prepend-icon="mdi-shield-lock-outline">{{ t('legacy.stagingOnly') }}</v-chip></div>
    <v-alert v-if="issue" type="error" variant="tonal" closable class="customer-error mb-4" role="alert" @click:close="issue = undefined">
      <div class="text-subtitle-1 font-weight-bold">{{ t('legacy.customerErrorTitle') }}</div>
      <div v-if="issue.fileName" class="mt-2"><strong>{{ t('legacy.problemFile') }}:</strong> <span dir="auto">{{ issue.fileName }}</span></div>
      <div><strong>{{ t('legacy.problemStage') }}:</strong> {{ t(`legacy.stages.${issue.stage}`) }}</div>
      <div><strong>{{ t('legacy.problemReason') }}:</strong> {{ t(issue.reasonKey) }}</div>
      <div class="mt-2"><strong>{{ t('legacy.problemSolution') }}:</strong> {{ t(issue.solutionKey) }}</div>
    </v-alert><v-alert type="info" variant="tonal" class="mb-4">{{ t('legacy.finalImportBlocked') }}</v-alert>
    <v-row><v-col cols="12" lg="4">
      <v-card rounded="xl" class="mb-4"><v-card-title>{{ t('legacy.migrationBatch') }}</v-card-title><v-card-text><v-text-field v-model="groupName" :label="t('legacy.migrationBatchName')" hide-details /><v-btn block color="primary" class="mt-4" :loading="busy" @click="createGroup">{{ t('legacy.createMigrationBatch') }}</v-btn></v-card-text></v-card>
      <v-card rounded="xl" class="mb-4"><v-card-title>{{ t('legacy.migrationBatches') }}</v-card-title><v-list lines="two"><v-list-item v-for="group in groups" :key="group.id" :active="selectedGroup?.id === group.id" @click="selectGroup(group)"><template #prepend><v-avatar color="primary" variant="tonal"><v-icon>mdi-clipboard-check-outline</v-icon></v-avatar></template><v-list-item-title>{{ group.name }}</v-list-item-title><v-list-item-subtitle><v-chip :color="statusColor(group.acceptance_status)" size="x-small">{{ enumLabel(group.acceptance_status) }}</v-chip></v-list-item-subtitle></v-list-item><v-list-item v-if="!groups.length" :title="t('legacy.emptyMigrationBatches')" /></v-list></v-card>
      <v-card rounded="xl" :disabled="!selectedGroup"><v-card-title>{{ t('legacy.addReports') }}</v-card-title><v-card-text><v-file-input v-model="selectedFiles" multiple accept=".rar,.zip,.xlsx" prepend-icon="mdi-file-excel-outline" :label="t('legacy.chooseReports')" :hint="t('legacy.reportUploadHint')" persistent-hint /><v-btn block color="primary" class="mt-4" :disabled="!selectedFiles.length" :loading="busy" @click="upload">{{ t('legacy.uploadReports') }}</v-btn><v-list v-if="uploadResults.length" class="mt-3" density="compact" bg-color="transparent"><v-list-item v-for="result in uploadResults" :key="result.fileName" :prepend-icon="result.success ? 'mdi-check-circle-outline' : 'mdi-alert-circle-outline'" :base-color="result.success ? 'success' : 'error'"><v-list-item-title dir="auto">{{ result.fileName }}</v-list-item-title><v-list-item-subtitle>{{ result.success ? t('legacy.uploadSucceeded') : t(result.issue!.reasonKey) }}</v-list-item-subtitle></v-list-item></v-list></v-card-text></v-card>
    </v-col><v-col cols="12" lg="8">
      <v-card v-if="selectedGroup" rounded="xl"><v-card-title class="d-flex flex-wrap align-center ga-3"><span>{{ selectedGroup.name }}</span><v-spacer /><v-btn variant="tonal" prepend-icon="mdi-download-outline" :disabled="!reconciliation" @click="downloadValidationReport">{{ t('legacy.downloadValidationReport') }}</v-btn><v-btn color="primary" prepend-icon="mdi-scale-balance" :loading="busy" @click="reconcile">{{ t('legacy.runReconciliation') }}</v-btn></v-card-title><v-card-text>
        <v-tabs v-model="tab" grow><v-tab value="overview">{{ t('legacy.overview') }}</v-tab><v-tab value="reports">{{ t('legacy.reports') }}</v-tab><v-tab value="reconciliation">{{ t('legacy.reconciliation') }}</v-tab><v-tab value="evidence">{{ t('legacy.evidence') }}</v-tab></v-tabs>
        <v-window v-model="tab" class="mt-4">
          <v-window-item value="overview"><v-row><v-col v-for="metric in ['journalRows','trialBalanceRows','chequeRows']" :key="metric" cols="6" md="3"><div class="metric pa-3"><div class="text-h6">{{ reconciliation?.[metric as keyof LegacyReconciliation] ?? '—' }}</div><div class="text-caption">{{ t(`legacy.${metric}`) }}</div></div></v-col></v-row><v-alert v-if="hasLowConfidenceDataset" type="warning" variant="tonal" class="mt-4">{{ t('legacy.confirmLowConfidence') }}</v-alert></v-window-item>
          <v-window-item value="reports"><v-list lines="two"><v-list-item v-for="batch in groupBatches" :key="batch.id" @click="select(batch)"><template #prepend><v-icon>mdi-file-chart-outline</v-icon></template><v-list-item-title>{{ batch.original_filename }}</v-list-item-title><v-list-item-subtitle>{{ batch.record_count }} {{ t('legacy.records') }} · {{ batch.evidence_type }}</v-list-item-subtitle><template #append><v-btn v-if="['UPLOADED','FAILED'].includes(batch.status)" size="small" variant="text" @click.stop="analyze(batch)">{{ t('legacy.analyze') }}</v-btn><v-btn v-if="batch.status === 'READY'" size="small" variant="text" @click.stop="stage(batch)">{{ t('legacy.importStaging') }}</v-btn></template></v-list-item></v-list><v-expansion-panels v-if="selected" class="mt-3"><v-expansion-panel v-for="dataset in datasets" :key="dataset.id"><v-expansion-panel-title>{{ dataset.source_table }} <v-chip class="ms-2" size="x-small" :color="dataset.metadata?.detectionConfidence === 'LOW' ? 'warning' : 'success'">{{ enumLabel(dataset.semantic_type) }}</v-chip></v-expansion-panel-title><v-expansion-panel-text><div>{{ t('legacy.confidence') }}: {{ dataset.metadata?.detectionConfidence ?? '—' }}</div><v-select v-if="dataset.metadata?.requiresTypeConfirmation" :items="reportTypes" :label="t('legacy.confirmReportType')" density="compact" class="mt-2" @update:model-value="confirmType(dataset, String($event))" /><v-table density="compact"><thead><tr><th>{{ t('legacy.sourceField') }}</th><th>{{ t('legacy.meaning') }}</th><th>{{ t('legacy.confidence') }}</th></tr></thead><tbody><tr v-for="field in dataset.field_dictionary" :key="String(field.sourceField)"><td>{{ field.sourceField }}</td><td>{{ field.meaning }}</td><td>{{ field.confidence }}</td></tr></tbody></v-table></v-expansion-panel-text></v-expansion-panel></v-expansion-panels></v-window-item>
          <v-window-item value="reconciliation"><v-list v-if="reconciliation?.checks?.length" lines="two"><v-list-item v-for="check in reconciliation.checks" :key="check.id"><template #prepend><v-chip :color="statusColor(check.status)" size="small">{{ enumLabel(check.status) }}</v-chip></template><v-list-item-title>{{ enumLabel(check.check_code) }}</v-list-item-title><v-list-item-subtitle>{{ check.evidence }}</v-list-item-subtitle></v-list-item></v-list><v-empty-state v-else icon="mdi-scale-balance" :title="t('legacy.noReconciliation')" /></v-window-item>
          <v-window-item value="evidence"><v-list v-if="reconciliation?.exceptions?.length" lines="three"><v-list-item v-for="item in reconciliation.exceptions" :key="item.id"><template #prepend><v-chip :color="statusColor(item.approval_status)" size="small">{{ enumLabel(item.classification) }}</v-chip></template><v-list-item-title>{{ enumLabel(item.domain) }} · {{ item.source_key }}</v-list-item-title><v-list-item-subtitle>{{ item.explanation || t('legacy.noExplanation') }}</v-list-item-subtitle><template #append><v-btn v-if="item.approval_status === 'NEEDS_INVESTIGATION'" size="small" variant="text" @click="explain(item.id)">{{ t('legacy.explainDifference') }}</v-btn></template></v-list-item></v-list><v-empty-state v-else icon="mdi-file-search-outline" :title="t('legacy.noEvidence')" /></v-window-item>
        </v-window>
      </v-card-text></v-card><v-empty-state v-else icon="mdi-database-search-outline" :title="t('legacy.selectMigrationBatch')" :text="t('legacy.selectMigrationBatchHint')" />
    </v-col></v-row>
  </div>
</template>

<style scoped>.legacy-page{max-width:1600px;margin:auto}.metric{border:1px solid rgba(var(--v-border-color),var(--v-border-opacity));border-radius:14px;background:rgb(var(--v-theme-surface));min-height:96px}@media (max-width:600px){.legacy-page{padding-bottom:calc(88px + env(safe-area-inset-bottom))}}</style>
