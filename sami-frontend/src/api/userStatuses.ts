import type { ApiResponse } from '@/types/api'
import type { UserStatus, UserStatusPayload } from '@/types/models'
import { http, unwrap } from './http'

/** Configurable user status API calls. */
export const userStatusesApi = {
  list: (): Promise<UserStatus[]> =>
    unwrap(http.get<ApiResponse<UserStatus[]>>('/v1/user-statuses')),

  create: (payload: UserStatusPayload): Promise<UserStatus> =>
    unwrap(http.post<ApiResponse<UserStatus>>('/v1/user-statuses', payload)),

  update: (id: number, payload: UserStatusPayload): Promise<UserStatus> =>
    unwrap(http.put<ApiResponse<UserStatus>>(`/v1/user-statuses/${id}`, payload)),

  remove: (id: number): Promise<void> =>
    http.delete(`/v1/user-statuses/${id}`).then(() => undefined),
}
