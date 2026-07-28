import type { ApiResponse, PageQuery, PageResponse } from '@/types/api'
import type {
  CreatePermissionPayload,
  ModulePermissionsGroup,
  Permission,
  PermissionFilter,
  UpdatePermissionPayload,
} from '@/types/models'
import { http, unwrap } from './http'

export type PermissionListParams = PageQuery & PermissionFilter

/** Permission administration API calls. */
export const permissionsApi = {
  list: (params: PermissionListParams = {}): Promise<PageResponse<Permission>> =>
    unwrap(http.get<ApiResponse<PageResponse<Permission>>>('/v1/permissions', { params })),

  /**
   * All permissions grouped per module (including disabled modules), ordered by
   * display order — the role permission matrix must show everything.
   */
  grouped: (): Promise<ModulePermissionsGroup[]> =>
    unwrap(http.get<ApiResponse<ModulePermissionsGroup[]>>('/v1/permissions/grouped')),

  create: (payload: CreatePermissionPayload): Promise<Permission> =>
    unwrap(http.post<ApiResponse<Permission>>('/v1/permissions', payload)),

  /** Only name/description are editable; action and code are immutable. */
  update: (id: number, payload: UpdatePermissionPayload): Promise<Permission> =>
    unwrap(http.put<ApiResponse<Permission>>(`/v1/permissions/${id}`, payload)),

  remove: (id: number): Promise<void> => http.delete(`/v1/permissions/${id}`).then(() => undefined),
}
