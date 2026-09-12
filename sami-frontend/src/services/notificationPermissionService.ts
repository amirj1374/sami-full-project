import type { PushNotificationPermission } from '@/types/pushNotifications'

export const notificationPermissionService = {
  current(): PushNotificationPermission {
    if (typeof Notification === 'undefined') return 'unsupported'
    if (typeof window !== 'undefined' && !window.isSecureContext) return 'unsupported'
    return Notification.permission
  },

  async request(): Promise<PushNotificationPermission> {
    if (typeof Notification === 'undefined') return 'unsupported'
    if (typeof window !== 'undefined' && !window.isSecureContext) return 'unsupported'
    if (Notification.permission !== 'default') return Notification.permission
    return Notification.requestPermission()
  },
}
