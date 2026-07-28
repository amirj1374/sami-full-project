import { computed, ref } from 'vue'
import { useTheme } from 'vuetify'

/**
 * Theme mode controller: light / dark / system, persisted to localStorage and
 * applied through Vuetify's theme. `system` follows the OS `prefers-color-scheme`
 * and reacts to changes live. Call `apply(mode.value)` once at startup (App.vue)
 * so the persisted choice is applied.
 */
export type ThemeMode = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'sami.theme'
const mode = ref<ThemeMode>(readStored())
const media = typeof window !== 'undefined' ? window.matchMedia('(prefers-color-scheme: dark)') : null
let systemListenerBound = false

function readStored(): ThemeMode {
  const v = typeof localStorage !== 'undefined' ? localStorage.getItem(STORAGE_KEY) : null
  return v === 'light' || v === 'dark' || v === 'system' ? v : 'light'
}

function resolve(m: ThemeMode): 'light' | 'dark' {
  if (m === 'system') return media?.matches ? 'dark' : 'light'
  return m
}

export function useThemeMode() {
  const theme = useTheme()

  function apply(m: ThemeMode): void {
    mode.value = m
    localStorage.setItem(STORAGE_KEY, m)
    theme.change(resolve(m))
  }

  // Keep `system` in sync with OS changes (registered once per composable use;
  // guarded so it is a no-op unless the current mode actually follows the OS).
  if (media && !systemListenerBound) {
    systemListenerBound = true
    media.addEventListener('change', () => {
      if (mode.value === 'system') theme.change(resolve('system'))
    })
  }

  const isDark = computed(() => theme.global.current.value.dark)

  return { mode: computed(() => mode.value), isDark, apply }
}
