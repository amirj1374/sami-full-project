<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useDebounceFn } from '@vueuse/core'
import { dashboardsApi, type DashboardListParams } from '@/api/dashboards'
import { usePermission } from '@/composables/usePermission'
import { useApiError } from '@/composables/useApiError'
import { useFormat } from '@/composables/useFormat'
import DashboardFormDialog from '@/components/DashboardFormDialog.vue'
import DashboardShareDialog from '@/components/DashboardShareDialog.vue'
import type { DashboardRow, DashboardStatus, DashboardVisibility } from '@/types/models'

/** Dashboard management: list, create/edit, share, import/export, favorites. */
const { t } = useI18n()
const router = useRouter()
const { can } = usePermission()
const { formatDate } = useFormat()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()

const statuses = ref<DashboardStatus[]>([])
const visibilities = ref<DashboardVisibility[]>([])

onMounted(async () => {
  try {
    ;[statuses.value, visibilities.value] = await Promise.all([
      dashboardsApi.statuses(),
      dashboardsApi.visibilities(),
    ])
  } catch (err) {
    setError(err)
  }
})

// --- Table ------------------------------------------------------------------
interface TableOptions {
  page: number
  itemsPerPage: number
  sortBy: { key: string; order?: 'asc' | 'desc' }[]
}

const items = ref<DashboardRow[]>([])
const totalItems = ref(0)
const loading = ref(false)
const lastOptions = ref<TableOptions>({ page: 1, itemsPerPage: 10, sortBy: [] })

const headers = computed(() => [
  { title: '', key: 'favorite', sortable: false, width: 40 },
  { title: t('common.code'), key: 'code' },
  { title: t('common.name'), key: 'name' },
  { title: t('common.status'), key: 'status', sortable: false },
  { title: t('dash.visibility'), key: 'visibility', sortable: false },
  { title: t('dash.created'), key: 'createdAt' },
  { title: '', key: 'actions', sortable: false, align: 'end' as const },
])

const searchFilter = ref('')
const statusFilter = ref<number | null>(null)
const favoritesOnly = ref(false)

async function loadItems(options: TableOptions): Promise<void> {
  lastOptions.value = options
  loading.value = true
  clearError()
  const sort = options.sortBy[0]
  const params: DashboardListParams = {
    page: options.page - 1,
    size: options.itemsPerPage,
    sort: sort ? `${sort.key},${sort.order ?? 'asc'}` : undefined,
    search: searchFilter.value || undefined,
    statusId: statusFilter.value ?? undefined,
    favoritesOnly: favoritesOnly.value || undefined,
  }
  try {
    const page = await dashboardsApi.list(params)
    items.value = page.content
    totalItems.value = page.totalElements
  } catch (err) {
    setError(err)
  } finally {
    loading.value = false
  }
}

function refresh(): void {
  void loadItems(lastOptions.value)
}

const reload = useDebounceFn(() => void loadItems({ ...lastOptions.value, page: 1 }), 300)
watch([searchFilter, statusFilter, favoritesOnly], reload)

const statusItems = computed(() => [
  { title: t('common.all'), value: null as number | null },
  ...statuses.value.map((s) => ({ title: s.name, value: s.id as number | null })),
])

// --- Dialogs / actions ------------------------------------------------------
const formOpen = ref(false)
const editing = ref<DashboardRow | null>(null)
const shareOpen = ref(false)
const shareTarget = ref<DashboardRow | null>(null)
const deleteTarget = ref<DashboardRow | null>(null)
const deleting = ref(false)
const importInput = ref<HTMLInputElement | null>(null)

function openCreate(): void {
  editing.value = null
  formOpen.value = true
}

function openEdit(row: DashboardRow): void {
  editing.value = row
  formOpen.value = true
}

function openViewer(row: DashboardRow): void {
  void router.push({ name: 'dashboard-viewer', params: { id: row.id } })
}

function openShare(row: DashboardRow): void {
  shareTarget.value = row
  shareOpen.value = true
}

async function toggleFavorite(row: DashboardRow): Promise<void> {
  try {
    if (row.favorite) await dashboardsApi.removeFavorite(row.id)
    else await dashboardsApi.addFavorite(row.id)
    refresh()
  } catch (err) {
    setError(err)
  }
}

async function exportDashboard(row: DashboardRow): Promise<void> {
  try {
    const snapshot = await dashboardsApi.export(row.id)
    const url = URL.createObjectURL(
      new Blob([JSON.stringify(snapshot, null, 2)], { type: 'application/json' }),
    )
    const link = document.createElement('a')
    link.href = url
    link.download = `dashboard-${row.code}.json`
    link.click()
    URL.revokeObjectURL(url)
  } catch (err) {
    setError(err)
  }
}

async function onImportFile(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  clearError()
  try {
    const snapshot = JSON.parse(await file.text()) as Record<string, unknown>
    await dashboardsApi.import(snapshot)
    refresh()
  } catch (err) {
    setError(err)
  }
}

async function confirmDelete(): Promise<void> {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    await dashboardsApi.remove(deleteTarget.value.id)
    deleteTarget.value = null
    refresh()
  } catch (err) {
    setError(err)
  } finally {
    deleting.value = false
  }
}

function statusColor(row: DashboardRow): string {
  const status = statuses.value.find((s) => s.id === row.statusId)
  if (status?.isArchivedState) return 'warning'
  return status?.isActiveState ? 'success' : 'default'
}
</script>

<template>
  <div>
    <div class="d-flex align-center mb-4 flex-wrap ga-2">
      <h1 class="text-h4">{{ t('dash.dashboards') }}</h1>
      <v-spacer />
      <input ref="importInput" type="file" accept=".json,application/json" class="d-none" @change="onImportFile" />
      <v-btn v-can="'dashboards:view'" variant="tonal" prepend-icon="mdi-chart-box" :to="{ name: 'dashboard-reports' }">
        {{ t('dash.reports') }}
      </v-btn>
      <v-btn v-can="'dashboards:manage-kpis'" variant="tonal" prepend-icon="mdi-speedometer" :to="{ name: 'kpis' }">
        {{ t('dash.kpis') }}
      </v-btn>
      <v-btn v-can="'dashboards:create'" variant="tonal" prepend-icon="mdi-upload" @click="importInput?.click()">
        {{ t('common.import') }}
      </v-btn>
      <v-btn v-can="'dashboards:create'" color="primary" prepend-icon="mdi-plus" @click="openCreate">
        {{ t('dash.newDashboard') }}
      </v-btn>
    </div>

    <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
      {{ errorMessage }}
    </v-alert>

    <v-card rounded="lg" border flat>
      <v-card-text>
        <v-row dense align="center">
          <v-col cols="12" sm="5">
            <v-text-field
              v-model="searchFilter"
              :label="t('common.search')"
              prepend-inner-icon="mdi-magnify"
              clearable
              hide-details
              density="comfortable"
            />
          </v-col>
          <v-col cols="6" sm="3">
            <v-select v-model="statusFilter" :label="t('common.status')" :items="statusItems" hide-details density="comfortable" />
          </v-col>
          <v-col cols="6" sm="3">
            <v-switch v-model="favoritesOnly" :label="t('dash.favoritesOnly')" color="primary" hide-details density="compact" />
          </v-col>
        </v-row>
      </v-card-text>

      <v-data-table-server
        :headers="headers"
        :items="items"
        :items-length="totalItems"
        :loading="loading"
        :items-per-page="10"
        item-value="id"
        @update:options="loadItems"
      >
        <template #[`item.favorite`]="{ item }">
          <v-btn
            :icon="item.favorite ? 'mdi-star' : 'mdi-star-outline'"
            size="small"
            variant="text"
            :color="item.favorite ? 'amber' : undefined"
            @click="toggleFavorite(item)"
          />
        </template>
        <template #[`item.name`]="{ item }">
          <a class="text-primary cursor-pointer" @click="openViewer(item)">{{ item.name }}</a>
          <v-chip v-if="item.isDefault" size="x-small" variant="tonal" class="ml-1">{{ t('dash.default') }}</v-chip>
        </template>
        <template #[`item.status`]="{ item }">
          <v-chip :color="statusColor(item)" size="small" variant="tonal">{{ item.statusName }}</v-chip>
        </template>
        <template #[`item.visibility`]="{ item }">
          <v-chip size="small" variant="tonal">{{ item.visibilityName }}</v-chip>
        </template>
        <template #[`item.createdAt`]="{ item }">{{ formatDate(item.createdAt) }}</template>
        <template #[`item.actions`]="{ item }">
          <v-btn icon="mdi-monitor-dashboard" size="small" variant="text" @click="openViewer(item)" />
          <v-btn
            v-if="item.canEdit && can('dashboards:edit')"
            icon="mdi-pencil"
            size="small"
            variant="text"
            @click="openEdit(item)"
          />
          <v-menu location="bottom end">
            <template #activator="{ props: menuProps }">
              <v-btn icon="mdi-dots-vertical" size="small" variant="text" v-bind="menuProps" />
            </template>
            <v-list density="compact" min-width="200">
              <v-list-item
                v-if="item.canEdit && can('dashboards:share')"
                prepend-icon="mdi-share-variant"
                :title="t('dash.shareDashboard')"
                @click="openShare(item)"
              />
              <v-list-item
                v-if="can('dashboards:export')"
                prepend-icon="mdi-download"
                :title="t('common.export')"
                @click="exportDashboard(item)"
              />
              <v-list-item
                v-if="item.canEdit && can('dashboards:delete')"
                prepend-icon="mdi-delete"
                base-color="error"
                :title="t('common.delete')"
                @click="deleteTarget = item"
              />
            </v-list>
          </v-menu>
        </template>
      </v-data-table-server>
    </v-card>

    <DashboardFormDialog
      v-model="formOpen"
      :dashboard="editing"
      :statuses="statuses"
      :visibilities="visibilities"
      @saved="refresh"
    />

    <DashboardShareDialog v-model="shareOpen" :dashboard="shareTarget" />

    <v-dialog :model-value="!!deleteTarget" max-width="420" @update:model-value="deleteTarget = null">
      <v-card rounded="lg">
        <v-card-title class="text-h6">{{ t('common.delete') }}</v-card-title>
        <v-card-text>
          {{ t('dash.deleteConfirm') }} <strong>{{ deleteTarget?.name }}</strong>
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
