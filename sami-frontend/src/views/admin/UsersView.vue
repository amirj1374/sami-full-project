<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDebounceFn } from '@vueuse/core'
import { useFormat } from '@/composables/useFormat'
import { usersApi, type RoleOption, type UserListParams } from '@/api/users'
import { userStatusesApi } from '@/api/userStatuses'
import { profileFieldsApi } from '@/api/profileFields'
import { useAuthStore } from '@/stores/auth'
import { usePermission } from '@/composables/usePermission'
import { useServerLabel } from '@/composables/useServerLabel'
import { useApiError } from '@/composables/useApiError'
import UserFormDialog from '@/components/UserFormDialog.vue'
import UserAuditDialog from '@/components/UserAuditDialog.vue'
import type {
  AdminUser,
  BulkResult,
  ProfileFieldDefinition,
  UserDetail,
  UserStatus,
} from '@/types/models'

const { t } = useI18n()
const { formatDate } = useFormat()
const auth = useAuthStore()
const { can } = usePermission()
const { text: srvLabel } = useServerLabel()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()

/** The signed-in admin cannot act on their own account from this screen. */
function isSelf(user: AdminUser): boolean {
  return auth.user?.id === user.id
}

// --- Reference data --------------------------------------------------------
const roleOptions = ref<RoleOption[]>([])
const statuses = ref<UserStatus[]>([])
const fieldDefs = ref<ProfileFieldDefinition[]>([])

onMounted(async () => {
  try {
    ;[roleOptions.value, statuses.value, fieldDefs.value] = await Promise.all([
      usersApi.roleOptions(),
      userStatusesApi.list(),
      profileFieldsApi.list(true),
    ])
  } catch (err) {
    setError(err)
  }
})

// --- Table state ------------------------------------------------------------
interface SortItem {
  key: string
  order?: 'asc' | 'desc'
}
interface TableOptions {
  page: number
  itemsPerPage: number
  sortBy: SortItem[]
}

const items = ref<AdminUser[]>([])
const totalItems = ref(0)
const loading = ref(false)
const selected = ref<number[]>([])
const lastOptions = ref<TableOptions>({ page: 1, itemsPerPage: 10, sortBy: [] })

const headers = computed(() => [
  { title: t('users.headerName'), key: 'fullName' },
  { title: t('users.headerEmail'), key: 'email' },
  { title: t('users.headerRole'), key: 'role', sortable: false },
  { title: t('users.headerStatus'), key: 'status', sortable: false },
  { title: t('users.headerCreated'), key: 'createdAt' },
  { title: '', key: 'actions', sortable: false, align: 'end' as const },
])

// --- Filters ----------------------------------------------------------------
const searchFilter = ref('')
const roleFilter = ref<number | null>(null)
const statusFilter = ref<number | null>(null)
const includeHidden = ref(false)

const roleFilterItems = computed(() => [
  { title: t('users.allRoles'), value: null as number | null },
  ...roleOptions.value.map((role) => ({ title: role.name, value: role.id as number | null })),
])
const statusFilterItems = computed(() => [
  { title: t('users.allStatuses'), value: null as number | null },
  ...statuses.value.map((s) => ({ title: s.name, value: s.id as number | null })),
])

function currentFilter() {
  return {
    search: searchFilter.value || undefined,
    roleId: roleFilter.value ?? undefined,
    statusId: statusFilter.value ?? undefined,
    includeHidden: includeHidden.value || undefined,
  }
}

async function loadItems(options: TableOptions) {
  lastOptions.value = options
  loading.value = true
  clearError()

  const sort = options.sortBy[0]
  const params: UserListParams = {
    page: options.page - 1, // Vuetify is 1-based; the API is 0-based.
    size: options.itemsPerPage,
    sort: sort ? `${sort.key},${sort.order ?? 'asc'}` : undefined,
    ...currentFilter(),
  }

  try {
    const pageResult = await usersApi.list(params)
    items.value = pageResult.content
    totalItems.value = pageResult.totalElements
    selected.value = []
  } catch (err) {
    setError(err)
  } finally {
    loading.value = false
  }
}

function refresh() {
  void loadItems(lastOptions.value)
}

const reload = useDebounceFn(() => {
  void loadItems({ ...lastOptions.value, page: 1 })
}, 300)
watch([searchFilter, roleFilter, statusFilter, includeHidden], reload)

// --- Create / edit dialog ---------------------------------------------------
const formOpen = ref(false)
const editingDetail = ref<UserDetail | null>(null)

function openCreate() {
  editingDetail.value = null
  formOpen.value = true
}

async function openEdit(user: AdminUser) {
  clearError()
  try {
    editingDetail.value = await usersApi.get(user.id)
    formOpen.value = true
  } catch (err) {
    setError(err)
  }
}

// --- Row actions ------------------------------------------------------------
const actionBusy = ref<number | null>(null)

async function rowAction(user: AdminUser, action: () => Promise<unknown>) {
  actionBusy.value = user.id
  clearError()
  try {
    await action()
    refresh()
  } catch (err) {
    setError(err)
  } finally {
    actionBusy.value = null
  }
}

// --- Confirmations (soft delete / purge) ------------------------------------
const deleteTarget = ref<AdminUser | null>(null)
const purgeTarget = ref<AdminUser | null>(null)
const confirming = ref(false)

async function confirmDelete() {
  if (!deleteTarget.value) return
  confirming.value = true
  try {
    await usersApi.softDelete(deleteTarget.value.id)
    deleteTarget.value = null
    refresh()
  } catch (err) {
    setError(err)
  } finally {
    confirming.value = false
  }
}

async function confirmPurge() {
  if (!purgeTarget.value) return
  confirming.value = true
  try {
    await usersApi.purge(purgeTarget.value.id)
    purgeTarget.value = null
    refresh()
  } catch (err) {
    setError(err)
  } finally {
    confirming.value = false
  }
}

// --- Audit dialog -----------------------------------------------------------
const auditOpen = ref(false)
const auditUser = ref<AdminUser | null>(null)

function openAudit(user: AdminUser) {
  auditUser.value = user
  auditOpen.value = true
}

// --- Bulk operations --------------------------------------------------------
const bulkBusy = ref(false)
const bulkResult = ref<BulkResult | null>(null)
const bulkStatusId = ref<number | null>(null)

const bulkStatusItems = computed(() =>
  statuses.value
    .filter((s) => !s.isArchivedState && !s.isDeletedState)
    .map((s) => ({ title: s.name, value: s.id })),
)

async function runBulk(operation: (ids: number[]) => Promise<BulkResult>) {
  if (selected.value.length === 0) return
  bulkBusy.value = true
  clearError()
  bulkResult.value = null
  try {
    bulkResult.value = await operation([...selected.value])
    refresh()
  } catch (err) {
    setError(err)
  } finally {
    bulkBusy.value = false
  }
}

function bulkSetStatus() {
  if (bulkStatusId.value === null) return
  const statusId = bulkStatusId.value
  void runBulk((ids) => usersApi.bulkStatus({ ids, statusId }))
}

// --- Export -----------------------------------------------------------------
const exporting = ref(false)

async function exportCsv() {
  exporting.value = true
  clearError()
  try {
    const csv = await usersApi.exportCsv(currentFilter())
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
    const link = document.createElement('a')
    link.href = url
    link.download = 'users.csv'
    link.click()
    URL.revokeObjectURL(url)
  } catch (err) {
    setError(err)
  } finally {
    exporting.value = false
  }
}

function statusColor(status: UserStatus): string {
  if (status.isDeletedState) return 'error'
  if (status.isArchivedState) return 'warning'
  return status.allowsLogin ? 'success' : 'default'
}
</script>

<template>
  <div>
    <div class="d-flex align-center mb-4">
      <h1 class="text-h4">{{ t('users.title') }}</h1>
      <v-spacer />
      <v-btn
        v-can="'users:export'"
        variant="tonal"
        prepend-icon="mdi-download"
        class="mr-2"
        :loading="exporting"
        @click="exportCsv"
      >
        {{ t('users.exportCsv') }}
      </v-btn>
      <v-btn v-can="'users:create'" color="primary" prepend-icon="mdi-plus" @click="openCreate">
        {{ t('users.newUser') }}
      </v-btn>
    </div>

    <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
      {{ errorMessage }}
    </v-alert>

    <v-card rounded="lg" border flat>
      <v-card-text>
        <v-row dense align="center">
          <v-col cols="12" sm="4">
            <v-text-field
              v-model="searchFilter"
              :label="t('users.searchLabel')"
              prepend-inner-icon="mdi-magnify"
              clearable
              hide-details
              density="comfortable"
            />
          </v-col>
          <v-col cols="6" sm="3">
            <v-select
              v-model="roleFilter"
              :label="t('users.headerRole')"
              :items="roleFilterItems"
              hide-details
              density="comfortable"
            />
          </v-col>
          <v-col cols="6" sm="3">
            <v-select
              v-model="statusFilter"
              :label="t('users.headerStatus')"
              :items="statusFilterItems"
              hide-details
              density="comfortable"
            />
          </v-col>
          <v-col cols="12" sm="2">
            <v-switch
              v-model="includeHidden"
              :label="t('users.archivedAndDeleted')"
              color="primary"
              hide-details
              density="compact"
            />
          </v-col>
        </v-row>
      </v-card-text>

      <!-- Bulk toolbar -->
      <v-expand-transition>
        <div v-if="selected.length > 0">
          <v-divider />
          <div class="d-flex align-center flex-wrap px-4 py-2 ga-2">
            <span class="text-body-2 mr-2">{{
              t('common.selectedCount', { count: selected.length })
            }}</span>
            <v-select
              v-if="can('users:edit')"
              v-model="bulkStatusId"
              :items="bulkStatusItems"
              :label="t('users.setStatusTo')"
              density="compact"
              hide-details
              style="max-width: 200px"
            />
            <v-btn
              v-can="'users:edit'"
              size="small"
              variant="tonal"
              :disabled="bulkStatusId === null"
              :loading="bulkBusy"
              @click="bulkSetStatus"
            >
              {{ t('common.apply') }}
            </v-btn>
            <v-btn
              v-can="'users:archive'"
              size="small"
              variant="tonal"
              color="warning"
              prepend-icon="mdi-archive"
              :loading="bulkBusy"
              @click="runBulk((ids) => usersApi.bulkArchive({ ids }))"
            >
              {{ t('users.archive') }}
            </v-btn>
            <v-btn
              v-can="'users:restore'"
              size="small"
              variant="tonal"
              color="success"
              prepend-icon="mdi-restore"
              :loading="bulkBusy"
              @click="runBulk((ids) => usersApi.bulkRestore({ ids }))"
            >
              {{ t('users.restore') }}
            </v-btn>
            <v-spacer />
            <span v-if="bulkResult" class="text-caption text-medium-emphasis">
              {{ t('users.bulkProcessed', { count: bulkResult.processed })
              }}<template v-if="bulkResult.skipped.length"
                >{{
                  t('users.bulkSkipped', {
                    count: bulkResult.skipped.length,
                    reason: bulkResult.skipped[0].reason,
                  })
                }}<template v-if="bulkResult.skipped.length > 1">, …</template>)</template
              >
            </span>
          </div>
        </div>
      </v-expand-transition>

      <v-data-table-server
        v-model="selected"
        :headers="headers"
        :items="items"
        :items-length="totalItems"
        :loading="loading"
        :items-per-page="10"
        item-value="id"
        show-select
        @update:options="loadItems"
      >
        <template #[`item.role`]="{ item }">
          <v-chip :color="item.role.isSuperAdmin ? 'primary' : 'default'" size="small" variant="tonal">
            {{ srvLabel(item.role.name) }}
          </v-chip>
        </template>
        <template #[`item.status`]="{ item }">
          <v-chip :color="statusColor(item.status)" size="small" variant="tonal">
            {{ srvLabel(item.status.name) }}
          </v-chip>
        </template>
        <template #[`item.createdAt`]="{ item }">{{ formatDate(item.createdAt) }}</template>
        <template #[`item.actions`]="{ item }">
          <v-btn
            v-can="'users:edit'"
            icon="mdi-pencil"
            size="small"
            variant="text"
            :disabled="isSelf(item)"
            @click="openEdit(item)"
          />
          <v-menu location="bottom end">
            <template #activator="{ props: menuProps }">
              <v-btn
                icon="mdi-dots-vertical"
                size="small"
                variant="text"
                v-bind="menuProps"
                :loading="actionBusy === item.id"
              />
            </template>
            <v-list density="compact" min-width="220">
              <v-list-item
                v-can="'users:view'"
                prepend-icon="mdi-history"
                :title="t('users.auditTrail')"
                @click="openAudit(item)"
              />
              <v-list-item
                v-if="can('users:archive') && !item.status.isArchivedState && !item.status.isDeletedState"
                prepend-icon="mdi-archive"
                :title="t('users.archive')"
                :disabled="isSelf(item)"
                @click="rowAction(item, () => usersApi.archive(item.id))"
              />
              <v-list-item
                v-if="can('users:restore') && (item.status.isArchivedState || item.status.isDeletedState)"
                prepend-icon="mdi-restore"
                :title="t('users.restore')"
                @click="rowAction(item, () => usersApi.restore(item.id))"
              />
              <v-list-item
                v-if="can('users:delete') && !item.status.isDeletedState"
                prepend-icon="mdi-delete"
                :title="t('users.deleteSoft')"
                :disabled="isSelf(item)"
                @click="deleteTarget = item"
              />
              <v-list-item
                v-if="can('users:purge') && item.status.isDeletedState"
                prepend-icon="mdi-delete-forever"
                base-color="error"
                :title="t('users.deletePermanently')"
                @click="purgeTarget = item"
              />
            </v-list>
          </v-menu>
        </template>
      </v-data-table-server>
    </v-card>

    <UserFormDialog
      v-model="formOpen"
      :user="editingDetail"
      :role-options="roleOptions"
      :status-options="statuses"
      :field-defs="fieldDefs"
      @saved="refresh"
    />

    <UserAuditDialog v-model="auditOpen" :user="auditUser" />

    <!-- Soft delete confirmation -->
    <v-dialog :model-value="!!deleteTarget" max-width="440" @update:model-value="deleteTarget = null">
      <v-card rounded="lg">
        <v-card-title class="text-h6">{{ t('users.deleteUserTitle') }}</v-card-title>
        <v-card-text>
          <i18n-t keypath="users.deleteConfirmBody" tag="span">
            <template #name><strong>{{ deleteTarget?.fullName }}</strong></template>
            <template #email>{{ deleteTarget?.email }}</template>
          </i18n-t>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="deleteTarget = null">{{ t('common.cancel') }}</v-btn>
          <v-btn color="error" :loading="confirming" @click="confirmDelete">{{ t('common.delete') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Permanent delete confirmation -->
    <v-dialog :model-value="!!purgeTarget" max-width="440" @update:model-value="purgeTarget = null">
      <v-card rounded="lg">
        <v-card-title class="text-h6">{{ t('users.deletePermanently') }}</v-card-title>
        <v-card-text>
          <i18n-t keypath="users.purgeConfirmBody" tag="span">
            <template #name><strong>{{ purgeTarget?.fullName }}</strong></template>
            <template #email>{{ purgeTarget?.email }}</template>
            <template #warning><strong>{{ t('users.purgeCannotUndo') }}</strong></template>
          </i18n-t>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="purgeTarget = null">{{ t('common.cancel') }}</v-btn>
          <v-btn color="error" :loading="confirming" @click="confirmPurge">{{ t('users.deleteForever') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
