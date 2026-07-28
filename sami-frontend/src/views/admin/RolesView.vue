<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { duplicateRoleSchema, roleSchema } from '@/schemas/admin'
import { rolesApi } from '@/api/roles'
import { useApiError } from '@/composables/useApiError'
import { useServerLabel } from '@/composables/useServerLabel'
import PermissionMatrixDialog from '@/components/PermissionMatrixDialog.vue'
import type { PageQuery } from '@/types/api'
import type { Role } from '@/types/models'

const { t } = useI18n()
const { text: srvLabel } = useServerLabel()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()

// --- Table state ----------------------------------------------------------
interface SortItem {
  key: string
  order?: 'asc' | 'desc'
}
interface TableOptions {
  page: number
  itemsPerPage: number
  sortBy: SortItem[]
}

const items = ref<Role[]>([])
const totalItems = ref(0)
const loading = ref(false)
const lastOptions = ref<TableOptions>({ page: 1, itemsPerPage: 10, sortBy: [] })

const headers = [
  { title: t('roles.name'), key: 'name' },
  { title: t('roles.description'), key: 'description', sortable: false },
  { title: t('roles.attributes'), key: 'attributes', sortable: false },
  { title: t('roles.users'), key: 'userCount', sortable: false, align: 'end' as const },
  { title: '', key: 'actions', sortable: false, align: 'end' as const },
]

async function loadItems(options: TableOptions) {
  lastOptions.value = options
  loading.value = true
  clearError()

  const sort = options.sortBy[0]
  const params: PageQuery = {
    page: options.page - 1, // Vuetify is 1-based; the API is 0-based.
    size: options.itemsPerPage,
    sort: sort ? `${sort.key},${sort.order ?? 'asc'}` : undefined,
  }

  try {
    const pageResult = await rolesApi.list(params)
    items.value = pageResult.content
    totalItems.value = pageResult.totalElements
  } catch (err) {
    setError(err)
  } finally {
    loading.value = false
  }
}

function reload() {
  loadItems(lastOptions.value)
}

// --- Create / edit dialog -------------------------------------------------
const formOpen = ref(false)
const editing = ref<Role | null>(null)
const saving = ref(false)
const { message: formError, set: setFormError, clear: clearFormError } = useApiError()

const {
  handleSubmit: handleFormSubmit,
  defineField: defineFormField,
  errors: formErrors,
  resetForm,
} = useForm({
  validationSchema: toTypedSchema(roleSchema(t)),
  initialValues: { name: '', description: '' },
})

const [name, nameProps] = defineFormField('name')
const [description, descriptionProps] = defineFormField('description')

function openCreate() {
  editing.value = null
  clearFormError()
  resetForm({ values: { name: '', description: '' } })
  formOpen.value = true
}

function openEdit(role: Role) {
  editing.value = role
  clearFormError()
  resetForm({ values: { name: role.name, description: role.description ?? '' } })
  formOpen.value = true
}

const onFormSubmit = handleFormSubmit(async (values) => {
  saving.value = true
  clearFormError()
  try {
    const payload = { name: values.name, description: values.description || undefined }
    if (editing.value) {
      await rolesApi.update(editing.value.id, payload)
    } else {
      await rolesApi.create(payload)
    }
    formOpen.value = false
    reload()
  } catch (err) {
    setFormError(err)
  } finally {
    saving.value = false
  }
})

// --- Duplicate dialog -----------------------------------------------------
const duplicateTarget = ref<Role | null>(null)
const duplicating = ref(false)
const { message: duplicateError, set: setDuplicateError, clear: clearDuplicateError } = useApiError()

const {
  handleSubmit: handleDuplicateSubmit,
  defineField: defineDuplicateField,
  errors: duplicateErrors,
  resetForm: resetDuplicateForm,
} = useForm({
  validationSchema: toTypedSchema(duplicateRoleSchema(t)),
  initialValues: { name: '' },
})

const [duplicateName, duplicateNameProps] = defineDuplicateField('name')

function openDuplicate(role: Role) {
  duplicateTarget.value = role
  clearDuplicateError()
  resetDuplicateForm({ values: { name: `${role.name} (copy)` } })
}

const onDuplicateSubmit = handleDuplicateSubmit(async (values) => {
  if (!duplicateTarget.value) return
  duplicating.value = true
  clearDuplicateError()
  try {
    await rolesApi.duplicate(duplicateTarget.value.id, { name: values.name })
    duplicateTarget.value = null
    reload()
  } catch (err) {
    setDuplicateError(err)
  } finally {
    duplicating.value = false
  }
})

// --- Permission matrix ----------------------------------------------------
const matrixOpen = ref(false)
const matrixRole = ref<Role | null>(null)

function openMatrix(role: Role) {
  matrixRole.value = role
  matrixOpen.value = true
}

// --- Delete confirmation --------------------------------------------------
const deleteTarget = ref<Role | null>(null)
const deleting = ref(false)

// System roles never reach the confirm dialog (the action is disabled), but
// guard anyway so a stale reference cannot trigger a doomed request.
watch(deleteTarget, (target) => {
  if (target?.isSystem) {
    deleteTarget.value = null
  }
})

async function confirmDelete() {
  if (!deleteTarget.value || deleteTarget.value.isSystem) return
  deleting.value = true
  try {
    await rolesApi.remove(deleteTarget.value.id)
    deleteTarget.value = null
    reload()
  } catch (err) {
    setError(err)
    deleteTarget.value = null
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <div>
    <div class="d-flex align-center mb-4">
      <h1 class="text-h4">{{ t('roles.title') }}</h1>
      <v-spacer />
      <v-btn v-can="'roles:create'" color="primary" prepend-icon="mdi-plus" @click="openCreate">
        {{ t('roles.new') }}
      </v-btn>
    </div>

    <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
      {{ errorMessage }}
    </v-alert>

    <v-card rounded="lg" border flat>
      <v-data-table-server
        :headers="headers"
        :items="items"
        :items-length="totalItems"
        :loading="loading"
        :items-per-page="10"
        @update:options="loadItems"
      >
        <template #[`item.name`]="{ item }">
          <span class="font-weight-medium">{{ srvLabel(item.name) }}</span>
        </template>
        <template #[`item.description`]="{ item }">
          <span class="text-medium-emphasis">{{ item.description ?? '—' }}</span>
        </template>
        <template #[`item.attributes`]="{ item }">
          <v-chip v-if="item.isSystem" size="small" variant="tonal" class="mr-1">{{ t('roles.system') }}</v-chip>
          <v-chip
            v-if="item.isSuperAdmin"
            color="warning"
            size="small"
            variant="tonal"
            prepend-icon="mdi-shield-crown"
            class="mr-1"
          >
            {{ t('roles.superAdmin') }}
          </v-chip>
          <v-chip v-if="item.isDefault" color="info" size="small" variant="tonal">{{ t('roles.default') }}</v-chip>
        </template>
        <template #[`item.actions`]="{ item }">
          <v-btn
            v-can="'roles:edit'"
            icon="mdi-key-outline"
            size="small"
            variant="text"
            :title="t('roles.permissions')"
            @click="openMatrix(item)"
          />
          <v-btn
            v-can="'roles:create'"
            icon="mdi-content-copy"
            size="small"
            variant="text"
            :title="t('roles.duplicate')"
            @click="openDuplicate(item)"
          />
          <v-btn
            v-can="'roles:edit'"
            icon="mdi-pencil"
            size="small"
            variant="text"
            :title="t('common.edit')"
            @click="openEdit(item)"
          />
          <v-btn
            v-can="'roles:delete'"
            icon="mdi-delete"
            size="small"
            variant="text"
            color="error"
            :title="t('common.delete')"
            :disabled="item.isSystem"
            @click="deleteTarget = item"
          />
        </template>
      </v-data-table-server>
    </v-card>

    <!-- Create / edit -->
    <v-dialog v-model="formOpen" max-width="560">
      <v-card rounded="lg">
        <v-card-title class="text-h6 pt-4 px-6">
          {{ editing ? t('roles.edit') : t('roles.new') }}
        </v-card-title>

        <v-card-text class="px-6">
          <v-alert v-if="formError" type="error" variant="tonal" density="compact" class="mb-4">
            {{ formError }}
          </v-alert>

          <v-form @submit.prevent="onFormSubmit">
            <v-text-field
              v-model="name"
              v-bind="nameProps"
              :error-messages="formErrors.name"
              :label="t('roles.name')"
              :disabled="editing?.isSystem ?? false"
              :hint="editing?.isSystem ? t('roles.systemNameHint') : undefined"
              :persistent-hint="editing?.isSystem ?? false"
            />
            <v-textarea
              v-model="description"
              v-bind="descriptionProps"
              :error-messages="formErrors.description"
              :label="t('roles.description')"
              rows="2"
              auto-grow
            />
          </v-form>
        </v-card-text>

        <v-card-actions class="px-6 pb-4">
          <v-spacer />
          <v-btn variant="text" @click="formOpen = false">{{ t('common.cancel') }}</v-btn>
          <v-btn color="primary" :loading="saving" @click="onFormSubmit">{{ t('common.save') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Duplicate -->
    <v-dialog
      :model-value="!!duplicateTarget"
      max-width="420"
      @update:model-value="duplicateTarget = null"
    >
      <v-card rounded="lg">
        <v-card-title class="text-h6 pt-4 px-6">{{ t('roles.duplicateTitle') }}</v-card-title>

        <v-card-text class="px-6">
          <v-alert v-if="duplicateError" type="error" variant="tonal" density="compact" class="mb-4">
            {{ duplicateError }}
          </v-alert>

          <i18n-t keypath="roles.duplicateBody" tag="p" class="text-body-2 text-medium-emphasis mb-4">
            <template #name>
              <strong>{{ duplicateTarget?.name }}</strong>
            </template>
          </i18n-t>

          <v-form @submit.prevent="onDuplicateSubmit">
            <v-text-field
              v-model="duplicateName"
              v-bind="duplicateNameProps"
              :error-messages="duplicateErrors.name"
              :label="t('roles.newRoleName')"
              autofocus
            />
          </v-form>
        </v-card-text>

        <v-card-actions class="px-6 pb-4">
          <v-spacer />
          <v-btn variant="text" @click="duplicateTarget = null">{{ t('common.cancel') }}</v-btn>
          <v-btn color="primary" :loading="duplicating" @click="onDuplicateSubmit">{{ t('roles.duplicate') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Permission matrix -->
    <PermissionMatrixDialog v-model="matrixOpen" :role="matrixRole" @saved="reload" />

    <!-- Delete confirmation -->
    <v-dialog :model-value="!!deleteTarget" max-width="420" @update:model-value="deleteTarget = null">
      <v-card rounded="lg">
        <v-card-title class="text-h6">{{ t('roles.deleteTitle') }}</v-card-title>
        <v-card-text>
          <i18n-t keypath="roles.deleteConfirm" tag="span">
            <template #name>
              <strong>{{ deleteTarget?.name }}</strong>
            </template>
          </i18n-t>
          <v-alert
            v-if="(deleteTarget?.userCount ?? 0) > 0"
            type="warning"
            variant="tonal"
            density="compact"
            class="mt-3"
          >
            {{ t('roles.assignedWarning', { count: deleteTarget?.userCount ?? 0 }, { plural: deleteTarget?.userCount ?? 0 }) }}
          </v-alert>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="deleteTarget = null">{{ t('common.cancel') }}</v-btn>
          <v-btn color="error" :loading="deleting" @click="confirmDelete">{{ t('common.delete') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
