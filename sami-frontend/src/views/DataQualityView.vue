<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import AppEmptyState from '@/components/AppEmptyState.vue'
import AppPageHeader from '@/components/AppPageHeader.vue'
import { dataQualityApi } from '@/api/dataQuality'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import { usePermission } from '@/composables/usePermission'
import type { QualityCatalog, QualityIssue, QualityIssueSummary, QualityRule, QualityRulePayload } from '@/types/dataQuality'

const { t } = useI18n()
const { xs } = useDisplay()
const { can } = usePermission()
const { formatDateTime, formatNumber } = useFormat()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()

const tab = ref('issues')
const loading = ref(false)
const saving = ref(false)
const rules = ref<QualityRule[]>([])
const issues = ref<QualityIssue[]>([])
const summary = ref<QualityIssueSummary>({})
const catalog = ref<QualityCatalog>({ validationTypes: [], duplicateStrategies: [], dimensions: [], severities: [], statuses: [], scoreBands: [], duplicateProviders: [] })
const status = ref<string>()
const page = ref(1)
const total = ref(0)
const formOpen = ref(false)
const actionIssue = ref<QualityIssue>()
const actionKind = ref<'resolve' | 'ignore'>('resolve')
const note = ref('')
const deleteTarget = ref<QualityRule>()
const configText = ref('{}')
const form = ref<QualityRulePayload>({
  code: '', name: '', moduleCode: '', entityCode: '', validationType: '',
  priority: 100, weight: 1, config: {}, conditionConfig: {},
})

const statusItems = computed(() => catalog.value.statuses.map((value) => ({ title: value, value })))
const summaryCards = computed(() => Object.entries(summary.value))

function parseConfig(): Record<string, unknown> {
  const parsed: unknown = JSON.parse(configText.value || '{}')
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') throw new Error(t('dataQuality.invalidJson'))
  return parsed as Record<string, unknown>
}

async function loadIssues(): Promise<void> {
  loading.value = true
  clearError()
  try {
    const [result, counts] = await Promise.all([
      dataQualityApi.issues(status.value, { page: page.value - 1, size: 20, sort: 'createdAt,desc' }),
      dataQualityApi.issueSummary(),
    ])
    issues.value = result.content
    total.value = result.totalElements
    summary.value = counts
  } catch (error) {
    setError(error)
  } finally {
    loading.value = false
  }
}

async function loadRules(): Promise<void> {
  loading.value = true
  clearError()
  try {
    rules.value = await dataQualityApi.rules()
  } catch (error) {
    setError(error)
  } finally {
    loading.value = false
  }
}

function openRule(): void {
  form.value = { code: '', name: '', moduleCode: '', entityCode: '', validationType: catalog.value.validationTypes[0] ?? '', priority: 100, weight: 1 }
  configText.value = '{}'
  formOpen.value = true
}

async function createRule(): Promise<void> {
  saving.value = true
  clearError()
  try {
    await dataQualityApi.createRule({ ...form.value, config: parseConfig() })
    formOpen.value = false
    await loadRules()
  } catch (error) {
    setError(error)
  } finally {
    saving.value = false
  }
}

async function changeStatus(rule: QualityRule, next: string): Promise<void> {
  try {
    await dataQualityApi.changeRuleStatus(rule.id, next)
    await loadRules()
  } catch (error) {
    setError(error)
  }
}

async function removeRule(): Promise<void> {
  if (!deleteTarget.value) return
  try {
    await dataQualityApi.deleteRule(deleteTarget.value.id)
    deleteTarget.value = undefined
    await loadRules()
  } catch (error) {
    setError(error)
  }
}

function openIssueAction(issue: QualityIssue, kind: 'resolve' | 'ignore'): void {
  actionIssue.value = issue
  actionKind.value = kind
  note.value = ''
}

async function completeIssueAction(): Promise<void> {
  if (!actionIssue.value) return
  saving.value = true
  try {
    if (actionKind.value === 'resolve') await dataQualityApi.resolveIssue(actionIssue.value.id, note.value || undefined)
    else await dataQualityApi.ignoreIssue(actionIssue.value.id, note.value || undefined)
    actionIssue.value = undefined
    await loadIssues()
  } catch (error) {
    setError(error)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    catalog.value = await dataQualityApi.catalog()
  } catch (error) {
    setError(error)
  }
  await Promise.all([loadIssues(), loadRules()])
})
</script>

<template>
  <div>
    <AppPageHeader icon="mdi-database-check-outline" :eyebrow="t('dataQuality.eyebrow')" :title="t('dataQuality.title')">
      <template #actions>
        <v-btn v-if="tab === 'rules' && can('data-quality:create')" color="primary" prepend-icon="mdi-plus" @click="openRule">
          {{ t('dataQuality.newRule') }}
        </v-btn>
      </template>
    </AppPageHeader>

    <v-alert v-if="errorMessage" type="error" variant="tonal" closable class="mb-4" @click:close="clearError">
      {{ errorMessage }}
      <template #append><v-btn variant="text" @click="tab === 'issues' ? loadIssues() : loadRules()">{{ t('common.retry') }}</v-btn></template>
    </v-alert>

    <div class="quality-summary mb-4">
      <v-card v-for="[key, count] in summaryCards" :key="key" rounded="xl" variant="tonal">
        <v-card-text><div class="text-caption text-medium-emphasis">{{ key }}</div><div class="text-h5 font-weight-bold">{{ formatNumber(count) }}</div></v-card-text>
      </v-card>
    </div>

    <v-tabs v-model="tab" class="mb-3" @update:model-value="$event === 'issues' ? loadIssues() : loadRules()">
      <v-tab value="issues">{{ t('dataQuality.issues') }}</v-tab>
      <v-tab value="rules">{{ t('dataQuality.rules') }}</v-tab>
    </v-tabs>

    <v-card rounded="xl">
      <v-progress-linear v-if="loading" indeterminate />
      <template v-if="tab === 'issues'">
        <v-card-text class="d-flex flex-column flex-sm-row ga-3">
          <v-select v-model="status" :items="statusItems" :label="t('dataQuality.status')" clearable @update:model-value="page = 1; loadIssues()" />
          <v-btn variant="tonal" prepend-icon="mdi-refresh" @click="loadIssues">{{ t('common.refresh') }}</v-btn>
        </v-card-text>
        <div v-if="issues.length" class="quality-grid pa-3 pa-sm-4">
          <v-card v-for="issue in issues" :key="issue.id" variant="tonal" rounded="lg">
            <v-card-text>
              <div class="d-flex align-start ga-3"><v-icon icon="mdi-alert-circle-outline" color="warning" /><div class="flex-grow-1 min-width-0"><div class="font-weight-bold">{{ issue.message }}</div><div class="text-caption">{{ issue.moduleCode }} / {{ issue.entityCode }} · {{ issue.fieldName || '—' }}</div></div><v-chip size="x-small" label>{{ issue.severityCode }}</v-chip></div>
              <div class="text-caption text-medium-emphasis mt-3">{{ formatDateTime(issue.createdAt) }} · {{ issue.status }}</div>
              <div v-if="can('data-quality:resolve')" class="d-flex ga-2 mt-4"><v-btn size="small" color="primary" variant="tonal" @click="openIssueAction(issue, 'resolve')">{{ t('dataQuality.resolve') }}</v-btn><v-btn size="small" variant="text" @click="openIssueAction(issue, 'ignore')">{{ t('dataQuality.ignore') }}</v-btn></div>
            </v-card-text>
          </v-card>
        </div>
        <AppEmptyState v-else-if="!loading" icon="mdi-check-decagram-outline" :title="t('dataQuality.noIssues')" :description="t('dataQuality.noIssuesDescription')" />
        <v-pagination v-if="total > 20" v-model="page" :length="Math.ceil(total / 20)" class="pb-4" @update:model-value="loadIssues" />
      </template>

      <template v-else>
        <div v-if="rules.length" class="quality-grid pa-3 pa-sm-4">
          <v-card v-for="rule in rules" :key="rule.id" variant="tonal" rounded="lg">
            <v-card-text>
              <div class="d-flex align-start"><div class="flex-grow-1 min-width-0"><div class="font-weight-bold">{{ rule.name }}</div><div class="text-caption">{{ rule.code }} · {{ rule.validationType }}</div></div><v-menu><template #activator="{ props }"><v-btn v-bind="props" icon="mdi-dots-vertical" variant="text" /></template><v-list density="compact"><v-list-item v-if="can('data-quality:delete')" base-color="error" prepend-icon="mdi-delete-outline" :title="t('common.delete')" @click="deleteTarget = rule" /></v-list></v-menu></div>
              <p class="text-body-2 text-medium-emphasis mt-3">{{ rule.description || `${rule.moduleCode} / ${rule.entityCode}` }}</p>
              <v-select v-if="can('data-quality:edit')" :model-value="rule.statusCode" :items="statusItems" density="compact" hide-details class="mt-3" @update:model-value="changeStatus(rule, $event)" />
              <v-chip v-else size="small" class="mt-3">{{ rule.statusCode }}</v-chip>
            </v-card-text>
          </v-card>
        </div>
        <AppEmptyState v-else-if="!loading" icon="mdi-ruler-square-compass" :title="t('dataQuality.noRules')" :action-label="can('data-quality:create') ? t('dataQuality.newRule') : undefined" @action="openRule" />
      </template>
    </v-card>

    <v-dialog v-model="formOpen" :fullscreen="xs" max-width="780" scrollable>
      <v-card :rounded="xs ? 0 : 'xl'"><v-card-title class="d-flex px-5 pt-5">{{ t('dataQuality.newRule') }}<v-spacer /><v-btn icon="mdi-close" variant="text" @click="formOpen = false" /></v-card-title>
        <v-card-text class="px-5"><v-row><v-col cols="12" sm="6"><v-text-field v-model="form.name" :label="t('dataQuality.name')" /></v-col><v-col cols="12" sm="6"><v-text-field v-model="form.code" :label="t('dataQuality.code')" /></v-col><v-col cols="12" sm="6"><v-text-field v-model="form.moduleCode" :label="t('dataQuality.module')" /></v-col><v-col cols="12" sm="6"><v-text-field v-model="form.entityCode" :label="t('dataQuality.entity')" /></v-col><v-col cols="12" sm="6"><v-select v-model="form.validationType" :items="catalog.validationTypes" :label="t('dataQuality.validationType')" /></v-col><v-col cols="12" sm="6"><v-select v-model="form.severityCode" :items="catalog.severities" :label="t('dataQuality.severity')" /></v-col><v-col cols="12"><v-textarea v-model="form.description" :label="t('dataQuality.description')" rows="2" /></v-col><v-col cols="12"><v-textarea v-model="configText" :label="t('dataQuality.configuration')" rows="5" class="json-field" /></v-col></v-row></v-card-text>
        <v-card-actions class="px-5 pb-5"><v-spacer /><v-btn variant="text" @click="formOpen = false">{{ t('common.cancel') }}</v-btn><v-btn color="primary" :loading="saving" :disabled="!form.name || !form.code || !form.moduleCode || !form.entityCode || !form.validationType" @click="createRule">{{ t('common.save') }}</v-btn></v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog :model-value="!!actionIssue" max-width="520" :fullscreen="xs" @update:model-value="actionIssue = undefined"><v-card :rounded="xs ? 0 : 'xl'"><v-card-title class="px-5 pt-5">{{ actionKind === 'resolve' ? t('dataQuality.resolve') : t('dataQuality.ignore') }}</v-card-title><v-card-text class="px-5"><p class="mb-4">{{ actionIssue?.message }}</p><v-textarea v-model="note" :label="t('dataQuality.resolutionNote')" rows="4" /></v-card-text><v-card-actions class="px-5 pb-5"><v-spacer /><v-btn variant="text" @click="actionIssue = undefined">{{ t('common.cancel') }}</v-btn><v-btn color="primary" :loading="saving" @click="completeIssueAction">{{ t('common.confirm') }}</v-btn></v-card-actions></v-card></v-dialog>
    <v-dialog :model-value="!!deleteTarget" max-width="440" @update:model-value="deleteTarget = undefined"><v-card rounded="xl"><v-card-title class="px-5 pt-5">{{ t('dataQuality.deleteRule') }}</v-card-title><v-card-text class="px-5">{{ t('dataQuality.deleteRuleMessage', { name: deleteTarget?.name }) }}</v-card-text><v-card-actions class="px-5 pb-5"><v-spacer /><v-btn variant="text" @click="deleteTarget = undefined">{{ t('common.cancel') }}</v-btn><v-btn color="error" @click="removeRule">{{ t('common.delete') }}</v-btn></v-card-actions></v-card></v-dialog>
  </div>
</template>

<style scoped>
.quality-summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 12px; }
.quality-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 330px), 1fr)); gap: 14px; }
.min-width-0 { min-width: 0; }
.json-field :deep(textarea) { direction: ltr; text-align: left; font-family: ui-monospace, monospace; }
@media (max-width: 599px) { .quality-grid { grid-template-columns: 1fr; padding: 12px !important; } }
</style>
