<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { permissionsApi } from '@/api/permissions'
import { rolesApi } from '@/api/roles'
import { useApiError } from '@/composables/useApiError'
import type { ModulePermissionsGroup, Permission, Role } from '@/types/models'

const props = defineProps<{
  modelValue: boolean
  /** The role whose permission set is being edited. */
  role: Role | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const { t } = useI18n()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()
const loading = ref(false)
const saving = ref(false)
const groups = ref<ModulePermissionsGroup[]>([])
const selected = ref<Set<number>>(new Set())

const isSuperAdmin = computed(() => props.role?.isSuperAdmin ?? false)

/** Standard actions come first in their canonical order; custom ones follow alphabetically. */
const STANDARD_ACTIONS = ['view', 'create', 'edit', 'delete', 'export', 'import']

/** Union of actions across all module groups — the matrix columns. */
const actions = computed<string[]>(() => {
  const present = new Set<string>()
  for (const group of groups.value) {
    for (const permission of group.permissions) {
      present.add(permission.action)
    }
  }
  const standard = STANDARD_ACTIONS.filter((action) => present.has(action))
  const custom = [...present].filter((action) => !STANDARD_ACTIONS.includes(action)).sort()
  return [...standard, ...custom]
})

/** Per-module lookup of action → permission, so absent combos render blank. */
const byModule = computed<Map<number, Map<string, Permission>>>(() => {
  const map = new Map<number, Map<string, Permission>>()
  for (const group of groups.value) {
    map.set(group.moduleId, new Map(group.permissions.map((p) => [p.action, p])))
  }
  return map
})

function permissionAt(group: ModulePermissionsGroup, action: string): Permission | undefined {
  return byModule.value.get(group.moduleId)?.get(action)
}

// Load the full matrix and seed the selection whenever the dialog opens.
watch(
  () => props.modelValue,
  async (open) => {
    if (!open || !props.role) return
    clearError()
    selected.value = new Set(props.role.permissionIds)
    loading.value = true
    try {
      groups.value = await permissionsApi.grouped()
    } catch (err) {
      setError(err)
    } finally {
      loading.value = false
    }
  },
)

function toggle(id: number, checked: boolean): void {
  if (checked) {
    selected.value.add(id)
  } else {
    selected.value.delete(id)
  }
}

function rowSelectedCount(group: ModulePermissionsGroup): number {
  return group.permissions.filter((p) => selected.value.has(p.id)).length
}

function isRowAllSelected(group: ModulePermissionsGroup): boolean {
  return group.permissions.length > 0 && rowSelectedCount(group) === group.permissions.length
}

function isRowIndeterminate(group: ModulePermissionsGroup): boolean {
  const count = rowSelectedCount(group)
  return count > 0 && count < group.permissions.length
}

function toggleRow(group: ModulePermissionsGroup, checked: boolean): void {
  for (const permission of group.permissions) {
    toggle(permission.id, checked)
  }
}

function close() {
  emit('update:modelValue', false)
}

async function save() {
  if (!props.role || isSuperAdmin.value) return
  saving.value = true
  clearError()
  try {
    await rolesApi.updatePermissions(props.role.id, { permissionIds: [...selected.value] })
    emit('saved')
    close()
  } catch (err) {
    setError(err)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <v-dialog
    :model-value="modelValue"
    max-width="900"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <v-card rounded="lg">
      <v-card-title class="text-h6 pt-4 px-6">
        {{ t('roles.matrixTitle', { name: role?.name }) }}
      </v-card-title>

      <v-card-text class="px-6">
        <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
          {{ errorMessage }}
        </v-alert>

        <v-alert
          v-if="isSuperAdmin"
          type="info"
          variant="tonal"
          density="compact"
          icon="mdi-shield-crown"
          class="mb-4"
        >
          {{ t('roles.superAdminNotice') }}
        </v-alert>

        <div v-if="loading" class="text-center py-8">
          <v-progress-circular indeterminate color="primary" />
        </div>

        <div v-else class="matrix-scroll">
          <v-table density="comfortable">
            <thead>
              <tr>
                <th>{{ t('roles.matrixModule') }}</th>
                <th class="text-center">{{ t('common.all') }}</th>
                <th v-for="action in actions" :key="action" class="text-center text-capitalize">
                  {{ action }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="group in groups" :key="group.moduleId">
                <td class="text-no-wrap">{{ group.moduleName }}</td>
                <td class="text-center">
                  <v-checkbox-btn
                    :model-value="isRowAllSelected(group)"
                    :indeterminate="isRowIndeterminate(group)"
                    :disabled="isSuperAdmin || group.permissions.length === 0"
                    class="d-inline-flex"
                    @update:model-value="toggleRow(group, $event === true)"
                  />
                </td>
                <td v-for="action in actions" :key="action" class="text-center">
                  <v-checkbox-btn
                    v-if="permissionAt(group, action)"
                    :model-value="selected.has(permissionAt(group, action)!.id)"
                    :disabled="isSuperAdmin"
                    class="d-inline-flex"
                    @update:model-value="toggle(permissionAt(group, action)!.id, $event === true)"
                  />
                </td>
              </tr>
            </tbody>
          </v-table>
        </div>
      </v-card-text>

      <v-card-actions class="px-6 pb-4">
        <v-spacer />
        <v-btn variant="text" @click="close">{{ t('common.cancel') }}</v-btn>
        <v-btn color="primary" :loading="saving" :disabled="isSuperAdmin || loading" @click="save">
          {{ t('common.save') }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.matrix-scroll {
  overflow-x: auto;
}
</style>
