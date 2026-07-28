<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { dashboardsApi } from '@/api/dashboards'
import { rolesApi } from '@/api/roles'
import { useApiError } from '@/composables/useApiError'
import type { DashboardRow, DashboardStatus, DashboardVisibility } from '@/types/models'

/** Create/edit dashboard dialog: code, name, status, visibility, role target. */
const props = defineProps<{
  modelValue: boolean
  dashboard: DashboardRow | null
  statuses: DashboardStatus[]
  visibilities: DashboardVisibility[]
}>()

const emit = defineEmits<{ 'update:modelValue': [boolean]; saved: [] }>()
const { t } = useI18n()
const { message: formError, set: setFormError, clear: clearFormError } = useApiError()

const open = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})
const isEdit = computed(() => props.dashboard !== null)
const saving = ref(false)

const code = ref('')
const name = ref('')
const description = ref('')
const statusId = ref<number | null>(null)
const visibilityId = ref<number | null>(null)
const roleId = ref<number | null>(null)
const roleOptions = ref<{ id: number; name: string }[]>([])

const selectedVisibility = computed(
  () => props.visibilities.find((v) => v.id === visibilityId.value) ?? null,
)

watch(open, async (isOpen) => {
  if (!isOpen) return
  clearFormError()
  const d = props.dashboard
  code.value = d?.code ?? ''
  name.value = d?.name ?? ''
  description.value = d?.description ?? ''
  statusId.value = d?.statusId ?? props.statuses.find((s) => s.isDefault)?.id ?? null
  visibilityId.value = d?.visibilityId ?? props.visibilities.find((v) => v.isDefault)?.id ?? null
  roleId.value = d?.roleId ?? null
  if (roleOptions.value.length === 0) {
    try {
      const page = await rolesApi.list({ size: 200 })
      roleOptions.value = page.content.map((r) => ({ id: r.id, name: r.name }))
    } catch {
      /* role list optional */
    }
  }
})

async function save(): Promise<void> {
  clearFormError()
  saving.value = true
  const payload = {
    code: code.value.trim(),
    name: name.value.trim(),
    description: description.value.trim() || undefined,
    statusId: statusId.value ?? undefined,
    visibilityId: visibilityId.value ?? undefined,
    roleId: selectedVisibility.value?.code === 'role' ? (roleId.value ?? undefined) : undefined,
    expectedVersion: props.dashboard?.version,
  }
  try {
    if (props.dashboard) {
      await dashboardsApi.update(props.dashboard.id, payload)
    } else {
      await dashboardsApi.create(payload)
    }
    open.value = false
    emit('saved')
  } catch (err) {
    setFormError(err)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <v-dialog v-model="open" max-width="560">
    <v-card rounded="lg">
      <v-card-title class="text-h6 pt-4 px-6">
        {{ isEdit ? t('dash.editDashboard') : t('dash.newDashboard') }}
      </v-card-title>

      <v-card-text class="px-6">
        <v-alert v-if="formError" type="error" variant="tonal" density="compact" class="mb-4">
          {{ formError }}
        </v-alert>

        <v-text-field v-model="code" :label="t('common.code') + ' *'" :disabled="isEdit" />
        <v-text-field v-model="name" :label="t('common.name') + ' *'" />
        <v-textarea v-model="description" :label="t('common.description')" rows="2" />
        <v-select
          v-model="statusId"
          :items="statuses"
          item-title="name"
          item-value="id"
          :label="t('common.status')"
        />
        <v-select
          v-model="visibilityId"
          :items="visibilities"
          item-title="name"
          item-value="id"
          :label="t('dash.visibility')"
        />
        <v-select
          v-if="selectedVisibility?.code === 'role'"
          v-model="roleId"
          :items="roleOptions"
          item-title="name"
          item-value="id"
          :label="t('dash.targetRole')"
        />
      </v-card-text>

      <v-card-actions class="px-6 pb-4">
        <v-spacer />
        <v-btn variant="text" @click="open = false">{{ t('common.cancel') }}</v-btn>
        <v-btn color="primary" :loading="saving" @click="save">{{ t('common.save') }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
