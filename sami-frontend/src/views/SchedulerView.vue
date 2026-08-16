<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import { schedulerApi } from '@/api/scheduler'
import type { ScheduledJob, SchedulerExecution, SchedulerHandler, SchedulerStatus } from '@/types/scheduler'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import { usePermission } from '@/composables/usePermission'
import { useServerLabel } from '@/composables/useServerLabel'
import AppEmptyState from '@/components/AppEmptyState.vue'
import AppLoadingState from '@/components/AppLoadingState.vue'
import AppErrorState from '@/components/AppErrorState.vue'
import AppPageHeader from '@/components/AppPageHeader.vue'

const { t } = useI18n()
const { xs } = useDisplay()
const { formatDateTime, formatNumber } = useFormat()
const { can } = usePermission()
const { enumLabel, statusLabel } = useServerLabel()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()
const loading = ref(false)
const saving = ref(false)
const jobs = ref<ScheduledJob[]>([])
const handlers = ref<SchedulerHandler[]>([])
const statuses = ref<SchedulerStatus[]>([])
const search = ref('')
const statusFilter = ref<string>()
const detail = ref<ScheduledJob>()
const formOpen = ref(false)
const editing = ref<ScheduledJob>()
const deleteTarget = ref<ScheduledJob>()
const historyJob = ref<ScheduledJob>()
const showingRecentExecutions = ref(false)
const executions = ref<SchedulerExecution[]>([])
const historyLoading = ref(false)
const formError = ref('')

const code = ref('')
const name = ref('')
const description = ref('')
const handlerKey = ref('')
const scheduleKind = ref('CRON')
const cronExpression = ref('')
const intervalSeconds = ref<number>()
const timezone = ref('Asia/Tehran')
const timeoutSeconds = ref(300)
const maxFailures = ref(3)
const catchUp = ref(false)
const runAt = ref('')
const config = ref('{}')

const visibleJobs = computed(() => {
  const query = search.value.trim().toLowerCase()
  return jobs.value.filter((job) =>
    (!statusFilter.value || job.statusCode === statusFilter.value)
    && (!query || `${job.name} ${job.code} ${job.handlerKey}`.toLowerCase().includes(query)),
  )
})
const statusItems = computed(() => statuses.value.map((s) => ({ title: s.name, value: s.code })))
const handlerItems = computed(() => handlers.value.map((h) => ({ title: h.description || h.key, value: h.key })))
const scheduleKinds = computed(() => [
  { title: t('scheduler.kinds.cron'), value: 'CRON' },
  { title: t('scheduler.kinds.interval'), value: 'INTERVAL' },
  { title: t('scheduler.kinds.once'), value: 'ONCE' },
])

async function load(): Promise<void> {
  loading.value = true
  clearError()
  try {
    ;[jobs.value, handlers.value, statuses.value] = await Promise.all([
      schedulerApi.jobs(), schedulerApi.handlers(), schedulerApi.statuses(),
    ])
  } catch (error) {
    setError(error)
  } finally {
    loading.value = false
  }
}

async function openDetail(job: ScheduledJob): Promise<void> {
  try {
    detail.value = await schedulerApi.get(job.id)
  } catch (error) {
    setError(error)
  }
}

function openForm(job?: ScheduledJob): void {
  editing.value = job
  formError.value = ''
  code.value = job?.code ?? ''
  name.value = job?.name ?? ''
  description.value = job?.description ?? ''
  handlerKey.value = job?.handlerKey ?? ''
  scheduleKind.value = job?.scheduleKind ?? 'CRON'
  cronExpression.value = job?.cronExpression ?? ''
  intervalSeconds.value = job?.intervalSeconds ?? undefined
  timezone.value = job?.timezone ?? 'Asia/Tehran'
  timeoutSeconds.value = job?.timeoutSeconds ?? 300
  maxFailures.value = job?.maxFailures ?? 3
  catchUp.value = job?.catchUp ?? false
  config.value = JSON.stringify(job?.config ?? {}, null, 2)
  runAt.value = ''
  formOpen.value = true
}

async function save(): Promise<void> {
  if (!name.value.trim() || (!editing.value && (!code.value.trim() || !handlerKey.value))) {
    formError.value = t('validation.required')
    return
  }
  saving.value = true
  formError.value = ''
  try {
    const parsed = JSON.parse(config.value || '{}') as Record<string, unknown>
    if (editing.value) {
      await schedulerApi.update(editing.value.id, {
        name: name.value.trim(), description: description.value || undefined,
        cronExpression: cronExpression.value || undefined, intervalSeconds: intervalSeconds.value,
        timezone: timezone.value || undefined, config: parsed, timeoutSeconds: timeoutSeconds.value,
        maxFailures: maxFailures.value, catchUp: catchUp.value,
      })
    } else {
      await schedulerApi.create({
        code: code.value.trim(), name: name.value.trim(), description: description.value || undefined,
        handlerKey: handlerKey.value, scheduleKind: scheduleKind.value,
        cronExpression: cronExpression.value || undefined, intervalSeconds: intervalSeconds.value,
        timezone: timezone.value || undefined, config: parsed, timeoutSeconds: timeoutSeconds.value,
        catchUp: catchUp.value, runAt: runAt.value || undefined,
      })
    }
    formOpen.value = false
    await load()
  } catch (error) {
    if (error instanceof SyntaxError) formError.value = t('scheduler.invalidJson')
    else setError(error)
  } finally {
    saving.value = false
  }
}

async function changeStatus(job: ScheduledJob, status: string): Promise<void> {
  try {
    await schedulerApi.changeStatus(job.id, status)
    await load()
  } catch (error) { setError(error) }
}
async function run(job: ScheduledJob): Promise<void> {
  try {
    await schedulerApi.run(job.id)
    await openHistory(job)
    await load()
  } catch (error) { setError(error) }
}
async function openHistory(job: ScheduledJob): Promise<void> {
  showingRecentExecutions.value = false
  historyJob.value = job
  historyLoading.value = true
  try {
    executions.value = (await schedulerApi.executions(job.id, { size: 50, sort: 'startedAt,desc' })).content
  } catch (error) { setError(error) }
  finally { historyLoading.value = false }
}
async function openRecentExecutions(): Promise<void> {
  historyJob.value = undefined
  showingRecentExecutions.value = true
  historyLoading.value = true
  try {
    executions.value = (await schedulerApi.recentExecutions({ size: 50, sort: 'startedAt,desc' })).content
  } catch (error) { setError(error) }
  finally { historyLoading.value = false }
}
async function remove(): Promise<void> {
  if (!deleteTarget.value) return
  try {
    await schedulerApi.remove(deleteTarget.value.id)
    deleteTarget.value = undefined
    await load()
  } catch (error) { setError(error) }
}
onMounted(load)
</script>

<template>
  <div>
    <AppPageHeader icon="mdi-calendar-clock-outline" :eyebrow="t('scheduler.eyebrow')" :title="t('scheduler.title')">
      <template #actions><div class="d-flex flex-wrap ga-2"><v-btn variant="tonal" prepend-icon="mdi-history" @click="openRecentExecutions">{{ t('scheduler.history') }}</v-btn><v-btn v-if="can('scheduler:create')" color="primary" prepend-icon="mdi-plus" @click="openForm()">{{ t('scheduler.create') }}</v-btn></div></template>
    </AppPageHeader>
    <AppErrorState v-if="errorMessage" class="mb-4" :title="errorMessage" :retry-label="t('common.retry')" @retry="load" />
    <v-card rounded="xl" class="mb-4"><v-card-text class="d-flex flex-column flex-sm-row ga-3">
      <v-text-field v-model="search" prepend-inner-icon="mdi-magnify" :label="t('common.search')" clearable />
      <v-select v-model="statusFilter" :items="statusItems" :label="t('scheduler.status')" clearable />
      <v-btn variant="tonal" prepend-icon="mdi-refresh" :loading="loading" @click="load">{{ t('common.refresh') }}</v-btn>
    </v-card-text></v-card>
    <AppLoadingState v-if="loading" variant="cards" :rows="4" :label="t('common.loading')" />
    <div v-else-if="visibleJobs.length" class="scheduler-grid">
      <v-card v-for="job in visibleJobs" :key="job.id" rounded="xl" class="job-card">
        <v-card-text>
          <div class="d-flex align-start ga-3">
            <v-avatar rounded="lg" :color="job.handlerRegistered ? 'primary' : 'error'" variant="tonal"><v-icon icon="mdi-timer-cog-outline" /></v-avatar>
            <button type="button" class="app-inline-action flex-grow-1 min-width-0" @click="openDetail(job)">
              <div class="font-weight-bold text-truncate">{{ job.name }}</div>
              <div class="text-caption text-medium-emphasis text-truncate">{{ job.code }} · {{ job.schedule }}</div>
            </button>
            <v-menu><template #activator="{ props }"><v-btn v-bind="props" icon="mdi-dots-vertical" variant="text" /></template>
              <v-list density="compact">
                <v-list-item prepend-icon="mdi-information-outline" :title="t('common.details')" @click="openDetail(job)" />
                <v-list-item v-if="can('scheduler:edit')" prepend-icon="mdi-pencil-outline" :title="t('common.edit')" @click="openForm(job)" />
                <v-list-item v-if="can('scheduler:execute') && job.handlerRegistered" prepend-icon="mdi-play-outline" :title="t('scheduler.run')" @click="run(job)" />
                <v-list-item prepend-icon="mdi-history" :title="t('scheduler.history')" @click="openHistory(job)" />
                <v-list-item v-if="can('scheduler:delete') && !job.system" base-color="error" prepend-icon="mdi-delete-outline" :title="t('common.delete')" @click="deleteTarget = job" />
              </v-list>
            </v-menu>
          </div>
          <div class="d-flex flex-wrap ga-2 mt-4">
            <v-chip size="x-small" variant="tonal">{{ statusLabel(job.statusName) }}</v-chip>
            <v-chip v-if="!job.handlerRegistered" size="x-small" color="error" variant="tonal">{{ t('scheduler.missingHandler') }}</v-chip>
            <v-chip v-if="job.system" size="x-small" variant="outlined">{{ t('scheduler.system') }}</v-chip>
          </div>
          <div class="mt-4 text-caption text-medium-emphasis d-flex justify-space-between"><span>{{ t('scheduler.nextRun') }}</span><strong>{{ job.nextRunAt ? formatDateTime(job.nextRunAt) : '—' }}</strong></div>
          <v-select v-if="can('scheduler:edit')" :model-value="job.statusCode" :items="statusItems" density="compact" hide-details class="mt-3" @update:model-value="changeStatus(job, $event)" />
        </v-card-text>
      </v-card>
    </div>
    <AppEmptyState v-else-if="!loading" icon="mdi-calendar-remove-outline" :title="t('scheduler.empty')" :action-label="can('scheduler:create') ? t('scheduler.create') : undefined" @action="openForm()" />

    <v-dialog v-model="formOpen" max-width="760" :fullscreen="xs" scrollable><v-card :rounded="xs ? 0 : 'xl'">
      <v-card-title class="d-flex px-5 pt-5">{{ editing ? t('scheduler.edit') : t('scheduler.create') }}<v-spacer /><v-btn icon="mdi-close" variant="text" @click="formOpen=false" /></v-card-title>
      <v-card-text class="px-5"><v-alert v-if="formError" type="error" variant="tonal" class="mb-4">{{ formError }}</v-alert><v-row>
        <v-col cols="12" sm="6"><v-text-field v-model="name" :label="t('scheduler.name')" /></v-col>
        <v-col cols="12" sm="6"><v-text-field v-model="code" :disabled="!!editing" :label="t('scheduler.code')" /></v-col>
        <v-col cols="12"><v-textarea v-model="description" rows="2" :label="t('scheduler.description')" /></v-col>
        <v-col cols="12" sm="6"><v-select v-model="handlerKey" :disabled="!!editing" :items="handlerItems" :label="t('scheduler.handler')" /></v-col>
        <v-col cols="12" sm="6"><v-select v-model="scheduleKind" :disabled="!!editing" :items="scheduleKinds" :label="t('scheduler.scheduleKind')" /></v-col>
        <v-col v-if="scheduleKind==='CRON'" cols="12" sm="6"><v-text-field v-model="cronExpression" dir="ltr" :label="t('scheduler.cron')" /></v-col>
        <v-col v-if="scheduleKind==='INTERVAL'" cols="12" sm="6"><v-text-field v-model.number="intervalSeconds" type="number" min="1" :label="t('scheduler.interval')" /></v-col>
        <v-col v-if="scheduleKind==='ONCE' && !editing" cols="12" sm="6"><v-text-field v-model="runAt" type="datetime-local" :label="t('scheduler.runAt')" /></v-col>
        <v-col cols="12" sm="6"><v-text-field v-model="timezone" dir="ltr" :label="t('scheduler.timezone')" /></v-col>
        <v-col cols="6"><v-text-field v-model.number="timeoutSeconds" type="number" :label="t('scheduler.timeout')" /></v-col>
        <v-col v-if="editing" cols="6"><v-text-field v-model.number="maxFailures" type="number" :label="t('scheduler.maxFailures')" /></v-col>
        <v-col cols="12"><v-textarea v-model="config" rows="5" dir="ltr" :label="t('scheduler.config')" /></v-col>
        <v-col cols="12"><v-switch v-model="catchUp" :label="t('scheduler.catchUp')" /></v-col>
      </v-row></v-card-text>
      <v-card-actions class="px-5 pb-5"><v-spacer /><v-btn variant="text" @click="formOpen=false">{{ t('common.cancel') }}</v-btn><v-btn color="primary" :loading="saving" @click="save">{{ t('common.save') }}</v-btn></v-card-actions>
    </v-card></v-dialog>

    <v-dialog :model-value="!!detail" max-width="620" :fullscreen="xs" @update:model-value="detail=undefined"><v-card :rounded="xs?0:'xl'"><v-card-title class="d-flex px-5 pt-5">{{ detail?.name }}<v-spacer/><v-btn icon="mdi-close" variant="text" @click="detail=undefined"/></v-card-title><v-card-text class="px-5"><v-list density="compact">
      <v-list-item :title="t('scheduler.handler')" :subtitle="detail?.handlerKey"/><v-list-item :title="t('scheduler.scheduleKind')" :subtitle="detail?.schedule"/><v-list-item :title="t('scheduler.lastRun')" :subtitle="detail?.lastRunAt ? formatDateTime(detail.lastRunAt) : '—'"/><v-list-item :title="t('scheduler.failures')" :subtitle="formatNumber(detail?.consecutiveFailures ?? 0)"/>
    </v-list></v-card-text></v-card></v-dialog>

    <v-dialog :model-value="!!historyJob" max-width="760" :fullscreen="xs" @update:model-value="historyJob=undefined"><v-card :rounded="xs?0:'xl'"><v-card-title class="d-flex px-5 pt-5">{{ t('scheduler.history') }}<v-spacer/><v-btn icon="mdi-close" variant="text" @click="historyJob=undefined"/></v-card-title><v-card-text class="px-5"><v-progress-linear v-if="historyLoading" indeterminate/><v-list v-else-if="executions.length"><v-list-item v-for="item in executions" :key="item.id" :title="item.executionNumber" :subtitle="`${formatDateTime(item.startedAt)} · ${item.durationMs ?? 0} ms`"><template #append><v-chip size="x-small" variant="tonal">{{ enumLabel(item.status) }}</v-chip></template><v-alert v-if="item.errorMessage" type="error" density="compact" variant="tonal" class="mt-2">{{ item.errorMessage }}</v-alert></v-list-item></v-list><AppEmptyState v-else dense :title="t('scheduler.noExecutions')"/></v-card-text></v-card></v-dialog>

    <v-dialog v-model="showingRecentExecutions" max-width="760" :fullscreen="xs"><v-card :rounded="xs?0:'xl'"><v-card-title class="d-flex px-5 pt-5">{{ t('scheduler.history') }}<v-spacer/><v-btn icon="mdi-close" variant="text" @click="showingRecentExecutions=false"/></v-card-title><v-card-text class="px-5"><v-progress-linear v-if="historyLoading" indeterminate/><v-list v-else-if="executions.length"><v-list-item v-for="item in executions" :key="item.id" :title="item.executionNumber" :subtitle="formatDateTime(item.startedAt)"><template #append><v-chip size="x-small" variant="tonal">{{ enumLabel(item.status) }}</v-chip></template></v-list-item></v-list><AppEmptyState v-else dense :title="t('scheduler.noExecutions')"/></v-card-text></v-card></v-dialog>

    <v-dialog :model-value="!!deleteTarget" max-width="420" @update:model-value="deleteTarget=undefined"><v-card rounded="xl"><v-card-title class="px-5 pt-5">{{ t('scheduler.deleteTitle') }}</v-card-title><v-card-text class="px-5">{{ t('scheduler.deleteMessage',{name:deleteTarget?.name}) }}</v-card-text><v-card-actions class="px-5 pb-5"><v-spacer/><v-btn variant="text" @click="deleteTarget=undefined">{{ t('common.cancel') }}</v-btn><v-btn color="error" @click="remove">{{ t('common.delete') }}</v-btn></v-card-actions></v-card></v-dialog>
  </div>
</template>

<style scoped>
.scheduler-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(100%,340px),1fr));gap:14px}.job-card{border:1px solid rgba(var(--v-theme-on-surface),.08)}.min-width-0{min-width:0}@media(max-width:599px){.scheduler-grid{grid-template-columns:1fr}}
</style>
