import type {
  PushNotificationCapabilities,
  PushNotificationPreferences,
  PushSubscriptionModel,
} from '@/types/pushNotifications'

const PREFERENCES_KEY = 'sami.push.preferences.v1'
const DEFAULT_PREFERENCES: PushNotificationPreferences = { enabled: false }

function decodeApplicationServerKey(value: string): Uint8Array<ArrayBuffer> {
  const padding = '='.repeat((4 - value.length % 4) % 4)
  const base64 = (value + padding).replace(/-/g, '+').replace(/_/g, '/')
  const decoded = atob(base64)
  const bytes = new Uint8Array(new ArrayBuffer(decoded.length))
  for (let index = 0; index < decoded.length; index += 1) {
    bytes[index] = decoded.charCodeAt(index)
  }
  return bytes
}

function toModel(subscription: PushSubscription): PushSubscriptionModel {
  const json = subscription.toJSON()
  if (!json.endpoint || !json.keys?.p256dh || !json.keys.auth) {
    throw new Error('The browser returned an incomplete push subscription')
  }
  return {
    endpoint: json.endpoint,
    expirationTime: json.expirationTime ?? null,
    keys: {
      p256dh: json.keys.p256dh,
      auth: json.keys.auth,
    },
  }
}

export const pushNotificationService = {
  capabilities(): PushNotificationCapabilities {
    const browser = typeof window !== 'undefined' && typeof navigator !== 'undefined'
    const serviceWorker = browser && 'serviceWorker' in navigator
    const pushManager = browser && 'PushManager' in window
    const notifications = browser && 'Notification' in window
    const secureContext = browser && window.isSecureContext
    const installed = browser && (
      window.matchMedia('(display-mode: standalone)').matches
      || ('standalone' in navigator && (navigator as Navigator & { standalone?: boolean }).standalone === true)
    )
    return {
      supported: serviceWorker && pushManager && notifications && secureContext,
      secureContext,
      serviceWorker,
      pushManager,
      notifications,
      installed,
    }
  },

  async registration(): Promise<ServiceWorkerRegistration> {
    if (!this.capabilities().supported) {
      throw new Error('Push notifications are not supported in this browser context')
    }
    return navigator.serviceWorker.ready
  },

  async currentSubscription(): Promise<PushSubscription | null> {
    if (!this.capabilities().supported) return null
    return (await this.registration()).pushManager.getSubscription()
  },

  async subscribe(applicationServerKey: string): Promise<PushSubscriptionModel> {
    if (!applicationServerKey.trim()) {
      throw new Error('A VAPID public key is required')
    }
    const registration = await this.registration()
    const existing = await registration.pushManager.getSubscription()
    const subscription = existing ?? await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: decodeApplicationServerKey(applicationServerKey),
    })
    return toModel(subscription)
  },

  async unsubscribe(): Promise<boolean> {
    const subscription = await this.currentSubscription()
    return subscription ? subscription.unsubscribe() : true
  },

  model(subscription: PushSubscription): PushSubscriptionModel {
    return toModel(subscription)
  },

  preferences(): PushNotificationPreferences {
    try {
      const stored = localStorage.getItem(PREFERENCES_KEY)
      if (!stored) return { ...DEFAULT_PREFERENCES }
      const parsed = JSON.parse(stored) as Partial<PushNotificationPreferences>
      return { enabled: parsed.enabled === true }
    } catch {
      return { ...DEFAULT_PREFERENCES }
    }
  },

  savePreferences(preferences: PushNotificationPreferences): void {
    localStorage.setItem(PREFERENCES_KEY, JSON.stringify(preferences))
  },
}
