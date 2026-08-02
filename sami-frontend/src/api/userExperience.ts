import type { ApiResponse } from '@/types/api'
import type {
  StaffNotification,
  UserExperiencePreferencePayload,
  UserExperiencePreferences,
} from '@/types/userExperience'
import { http, unwrap } from './http'

export const userExperienceApi = {
  preferences: (): Promise<UserExperiencePreferences> =>
    unwrap(http.get<ApiResponse<UserExperiencePreferences>>('/v1/users/me/preferences')),

  updatePreferences: (payload: UserExperiencePreferencePayload): Promise<UserExperiencePreferences> =>
    unwrap(http.put<ApiResponse<UserExperiencePreferences>>('/v1/users/me/preferences', payload)),

  notifications: (): Promise<StaffNotification[]> =>
    unwrap(http.get<ApiResponse<StaffNotification[]>>('/v1/notifications')),

  unreadCount: (): Promise<number> =>
    unwrap(http.get<ApiResponse<{ count: number }>>('/v1/notifications/unread-count')).then((value) => value.count),

  markRead: (id: number): Promise<StaffNotification> =>
    unwrap(http.patch<ApiResponse<StaffNotification>>(`/v1/notifications/${id}/read`)),

  markAllRead: (): Promise<void> =>
    http.patch('/v1/notifications/read-all').then(() => undefined),
}
