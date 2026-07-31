import type { PushNotificationPermission } from '@/types/pushNotifications'

export const notificationPermissionService = {
  current(): PushNotificationPermission {
    return typeof Notification === 'undefined' ? 'unsupported' : Notification.permission
  },

  async request(): Promise<PushNotificationPermission> {
    if (typeof Notification === 'undefined') return 'unsupported'
    if (Notification.permission !== 'default') return Notification.permission
    return Notification.requestPermission()
  },
}
