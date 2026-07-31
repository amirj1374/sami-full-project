import { computed, ref } from 'vue'

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>
}

const online = ref(typeof navigator === 'undefined' ? true : navigator.onLine)
const installEvent = ref<BeforeInstallPromptEvent | null>(null)
const updateRegistration = ref<ServiceWorkerRegistration | null>(null)
const installed = ref(typeof window !== 'undefined' && window.matchMedia('(display-mode: standalone)').matches)
let initialized = false

export function usePwa() {
  function initialize(): void {
    if (initialized || typeof window === 'undefined') return
    initialized = true
    window.addEventListener('online', () => { online.value = true })
    window.addEventListener('offline', () => { online.value = false })
    window.addEventListener('beforeinstallprompt', (event) => {
      event.preventDefault()
      installEvent.value = event as BeforeInstallPromptEvent
    })
    window.addEventListener('appinstalled', () => {
      installed.value = true
      installEvent.value = null
    })
  }

  function watchRegistration(registration: ServiceWorkerRegistration): void {
    if (registration.waiting) updateRegistration.value = registration
    registration.addEventListener('updatefound', () => {
      const worker = registration.installing
      worker?.addEventListener('statechange', () => {
        if (worker.state === 'installed' && navigator.serviceWorker.controller) {
          updateRegistration.value = registration
        }
      })
    })
  }

  async function install(): Promise<boolean> {
    if (!installEvent.value) return false
    await installEvent.value.prompt()
    const choice = await installEvent.value.userChoice
    installEvent.value = null
    return choice.outcome === 'accepted'
  }

  function applyUpdate(): void {
    updateRegistration.value?.waiting?.postMessage({ type: 'SKIP_WAITING' })
  }

  return {
    online: computed(() => online.value),
    canInstall: computed(() => !!installEvent.value && !installed.value),
    updateAvailable: computed(() => !!updateRegistration.value),
    installed: computed(() => installed.value),
    initialize,
    watchRegistration,
    install,
    applyUpdate,
  }
}
