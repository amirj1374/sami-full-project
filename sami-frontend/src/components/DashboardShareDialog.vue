<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { dashboardsApi } from '@/api/dashboards'
import { usersApi } from '@/api/users'
import { rolesApi } from '@/api/roles'
import { useApiError } from '@/composables/useApiError'
import type { DashboardRow, DashboardShare } from '@/types/models'

/** Share a dashboard with users or roles (optionally with edit rights). */
const props = defineProps<{
  modelValue: boolean
  dashboard: DashboardRow | null
}>()

const emit = defineEmits<{ 'update:modelValue': [boolean] }>()
const { t } = useI18n()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()

const open = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const shares = ref<DashboardShare[]>([])
const targetType = ref<'USER' | 'ROLE'>('USER')
const targetUserId = ref<number | null>(null)
const targetRoleId = ref<number | null>(null)
const canEdit = ref(false)
const busy = ref(false)

const userOptions = ref<{ id: number; email: string }[]>([])
const roleOptions = ref<{ id: number; name: string }[]>([])

async function load(): Promise<void> {
  if (!props.dashboard) return
  clearError()
  try {
    shares.value = await dashboardsApi.shares(props.dashboard.id)
    if (userOptions.value.length === 0) {
      const [users, roles] = await Promise.all([
        usersApi.list({ size: 200 }),
        rolesApi.list({ size: 200 }),
      ])
      userOptions.value = users.content.map((u) => ({ id: u.id, email: u.email }))
      roleOptions.value = roles.content.map((r) => ({ id: r.id, name: r.name }))
    }
  } catch (err) {
    setError(err)
  }
}

watch(open, (isOpen) => {
  if (isOpen) {
    targetType.value = 'USER'
    targetUserId.value = null
    targetRoleId.value = null
    canEdit.value = false
    void load()
  }
})

async function addShare(): Promise<void> {
  if (!props.dashboard) return
  busy.value = true
  clearError()
  try {
    await dashboardsApi.share(props.dashboard.id, {
      targetType: targetType.value,
      targetUserId: targetType.value === 'USER' ? (targetUserId.value ?? undefined) : undefined,
      targetRoleId: targetType.value === 'ROLE' ? (targetRoleId.value ?? undefined) : undefined,
      canEdit: canEdit.value,
    })
    await load()
  } catch (err) {
    setError(err)
  } finally {
    busy.value = false
  }
}

async function removeShare(share: DashboardShare): Promise<void> {
  if (!props.dashboard) return
  try {
    await dashboardsApi.unshare(props.dashboard.id, share.id)
    await load()
  } catch (err) {
    setError(err)
  }
}
</script>

<template>
  <v-dialog v-model="open" max-width="560">
    <v-card rounded="lg">
      <v-card-title class="text-h6 pt-4 px-6">
        {{ t('dash.shareDashboard') }} — {{ dashboard?.name }}
      </v-card-title>

      <v-card-text class="px-6">
        <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
          {{ errorMessage }}
        </v-alert>

        <div class="d-flex align-center ga-2 mb-4 flex-wrap">
          <v-btn-toggle v-model="targetType" mandatory density="compact">
            <v-btn value="USER" size="small">{{ t('dash.user') }}</v-btn>
            <v-btn value="ROLE" size="small">{{ t('dash.role') }}</v-btn>
          </v-btn-toggle>
          <v-autocomplete
            v-if="targetType === 'USER'"
            v-model="targetUserId"
            :items="userOptions"
            item-title="email"
            item-value="id"
            :label="t('dash.user')"
            density="compact"
            hide-details
            style="min-width: 200px"
          />
          <v-select
            v-else
            v-model="targetRoleId"
            :items="roleOptions"
            item-title="name"
            item-value="id"
            :label="t('dash.role')"
            density="compact"
            hide-details
            style="min-width: 200px"
          />
          <v-checkbox v-model="canEdit" :label="t('dash.canEdit')" density="compact" hide-details />
          <v-btn
            size="small"
            color="primary"
            :disabled="targetType === 'USER' ? targetUserId === null : targetRoleId === null"
            :loading="busy"
            @click="addShare"
          >
            {{ t('common.add') }}
          </v-btn>
        </div>

        <p v-if="shares.length === 0" class="text-body-2 text-medium-emphasis">
          {{ t('dash.noShares') }}
        </p>
        <v-list v-else density="compact">
          <v-list-item v-for="share in shares" :key="share.id" class="px-0">
            <template #prepend>
              <v-icon :icon="share.targetType === 'USER' ? 'mdi-account' : 'mdi-shield-account'" size="small" class="mr-2" />
            </template>
            <v-list-item-title class="text-body-2">
              {{ share.targetUserEmail ?? share.targetRoleName }}
              <v-chip v-if="share.canEdit" size="x-small" variant="tonal" color="primary" class="ml-1">
                {{ t('dash.canEdit') }}
              </v-chip>
            </v-list-item-title>
            <template #append>
              <v-btn icon="mdi-close" size="x-small" variant="text" @click="removeShare(share)" />
            </template>
          </v-list-item>
        </v-list>
      </v-card-text>

      <v-card-actions class="px-6 pb-4">
        <v-spacer />
        <v-btn variant="text" @click="open = false">{{ t('common.close') }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
