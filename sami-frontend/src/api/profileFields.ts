import type { ApiResponse } from '@/types/api'
import type { ProfileFieldDefinition, ProfileFieldPayload } from '@/types/models'
import { http, unwrap } from './http'

/** Custom profile field definition API calls. */
export const profileFieldsApi = {
  list: (activeOnly = false): Promise<ProfileFieldDefinition[]> =>
    unwrap(
      http.get<ApiResponse<ProfileFieldDefinition[]>>('/v1/profile-fields', {
        params: { activeOnly },
      }),
    ),

  create: (payload: ProfileFieldPayload): Promise<ProfileFieldDefinition> =>
    unwrap(http.post<ApiResponse<ProfileFieldDefinition>>('/v1/profile-fields', payload)),

  update: (id: number, payload: ProfileFieldPayload): Promise<ProfileFieldDefinition> =>
    unwrap(http.put<ApiResponse<ProfileFieldDefinition>>(`/v1/profile-fields/${id}`, payload)),

  remove: (id: number): Promise<void> =>
    http.delete(`/v1/profile-fields/${id}`).then(() => undefined),
}
