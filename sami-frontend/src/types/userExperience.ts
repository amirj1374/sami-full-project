export interface UserExperiencePreferences {
  mobileNavigationCodes: string[]
  demoNotificationsEnabled: boolean
  keyboardShortcutsEnabled: boolean
  mobileNavigationConfigured: boolean
  version: number
}

export interface UserExperiencePreferencePayload {
  mobileNavigationCodes: string[]
  demoNotificationsEnabled: boolean
  keyboardShortcutsEnabled: boolean
}

export interface StaffNotification {
  id: number
  type: string
  titleKey: string
  messageKey: string
  route: string
  readAt: string | null
  createdAt: string
}
