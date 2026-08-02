import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { userExperienceApi } from '@/api/userExperience'
import type { UserExperiencePreferencePayload, UserExperiencePreferences } from '@/types/userExperience'

const defaults = (): UserExperiencePreferences => ({
  mobileNavigationCodes: [],
  demoNotificationsEnabled: false,
  mobileNavigationConfigured: false,
  version: 0,
})

export const useUserExperienceStore = defineStore('userExperience', () => {
  const preferences = ref<UserExperiencePreferences>(defaults())
  const loaded = ref(false)
  const loading = ref(false)
  const saving = ref(false)
  const error = ref<string | null>(null)
  let loadPromise: Promise<void> | null = null

  async function load(force = false): Promise<void> {
    if (loaded.value && !force) return
    loadPromise ??= (async () => {
      loading.value = true
      error.value = null
      try {
        preferences.value = await userExperienceApi.preferences()
        loaded.value = true
      } catch (cause) {
        error.value = cause instanceof Error ? cause.message : String(cause)
        throw cause
      } finally {
        loading.value = false
      }
    })().finally(() => { loadPromise = null })
    return loadPromise
  }

  async function save(payload: UserExperiencePreferencePayload): Promise<void> {
    saving.value = true
    error.value = null
    try {
      preferences.value = await userExperienceApi.updatePreferences(payload)
      loaded.value = true
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : String(cause)
      throw cause
    } finally {
      saving.value = false
    }
  }

  function reset(): void {
    preferences.value = defaults()
    loaded.value = false
    error.value = null
  }

  return {
    preferences,
    loaded,
    loading,
    saving,
    error,
    mobileNavigationCodes: computed(() => preferences.value.mobileNavigationCodes),
    demoNotificationsEnabled: computed(() => preferences.value.demoNotificationsEnabled),
    load,
    save,
    reset,
  }
})
