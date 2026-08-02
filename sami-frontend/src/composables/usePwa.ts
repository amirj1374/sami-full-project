import { computed, ref } from 'vue'

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>
}

const online = ref(typeof navigator === 'undefined' ? true : navigator.onLine)
const installEvent = ref<BeforeInstallPromptEvent | null>(null)
const updateRegistration = ref<ServiceWorkerRegistration | null>(null)
const installed = ref(typeof window !== 'undefined' && (
  window.matchMedia('(display-mode: standalone)').matches
  || ('standalone' in navigator && (navigator as Navigator & { standalone?: boolean }).standalone === true)
))
const installBusy = ref(false)
const installError = ref<string | null>(null)
const BANNER_DISMISSED_KEY = 'sami.pwa.install-banner-dismissed.v1'
const bannerDismissed = ref(typeof localStorage !== 'undefined' && localStorage.getItem(BANNER_DISMISSED_KEY) === '1')
let initialized = false

export type PwaInstallInstruction = 'ios-safari' | 'browser-menu' | 'unsupported'

function isIos(): boolean {
  if (typeof navigator === 'undefined') return false
  return /iphone|ipad|ipod/i.test(navigator.userAgent)
}

function isSafari(): boolean {
  if (typeof navigator === 'undefined') return false
  return /safari/i.test(navigator.userAgent) && !/crios|fxios|edgios|chrome|android/i.test(navigator.userAgent)
}

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

  async function promptInstall(): Promise<boolean> {
    if (!installEvent.value) return false
    installBusy.value = true
    installError.value = null
    try {
      await installEvent.value.prompt()
      const choice = await installEvent.value.userChoice
      installEvent.value = null
      return choice.outcome === 'accepted'
    } catch (cause) {
      installError.value = cause instanceof Error ? cause.message : String(cause)
      return false
    } finally {
      installBusy.value = false
    }
  }

  function getInstallInstructions(): PwaInstallInstruction {
    if (isIos() && isSafari()) return 'ios-safari'
    if (typeof window !== 'undefined' && window.isSecureContext) return 'browser-menu'
    return 'unsupported'
  }

  function dismissInstallBanner(): void {
    bannerDismissed.value = true
    localStorage.setItem(BANNER_DISMISSED_KEY, '1')
  }

  function applyUpdate(): void {
    updateRegistration.value?.waiting?.postMessage({ type: 'SKIP_WAITING' })
  }

  return {
    online: computed(() => online.value),
    isInstallSupported: computed(() => !installed.value && (
      !!installEvent.value || getInstallInstructions() !== 'unsupported'
    )),
    canPromptInstall: computed(() => !!installEvent.value && !installed.value),
    canInstall: computed(() => !!installEvent.value && !installed.value),
    updateAvailable: computed(() => !!updateRegistration.value),
    isInstalled: computed(() => installed.value),
    installed: computed(() => installed.value),
    installBusy: computed(() => installBusy.value),
    installError: computed(() => installError.value),
    showInstallBanner: computed(() => !!installEvent.value && !installed.value && !bannerDismissed.value),
    initialize,
    watchRegistration,
    install: promptInstall,
    promptInstall,
    getInstallInstructions,
    dismissInstallBanner,
    applyUpdate,
  }
}
