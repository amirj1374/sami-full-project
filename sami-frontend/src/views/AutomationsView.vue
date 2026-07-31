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
  AutomationStatus,
  AutomationTrigger,
} from '@/types/automation'
import { automationRuleSchema } from '@/schemas/automation'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import { usePermission } from '@/composables/usePermission'
import AppEmptyState from '@/components/AppEmptyState.vue'
import AppPageHeader from '@/components/AppPageHeader.vue'

const { t } = useI18n()
const { xs } = useDisplay()
const { formatDateTime, formatNumber } = useFormat()
const { can } = usePermission()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()

const loading = ref(false)
const saving = ref(false)
const rows = ref<AutomationRule[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const search = ref('')
const statusCode = ref<string>()
const statuses = ref<AutomationStatus[]>([])
const triggers = ref<AutomationTrigger[]>([])
const actionDescriptors = ref<AutomationActionDescriptor[]>([])
const formOpen = ref(false)
const editing = ref<AutomationRule>()
const deleteTarget = ref<AutomationRule>()
const historyRule = ref<AutomationRule>()
const executions = ref<AutomationExecution[]>([])
const historyLoading = ref(false)
const logs = ref<AutomationExecutionLog[]>([])
const logsOpen = ref(false)
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

const { handleSubmit, defineField, errors, resetForm } = useForm({
  validationSchema: computed(() => toTypedSchema(automationRuleSchema(t))),
  initialValues: { code: '', name: '', statusCode: '', triggerType: '' },
})
const [code, codeProps] = defineField('code')
const [name, nameProps] = defineField('name')
const [formStatus, statusProps] = defineField('statusCode')
const [triggerType, triggerProps] = defineField('triggerType')

const statusItems = computed(() => statuses.value.map((s) => ({ title: s.name, value: s.code })))
const triggerItems = computed(() => triggers.value.map((v) => ({ title: v.label, value: v.type, subtitle: v.category })))
const actionItems = computed(() => actionDescriptors.value.map((v) => ({ title: v.label, value: v.type })))

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
      sort: 'priority,asc',
    }
    const result = await automationsApi.list(params)
    rows.value = result.content
    total.value = result.totalElements
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
}

function removeAction(index: number): void {
  actions.value.splice(index, 1)
  actionConfigDrafts.value.splice(index, 1)
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
    await load()
  } catch (error) {
    setError(error)
  }
}

async function run(row: AutomationRule): Promise<void> {
  try {
    await automationsApi.run(row.id)
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
    logs.value = await automationsApi.executionLogs(execution.id)
    logsOpen.value = true
  } catch (error) {
    setError(error)
  }
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
  await load()
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
      {{ errorMessage }}
    </v-alert>

    <v-card class="mb-4" rounded="xl">
      <v-card-text class="d-flex flex-column flex-sm-row ga-3">
        <v-text-field v-model="search" :label="t('common.search')" prepend-inner-icon="mdi-magnify" clearable @keyup.enter="page = 1; load()" />
        <v-select v-model="statusCode" :items="statusItems" :label="t('automation.status')" clearable @update:model-value="page = 1; load()" />
        <v-btn variant="tonal" prepend-icon="mdi-refresh" :loading="loading" @click="load">{{ t('common.refresh') }}</v-btn>
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
                  <v-chip size="x-small" label variant="tonal">{{ row.statusName }}</v-chip>
                </div>
                <div class="text-caption text-medium-emphasis">{{ row.code }} · {{ row.triggerType }}</div>
              </div>
              <v-menu>
                <template #activator="{ props }"><v-btn v-bind="props" icon="mdi-dots-vertical" variant="text" /></template>
                <v-list density="compact">
                  <v-list-item v-if="can('automation:edit')" prepend-icon="mdi-pencil-outline" :title="t('common.edit')" @click="openForm(row)" />
                  <v-list-item v-if="can('automation:execute')" prepend-icon="mdi-play-outline" :title="t('automation.run')" @click="run(row)" />
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
              <v-textarea v-model="actionConfigDrafts[index]" rows="3" :label="t('automation.configuration')" />
              <div class="d-flex flex-wrap ga-3"><v-switch v-model="action.continueOnError" :label="t('automation.continueOnError')" /><v-text-field v-model.number="action.retryCount" type="number" min="0" :label="t('automation.retries')" class="small-number" /></div>
            </v-card-text>
          </v-card>
        </v-card-text>
        <v-card-actions class="px-5 pb-5"><v-spacer /><v-btn variant="text" @click="formOpen = false">{{ t('common.cancel') }}</v-btn><v-btn color="primary" :loading="saving" @click="submit">{{ t('common.save') }}</v-btn></v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog :model-value="!!historyRule" max-width="820" :fullscreen="xs" @update:model-value="historyRule = undefined">
      <v-card :rounded="xs ? 0 : 'xl'">
        <v-card-title class="d-flex px-5 pt-5">{{ t('automation.history') }}<v-spacer /><v-btn icon="mdi-close" variant="text" @click="historyRule = undefined" /></v-card-title>
        <v-card-text class="px-5">
          <v-progress-linear v-if="historyLoading" indeterminate />
          <v-list v-else-if="executions.length">
            <v-list-item v-for="execution in executions" :key="execution.id" :title="execution.executionNumber" :subtitle="formatDateTime(execution.startedAt)" @click="openLogs(execution)">
              <template #append><v-chip size="x-small" variant="tonal">{{ execution.status }}</v-chip></template>
            </v-list-item>
          </v-list>
          <AppEmptyState v-else dense :title="t('automation.noExecutions')" />
        </v-card-text>
      </v-card>
    </v-dialog>

    <v-dialog v-model="logsOpen" max-width="680" :fullscreen="xs">
      <v-card :rounded="xs ? 0 : 'xl'"><v-card-title class="px-5 pt-5">{{ t('automation.executionLogs') }}</v-card-title><v-card-text class="px-5"><v-timeline side="end" density="compact"><v-timeline-item v-for="log in logs" :key="log.id" size="small"><div class="font-weight-medium">{{ log.actionType }} · {{ log.status }}</div><div class="text-body-2 text-medium-emphasis">{{ log.message }}</div><div class="text-caption">{{ formatDateTime(log.occurredAt) }}</div></v-timeline-item></v-timeline></v-card-text></v-card>
    </v-dialog>

    <v-dialog :model-value="!!deleteTarget" max-width="420" @update:model-value="deleteTarget = undefined">
      <v-card rounded="xl"><v-card-title class="px-5 pt-5">{{ t('automation.deleteTitle') }}</v-card-title><v-card-text class="px-5">{{ t('automation.deleteMessage', { name: deleteTarget?.name }) }}</v-card-text><v-card-actions class="px-5 pb-5"><v-spacer /><v-btn variant="text" @click="deleteTarget = undefined">{{ t('common.cancel') }}</v-btn><v-btn color="error" @click="remove">{{ t('common.delete') }}</v-btn></v-card-actions></v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.automation-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 340px), 1fr)); gap: 14px; }
.rule-card { border: 1px solid rgba(var(--v-theme-on-surface), 0.08); }
.min-width-0 { min-width: 0; }
.status-select { max-width: 150px; }
.small-number { max-width: 150px; }
.json-field :deep(textarea) { direction: ltr; text-align: left; font-family: ui-monospace, monospace; font-size: 0.8rem; }
@media (max-width: 599px) {
  .automation-grid { grid-template-columns: 1fr; padding: 12px !important; }
  .status-select { max-width: 130px; }
}
</style>
