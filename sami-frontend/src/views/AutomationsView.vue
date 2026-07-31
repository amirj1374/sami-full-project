<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { automationsApi, type AutomationListParams } from '@/api/automations'
import type {
  AutomationAction,
  AutomationActionDescriptor,
  AutomationExecution,
  AutomationExecutionLog,
  AutomationRule,
  AutomationRulePayload,
  AutomationRunPayload,
  AutomationStatus,
  AutomationTrigger,
} from '@/types/automation'
import { automationRuleSchema } from '@/schemas/automation'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import { usePermission } from '@/composables/usePermission'
import { useServerLabel } from '@/composables/useServerLabel'
import { useNotifications } from '@/composables/useNotifications'
import AppEmptyState from '@/components/AppEmptyState.vue'
import AppPageHeader from '@/components/AppPageHeader.vue'

const { t } = useI18n()
const { xs } = useDisplay()
const { formatDateTime, formatNumber } = useFormat()
const { can } = usePermission()
const { enumLabel, statusLabel } = useServerLabel()
const notifications = useNotifications()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()

const loading = ref(false)
const saving = ref(false)
const rows = ref<AutomationRule[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const search = ref('')
const statusCode = ref<string>()
const triggerFilter = ref<string>()
const categoryFilter = ref('')
const sort = ref('priority,asc')
const totalRules = ref(0)
const activeRules = ref(0)
const archivedRules = ref(0)
const statuses = ref<AutomationStatus[]>([])
const triggers = ref<AutomationTrigger[]>([])
const actionDescriptors = ref<AutomationActionDescriptor[]>([])
const formOpen = ref(false)
const detailRule = ref<AutomationRule>()
const detailLoading = ref(false)
const editing = ref<AutomationRule>()
const deleteTarget = ref<AutomationRule>()
const historyRule = ref<AutomationRule>()
const executions = ref<AutomationExecution[]>([])
const historyLoading = ref(false)
const logs = ref<AutomationExecutionLog[]>([])
const logsOpen = ref(false)
const selectedExecution = ref<AutomationExecution>()
const runTarget = ref<AutomationRule>()
const runEntityType = ref('')
const runEntityId = ref<number>()
const runData = ref('{}')
const jsonError = ref('')
const description = ref('')
const category = ref('')
const priority = ref(100)
const allowRecursion = ref(false)
const maxExecutions = ref<number>()
const triggerConfig = ref('{}')
const conditionConfig = ref('{}')
const executionPolicy = ref('{}')
const actions = ref<AutomationAction[]>([])
const actionConfigDrafts = ref<string[]>([])
const actionConditionDrafts = ref<string[]>([])

const { handleSubmit, defineField, errors, resetForm } = useForm({
  validationSchema: computed(() => toTypedSchema(automationRuleSchema(t))),
  initialValues: { code: '', name: '', statusCode: '', triggerType: '' },
})
const [code, codeProps] = defineField('code')
const [name, nameProps] = defineField('name')
const [formStatus, statusProps] = defineField('statusCode')
const [triggerType, triggerProps] = defineField('triggerType')

const triggerTranslationKeys: Record<string, string> = { manual: 'manual', scheduled: 'scheduled', webhook: 'webhook', 'crm.customer.*': 'customer', 'purchasing.purchase.*': 'purchase', 'supplier.supplier.*': 'supplier', 'dashboard.*': 'dashboard' }
const actionTranslationKeys: Record<string, string> = { log: 'log', notify: 'notify', 'customer.timeline': 'customerTimeline' }
const triggerTitle = (item: AutomationTrigger): string => triggerTranslationKeys[item.type] ? t(`automation.triggerTypes.${triggerTranslationKeys[item.type]}`) : item.label
const actionTitle = (item: AutomationActionDescriptor): string => actionTranslationKeys[item.type] ? t(`automation.actionTypes.${actionTranslationKeys[item.type]}`) : item.label
const statusItems = computed(() => statuses.value.map((s) => ({ title: statusLabel(s.name), value: s.code })))
const triggerItems = computed(() => triggers.value.map((v) => ({ title: triggerTitle(v), value: v.type, subtitle: enumLabel(v.category) })))
const actionItems = computed(() => actionDescriptors.value.map((v) => ({ title: actionTitle(v), value: v.type })))
const sortItems = computed(() => [
  { title: t('automation.sort.priority'), value: 'priority,asc' },
  { title: t('automation.sort.name'), value: 'name,asc' },
  { title: t('automation.sort.updated'), value: 'updatedAt,desc' },
  { title: t('automation.sort.created'), value: 'createdAt,desc' },
])

function parseObject(value: string): Record<string, unknown> {
  const parsed: unknown = JSON.parse(value || '{}')
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') throw new Error()
  return parsed as Record<string, unknown>
}

function validateActionConfigs(): AutomationAction[] {
  return actions.value.map((action, index) => ({
    ...action,
    stepOrder: index + 1,
    config: parseObject(actionConfigDrafts.value[index] ?? '{}'),
    stepCondition: parseObject(actionConditionDrafts.value[index] ?? '{}'),
  }))
}

async function load(): Promise<void> {
  loading.value = true
  clearError()
  try {
    const params: AutomationListParams = {
      page: page.value - 1,
      size: size.value,
      search: search.value || undefined,
      statusCode: statusCode.value,
      triggerType: triggerFilter.value,
      category: categoryFilter.value || undefined,
      sort: sort.value,
    }
    const result = await automationsApi.list(params)
    rows.value = result.content
    total.value = result.totalElements
    totalRules.value = result.totalElements
  } catch (error) {
    setError(error)
  } finally {
    loading.value = false
  }
}

async function openForm(row?: AutomationRule): Promise<void> {
  clearError()
  jsonError.value = ''
  editing.value = row ? await automationsApi.get(row.id) : undefined
  const value = editing.value
  resetForm({
    values: {
      code: value?.code ?? '',
      name: value?.name ?? '',
      statusCode: value?.statusCode ?? statuses.value.find((s) => s.isDefault)?.code ?? '',
      triggerType: value?.triggerType ?? '',
    },
  })
  description.value = value?.description ?? ''
  category.value = value?.category ?? ''
  priority.value = value?.priority ?? 100
  allowRecursion.value = value?.allowRecursion ?? false
  maxExecutions.value = value?.maxExecutions ?? undefined
  triggerConfig.value = JSON.stringify(value?.triggerConfig ?? {}, null, 2)
  conditionConfig.value = JSON.stringify(value?.conditionConfig ?? {}, null, 2)
  executionPolicy.value = JSON.stringify(value?.executionPolicy ?? {}, null, 2)
  actions.value = (value?.actions ?? []).map((action) => ({ ...action }))
  actionConfigDrafts.value = actions.value.map((action) => JSON.stringify(action.config ?? {}, null, 2))
  actionConditionDrafts.value = actions.value.map((action) => JSON.stringify(action.stepCondition ?? {}, null, 2))
  formOpen.value = true
}

function addAction(): void {
  actions.value.push({
    stepOrder: actions.value.length + 1,
    actionType: actionDescriptors.value[0]?.type ?? '',
    name: '',
    config: {},
    stepCondition: {},
    runMode: 'SYNC',
    continueOnError: false,
    delaySeconds: 0,
    retryCount: 0,
    timeoutSeconds: null,
  })
  actionConfigDrafts.value.push('{}')
  actionConditionDrafts.value.push('{}')
}

function removeAction(index: number): void {
  actions.value.splice(index, 1)
  actionConfigDrafts.value.splice(index, 1)
  actionConditionDrafts.value.splice(index, 1)
}

const submit = handleSubmit(async (values) => {
  saving.value = true
  clearError()
  jsonError.value = ''
  try {
    const payload: AutomationRulePayload = {
      ...values,
      description: description.value || undefined,
      category: category.value || undefined,
      priority: priority.value,
      allowRecursion: allowRecursion.value,
      maxExecutions: maxExecutions.value,
      triggerConfig: parseObject(triggerConfig.value),
      conditionConfig: parseObject(conditionConfig.value),
      executionPolicy: parseObject(executionPolicy.value),
      actions: validateActionConfigs(),
      expectedVersion: editing.value?.version,
    }
    if (editing.value) await automationsApi.update(editing.value.id, payload)
    else await automationsApi.create(payload)
    formOpen.value = false
    notifications.success(t(editing.value ? 'automation.updated' : 'automation.created'))
    await load()
  } catch (error) {
    if (error instanceof SyntaxError || (error instanceof Error && !error.message)) {
      jsonError.value = t('automation.validation.json')
    } else setError(error)
  } finally {
    saving.value = false
  }
})

async function changeStatus(row: AutomationRule, next: string): Promise<void> {
  try {
    await automationsApi.changeStatus(row.id, next, row.version)
    notifications.success(t('automation.statusChanged'))
    await load()
  } catch (error) {
    setError(error)
  }
}

function openRun(row: AutomationRule): void {
  runTarget.value = row
  runEntityType.value = ''
  runEntityId.value = undefined
  runData.value = '{}'
}

async function run(): Promise<void> {
  if (!runTarget.value) return
  try {
    const payload: AutomationRunPayload = {
      entityType: runEntityType.value || undefined,
      entityId: runEntityId.value,
      data: parseObject(runData.value),
    }
    const row = runTarget.value
    await automationsApi.run(row.id, payload)
    runTarget.value = undefined
    notifications.success(t('automation.runStarted'))
    await openHistory(row)
  } catch (error) {
    setError(error)
  }
}

async function remove(): Promise<void> {
  if (!deleteTarget.value) return
  try {
    await automationsApi.remove(deleteTarget.value.id)
    deleteTarget.value = undefined
    notifications.success(t('automation.deleted'))
    await load()
  } catch (error) {
    setError(error)
  }
}

async function openHistory(row: AutomationRule): Promise<void> {
  historyRule.value = row
  historyLoading.value = true
  try {
    executions.value = (await automationsApi.executions(row.id, { size: 50, sort: 'startedAt,desc' })).content
  } catch (error) {
    setError(error)
  } finally {
    historyLoading.value = false
  }
}

async function openLogs(execution: AutomationExecution): Promise<void> {
  try {
    selectedExecution.value = execution
    logs.value = await automationsApi.executionLogs(execution.id)
    logsOpen.value = true
  } catch (error) {
    setError(error)
  }
}

async function openDetail(row: AutomationRule): Promise<void> {
  detailLoading.value = true
  try {
    detailRule.value = await automationsApi.get(row.id)
  } catch (error) {
    setError(error)
  } finally {
    detailLoading.value = false
  }
}

function editFromDetail(): void {
  const row = detailRule.value
  if (!row) return
  detailRule.value = undefined
  void openForm(row)
}

function historyFromDetail(): void {
  const row = detailRule.value
  if (!row) return
  detailRule.value = undefined
  void openHistory(row)
}

async function loadSummary(): Promise<void> {
  const active = statuses.value.filter((item) => item.isActiveState)
  const archived = statuses.value.filter((item) => item.isArchivedState)
  const [all, ...counts] = await Promise.all([
    automationsApi.list({ page: 0, size: 1 }),
    ...active.map((item) => automationsApi.list({ page: 0, size: 1, statusCode: item.code })),
    ...archived.map((item) => automationsApi.list({ page: 0, size: 1, statusCode: item.code })),
  ])
  totalRules.value = all.totalElements
  activeRules.value = counts.slice(0, active.length).reduce((sum, item) => sum + item.totalElements, 0)
  archivedRules.value = counts.slice(active.length).reduce((sum, item) => sum + item.totalElements, 0)
}

onMounted(async () => {
  try {
    ;[statuses.value, triggers.value, actionDescriptors.value] = await Promise.all([
      automationsApi.statuses(),
      automationsApi.triggers(),
      automationsApi.actions(),
    ])
  } catch (error) {
    setError(error)
  }
  await Promise.all([load(), loadSummary()])
})
</script>

<template>
  <div>
    <AppPageHeader icon="mdi-robot-outline" :eyebrow="t('automation.eyebrow')" :title="t('automation.title')">
      <template #actions>
        <v-btn v-if="can('automation:create')" color="primary" prepend-icon="mdi-plus" @click="openForm()">
          {{ t('automation.create') }}
        </v-btn>
      </template>
    </AppPageHeader>

    <v-alert v-if="errorMessage" type="error" variant="tonal" closable class="mb-4" @click:close="clearError">
      {{ errorMessage }}<template #append><v-btn variant="text" @click="load">{{ t('common.retry') }}</v-btn></template>
    </v-alert>

    <div class="summary-grid mb-4">
      <v-card rounded="xl" variant="tonal"><v-card-text><div class="text-caption text-medium-emphasis">{{ t('automation.summary.total') }}</div><div class="text-h4 font-weight-bold">{{ formatNumber(totalRules) }}</div></v-card-text></v-card>
      <v-card rounded="xl" variant="tonal" color="success"><v-card-text><div class="text-caption">{{ t('automation.summary.active') }}</div><div class="text-h4 font-weight-bold">{{ formatNumber(activeRules) }}</div></v-card-text></v-card>
      <v-card rounded="xl" variant="tonal"><v-card-text><div class="text-caption text-medium-emphasis">{{ t('automation.summary.archived') }}</div><div class="text-h4 font-weight-bold">{{ formatNumber(archivedRules) }}</div></v-card-text></v-card>
    </div>

    <v-card class="mb-4" rounded="xl">
      <v-card-text class="filter-grid">
        <v-text-field v-model="search" :label="t('common.search')" prepend-inner-icon="mdi-magnify" clearable @keyup.enter="page = 1; load()" />
        <v-select v-model="statusCode" :items="statusItems" :label="t('automation.status')" clearable @update:model-value="page = 1; load()" />
        <v-select v-model="triggerFilter" :items="triggerItems" :label="t('automation.trigger')" clearable @update:model-value="page = 1; load()" />
        <v-text-field v-model="categoryFilter" :label="t('automation.category')" clearable @keyup.enter="page = 1; load()" />
        <v-select v-model="sort" :items="sortItems" :label="t('automation.sort.label')" @update:model-value="page = 1; load()" />
        <v-btn variant="tonal" prepend-icon="mdi-filter-check" :loading="loading" @click="page = 1; load()">{{ t('automation.applyFilters') }}</v-btn>
      </v-card-text>
    </v-card>

    <v-card rounded="xl">
      <v-progress-linear v-if="loading" indeterminate />
      <div v-if="rows.length" class="pa-3 pa-sm-4 d-grid automation-grid">
        <v-card v-for="row in rows" :key="row.id" variant="tonal" rounded="lg" class="rule-card">
          <v-card-text>
            <div class="d-flex align-start ga-3">
              <v-avatar rounded="lg" color="primary" variant="tonal"><v-icon icon="mdi-transit-connection-variant" /></v-avatar>
              <div class="flex-grow-1 min-width-0">
                <div class="d-flex align-center flex-wrap ga-2">
                  <span class="font-weight-bold">{{ row.name }}</span>
                  <v-chip size="x-small" label variant="tonal">{{ statusLabel(row.statusName) }}</v-chip>
                </div>
                <div class="text-caption text-medium-emphasis">{{ row.code }} · {{ enumLabel(row.triggerType) }}</div>
              </div>
              <v-menu>
                <template #activator="{ props }"><v-btn v-bind="props" icon="mdi-dots-vertical" variant="text" /></template>
                <v-list density="compact">
                  <v-list-item v-if="can('automation:edit')" prepend-icon="mdi-pencil-outline" :title="t('common.edit')" @click="openForm(row)" />
                  <v-list-item prepend-icon="mdi-eye-outline" :title="t('automation.viewDetails')" @click="openDetail(row)" />
                  <v-list-item v-if="can('automation:execute')" prepend-icon="mdi-play-outline" :title="t('automation.run')" @click="openRun(row)" />
                  <v-list-item prepend-icon="mdi-history" :title="t('automation.history')" @click="openHistory(row)" />
                  <v-list-item v-if="can('automation:delete')" base-color="error" prepend-icon="mdi-delete-outline" :title="t('common.delete')" @click="deleteTarget = row" />
                </v-list>
              </v-menu>
            </div>
            <p v-if="row.description" class="text-body-2 text-medium-emphasis mt-3 text-truncate-2">{{ row.description }}</p>
            <div class="d-flex align-center justify-space-between mt-4 text-caption text-medium-emphasis">
              <span>{{ t('automation.executions', { count: formatNumber(row.executionCount) }) }}</span>
              <v-select
                v-if="can('automation:manage-status')"
                :model-value="row.statusCode"
                :items="statusItems"
                density="compact"
                hide-details
                class="status-select"
                @update:model-value="changeStatus(row, $event)"
              />
            </div>
          </v-card-text>
        </v-card>
      </div>
      <AppEmptyState v-else-if="!loading" icon="mdi-robot-off-outline" :title="t('automation.empty')" :description="t('automation.emptyDescription')" :action-label="can('automation:create') ? t('automation.create') : undefined" @action="openForm()" />
      <v-pagination v-if="total > size" v-model="page" :length="Math.ceil(total / size)" class="pb-4" @update:model-value="load" />
    </v-card>

    <v-dialog v-model="formOpen" :fullscreen="xs" max-width="900" scrollable>
      <v-card :rounded="xs ? 0 : 'xl'">
        <v-card-title class="d-flex align-center px-5 pt-5">
          {{ editing ? t('automation.edit') : t('automation.create') }}<v-spacer />
          <v-btn icon="mdi-close" variant="text" @click="formOpen = false" />
        </v-card-title>
        <v-card-text class="px-5">
          <v-alert v-if="errorMessage || jsonError" type="error" variant="tonal" class="mb-4">{{ jsonError || errorMessage }}</v-alert>
          <v-row>
            <v-col cols="12" sm="6"><v-text-field v-model="name" v-bind="nameProps" :error-messages="errors.name" :label="t('automation.name')" /></v-col>
            <v-col cols="12" sm="6"><v-text-field v-model="code" v-bind="codeProps" :error-messages="errors.code" :label="t('automation.code')" :disabled="!!editing" /></v-col>
            <v-col cols="12" sm="6"><v-select v-model="triggerType" v-bind="triggerProps" :error-messages="errors.triggerType" :items="triggerItems" :label="t('automation.trigger')" /></v-col>
            <v-col cols="12" sm="6"><v-select v-model="formStatus" v-bind="statusProps" :error-messages="errors.statusCode" :items="statusItems" :label="t('automation.status')" /></v-col>
            <v-col cols="12" sm="6"><v-text-field v-model="category" :label="t('automation.category')" /></v-col>
            <v-col cols="12" sm="6"><v-text-field v-model.number="priority" type="number" :label="t('automation.priority')" /></v-col>
            <v-col cols="12"><v-textarea v-model="description" rows="2" :label="t('automation.description')" /></v-col>
            <v-col cols="12" md="4"><v-textarea v-model="triggerConfig" rows="4" :label="t('automation.triggerConfig')" class="json-field" /></v-col>
            <v-col cols="12" md="4"><v-textarea v-model="conditionConfig" rows="4" :label="t('automation.conditionConfig')" class="json-field" /></v-col>
            <v-col cols="12" md="4"><v-textarea v-model="executionPolicy" rows="4" :label="t('automation.executionPolicy')" class="json-field" /></v-col>
            <v-col cols="12" sm="6"><v-switch v-model="allowRecursion" :label="t('automation.allowRecursion')" /></v-col>
            <v-col cols="12" sm="6"><v-text-field v-model.number="maxExecutions" type="number" min="1" :label="t('automation.maxExecutions')" /></v-col>
          </v-row>
          <div class="d-flex align-center mb-3"><h3 class="text-subtitle-1 font-weight-bold">{{ t('automation.actions') }}</h3><v-spacer /><v-btn size="small" variant="tonal" prepend-icon="mdi-plus" @click="addAction">{{ t('automation.addAction') }}</v-btn></div>
          <v-card v-for="(action, index) in actions" :key="index" variant="tonal" class="mb-3">
            <v-card-text>
              <div class="d-flex ga-3 align-start">
                <v-select v-model="action.actionType" :items="actionItems" :label="t('automation.actionType')" class="flex-grow-1" />
                <v-btn icon="mdi-delete-outline" variant="text" color="error" @click="removeAction(index)" />
              </div>
              <v-text-field v-model="action.name" :label="t('automation.actionName')" />
              <v-row><v-col cols="12" md="6"><v-textarea v-model="actionConfigDrafts[index]" rows="3" :label="t('automation.configuration')" class="json-field" /></v-col><v-col cols="12" md="6"><v-textarea v-model="actionConditionDrafts[index]" rows="3" :label="t('automation.stepCondition')" class="json-field" /></v-col></v-row>
              <v-row><v-col cols="12" sm="6" md="3"><v-select v-model="action.runMode" :items="[{ title: t('automation.runModes.sequential'), value: 'SEQUENTIAL' }, { title: t('automation.runModes.parallel'), value: 'PARALLEL' }]" :label="t('automation.runMode')" /></v-col><v-col cols="6" sm="3"><v-text-field v-model.number="action.delaySeconds" type="number" min="0" :label="t('automation.delay')" /></v-col><v-col cols="6" sm="3"><v-text-field v-model.number="action.retryCount" type="number" min="0" :label="t('automation.retries')" /></v-col><v-col cols="12" sm="3"><v-text-field v-model.number="action.timeoutSeconds" type="number" min="1" clearable :label="t('automation.timeout')" /></v-col></v-row>
              <v-switch v-model="action.continueOnError" color="primary" :label="t('automation.continueOnError')" />
            </v-card-text>
          </v-card>
        </v-card-text>
        <v-card-actions class="px-5 pb-5"><v-spacer /><v-btn variant="text" @click="formOpen = false">{{ t('common.cancel') }}</v-btn><v-btn color="primary" :loading="saving" @click="submit">{{ t('common.save') }}</v-btn></v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog :model-value="!!detailRule || detailLoading" max-width="900" :fullscreen="xs" scrollable @update:model-value="detailRule = undefined">
      <v-card :rounded="xs ? 0 : 'xl'">
        <v-progress-linear v-if="detailLoading" indeterminate />
        <template v-if="detailRule">
          <v-card-title class="d-flex align-center px-5 pt-5"><div class="min-width-0"><div class="text-truncate">{{ detailRule.name }}</div><div class="text-caption text-medium-emphasis">{{ detailRule.code }}</div></div><v-spacer /><v-btn icon="mdi-close" variant="text" @click="detailRule = undefined" /></v-card-title>
          <v-card-text class="px-5">
            <div class="detail-grid mb-5"><div><div class="text-caption text-medium-emphasis">{{ t('automation.status') }}</div><v-chip size="small" class="mt-1">{{ statusLabel(detailRule.statusName) }}</v-chip></div><div><div class="text-caption text-medium-emphasis">{{ t('automation.trigger') }}</div><div class="mt-1">{{ enumLabel(detailRule.triggerType) }}</div></div><div><div class="text-caption text-medium-emphasis">{{ t('automation.priority') }}</div><div class="mt-1">{{ formatNumber(detailRule.priority) }}</div></div><div><div class="text-caption text-medium-emphasis">{{ t('automation.executionsLabel') }}</div><div class="mt-1">{{ formatNumber(detailRule.executionCount) }}</div></div></div>
            <p v-if="detailRule.description" class="text-body-2 mb-5">{{ detailRule.description }}</p>
            <v-expansion-panels variant="accordion">
              <v-expansion-panel :title="t('automation.conditions')"><v-expansion-panel-text><pre class="json-preview">{{ JSON.stringify(detailRule.conditionConfig, null, 2) }}</pre></v-expansion-panel-text></v-expansion-panel>
              <v-expansion-panel :title="t('automation.triggerConfig')"><v-expansion-panel-text><pre class="json-preview">{{ JSON.stringify(detailRule.triggerConfig, null, 2) }}</pre></v-expansion-panel-text></v-expansion-panel>
              <v-expansion-panel :title="t('automation.actions')"><v-expansion-panel-text><v-list v-if="detailRule.actions.length" lines="two"><v-list-item v-for="action in detailRule.actions" :key="action.id ?? action.stepOrder" :title="action.name || enumLabel(action.actionType)" :subtitle="`${t('automation.step')} ${formatNumber(action.stepOrder)} · ${enumLabel(action.runMode)}`" /></v-list><AppEmptyState v-else dense :title="t('automation.noActions')" /></v-expansion-panel-text></v-expansion-panel>
              <v-expansion-panel :title="t('automation.auditInformation')"><v-expansion-panel-text><v-list lines="two"><v-list-item :title="t('automation.createdAt')" :subtitle="formatDateTime(detailRule.createdAt)"/><v-list-item :title="t('automation.updatedAt')" :subtitle="formatDateTime(detailRule.updatedAt)"/><v-list-item :title="t('automation.version')" :subtitle="formatNumber(detailRule.version)"/></v-list></v-expansion-panel-text></v-expansion-panel>
            </v-expansion-panels>
          </v-card-text>
          <v-card-actions class="px-5 pb-5"><v-btn prepend-icon="mdi-history" variant="tonal" @click="historyFromDetail">{{ t('automation.history') }}</v-btn><v-spacer/><v-btn v-if="can('automation:edit')" color="primary" @click="editFromDetail">{{ t('common.edit') }}</v-btn></v-card-actions>
        </template>
      </v-card>
    </v-dialog>

    <v-dialog :model-value="!!runTarget" max-width="620" :fullscreen="xs" @update:model-value="runTarget = undefined">
      <v-card :rounded="xs ? 0 : 'xl'"><v-card-title class="px-5 pt-5">{{ t('automation.manualExecution') }}</v-card-title><v-card-text class="px-5"><p class="text-body-2 text-medium-emphasis mb-4">{{ runTarget?.name }}</p><v-text-field v-model="runEntityType" :label="t('automation.entityType')"/><v-text-field v-model.number="runEntityId" type="number" :label="t('automation.entityId')"/><v-textarea v-model="runData" rows="5" :label="t('automation.executionData')" class="json-field"/></v-card-text><v-card-actions class="px-5 pb-5"><v-spacer/><v-btn variant="text" @click="runTarget = undefined">{{ t('common.cancel') }}</v-btn><v-btn color="primary" prepend-icon="mdi-play" @click="run">{{ t('automation.run') }}</v-btn></v-card-actions></v-card>
    </v-dialog>

    <v-dialog :model-value="!!historyRule" max-width="820" :fullscreen="xs" @update:model-value="historyRule = undefined">
      <v-card :rounded="xs ? 0 : 'xl'">
        <v-card-title class="d-flex px-5 pt-5">{{ t('automation.history') }}<v-spacer /><v-btn icon="mdi-close" variant="text" @click="historyRule = undefined" /></v-card-title>
        <v-card-text class="px-5">
          <v-progress-linear v-if="historyLoading" indeterminate />
          <v-list v-else-if="executions.length">
            <v-list-item v-for="execution in executions" :key="execution.id" :title="execution.executionNumber" :subtitle="`${formatDateTime(execution.startedAt)} · ${execution.durationMs == null ? t('automation.running') : t('automation.durationMs', { value: formatNumber(execution.durationMs) })}`" @click="openLogs(execution)">
              <template #append><v-chip size="x-small" :color="execution.status === 'FAILED' ? 'error' : execution.status === 'SUCCEEDED' ? 'success' : undefined" variant="tonal">{{ enumLabel(execution.status) }}</v-chip></template>
            </v-list-item>
          </v-list>
          <AppEmptyState v-else dense :title="t('automation.noExecutions')" />
        </v-card-text>
      </v-card>
    </v-dialog>

    <v-dialog v-model="logsOpen" max-width="680" :fullscreen="xs">
      <v-card :rounded="xs ? 0 : 'xl'"><v-card-title class="d-flex px-5 pt-5">{{ t('automation.executionLogs') }}<v-spacer/><v-btn icon="mdi-close" variant="text" @click="logsOpen=false"/></v-card-title><v-card-text class="px-5"><v-alert v-if="selectedExecution?.error" type="error" variant="tonal" class="mb-4">{{ selectedExecution.error }}</v-alert><v-timeline v-if="logs.length" side="end" density="compact"><v-timeline-item v-for="log in logs" :key="log.id" size="small" :dot-color="log.status === 'FAILED' ? 'error' : log.status === 'SUCCEEDED' ? 'success' : undefined"><div class="font-weight-medium">{{ enumLabel(log.actionType) }} · {{ enumLabel(log.status) }}</div><div class="text-body-2 text-medium-emphasis">{{ log.message }}</div><div class="text-caption">{{ formatDateTime(log.occurredAt) }}</div><pre v-if="log.detail && Object.keys(log.detail).length" class="json-preview mt-2">{{ JSON.stringify(log.detail, null, 2) }}</pre></v-timeline-item></v-timeline><AppEmptyState v-else dense :title="t('automation.noLogs')"/></v-card-text></v-card>
    </v-dialog>

    <v-dialog :model-value="!!deleteTarget" max-width="420" @update:model-value="deleteTarget = undefined">
      <v-card rounded="xl"><v-card-title class="px-5 pt-5">{{ t('automation.deleteTitle') }}</v-card-title><v-card-text class="px-5">{{ t('automation.deleteMessage', { name: deleteTarget?.name }) }}</v-card-text><v-card-actions class="px-5 pb-5"><v-spacer /><v-btn variant="text" @click="deleteTarget = undefined">{{ t('common.cancel') }}</v-btn><v-btn color="error" @click="remove">{{ t('common.delete') }}</v-btn></v-card-actions></v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.automation-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 340px), 1fr)); gap: 14px; }
.summary-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.filter-grid { display: grid; grid-template-columns: repeat(3, minmax(180px, 1fr)); gap: 12px; align-items: center; }
.detail-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.json-preview { direction: ltr; text-align: left; white-space: pre-wrap; overflow-wrap: anywhere; padding: 12px; border-radius: 10px; background: rgba(var(--v-theme-on-surface), 0.05); font: 0.78rem/1.6 ui-monospace, monospace; }
.rule-card { border: 1px solid rgba(var(--v-theme-on-surface), 0.08); }
.min-width-0 { min-width: 0; }
.status-select { max-width: 150px; }
.small-number { max-width: 150px; }
.json-field :deep(textarea) { direction: ltr; text-align: left; font-family: ui-monospace, monospace; font-size: 0.8rem; }
@media (max-width: 599px) {
  .summary-grid, .filter-grid, .detail-grid { grid-template-columns: 1fr; }
  .automation-grid { grid-template-columns: 1fr; padding: 12px !important; }
  .status-select { max-width: 130px; }
}
@media (min-width: 600px) and (max-width: 959px) { .filter-grid, .detail-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
