import { computed, readonly, ref } from 'vue'
import { notificationPermissionService } from '@/services/notificationPermissionService'
import { pushNotificationService } from '@/services/pushNotificationService'
import type {
  PushNotificationPreferences,
  PushSubscriptionModel,
} from '@/types/pushNotifications'

const capabilities = ref(pushNotificationService.capabilities())
const permission = ref(notificationPermissionService.current())
const subscription = ref<PushSubscriptionModel | null>(null)
const preferences = ref<PushNotificationPreferences>(pushNotificationService.preferences())
const busy = ref(false)
const error = ref<string | null>(null)
let initialized = false

export function usePushNotifications() {
  function initialize(): void {
    if (initialized || typeof window === 'undefined') return
    initialized = true
    window.addEventListener('sami:push-subscription-changed', () => void refresh())
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') void refresh()
    })
    void refresh()
  }

  async function refresh(): Promise<void> {
    capabilities.value = pushNotificationService.capabilities()
    permission.value = notificationPermissionService.current()
    if (!capabilities.value.supported) {
      subscription.value = null
      return
    }
    const current = await pushNotificationService.currentSubscription()
    subscription.value = current ? pushNotificationService.model(current) : null
  }

  async function requestPermission(): Promise<NotificationPermission | 'unsupported'> {
    permission.value = await notificationPermissionService.request()
    return permission.value
  }

  async function enable(applicationServerKey: string): Promise<PushSubscriptionModel | null> {
    busy.value = true
    error.value = null
    try {
      if (await requestPermission() !== 'granted') return null
      subscription.value = await pushNotificationService.subscribe(applicationServerKey)
      preferences.value = { enabled: true }
      pushNotificationService.savePreferences(preferences.value)
      return subscription.value
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : String(cause)
      throw cause
    } finally {
      busy.value = false
    }
  }

  async function disable(): Promise<boolean> {
    busy.value = true
    error.value = null
    try {
      const removed = await pushNotificationService.unsubscribe()
      if (removed) {
        subscription.value = null
        preferences.value = { enabled: false }
        pushNotificationService.savePreferences(preferences.value)
      }
      return removed
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : String(cause)
      throw cause
    } finally {
      busy.value = false
    }
  }

  return {
    capabilities: readonly(capabilities),
    permission: readonly(permission),
    subscription: readonly(subscription),
    preferences: readonly(preferences),
    busy: readonly(busy),
    error: readonly(error),
    supported: computed(() => capabilities.value.supported),
    subscribed: computed(() => subscription.value !== null),
    initialize,
    refresh,
    requestPermission,
    enable,
    disable,
  }
}
