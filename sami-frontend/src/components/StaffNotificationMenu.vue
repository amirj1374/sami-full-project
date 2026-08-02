<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { userExperienceApi } from '@/api/userExperience'
import { useFormat } from '@/composables/useFormat'
import type { StaffNotification } from '@/types/userExperience'

const { t, te } = useI18n()
const { formatDateTime } = useFormat()
const router = useRouter()
const open = ref(false)
const loading = ref(false)
const items = ref<StaffNotification[]>([])
const unread = ref(0)
let timer: number | undefined

function label(key: string): string {
  return te(key) ? t(key) : key
}

async function refresh(): Promise<void> {
  try {
    const [notifications, count] = await Promise.all([
      userExperienceApi.notifications(),
      userExperienceApi.unreadCount(),
    ])
    items.value = notifications
    unread.value = count
  } catch {
    // The shell remains usable when notification retrieval is temporarily unavailable.
  }
}

async function onOpen(value: boolean): Promise<void> {
  open.value = value
  if (!value) return
  loading.value = true
  await refresh()
  loading.value = false
}

async function openNotification(item: StaffNotification): Promise<void> {
  if (!item.readAt) {
    await userExperienceApi.markRead(item.id)
    item.readAt = new Date().toISOString()
    unread.value = Math.max(0, unread.value - 1)
  }
  open.value = false
  await router.push(item.route)
}

async function markAllRead(): Promise<void> {
  await userExperienceApi.markAllRead()
  const readAt = new Date().toISOString()
  items.value = items.value.map((item) => ({ ...item, readAt: item.readAt ?? readAt }))
  unread.value = 0
}

onMounted(() => {
  void refresh()
  timer = window.setInterval(refresh, 60_000)
})
onBeforeUnmount(() => window.clearInterval(timer))
</script>

<template>
  <v-menu :model-value="open" location="bottom end" :close-on-content-click="false" @update:model-value="onOpen">
    <template #activator="{ props }">
      <v-badge :content="unread" :model-value="unread > 0" color="error" offset-x="8" offset-y="8">
        <v-btn icon="mdi-bell-outline" variant="text" v-bind="props" :aria-label="t('layout.notifications')" />
      </v-badge>
    </template>
    <v-card width="min(92vw, 380px)" max-height="min(70vh, 560px)" class="notification-card">
      <div class="d-flex align-center px-4 py-3 ga-2">
        <span class="text-subtitle-2 font-weight-bold flex-grow-1">{{ t('layout.notifications') }}</span>
        <v-btn v-if="unread" size="small" variant="text" @click="markAllRead">{{ t('notifications.markAllRead') }}</v-btn>
      </div>
      <v-divider />
      <v-skeleton-loader v-if="loading" type="list-item-two-line@3" />
      <div v-else-if="!items.length" class="pa-6 text-center">
        <v-icon icon="mdi-bell-sleep-outline" size="36" class="mb-2 text-disabled" />
        <div class="text-body-2 text-medium-emphasis">{{ t('layout.noNotifications') }}</div>
      </div>
      <v-list v-else lines="three" class="notification-list pa-2">
        <v-list-item
          v-for="item in items"
          :key="item.id"
          :class="{ 'notification-list__item--unread': !item.readAt }"
          rounded="lg"
          prepend-icon="mdi-emoticon-happy-outline"
          @click="openNotification(item)"
        >
          <v-list-item-title class="font-weight-medium">{{ label(item.titleKey) }}</v-list-item-title>
          <v-list-item-subtitle>{{ label(item.messageKey) }}</v-list-item-subtitle>
          <template #append>
            <span class="text-caption text-medium-emphasis">{{ formatDateTime(item.createdAt) }}</span>
          </template>
        </v-list-item>
      </v-list>
    </v-card>
  </v-menu>
</template>

<style scoped>
.notification-card { overflow: hidden; }
.notification-list { max-height: 480px; overflow-y: auto; }
.notification-list__item--unread { background: rgba(var(--v-theme-primary), 0.08); }
.notification-list :deep(.v-list-item__append) { align-self: start; margin-inline-start: 8px; }
</style>
