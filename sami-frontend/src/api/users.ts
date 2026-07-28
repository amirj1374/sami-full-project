import type { ApiResponse, PageQuery, PageResponse } from '@/types/api'
import type {
  AdminUser,
  BulkResult,
  BulkStatusPayload,
  BulkUserPayload,
  CreateUserPayload,
  Role,
  UpdateUserPayload,
  UpdateUserStatusPayload,
  UserAuditEntry,
  UserDetail,
  UserFilter,
} from '@/types/models'
import { http, unwrap } from './http'

export type UserListParams = PageQuery & UserFilter

/** Compact role entry for form/filter dropdowns. */
export interface RoleOption {
  id: number
  name: string
}

/** User administration API calls (lifecycle, bulk operations, avatar, audit). */
export const usersApi = {
  list: (params: UserListParams = {}): Promise<PageResponse<AdminUser>> =>
    unwrap(http.get<ApiResponse<PageResponse<AdminUser>>>('/v1/users', { params })),

  get: (id: number): Promise<UserDetail> =>
    unwrap(http.get<ApiResponse<UserDetail>>(`/v1/users/${id}`)),

  create: (payload: CreateUserPayload): Promise<UserDetail> =>
    unwrap(http.post<ApiResponse<UserDetail>>('/v1/users', payload)),

  update: (id: number, payload: UpdateUserPayload): Promise<UserDetail> =>
    unwrap(http.put<ApiResponse<UserDetail>>(`/v1/users/${id}`, payload)),

  changeStatus: (id: number, payload: UpdateUserStatusPayload): Promise<AdminUser> =>
    unwrap(http.patch<ApiResponse<AdminUser>>(`/v1/users/${id}/status`, payload)),

  archive: (id: number): Promise<AdminUser> =>
    unwrap(http.post<ApiResponse<AdminUser>>(`/v1/users/${id}/archive`)),

  restore: (id: number): Promise<AdminUser> =>
    unwrap(http.post<ApiResponse<AdminUser>>(`/v1/users/${id}/restore`)),

  /** Soft delete: the record is preserved and restorable. */
  softDelete: (id: number): Promise<AdminUser> =>
    unwrap(http.delete<ApiResponse<AdminUser>>(`/v1/users/${id}`)),

  /** Permanent, unrecoverable removal (separately permissioned). */
  purge: (id: number): Promise<void> =>
    http.delete(`/v1/users/${id}/permanent`).then(() => undefined),

  bulkStatus: (payload: BulkStatusPayload): Promise<BulkResult> =>
    unwrap(http.post<ApiResponse<BulkResult>>('/v1/users/bulk/status', payload)),

  bulkArchive: (payload: BulkUserPayload): Promise<BulkResult> =>
    unwrap(http.post<ApiResponse<BulkResult>>('/v1/users/bulk/archive', payload)),

  bulkRestore: (payload: BulkUserPayload): Promise<BulkResult> =>
    unwrap(http.post<ApiResponse<BulkResult>>('/v1/users/bulk/restore', payload)),

  audit: (id: number, params: PageQuery = {}): Promise<PageResponse<UserAuditEntry>> =>
    unwrap(http.get<ApiResponse<PageResponse<UserAuditEntry>>>(`/v1/users/${id}/audit`, { params })),

  /** CSV export honoring the given filters; returns the raw CSV text. */
  exportCsv: (filter: UserFilter = {}): Promise<string> =>
    http.get<string>('/v1/users/export', { params: filter, responseType: 'text' }).then((r) => r.data),

  uploadAvatar: (id: number, file: File): Promise<void> => {
    const form = new FormData()
    form.append('file', file)
    return http
      .put(`/v1/users/${id}/avatar`, form, { headers: { 'Content-Type': 'multipart/form-data' } })
      .then(() => undefined)
  },

  /**
   * Avatar bytes as a Blob (null when the user has none). Fetched through the
   * authenticated HTTP client because plain <img> tags cannot send the Bearer
   * token; callers turn it into an object URL.
   */
  avatarBlob: (id: number): Promise<Blob | null> =>
    http
      .get<Blob>(`/v1/users/${id}/avatar`, { responseType: 'blob' })
      .then((r) => r.data)
      .catch(() => null),

  removeAvatar: (id: number): Promise<void> =>
    http.delete(`/v1/users/${id}/avatar`).then(() => undefined),

  /**
   * Roles for the user screen's dropdowns, fetched as one large page (the role
   * list is small). Kept self-contained so this module does not depend on the
   * roles administration API.
   */
  roleOptions: (): Promise<RoleOption[]> =>
    unwrap(http.get<ApiResponse<PageResponse<Role>>>('/v1/roles', { params: { size: 200 } })).then(
      (page) => page.content.map((role) => ({ id: role.id, name: role.name })),
    ),
}
