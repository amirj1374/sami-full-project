<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { moduleSchema } from '@/schemas/admin'
import { modulesApi } from '@/api/modules'
import { useApiError } from '@/composables/useApiError'
import type { PageQuery } from '@/types/api'
import type { AppModule, ModuleStatus } from '@/types/models'

const { t } = useI18n()
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

const items = ref<AppModule[]>([])
const totalItems = ref(0)
const loading = ref(false)
const lastOptions = ref<TableOptions>({ page: 1, itemsPerPage: 10, sortBy: [] })

const headers = [
  { title: t('modules.order'), key: 'displayOrder', align: 'end' as const },
  { title: t('modules.name'), key: 'name' },
  { title: t('modules.code'), key: 'code' },
  { title: t('modules.lifecycle.column'), key: 'lifecycle', sortable: false },
  { title: t('modules.permissions'), key: 'permissionCount', sortable: false, align: 'end' as const },
  { title: t('modules.enabled'), key: 'enabled', sortable: false },
  { title: '', key: 'actions', sortable: false, align: 'end' as const },
]

// ---------------------------------------------------------------------
// Lifecycle management
//
// The stage list comes from the backend so the dropdowns reflect whatever
// module_statuses holds; adding a stage needs no frontend change. Each
// dropdown is filtered to the stages valid for its axis, which is why the
// backend also rejects an invalid pair — the UI narrows, the API enforces.
// ---------------------------------------------------------------------
const lifecycleStatuses = ref<ModuleStatus[]>([])
const lifecycleOpen = ref(false)
const lifecycleTarget = ref<AppModule | null>(null)
const lifecycleSaving = ref(false)
const lifecycleForm = ref({
  backendStatusCode: '',
  frontendStatusCode: '',
  overallStatusCode: '' as string | null,
  progressPercentage: 0,
  releaseVersion: '',
  developmentNotes: '',
  available: true,
  productionReady: false,
})

/**
 * A stage is offered for an axis only when the backend says it applies there
 * (`appliesToBackend` / `appliesToFrontend`). No stage code is named here, so
 * adding or reordering a lifecycle stage in `module_statuses` needs no
 * frontend change. `lifecycleRank` orders the list as a progression.
 */
const byRank = (a: ModuleStatus, b: ModuleStatus) => a.lifecycleRank - b.lifecycleRank

const stagesFor = (axis: 'backend' | 'frontend') =>
  lifecycleStatuses.value
    .filter((s) => (axis === 'backend' ? s.appliesToBackend : s.appliesToFrontend))
    .sort(byRank)

const backendStageOptions = computed(() => stagesFor('backend'))
const frontendStageOptions = computed(() => stagesFor('frontend'))
/** The overall override may be pinned to any stage, so it is unfiltered. */
const overallStageOptions = computed(() => [...lifecycleStatuses.value].sort(byRank))

// --- Lifecycle form validation -------------------------------------------
const MAX_RELEASE_VERSION = 32
const MAX_NOTES = 2000

/**
 * Validation mirrors the backend's own rules so the user is told before the
 * request rather than by a 400. Applicability is read from the status flags,
 * never from stage codes.
 */
const lifecycleErrors = computed(() => {
  const e: Record<string, string> = {}
  const f = lifecycleForm.value
  const find = (code: string) => lifecycleStatuses.value.find((s) => s.code === code)

  if (!f.backendStatusCode) e.backend = t('modules.lifecycle.validation.backendRequired')
  if (!f.frontendStatusCode) e.frontend = t('modules.lifecycle.validation.frontendRequired')

  const be = find(f.backendStatusCode)
  const fe = find(f.frontendStatusCode)
  if (be && !be.appliesToBackend) e.backend = t('modules.lifecycle.validation.notBackendAxis', { name: be.name })
  if (fe && !fe.appliesToFrontend) e.frontend = t('modules.lifecycle.validation.notFrontendAxis', { name: fe.name })

  if (f.progressPercentage < 0 || f.progressPercentage > 100) {
    e.progress = t('modules.lifecycle.validation.progressRange')
  }
  if ((f.releaseVersion ?? '').length > MAX_RELEASE_VERSION) {
    e.releaseVersion = t('modules.lifecycle.validation.tooLong', { max: MAX_RELEASE_VERSION })
  }
  if ((f.developmentNotes ?? '').length > MAX_NOTES) {
    e.notes = t('modules.lifecycle.validation.tooLong', { max: MAX_NOTES })
  }
  // A module cannot be production-ready while neither axis is.
  if (f.productionReady && be && fe && !be.productionReady && !fe.productionReady) {
    e.productionReady = t('modules.lifecycle.validation.notProductionReady')
  }
  return e
})

const lifecycleValid = computed(() => Object.keys(lifecycleErrors.value).length === 0)

async function loadLifecycleStatuses() {
  if (lifecycleStatuses.value.length) return
  try {
    lifecycleStatuses.value = await modulesApi.lifecycleStatuses()
  } catch {
    // Non-fatal: the dialog simply offers no options and the table still
    // renders. Surfacing an error here would block module administration
    // over a secondary feature.
    lifecycleStatuses.value = []
  }
}

async function openLifecycle(module: AppModule) {
  await loadLifecycleStatuses()
  lifecycleTarget.value = module
  lifecycleForm.value = {
    backendStatusCode: module.backendStatus?.code ?? '',
    frontendStatusCode: module.frontendStatus?.code ?? '',
    // Empty means "derive"; the backend clears the override on a blank code.
    overallStatusCode: module.overallStatusDerived ? '' : (module.overallStatus?.code ?? ''),
    progressPercentage: module.progressPercentage ?? 0,
    releaseVersion: module.releaseVersion ?? '',
    developmentNotes: module.developmentNotes ?? '',
    available: module.available ?? true,
    productionReady: module.productionReady ?? false,
  }
  lifecycleOpen.value = true
}

async function saveLifecycle() {
  if (!lifecycleTarget.value || !lifecycleValid.value) return
  lifecycleSaving.value = true
  clearError()
  try {
    await modulesApi.updateLifecycle(lifecycleTarget.value.id, {
      ...lifecycleForm.value,
      overallStatusCode: lifecycleForm.value.overallStatusCode || null,
      releaseVersion: lifecycleForm.value.releaseVersion || null,
      developmentNotes: lifecycleForm.value.developmentNotes || null,
    })
    lifecycleOpen.value = false
    await loadItems(lastOptions.value)
  } catch (e) {
    setError(e)
  } finally {
    lifecycleSaving.value = false
  }
}

async function loadItems(options: TableOptions) {
  lastOptions.value = options
  loading.value = true
  clearError()

  const sort = options.sortBy[0]
  const params: PageQuery = {
    page: options.page - 1, // Vuetify is 1-based; the API is 0-based.
    size: options.itemsPerPage,
    // The backend defaults to displayOrder,asc when no sort is given.
    sort: sort ? `${sort.key},${sort.order ?? 'asc'}` : undefined,
  }

  try {
    const pageResult = await modulesApi.list(params)
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

// --- Enable / disable toggle ----------------------------------------------
const togglingId = ref<number | null>(null)

async function toggleEnabled(module: AppModule, enabled: boolean) {
  togglingId.value = module.id
  clearError()
  try {
    await modulesApi.updateStatus(module.id, { enabled })
  } catch (err) {
    setError(err)
  } finally {
    togglingId.value = null
    reload()
  }
}

// --- Create / edit dialog -------------------------------------------------
const formOpen = ref(false)
const editing = ref<AppModule | null>(null)
const saving = ref(false)
const { message: formError, set: setFormError, clear: clearFormError } = useApiError()

const { handleSubmit, defineField, errors, resetForm } = useForm({
  validationSchema: toTypedSchema(moduleSchema(t)),
  initialValues: {
    code: '',
    name: '',
    description: '',
    icon: '',
    path: '',
    displayOrder: 10,
    enabled: true,
    createDefaultPermissions: true,
  },
})

const [code, codeProps] = defineField('code')
const [name, nameProps] = defineField('name')
const [description, descriptionProps] = defineField('description')
const [icon, iconProps] = defineField('icon')
const [path, pathProps] = defineField('path')
const [displayOrder, displayOrderProps] = defineField('displayOrder')
const [enabled, enabledProps] = defineField('enabled')
const [createDefaultPermissions, createDefaultPermissionsProps] =
  defineField('createDefaultPermissions')

/** Live preview shown while the icon field holds a plausible mdi name. */
const iconPreview = computed(() =>
  typeof icon.value === 'string' && icon.value.startsWith('mdi-') ? icon.value : 'mdi-help-circle-outline',
)

function openCreate() {
  editing.value = null
  clearFormError()
  resetForm({
    values: {
      code: '',
      name: '',
      description: '',
      icon: '',
      path: '',
      displayOrder: 10,
      enabled: true,
      createDefaultPermissions: true,
    },
  })
  formOpen.value = true
}

function openEdit(module: AppModule) {
  editing.value = module
  clearFormError()
  // The code field is not rendered when editing (it is immutable) but stays
  // seeded so the shared schema still validates.
  resetForm({
    values: {
      code: module.code,
      name: module.name,
      description: module.description ?? '',
      icon: module.icon,
      path: module.path,
      displayOrder: module.displayOrder,
      enabled: module.enabled,
      createDefaultPermissions: false,
    },
  })
  formOpen.value = true
}

const onSubmit = handleSubmit(async (values) => {
  saving.value = true
  clearFormError()
  try {
    if (editing.value) {
      await modulesApi.update(editing.value.id, {
        name: values.name,
        description: values.description || undefined,
        icon: values.icon,
        path: values.path,
        displayOrder: values.displayOrder,
      })
    } else {
      await modulesApi.create({
        code: values.code,
        name: values.name,
        description: values.description || undefined,
        icon: values.icon,
        path: values.path,
        displayOrder: values.displayOrder,
        enabled: values.enabled,
        createDefaultPermissions: values.createDefaultPermissions,
      })
    }
    formOpen.value = false
    reload()
  } catch (err) {
    setFormError(err)
  } finally {
    saving.value = false
  }
})

// --- Delete confirmation --------------------------------------------------
const deleteTarget = ref<AppModule | null>(null)
const deleting = ref(false)

async function confirmDelete() {
  if (!deleteTarget.value || deleteTarget.value.isSystem) return
  deleting.value = true
  try {
    await modulesApi.remove(deleteTarget.value.id)
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
      <h1 class="text-h4">{{ t('modules.title') }}</h1>
      <v-spacer />
      <v-btn v-can="'modules:create'" color="primary" prepend-icon="mdi-plus" @click="openCreate">
        {{ t('modules.new') }}
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
          <v-icon :icon="item.icon" size="small" class="mr-2" />
          {{ item.name }}
          <v-chip v-if="item.isSystem" size="small" variant="tonal" class="ml-2">{{ t('modules.system') }}</v-chip>
        </template>
        <template #[`item.code`]="{ item }">
          <code>{{ item.code }}</code>
        </template>
        <template #[`item.lifecycle`]="{ item }">
          <div v-if="item.overallStatus" class="d-flex align-center ga-2">
            <v-chip
              size="small"
              variant="tonal"
              :color="item.overallStatus.color ?? undefined"
              :prepend-icon="item.overallStatus.icon ?? undefined"
            >
              {{ item.overallStatus.name }}
            </v-chip>
            <span class="text-caption text-medium-emphasis">
              {{ item.backendStatus?.name }} / {{ item.frontendStatus?.name }}
            </span>
            <v-progress-linear
              :model-value="item.progressPercentage ?? 0"
              :color="item.overallStatus.color ?? 'primary'"
              height="4"
              rounded
              style="max-width: 60px"
            />
          </div>
          <code v-else>{{ item.path }}</code>
        </template>
        <template #[`item.enabled`]="{ item }">
          <v-switch
            v-can.disable="'modules:edit'"
            :model-value="item.enabled"
            :disabled="item.isSystem"
            :loading="togglingId === item.id"
            color="primary"
            density="compact"
            hide-details
            inset
            @update:model-value="toggleEnabled(item, $event === true)"
          />
        </template>
        <template #[`item.actions`]="{ item }">
          <v-btn
            v-can="'modules:edit'"
            icon="mdi-pencil"
            size="small"
            variant="text"
            :title="t('common.edit')"
            @click="openEdit(item)"
          />
          <v-btn
            v-can="'modules:edit'"
            icon="mdi-progress-wrench"
            size="small"
            variant="text"
            :title="t('modules.lifecycle.manage')"
            @click="openLifecycle(item)"
          />
          <v-btn
            v-can="'modules:delete'"
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
          {{ editing ? t('modules.edit') : t('modules.new') }}
        </v-card-title>

        <v-card-text class="px-6">
          <v-alert v-if="formError" type="error" variant="tonal" density="compact" class="mb-4">
            {{ formError }}
          </v-alert>

          <v-form @submit.prevent="onSubmit">
            <v-text-field
              v-if="!editing"
              v-model="code"
              v-bind="codeProps"
              :error-messages="errors.code"
              :label="t('modules.code')"
              :hint="t('modules.codeHint')"
              persistent-hint
            />
            <v-text-field
              v-model="name"
              v-bind="nameProps"
              :error-messages="errors.name"
              :label="t('modules.name')"
            />
            <v-textarea
              v-model="description"
              v-bind="descriptionProps"
              :error-messages="errors.description"
              :label="t('modules.description')"
              rows="2"
              auto-grow
            />
            <v-text-field
              v-model="icon"
              v-bind="iconProps"
              :error-messages="errors.icon"
              :label="t('modules.icon')"
              :prepend-inner-icon="iconPreview"
              :hint="t('modules.iconHint')"
              persistent-hint
            />
            <v-row>
              <v-col cols="8">
                <v-text-field
                  v-model="path"
                  v-bind="pathProps"
                  :error-messages="errors.path"
                  :label="t('modules.path')"
                  :hint="t('modules.pathHint')"
                  persistent-hint
                />
              </v-col>
              <v-col cols="4">
                <v-text-field
                  v-model="displayOrder"
                  v-bind="displayOrderProps"
                  :error-messages="errors.displayOrder"
                  :label="t('modules.order')"
                  type="number"
                />
              </v-col>
            </v-row>
            <template v-if="!editing">
              <v-switch
                v-model="enabled"
                v-bind="enabledProps"
                :label="t('modules.enabled')"
                color="primary"
                inset
                hide-details
              />
              <v-checkbox
                v-model="createDefaultPermissions"
                v-bind="createDefaultPermissionsProps"
                :label="t('modules.createPermissions')"
                hide-details
              />
            </template>
          </v-form>
        </v-card-text>

        <v-card-actions class="px-6 pb-4">
          <v-spacer />
          <v-btn variant="text" @click="formOpen = false">{{ t('common.cancel') }}</v-btn>
          <v-btn color="primary" :loading="saving" @click="onSubmit">{{ t('common.save') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>


    <!-- Lifecycle management -->
    <v-dialog v-model="lifecycleOpen" max-width="620">
      <v-card rounded="lg">
        <v-card-title class="text-h6 pt-4 px-6">
          {{ t('modules.lifecycle.title', { name: lifecycleTarget?.name ?? '' }) }}
        </v-card-title>
        <v-card-text class="px-6">
          <v-row dense>
            <v-col cols="12" sm="6">
              <v-select
                v-model="lifecycleForm.backendStatusCode"
                :items="backendStageOptions"
                item-title="name"
                item-value="code"
                :label="t('modules.lifecycle.backendStatus')"
                :error-messages="lifecycleErrors.backend"
                density="comfortable"
                variant="outlined"
              />
            </v-col>
            <v-col cols="12" sm="6">
              <v-select
                v-model="lifecycleForm.frontendStatusCode"
                :items="frontendStageOptions"
                item-title="name"
                item-value="code"
                :label="t('modules.lifecycle.frontendStatus')"
                :error-messages="lifecycleErrors.frontend"
                density="comfortable"
                variant="outlined"
              />
            </v-col>

            <v-col cols="12">
              <v-select
                v-model="lifecycleForm.overallStatusCode"
                :items="[{ name: t('modules.lifecycle.derived'), code: '' }, ...overallStageOptions]"
                item-title="name"
                item-value="code"
                :label="t('modules.lifecycle.overallStatus')"
                :hint="t('modules.lifecycle.overallHint')"
                persistent-hint
                density="comfortable"
                variant="outlined"
              />
            </v-col>

            <v-col cols="12" sm="8">
              <v-slider
                v-model="lifecycleForm.progressPercentage"
                :label="t('modules.lifecycle.progress')"
                :min="0"
                :max="100"
                :step="5"
                thumb-label
                class="mt-4"
              />
            </v-col>
            <v-col cols="12" sm="4">
              <v-text-field
                v-model="lifecycleForm.releaseVersion"
                :label="t('modules.lifecycle.releaseVersion')"
                :error-messages="lifecycleErrors.releaseVersion"
                :counter="MAX_RELEASE_VERSION"
                :maxlength="MAX_RELEASE_VERSION"
                density="comfortable"
                variant="outlined"
                placeholder="1.4.0"
              />
            </v-col>

            <v-col cols="12">
              <v-textarea
                v-model="lifecycleForm.developmentNotes"
                :label="t('modules.lifecycle.notes')"
                :error-messages="lifecycleErrors.notes"
                :counter="MAX_NOTES"
                :maxlength="MAX_NOTES"
                rows="2"
                auto-grow
                density="comfortable"
                variant="outlined"
              />
            </v-col>

            <v-col cols="12" sm="6">
              <v-switch
                v-model="lifecycleForm.available"
                :label="t('modules.lifecycle.available')"
                color="primary"
                density="compact"
                hide-details
                inset
              />
            </v-col>
            <v-col cols="12" sm="6">
              <v-switch
                v-model="lifecycleForm.productionReady"
                :label="t('modules.lifecycle.productionReady')"
                :error-messages="lifecycleErrors.productionReady"
                color="primary"
                density="compact"
                hide-details="auto"
                inset
              />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions class="px-6 pb-4">
          <v-spacer />
          <v-btn variant="text" @click="lifecycleOpen = false">{{ t('common.cancel') }}</v-btn>
          <v-btn
            color="primary"
            variant="flat"
            :loading="lifecycleSaving"
            :disabled="!lifecycleValid"
            @click="saveLifecycle"
          >
            {{ t('common.save') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Delete confirmation -->
    <v-dialog :model-value="!!deleteTarget" max-width="420" @update:model-value="deleteTarget = null">
      <v-card rounded="lg">
        <v-card-title class="text-h6">{{ t('modules.deleteTitle') }}</v-card-title>
        <v-card-text>
          <i18n-t keypath="modules.deleteConfirm" tag="span">
            <template #name>
              <strong>{{ deleteTarget?.name }}</strong>
            </template>
          </i18n-t>
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
