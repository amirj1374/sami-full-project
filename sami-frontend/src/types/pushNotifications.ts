export type PushNotificationPermission = NotificationPermission | 'unsupported'

export interface NotificationAction {
  action: string
  title: string
  icon?: string
}

export interface PushNotificationPayload {
  title: string
  body?: string
  icon?: string
  badge?: string
  url?: string
  type?: string
  data?: Record<string, unknown>
  actions?: NotificationAction[]
}

export interface PushSubscriptionKeys {
  p256dh: string
  auth: string
}

export interface PushSubscriptionModel {
  endpoint: string
  expirationTime: number | null
  keys: PushSubscriptionKeys
}

export interface PushNotificationCapabilities {
  supported: boolean
  secureContext: boolean
  serviceWorker: boolean
  pushManager: boolean
  notifications: boolean
  installed: boolean
}

export interface PushNotificationPreferences {
  enabled: boolean
}

export const PUSH_NOTIFICATION_CLICK_MESSAGE = 'SAMI_PUSH_NOTIFICATION_CLICK' as const
export const PUSH_SUBSCRIPTION_CHANGED_MESSAGE = 'SAMI_PUSH_SUBSCRIPTION_CHANGED' as const

export interface PushNotificationClickMessage {
  type: typeof PUSH_NOTIFICATION_CLICK_MESSAGE
  url: string
  action?: string
}
