import type { ApiResponse, PageQuery, PageResponse } from '@/types/api'
import type {
  CreateRolePayload,
  DuplicateRolePayload,
  Role,
  UpdateRolePayload,
  UpdateRolePermissionsPayload,
} from '@/types/models'
import { http, unwrap, unwrapVoid } from './http'

/** Role administration API calls. */
export const rolesApi = {
  list: (params: PageQuery = {}): Promise<PageResponse<Role>> =>
    unwrap(http.get<ApiResponse<PageResponse<Role>>>('/v1/roles', { params })),

  get: (id: number): Promise<Role> => unwrap(http.get<ApiResponse<Role>>(`/v1/roles/${id}`)),

  create: (payload: CreateRolePayload): Promise<Role> =>
    unwrap(http.post<ApiResponse<Role>>('/v1/roles', payload)),

  update: (id: number, payload: UpdateRolePayload): Promise<Role> =>
    unwrap(http.put<ApiResponse<Role>>(`/v1/roles/${id}`, payload)),

  remove: (id: number): Promise<void> => http.delete(`/v1/roles/${id}`).then(() => undefined),

  /** Copies the role's permission set; the copy is never system/super/default. */
  duplicate: (id: number, payload: DuplicateRolePayload): Promise<Role> =>
    unwrap(http.post<ApiResponse<Role>>(`/v1/roles/${id}/duplicate`, payload)),

  /** Full replacement of the role's permission set. */
  updatePermissions: (id: number, payload: UpdateRolePermissionsPayload): Promise<void> =>
    unwrapVoid(http.put<ApiResponse<unknown>>(`/v1/roles/${id}/permissions`, payload)),
}
