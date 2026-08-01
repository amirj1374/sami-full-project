<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import { purchasingConfigApi } from '@/api/purchases'
import AppEmptyState from '@/components/AppEmptyState.vue'
import AppErrorState from '@/components/AppErrorState.vue'
import AppLoadingState from '@/components/AppLoadingState.vue'
import { useApiError } from '@/composables/useApiError'
import { useNotifications } from '@/composables/useNotifications'
import type {
  PurApprovalRule,
  PurCancelReason,
  PurIdentifierType,
  PurType,
} from '@/types/models'

type ConfigKind = 'types' | 'reasons' | 'identifiers' | 'rules'
type ConfigItem = PurType | PurCancelReason | PurIdentifierType | PurApprovalRule

const emit = defineEmits<{ changed: [] }>()
const { t } = useI18n()
const { smAndDown } = useDisplay()
const error = useApiError()
const notifications = useNotifications()
const loading = ref(false)
const saving = ref(false)
const tab = ref<ConfigKind>('types')
const types = ref<PurType[]>([])
const reasons = ref<PurCancelReason[]>([])
const identifiers = ref<PurIdentifierType[]>([])
const rules = ref<PurApprovalRule[]>([])
const editorOpen = ref(false)
const deleteTarget = ref<ConfigItem | null>(null)
const editingId = ref<number | null>(null)

const form = reactive({
  code: '', name: '', description: '', numberPrefix: 'PUR', active: true,
  displayOrder: 10, satisfiesSerial: false, satisfiesImei: false, minAmount: 0,
})

const tabs = computed(() => [
  { value: 'types' as const, label: t('purchases.config.types'), icon: 'mdi-shape-outline' },
  { value: 'reasons' as const, label: t('purchases.config.reasons'), icon: 'mdi-cancel' },
  { value: 'identifiers' as const, label: t('purchases.config.identifiers'), icon: 'mdi-barcode-scan' },
  { value: 'rules' as const, label: t('purchases.config.rules'), icon: 'mdi-account-check-outline' },
])

const items = computed<ConfigItem[]>(() => {
  if (tab.value === 'types') return types.value
  if (tab.value === 'reasons') return reasons.value
  if (tab.value === 'identifiers') return identifiers.value
  return rules.value
})

async function load() {
  loading.value = true
  error.clear()
  try {
    ;[types.value, reasons.value, identifiers.value, rules.value] = await Promise.all([
      purchasingConfigApi.types(), purchasingConfigApi.cancelReasons(),
      purchasingConfigApi.identifierTypes(), purchasingConfigApi.approvalRules(),
    ])
  } catch (reason) {
    error.set(reason)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    code: '', name: '', description: '', numberPrefix: 'PUR', active: true,
    displayOrder: 10, satisfiesSerial: false, satisfiesImei: false, minAmount: 0,
  })
}

function openCreate() {
  editingId.value = null
  resetForm()
  editorOpen.value = true
}

function openEdit(item: ConfigItem) {
  resetForm()
  form.name = item.name
  form.active = item.active
  editingId.value = item.tenantOwned ? item.id : null
  if ('code' in item) {
    form.code = item.code
    form.displayOrder = item.displayOrder
  }
  if ('numberPrefix' in item) {
    form.description = item.description ?? ''
    form.numberPrefix = item.numberPrefix
  }
  if ('satisfiesSerial' in item) {
    form.satisfiesSerial = item.satisfiesSerial
    form.satisfiesImei = item.satisfiesImei
  }
  if ('minAmount' in item) form.minAmount = item.minAmount
  editorOpen.value = true
}

async function save() {
  if (!form.name.trim() || (tab.value !== 'rules' && !form.code.trim())) return
  saving.value = true
  error.clear()
  try {
    if (tab.value === 'types') {
      const payload = {
        code: form.code.trim(), name: form.name.trim(), description: form.description.trim() || undefined,
        numberPrefix: form.numberPrefix.trim(), active: form.active, displayOrder: form.displayOrder,
      }
      if (editingId.value) await purchasingConfigApi.updateType(editingId.value, payload)
      else await purchasingConfigApi.createType(payload)
    } else if (tab.value === 'reasons') {
      const payload = { code: form.code.trim(), name: form.name.trim(), active: form.active, displayOrder: form.displayOrder }
      if (editingId.value) await purchasingConfigApi.updateCancelReason(editingId.value, payload)
      else await purchasingConfigApi.createCancelReason(payload)
    } else if (tab.value === 'identifiers') {
      const payload = {
        code: form.code.trim(), name: form.name.trim(), active: form.active,
        displayOrder: form.displayOrder, satisfiesSerial: form.satisfiesSerial,
        satisfiesImei: form.satisfiesImei,
      }
      if (editingId.value) await purchasingConfigApi.updateIdentifierType(editingId.value, payload)
      else await purchasingConfigApi.createIdentifierType(payload)
    } else {
      const payload = { name: form.name.trim(), minAmount: form.minAmount, active: form.active }
      if (editingId.value) await purchasingConfigApi.updateApprovalRule(editingId.value, payload)
      else await purchasingConfigApi.createApprovalRule(payload)
    }
    editorOpen.value = false
    notifications.success(t('purchases.config.saved'))
    await load()
    emit('changed')
  } catch (reason) {
    error.set(reason)
  } finally {
    saving.value = false
  }
}

async function remove() {
  const item = deleteTarget.value
  if (!item) return
  saving.value = true
  error.clear()
  try {
    if (tab.value === 'types') await purchasingConfigApi.deleteType(item.id)
    else if (tab.value === 'reasons') await purchasingConfigApi.deleteCancelReason(item.id)
    else if (tab.value === 'identifiers') await purchasingConfigApi.deleteIdentifierType(item.id)
    else await purchasingConfigApi.deleteApprovalRule(item.id)
    deleteTarget.value = null
    notifications.success(t('purchases.config.deleted'))
    await load()
    emit('changed')
  } catch (reason) {
    error.set(reason)
  } finally {
    saving.value = false
  }
}

function codeOf(item: ConfigItem) {
  return 'code' in item ? item.code : null
}

function detailOf(item: ConfigItem) {
  if ('numberPrefix' in item) return t('purchases.config.prefixValue', { value: item.numberPrefix })
  if ('satisfiesSerial' in item) {
    const values = [item.satisfiesSerial ? 'S/N' : '', item.satisfiesImei ? 'IMEI' : ''].filter(Boolean)
    return values.join(' · ') || t('purchases.config.otherIdentifier')
  }
  if ('minAmount' in item) return t('purchases.config.thresholdValue', { value: item.minAmount })
  return null
}

onMounted(load)
</script>

<template>
  <section aria-labelledby="purchasing-config-title">
    <div class="d-flex align-center flex-wrap ga-2 mb-4">
      <div>
        <h2 id="purchasing-config-title" class="text-h6">{{ t('purchases.config.title') }}</h2>
        <p class="text-body-2 text-medium-emphasis mb-0">{{ t('purchases.config.subtitle') }}</p>
      </div>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openCreate">{{ t('common.add') }}</v-btn>
    </div>

    <AppErrorState
      v-if="error.message.value"
      class="mb-4"
      :title="error.message.value"
      :retry-label="t('common.retry')"
      @retry="load"
    />

    <v-card rounded="xl" class="app-data-surface mb-4">
      <v-tabs v-model="tab" show-arrows density="comfortable">
        <v-tab v-for="item in tabs" :key="item.value" :value="item.value" :prepend-icon="item.icon">
          {{ item.label }}
        </v-tab>
      </v-tabs>
    </v-card>

    <AppLoadingState v-if="loading" variant="table" :rows="5" :label="t('common.loading')" />
    <v-card v-else rounded="xl" class="app-data-surface">
      <v-list v-if="items.length" lines="two">
        <v-list-item v-for="item in items" :key="`${tab}-${item.id}`" :title="item.name">
          <v-list-item-subtitle>
            <span v-if="codeOf(item)">{{ codeOf(item) }}</span>
            <span v-if="codeOf(item) && detailOf(item)"> · </span>
            <span v-if="detailOf(item)">{{ detailOf(item) }}</span>
          </v-list-item-subtitle>
          <template #prepend>
            <v-avatar :color="item.active ? 'success' : 'default'" variant="tonal" rounded="lg">
              <v-icon :icon="item.active ? 'mdi-check' : 'mdi-pause'" />
            </v-avatar>
          </template>
          <template #append>
            <v-chip v-if="!item.tenantOwned" size="x-small" variant="tonal" class="me-2">
              {{ t('purchases.config.platformDefault') }}
            </v-chip>
            <v-btn :aria-label="t('common.edit')" icon="mdi-pencil" size="small" variant="text" @click="openEdit(item)" />
            <v-btn
              v-if="item.tenantOwned"
              :aria-label="t('common.delete')"
              icon="mdi-delete"
              size="small"
              variant="text"
              color="error"
              @click="deleteTarget = item"
            />
          </template>
        </v-list-item>
      </v-list>
      <AppEmptyState v-else icon="mdi-tune-variant" :title="t('purchases.config.empty')" />
    </v-card>

    <v-dialog v-model="editorOpen" :fullscreen="smAndDown" max-width="620" persistent>
      <v-card rounded="lg">
        <v-card-title>{{ editingId ? t('common.edit') : t('common.add') }} · {{ tabs.find((item) => item.value === tab)?.label }}</v-card-title>
        <v-card-text>
          <v-row dense>
            <v-col v-if="tab !== 'rules'" cols="12" sm="5">
              <v-text-field v-model="form.code" :label="t('purchases.config.code')" :disabled="editingId !== null" maxlength="64" />
            </v-col>
            <v-col cols="12" :sm="tab === 'rules' ? 12 : 7">
              <v-text-field v-model="form.name" :label="t('purchases.config.name')" maxlength="100" />
            </v-col>
            <v-col v-if="tab === 'types'" cols="12" sm="5">
              <v-text-field v-model="form.numberPrefix" :label="t('purchases.config.prefix')" maxlength="16" />
            </v-col>
            <v-col v-if="tab === 'types'" cols="12" sm="7">
              <v-text-field v-model="form.description" :label="t('purchases.config.description')" maxlength="255" />
            </v-col>
            <v-col v-if="tab !== 'rules'" cols="12" sm="5">
              <v-text-field v-model.number="form.displayOrder" type="number" :label="t('purchases.config.displayOrder')" />
            </v-col>
            <v-col v-if="tab === 'rules'" cols="12" sm="6">
              <v-text-field v-model.number="form.minAmount" type="number" min="0" :label="t('purchases.config.threshold')" />
            </v-col>
            <v-col v-if="tab === 'identifiers'" cols="12">
              <v-checkbox v-model="form.satisfiesSerial" :label="t('purchases.config.satisfiesSerial')" hide-details />
              <v-checkbox v-model="form.satisfiesImei" :label="t('purchases.config.satisfiesImei')" hide-details />
            </v-col>
            <v-col cols="12"><v-switch v-model="form.active" :label="t('purchases.config.active')" color="primary" hide-details /></v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="editorOpen = false">{{ t('common.cancel') }}</v-btn>
          <v-btn color="primary" :loading="saving" :disabled="!form.name.trim() || (tab !== 'rules' && !form.code.trim())" @click="save">
            {{ t('common.save') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog :model-value="deleteTarget !== null" max-width="430" @update:model-value="deleteTarget = null">
      <v-card rounded="lg">
        <v-card-title>{{ t('purchases.config.deleteTitle') }}</v-card-title>
        <v-card-text>{{ t('purchases.config.deleteBody', { name: deleteTarget?.name }) }}</v-card-text>
        <v-card-actions><v-spacer /><v-btn variant="text" @click="deleteTarget = null">{{ t('common.cancel') }}</v-btn><v-btn color="error" :loading="saving" @click="remove">{{ t('common.delete') }}</v-btn></v-card-actions>
      </v-card>
    </v-dialog>
  </section>
</template>

<style scoped>
@media (max-width: 599px) {
  :deep(.v-btn) { min-height: 44px; }
  :deep(.v-list-item__append) { margin-inline-start: 6px; }
  :deep(.v-list-item__append .v-chip) { display: none; }
}
</style>
