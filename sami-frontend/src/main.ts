import { createApp } from 'vue'
import { createPinia } from 'pinia'
import '@fontsource-variable/vazirmatn'
import './styles/global.css'
import App from './App.vue'
import { router } from './router'
import { vuetify } from './plugins/vuetify'
import { i18n, getStoredLocale, setLocale } from './i18n'
import { registerAuthFailureHandler, registerAuthRefreshHandler } from './api/http'
import { permissionDirective } from './directives/permission'
import { useAuthStore } from './stores/auth'
import { MOCK_MODE } from './mocks/config'
import { tokenStorage } from './api/tokenStorage'
import { usePwa } from './composables/usePwa'
import { usePushNotifications } from './composables/usePushNotifications'
import {
  PUSH_NOTIFICATION_CLICK_MESSAGE,
  PUSH_SUBSCRIPTION_CHANGED_MESSAGE,
} from './types/pushNotifications'
import { exposeBuildInfo } from './buildInfo'
import AppPersianDatePicker from './components/AppPersianDatePicker.vue'

// Apply the persisted (or default Persian) locale, setting <html lang/dir> before mount.
setLocale(getStoredLocale())

const app = createApp(App)
const pinia = createPinia()

app.component('AppPersianDatePicker', AppPersianDatePicker)
app.use(pinia)
app.use(i18n)
app.use(router)
app.use(vuetify)

// `v-can="'users:create'"` / `v-can.disable`: declarative permission checks.
app.directive('can', permissionDirective)

// When a token refresh fails, log out and bounce to the login page.
registerAuthFailureHandler(() => {
  const auth = useAuthStore(pinia)
  void auth.logout()
  void router.push({ name: 'login' })
})

// A successful refresh returns a fresh profile; keep permissions current.
registerAuthRefreshHandler((user) => {
  const auth = useAuthStore(pinia)
  auth.setUser(user)
})

/**
 * Boot the app. In Mock Mode (VITE_ENABLE_MOCK_MODE=true) the MSW worker is
 * started first and a fake session is seeded, so the existing auth flow
 * authenticates automatically against the mock user with no backend. When the
 * flag is off, none of this runs and startup is byte-for-byte the original.
 */
async function bootstrap(): Promise<void> {
  exposeBuildInfo()
  if (MOCK_MODE) {
    const { startMockWorker } = await import('./mocks/browser')
    await startMockWorker()
    // Seed a fake token so the HTTP interceptor has one to attach. Auth itself
    // is handled offline by the store's initialize() mock branch (no request).
    tokenStorage.set('mock-access-token', 'mock-refresh-token')
  }
  app.mount('#app')
  const pwa = usePwa()
  pwa.initialize()
  usePushNotifications().initialize()
  if ('serviceWorker' in navigator && import.meta.env.PROD) {
    try {
      const registration = await navigator.serviceWorker.register('/service-worker.js', { scope: '/' })
      pwa.watchRegistration(registration)
      navigator.serviceWorker.addEventListener('controllerchange', () => window.location.reload(), { once: true })
      navigator.serviceWorker.addEventListener('message', (event: MessageEvent<unknown>) => {
        const message = event.data as { type?: unknown; url?: unknown } | null
        if (message?.type === PUSH_SUBSCRIPTION_CHANGED_MESSAGE) {
          window.dispatchEvent(new CustomEvent('sami:push-subscription-changed'))
          return
        }
        if (
          message?.type === PUSH_NOTIFICATION_CLICK_MESSAGE
          && typeof message.url === 'string'
          && message.url.startsWith('/')
          && !message.url.startsWith('//')
          && message.url !== '/api'
          && !message.url.startsWith('/api/')
        ) {
          void router.push(message.url)
        }
      })
    } catch {
      // The application remains fully functional when service-worker registration is unavailable.
    }
  }
}

void bootstrap()
