<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import { crmConfigApi } from '@/api/crmConfig'
import AppEmptyState from '@/components/AppEmptyState.vue'
import AppErrorState from '@/components/AppErrorState.vue'
import AppLoadingState from '@/components/AppLoadingState.vue'
import { useApiError } from '@/composables/useApiError'
import { useNotifications } from '@/composables/useNotifications'
import { useServerLabel } from '@/composables/useServerLabel'
import type {
  CustomerSource,
  CustomerStatus,
  CustomerTag,
  CustomerType,
  PreferenceDefinition,
  ProfileFieldType,
} from '@/types/models'

type ConfigKind = 'types' | 'statuses' | 'sources' | 'tags' | 'preferences' | 'rules'
type ConfigItem = CustomerType | CustomerStatus | CustomerSource | CustomerTag | PreferenceDefinition

const emit = defineEmits<{ changed: [] }>()
const { t } = useI18n()
const { smAndDown } = useDisplay()
const { text: serverLabel } = useServerLabel()
const notifications = useNotifications()
const error = useApiError()
const loading = ref(false)
const saving = ref(false)
const tab = ref<ConfigKind>('types')
const types = ref<CustomerType[]>([])
const statuses = ref<CustomerStatus[]>([])
const sources = ref<CustomerSource[]>([])
const tags = ref<CustomerTag[]>([])
const preferences = ref<PreferenceDefinition[]>([])
const rules = ref<Record<string, boolean>>({})
const editorOpen = ref(false)
const editingId = ref<number | null>(null)
const deleteTarget = ref<ConfigItem | null>(null)

const form = reactive({
  code: '',
  name: '',
  description: '',
  active: true,
  displayOrder: 10,
  isBlocking: false,
  hiddenByDefault: false,
  color: '',
  prefKey: '',
  fieldType: 'TEXT' as ProfileFieldType,
  required: false,
  minLength: null as number | null,
  maxLength: null as number | null,
  pattern: '',
  optionsText: '',
})

const tabs = computed(() => [
  { value: 'types' as const, label: t('customers.config.types'), icon: 'mdi-shape-outline' },
  { value: 'statuses' as const, label: t('customers.config.statuses'), icon: 'mdi-state-machine' },
  { value: 'sources' as const, label: t('customers.config.sources'), icon: 'mdi-source-branch' },
  { value: 'tags' as const, label: t('customers.config.tags'), icon: 'mdi-tag-multiple-outline' },
  { value: 'preferences' as const, label: t('customers.config.preferences'), icon: 'mdi-form-textbox' },
  { value: 'rules' as const, label: t('customers.config.duplicateRules'), icon: 'mdi-content-duplicate' },
])

const items = computed<ConfigItem[]>(() => {
  if (tab.value === 'types') return types.value
  if (tab.value === 'statuses') return statuses.value
  if (tab.value === 'sources') return sources.value
  if (tab.value === 'tags') return tags.value
  if (tab.value === 'preferences') return preferences.value
  return []
})

const fieldTypes = computed(() =>
  (['TEXT', 'NUMBER', 'DATE', 'BOOLEAN', 'SELECT'] as ProfileFieldType[]).map((value) => ({
    value,
    title: t(`customers.config.fieldTypes.${value}`),
  })),
)

async function load() {
  loading.value = true
  error.clear()
  try {
    ;[types.value, statuses.value, sources.value, tags.value, preferences.value, rules.value] = await Promise.all([
      crmConfigApi.types(),
      crmConfigApi.statuses(),
      crmConfigApi.sources(),
      crmConfigApi.tags(),
      crmConfigApi.preferenceDefinitions(false),
      crmConfigApi.duplicateRules(),
    ])
  } catch (reason) {
    error.set(reason)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    code: '', name: '', description: '', active: true, displayOrder: 10,
    isBlocking: false, hiddenByDefault: false, color: '', prefKey: '',
    fieldType: 'TEXT', required: false, minLength: null, maxLength: null,
    pattern: '', optionsText: '',
  })
}

function openCreate() {
  editingId.value = null
  resetForm()
  editorOpen.value = true
}

function openEdit(item: ConfigItem) {
  resetForm()
  editingId.value = item.id
  if ('code' in item) form.code = item.code
  form.name = 'label' in item ? item.label : item.name
  if ('description' in item) form.description = item.description ?? ''
  if ('active' in item) form.active = item.active
  if ('displayOrder' in item) form.displayOrder = item.displayOrder
  if ('isBlocking' in item) {
    form.isBlocking = item.isBlocking
    form.hiddenByDefault = item.hiddenByDefault
  }
  if ('color' in item) form.color = item.color ?? ''
  if ('prefKey' in item) {
    form.prefKey = item.prefKey
    form.fieldType = item.fieldType
    form.required = item.required
    form.minLength = item.minLength
    form.maxLength = item.maxLength
    form.pattern = item.pattern ?? ''
    form.optionsText = item.options?.join('\n') ?? ''
  }
  editorOpen.value = true
}

async function save() {
  if (!form.name.trim()) return
  saving.value = true
  error.clear()
  try {
    if (tab.value === 'types') {
      const payload = { code: form.code.trim(), name: form.name.trim(), description: form.description.trim() || undefined, active: form.active, displayOrder: form.displayOrder }
      if (editingId.value) await crmConfigApi.updateType(editingId.value, payload)
      else await crmConfigApi.createType(payload)
    } else if (tab.value === 'statuses') {
      const payload = { code: form.code.trim(), name: form.name.trim(), description: form.description.trim() || undefined, isBlocking: form.isBlocking, hiddenByDefault: form.hiddenByDefault, displayOrder: form.displayOrder }
      if (editingId.value) await crmConfigApi.updateStatus(editingId.value, payload)
      else await crmConfigApi.createStatus(payload)
    } else if (tab.value === 'sources') {
      const payload = { code: form.code.trim(), name: form.name.trim(), active: form.active, displayOrder: form.displayOrder }
      if (editingId.value) await crmConfigApi.updateSource(editingId.value, payload)
      else await crmConfigApi.createSource(payload)
    } else if (tab.value === 'tags') {
      const payload = { name: form.name.trim(), description: form.description.trim() || undefined, color: form.color.trim() || undefined, active: form.active }
      if (editingId.value) await crmConfigApi.updateTag(editingId.value, payload)
      else await crmConfigApi.createTag(payload)
    } else if (tab.value === 'preferences') {
      const payload = {
        prefKey: form.prefKey.trim(), label: form.name.trim(), fieldType: form.fieldType,
        required: form.required, minLength: form.minLength ?? undefined,
        maxLength: form.maxLength ?? undefined, pattern: form.pattern.trim() || undefined,
        options: form.optionsText.split('\n').map((value) => value.trim()).filter(Boolean),
        active: form.active, displayOrder: form.displayOrder,
      }
      if (editingId.value) await crmConfigApi.updatePreferenceDefinition(editingId.value, payload)
      else await crmConfigApi.createPreferenceDefinition(payload)
    }
    editorOpen.value = false
    notifications.success(t('customers.config.saved'))
    await load()
    emit('changed')
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
    if (tab.value === 'types') await crmConfigApi.deleteType(deleteTarget.value.id)
    else if (tab.value === 'statuses') await crmConfigApi.deleteStatus(deleteTarget.value.id)
    else if (tab.value === 'sources') await crmConfigApi.deleteSource(deleteTarget.value.id)
    else if (tab.value === 'tags') await crmConfigApi.deleteTag(deleteTarget.value.id)
    else if (tab.value === 'preferences') await crmConfigApi.deletePreferenceDefinition(deleteTarget.value.id)
    deleteTarget.value = null
    notifications.success(t('customers.config.deleted'))
    await load()
    emit('changed')
  } catch (reason) {
    error.set(reason)
  } finally {
    saving.value = false
  }
}

async function saveRules() {
  saving.value = true
  error.clear()
  try {
    rules.value = await crmConfigApi.updateDuplicateRules(
      Object.entries(rules.value).map(([identifier, enabled]) => ({ identifier, enabled })),
    )
    notifications.success(t('customers.config.saved'))
  } catch (reason) {
    error.set(reason)
  } finally {
    saving.value = false
  }
}

function itemTitle(item: ConfigItem) {
  return serverLabel('label' in item ? item.label : item.name)
}

function itemSubtitle(item: ConfigItem) {
  if ('prefKey' in item) return `${item.prefKey} · ${t(`customers.config.fieldTypes.${item.fieldType}`)}`
  if ('code' in item) return item.code
  return 'description' in item ? item.description : null
}

function itemActive(item: ConfigItem) {
  return 'active' in item ? item.active : true
}

function isSystem(item: ConfigItem) {
  return 'isSystem' in item && item.isSystem
}

function requiresCode() {
  return tab.value === 'types' || tab.value === 'statuses' || tab.value === 'sources'
}

onMounted(load)
</script>

<template>
  <section aria-labelledby="crm-config-title">
    <div class="d-flex align-center flex-wrap ga-2 mb-4">
      <div>
        <h2 id="crm-config-title" class="text-h6">{{ t('customers.config.title') }}</h2>
        <p class="text-body-2 text-medium-emphasis mb-0">{{ t('customers.config.subtitle') }}</p>
      </div>
      <v-spacer />
      <v-btn v-if="tab !== 'rules'" color="primary" prepend-icon="mdi-plus" @click="openCreate">{{ t('common.add') }}</v-btn>
    </div>

    <AppErrorState v-if="error.message.value" class="mb-4" :title="error.message.value" :retry-label="t('common.retry')" @retry="load" />
    <v-card rounded="xl" class="app-data-surface mb-4">
      <v-tabs v-model="tab" show-arrows density="comfortable">
        <v-tab v-for="item in tabs" :key="item.value" :value="item.value" :prepend-icon="item.icon">{{ item.label }}</v-tab>
      </v-tabs>
    </v-card>

    <AppLoadingState v-if="loading" variant="table" :rows="5" :label="t('common.loading')" />
    <v-card v-else-if="tab === 'rules'" rounded="xl" class="app-data-surface">
      <v-list lines="two">
        <v-list-item v-for="identifier in Object.keys(rules)" :key="identifier" :title="t(`customers.config.ruleNames.${identifier}`)" :subtitle="t('customers.config.ruleHint')">
          <template #prepend><v-avatar color="primary" variant="tonal" rounded="lg"><v-icon icon="mdi-content-duplicate" /></v-avatar></template>
          <template #append><v-switch v-model="rules[identifier]" color="primary" hide-details :aria-label="t(`customers.config.ruleNames.${identifier}`)" /></template>
        </v-list-item>
      </v-list>
      <v-card-actions><v-spacer /><v-btn color="primary" prepend-icon="mdi-content-save" :loading="saving" @click="saveRules">{{ t('common.save') }}</v-btn></v-card-actions>
    </v-card>
    <v-card v-else rounded="xl" class="app-data-surface">
      <v-list v-if="items.length" lines="two">
        <v-list-item v-for="item in items" :key="`${tab}-${item.id}`" :title="itemTitle(item)" :subtitle="itemSubtitle(item) || undefined">
          <template #prepend>
            <v-avatar :color="itemActive(item) ? 'success' : 'default'" variant="tonal" rounded="lg"><v-icon :icon="itemActive(item) ? 'mdi-check' : 'mdi-pause'" /></v-avatar>
          </template>
          <template #append>
            <v-chip v-if="isSystem(item)" size="x-small" variant="tonal" class="me-2">{{ t('customers.config.platformDefault') }}</v-chip>
            <v-btn :aria-label="t('common.edit')" icon="mdi-pencil" size="small" variant="text" @click="openEdit(item)" />
            <v-btn v-if="!isSystem(item)" :aria-label="t('common.delete')" icon="mdi-delete" size="small" variant="text" color="error" @click="deleteTarget = item" />
          </template>
        </v-list-item>
      </v-list>
      <AppEmptyState v-else icon="mdi-tune-variant" :title="t('customers.config.empty')" />
    </v-card>

    <v-dialog v-model="editorOpen" :fullscreen="smAndDown" max-width="680" persistent>
      <v-card rounded="lg">
        <v-card-title>{{ editingId ? t('common.edit') : t('common.add') }} · {{ tabs.find((item) => item.value === tab)?.label }}</v-card-title>
        <v-card-text>
          <v-row dense>
            <v-col v-if="requiresCode()" cols="12" sm="5"><v-text-field v-model="form.code" :label="t('customers.config.code')" :disabled="editingId !== null" maxlength="64" /></v-col>
            <v-col v-if="tab === 'preferences'" cols="12" sm="5"><v-text-field v-model="form.prefKey" :label="t('customers.config.preferenceKey')" :disabled="editingId !== null" maxlength="64" /></v-col>
            <v-col cols="12" :sm="requiresCode() || tab === 'preferences' ? 7 : 12"><v-text-field v-model="form.name" :label="tab === 'preferences' ? t('customers.config.label') : t('customers.config.name')" maxlength="100" /></v-col>
            <v-col v-if="tab === 'types' || tab === 'statuses' || tab === 'tags'" cols="12"><v-text-field v-model="form.description" :label="t('customers.config.description')" maxlength="255" /></v-col>
            <v-col v-if="tab === 'tags'" cols="12" sm="6"><v-text-field v-model="form.color" :label="t('customers.config.color')" placeholder="#2563EB" maxlength="32" /></v-col>
            <v-col v-if="tab === 'preferences'" cols="12" sm="6"><v-select v-model="form.fieldType" :items="fieldTypes" :label="t('customers.config.fieldType')" :disabled="editingId !== null" /></v-col>
            <v-col v-if="tab === 'preferences'" cols="6" sm="3"><v-text-field v-model.number="form.minLength" type="number" min="0" :label="t('customers.config.minLength')" clearable /></v-col>
            <v-col v-if="tab === 'preferences'" cols="6" sm="3"><v-text-field v-model.number="form.maxLength" type="number" min="0" :label="t('customers.config.maxLength')" clearable /></v-col>
            <v-col v-if="tab === 'preferences'" cols="12"><v-text-field v-model="form.pattern" :label="t('customers.config.pattern')" maxlength="255" /></v-col>
            <v-col v-if="tab === 'preferences' && form.fieldType === 'SELECT'" cols="12"><v-textarea v-model="form.optionsText" :label="t('customers.config.options')" :hint="t('customers.config.optionsHint')" rows="3" /></v-col>
            <v-col v-if="tab !== 'tags'" cols="12" sm="5"><v-text-field v-model.number="form.displayOrder" type="number" :label="t('customers.config.displayOrder')" /></v-col>
            <v-col v-if="tab === 'statuses'" cols="12"><v-checkbox v-model="form.isBlocking" :label="t('customers.config.blocking')" hide-details /><v-checkbox v-model="form.hiddenByDefault" :label="t('customers.config.hiddenByDefault')" hide-details /></v-col>
            <v-col v-if="tab === 'preferences'" cols="12"><v-checkbox v-model="form.required" :label="t('customers.config.required')" hide-details /></v-col>
            <v-col v-if="tab !== 'statuses'" cols="12"><v-switch v-model="form.active" :label="t('customers.config.active')" color="primary" hide-details /></v-col>
          </v-row>
        </v-card-text>
        <v-card-actions><v-spacer /><v-btn variant="text" @click="editorOpen = false">{{ t('common.cancel') }}</v-btn><v-btn color="primary" :loading="saving" :disabled="!form.name.trim() || (requiresCode() && !form.code.trim()) || (tab === 'preferences' && !form.prefKey.trim())" @click="save">{{ t('common.save') }}</v-btn></v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog :model-value="deleteTarget !== null" max-width="430" @update:model-value="deleteTarget = null">
      <v-card rounded="lg">
        <v-card-title>{{ t('customers.config.deleteTitle') }}</v-card-title>
        <v-card-text>{{ t('customers.config.deleteBody', { name: deleteTarget ? itemTitle(deleteTarget) : '' }) }}</v-card-text>
        <v-card-actions><v-spacer /><v-btn variant="text" @click="deleteTarget = null">{{ t('common.cancel') }}</v-btn><v-btn color="error" :loading="saving" @click="remove">{{ t('common.delete') }}</v-btn></v-card-actions>
      </v-card>
    </v-dialog>
  </section>
</template>

<style scoped>
@media (max-width: 599px) {
  :deep(.v-btn) { min-height: 44px; }
  :deep(.v-list-item__append .v-chip) { display: none; }
}
</style>
