<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import { crmConfigApi, type CrmSegmentPayload } from '@/api/crmConfig'
import { customersApi } from '@/api/customers'
import AppEmptyState from '@/components/AppEmptyState.vue'
import AppErrorState from '@/components/AppErrorState.vue'
import AppLoadingState from '@/components/AppLoadingState.vue'
import { useApiError } from '@/composables/useApiError'
import { useNotifications } from '@/composables/useNotifications'
import { usePermission } from '@/composables/usePermission'
import { useServerLabel } from '@/composables/useServerLabel'
import type {
  CrmSegment,
  Customer,
  CustomerSource,
  CustomerStatus,
  CustomerTag,
  CustomerType,
} from '@/types/models'

defineProps<{
  types: CustomerType[]
  statuses: CustomerStatus[]
  sources: CustomerSource[]
  tags: CustomerTag[]
}>()

const { t } = useI18n()
const { smAndDown } = useDisplay()
const { can } = usePermission()
const { text: serverLabel } = useServerLabel()
const notifications = useNotifications()
const error = useApiError()
const loading = ref(false)
const saving = ref(false)
const running = ref(false)
const segments = ref<CrmSegment[]>([])
const selected = ref<CrmSegment | null>(null)
const results = ref<Customer[]>([])
const resultTotal = ref(0)
const resultPage = ref(1)
const resultPages = computed(() => Math.max(1, Math.ceil(resultTotal.value / 10)))
const editorOpen = ref(false)
const editingId = ref<number | null>(null)
const deleteTarget = ref<CrmSegment | null>(null)

const form = reactive({
  name: '',
  description: '',
  search: '',
  statusId: null as number | null,
  typeId: null as number | null,
  sourceId: null as number | null,
  tagIds: [] as number[],
  city: '',
  noEventSinceDays: null as number | null,
  includeHidden: false,
})

const canManage = computed(() => can('customers:manage-config') || can('customers:edit'))

async function load() {
  loading.value = true
  error.clear()
  try {
    segments.value = await crmConfigApi.segments()
  } catch (reason) {
    error.set(reason)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    name: '', description: '', search: '', statusId: null, typeId: null,
    sourceId: null, tagIds: [], city: '', noEventSinceDays: null, includeHidden: false,
  })
}

function openCreate() {
  editingId.value = null
  resetForm()
  editorOpen.value = true
}

function openEdit(segment: CrmSegment) {
  editingId.value = segment.id
  resetForm()
  const filter = segment.filter
  Object.assign(form, {
    name: segment.name,
    description: segment.description ?? '',
    search: typeof filter.search === 'string' ? filter.search : '',
    statusId: typeof filter.statusId === 'number' ? filter.statusId : null,
    typeId: typeof filter.typeId === 'number' ? filter.typeId : null,
    sourceId: typeof filter.sourceId === 'number' ? filter.sourceId : null,
    tagIds: Array.isArray(filter.tagIds) ? filter.tagIds.filter((id): id is number => typeof id === 'number') : [],
    city: typeof filter.city === 'string' ? filter.city : '',
    noEventSinceDays: typeof filter.noEventSinceDays === 'number' ? filter.noEventSinceDays : null,
    includeHidden: filter.includeHidden === true,
  })
  editorOpen.value = true
}

function payload(): CrmSegmentPayload {
  const filter: Record<string, unknown> = {}
  if (form.search.trim()) filter.search = form.search.trim()
  if (form.statusId !== null) filter.statusId = form.statusId
  if (form.typeId !== null) filter.typeId = form.typeId
  if (form.sourceId !== null) filter.sourceId = form.sourceId
  if (form.tagIds.length) filter.tagIds = form.tagIds
  if (form.city.trim()) filter.city = form.city.trim()
  if (form.noEventSinceDays !== null) filter.noEventSinceDays = form.noEventSinceDays
  if (form.includeHidden) filter.includeHidden = true
  return { name: form.name.trim(), description: form.description.trim() || undefined, filter }
}

async function save() {
  if (!form.name.trim()) return
  saving.value = true
  error.clear()
  try {
    if (editingId.value) await crmConfigApi.updateSegment(editingId.value, payload())
    else await crmConfigApi.createSegment(payload())
    editorOpen.value = false
    notifications.success(t('customers.segments.saved'))
    await load()
  } catch (reason) {
    error.set(reason)
  } finally {
    saving.value = false
  }
}

async function remove() {
  if (!deleteTarget.value) return
  saving.value = true
  error.clear()
  try {
    await crmConfigApi.deleteSegment(deleteTarget.value.id)
    if (selected.value?.id === deleteTarget.value.id) {
      selected.value = null
      results.value = []
      resultTotal.value = 0
    }
    deleteTarget.value = null
    notifications.success(t('customers.segments.deleted'))
    await load()
  } catch (reason) {
    error.set(reason)
  } finally {
    saving.value = false
  }
}

async function run(segment: CrmSegment, page = 1) {
  selected.value = segment
  resultPage.value = page
  running.value = true
  error.clear()
  try {
    const response = await customersApi.runSegment(segment.id, { page: page - 1, size: 10 })
    results.value = response.content
    resultTotal.value = response.totalElements
  } catch (reason) {
    error.set(reason)
  } finally {
    running.value = false
  }
}

function filterSummary(segment: CrmSegment) {
  const keys = Object.keys(segment.filter)
  return keys.length ? keys.map((key) => t(`customers.segments.filterNames.${key}`)).join(' · ') : t('customers.segments.allCustomers')
}

onMounted(load)
</script>

<template>
  <section aria-labelledby="crm-segments-title">
    <div class="d-flex align-center flex-wrap ga-2 mb-4">
      <div>
        <h2 id="crm-segments-title" class="text-h6">{{ t('customers.segments.title') }}</h2>
        <p class="text-body-2 text-medium-emphasis mb-0">{{ t('customers.segments.subtitle') }}</p>
      </div>
      <v-spacer />
      <v-btn v-if="canManage" color="primary" prepend-icon="mdi-plus" @click="openCreate">
        {{ t('customers.segments.new') }}
      </v-btn>
    </div>

    <AppErrorState v-if="error.message.value" class="mb-4" :title="error.message.value" :retry-label="t('common.retry')" @retry="load" />
    <AppLoadingState v-if="loading" variant="table" :rows="4" :label="t('common.loading')" />
    <v-row v-else-if="segments.length">
      <v-col v-for="segment in segments" :key="segment.id" cols="12" md="6" xl="4">
        <v-card rounded="xl" class="app-data-surface h-100">
          <v-card-text>
            <div class="d-flex align-start ga-3">
              <v-avatar color="primary" variant="tonal" rounded="lg"><v-icon icon="mdi-account-filter-outline" /></v-avatar>
              <div class="flex-grow-1 min-width-0">
                <div class="font-weight-bold">{{ segment.name }}</div>
                <div class="text-body-2 text-medium-emphasis mt-1">{{ segment.description || filterSummary(segment) }}</div>
                <div class="text-caption text-medium-emphasis mt-2">{{ filterSummary(segment) }}</div>
              </div>
            </div>
          </v-card-text>
          <v-card-actions>
            <v-btn color="primary" variant="tonal" prepend-icon="mdi-play" :loading="running && selected?.id === segment.id" @click="run(segment)">
              {{ t('customers.segments.run') }}
            </v-btn>
            <v-spacer />
            <v-btn v-if="canManage" :aria-label="t('common.edit')" icon="mdi-pencil" size="small" variant="text" @click="openEdit(segment)" />
            <v-btn v-if="canManage" :aria-label="t('common.delete')" icon="mdi-delete" size="small" variant="text" color="error" @click="deleteTarget = segment" />
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
    <AppEmptyState v-else icon="mdi-account-filter-outline" :title="t('customers.segments.empty')" :description="t('customers.segments.emptyDescription')" />

    <v-card v-if="selected" rounded="xl" class="app-data-surface mt-5">
      <v-card-title class="d-flex align-center flex-wrap ga-2">
        <span>{{ t('customers.segments.results', { name: selected.name }) }}</span>
        <v-chip color="primary" variant="tonal">{{ resultTotal }}</v-chip>
      </v-card-title>
      <v-divider />
      <AppLoadingState v-if="running" variant="table" :rows="4" :label="t('common.loading')" />
      <v-list v-else-if="results.length" lines="two">
        <v-list-item v-for="customer in results" :key="customer.id" :title="customer.displayName" :subtitle="`${customer.customerCode} · ${serverLabel(customer.status.name)}`">
          <template #prepend><v-avatar color="primary" variant="tonal"><v-icon icon="mdi-account" /></v-avatar></template>
          <template #append><v-chip size="small" variant="tonal">{{ serverLabel(customer.type.name) }}</v-chip></template>
        </v-list-item>
      </v-list>
      <AppEmptyState v-else icon="mdi-account-search-outline" :title="t('customers.segments.noResults')" />
      <v-card-actions v-if="resultPages > 1" class="justify-center">
        <v-pagination v-model="resultPage" :length="resultPages" :total-visible="5" @update:model-value="(page) => run(selected!, page)" />
      </v-card-actions>
    </v-card>

    <v-dialog v-model="editorOpen" :fullscreen="smAndDown" max-width="760" persistent>
      <v-card rounded="lg">
        <v-card-title>{{ editingId ? t('customers.segments.edit') : t('customers.segments.new') }}</v-card-title>
        <v-card-text>
          <v-row dense>
            <v-col cols="12" sm="6"><v-text-field v-model="form.name" :label="t('customers.segments.name')" maxlength="100" /></v-col>
            <v-col cols="12" sm="6"><v-text-field v-model="form.description" :label="t('customers.segments.description')" maxlength="255" /></v-col>
            <v-col cols="12"><v-text-field v-model="form.search" :label="t('customers.filters.searchPlaceholder')" prepend-inner-icon="mdi-magnify" clearable /></v-col>
            <v-col cols="12" sm="4"><v-select v-model="form.statusId" :items="statuses" item-title="name" item-value="id" :label="t('customers.filters.status')" clearable /></v-col>
            <v-col cols="12" sm="4"><v-select v-model="form.typeId" :items="types" item-title="name" item-value="id" :label="t('customers.filters.type')" clearable /></v-col>
            <v-col cols="12" sm="4"><v-select v-model="form.sourceId" :items="sources" item-title="name" item-value="id" :label="t('customers.filters.source')" clearable /></v-col>
            <v-col cols="12" sm="6"><v-autocomplete v-model="form.tagIds" :items="tags" item-title="name" item-value="id" :label="t('customers.filters.tags')" multiple chips closable-chips /></v-col>
            <v-col cols="12" sm="3"><v-text-field v-model="form.city" :label="t('customers.filters.city')" clearable /></v-col>
            <v-col cols="12" sm="3"><v-text-field v-model.number="form.noEventSinceDays" type="number" min="1" :label="t('customers.segments.inactiveDays')" clearable /></v-col>
            <v-col cols="12"><v-switch v-model="form.includeHidden" :label="t('customers.filters.includeHidden')" color="primary" hide-details /></v-col>
          </v-row>
        </v-card-text>
        <v-card-actions><v-spacer /><v-btn variant="text" @click="editorOpen = false">{{ t('common.cancel') }}</v-btn><v-btn color="primary" :loading="saving" :disabled="!form.name.trim()" @click="save">{{ t('common.save') }}</v-btn></v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog :model-value="deleteTarget !== null" max-width="430" @update:model-value="deleteTarget = null">
      <v-card rounded="lg">
        <v-card-title>{{ t('customers.segments.deleteTitle') }}</v-card-title>
        <v-card-text>{{ t('customers.segments.deleteBody', { name: deleteTarget?.name }) }}</v-card-text>
        <v-card-actions><v-spacer /><v-btn variant="text" @click="deleteTarget = null">{{ t('common.cancel') }}</v-btn><v-btn color="error" :loading="saving" @click="remove">{{ t('common.delete') }}</v-btn></v-card-actions>
      </v-card>
    </v-dialog>
  </section>
</template>

<style scoped>
.min-width-0 { min-width: 0; }
@media (max-width: 599px) {
  :deep(.v-btn) { min-height: 44px; }
}
</style>
