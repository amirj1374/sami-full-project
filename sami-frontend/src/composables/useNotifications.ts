import { ref } from 'vue'
import { i18n } from '@/i18n'

export type NotificationKind = 'success' | 'error' | 'warning' | 'info' | 'loading'
export interface AppNotification {
  id: number
  message: string
  kind: NotificationKind
  actionLabel?: string
  action?: () => void
  timeout?: number
}

const queue = ref<AppNotification[]>([])
let nextId = 1

export function useNotifications() {
  function show(message: string, kind: NotificationKind = 'info', options: Partial<Omit<AppNotification, 'id' | 'message' | 'kind'>> = {}) {
    const notification = { id: nextId++, message, kind, ...options }
    queue.value.push(notification)
    return notification.id
  }
  function dismiss(id: number) {
    queue.value = queue.value.filter((item) => item.id !== id)
  }
  return {
    queue,
    show,
    dismiss,
    success: (message: string) => show(message, 'success'),
    error: (message: string, action?: () => void) => show(message, 'error', action ? { action, actionLabel: i18n.global.t('common.retry'), timeout: 8000 } : {}),
  }
}
