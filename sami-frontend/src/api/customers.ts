import type { ApiResponse, PageQuery, PageResponse } from '@/types/api'
import type {
  BlacklistEntry,
  Customer,
  CustomerDetail,
  CustomerEvent,
  CustomerFilterQuery,
  CustomerImportResult,
  CustomerNote,
  CustomerNotePayload,
  CustomerPayload,
  CustomerRelation,
  CustomerRelationPayload,
  DuplicateMatch,
} from '@/types/models'
import { http, unwrap } from './http'

export type CustomerListParams = PageQuery & Omit<CustomerFilterQuery, 'tagIds'> & {
  tagIds?: number[]
}

/** Customer 360° API: CRUD, lifecycle, merge, timeline, notes, avatar, export. */
export const customersApi = {
  list: (params: CustomerListParams = {}): Promise<PageResponse<Customer>> =>
    unwrap(http.get<ApiResponse<PageResponse<Customer>>>('/v1/customers', { params })),

  get: (id: number): Promise<CustomerDetail> =>
    unwrap(http.get<ApiResponse<CustomerDetail>>(`/v1/customers/${id}`)),

  create: (payload: CustomerPayload): Promise<CustomerDetail> =>
    unwrap(http.post<ApiResponse<CustomerDetail>>('/v1/customers', payload)),

  update: (id: number, payload: CustomerPayload): Promise<CustomerDetail> =>
    unwrap(http.put<ApiResponse<CustomerDetail>>(`/v1/customers/${id}`, payload)),

  checkDuplicates: (payload: CustomerPayload, excludeId?: number): Promise<DuplicateMatch[]> =>
    unwrap(
      http.post<ApiResponse<DuplicateMatch[]>>('/v1/customers/duplicates/check', payload, {
        params: { excludeId },
      }),
    ),

  merge: (sourceId: number, targetId: number): Promise<CustomerDetail> =>
    unwrap(
      http.post<ApiResponse<CustomerDetail>>(`/v1/customers/${sourceId}/merge-into/${targetId}`),
    ),

  archive: (id: number): Promise<Customer> =>
    unwrap(http.post<ApiResponse<Customer>>(`/v1/customers/${id}/archive`)),

  restore: (id: number): Promise<Customer> =>
    unwrap(http.post<ApiResponse<Customer>>(`/v1/customers/${id}/restore`)),

  softDelete: (id: number): Promise<Customer> =>
    unwrap(http.delete<ApiResponse<Customer>>(`/v1/customers/${id}`)),

  purge: (id: number): Promise<void> =>
    http.delete(`/v1/customers/${id}/permanent`).then(() => undefined),

  timeline: (
    id: number,
    params: PageQuery & { eventType?: string } = {},
  ): Promise<PageResponse<CustomerEvent>> =>
    unwrap(
      http.get<ApiResponse<PageResponse<CustomerEvent>>>(`/v1/customers/${id}/timeline`, {
        params,
      }),
    ),

  notes: (id: number, params: PageQuery = {}): Promise<PageResponse<CustomerNote>> =>
    unwrap(http.get<ApiResponse<PageResponse<CustomerNote>>>(`/v1/customers/${id}/notes`, { params })),

  addNote: (id: number, payload: CustomerNotePayload): Promise<CustomerNote> =>
    unwrap(http.post<ApiResponse<CustomerNote>>(`/v1/customers/${id}/notes`, payload)),

  deleteNote: (id: number, noteId: number): Promise<void> =>
    http.delete(`/v1/customers/${id}/notes/${noteId}`).then(() => undefined),

  exportCsv: (filter: CustomerFilterQuery = {}): Promise<string> =>
    http
      .get<string>('/v1/customers/export', { params: filter, responseType: 'text' })
      .then((r) => r.data),

  uploadAvatar: (id: number, file: File): Promise<void> => {
    const form = new FormData()
    form.append('file', file)
    return http
      .put(`/v1/customers/${id}/avatar`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then(() => undefined)
  },

  avatarBlob: (id: number): Promise<Blob | null> =>
    http
      .get<Blob>(`/v1/customers/${id}/avatar`, { responseType: 'blob' })
      .then((r) => r.data)
      .catch(() => null),

  // --- Relations -------------------------------------------------------------

  relations: (id: number): Promise<CustomerRelation[]> =>
    unwrap(http.get<ApiResponse<CustomerRelation[]>>(`/v1/customers/${id}/relations`)),

  addRelation: (id: number, payload: CustomerRelationPayload): Promise<CustomerRelation> =>
    unwrap(http.post<ApiResponse<CustomerRelation>>(`/v1/customers/${id}/relations`, payload)),

  removeRelation: (id: number, relationId: number): Promise<void> =>
    http.delete(`/v1/customers/${id}/relations/${relationId}`).then(() => undefined),

  // --- Preferences -----------------------------------------------------------

  updatePreferences: (
    id: number,
    preferences: Record<string, unknown>,
  ): Promise<Record<string, unknown>> =>
    unwrap(
      http.put<ApiResponse<Record<string, unknown>>>(`/v1/customers/${id}/preferences`, {
        preferences,
      }),
    ),

  // --- Blacklist -------------------------------------------------------------

  blacklist: (id: number, payload: { reasonId: number; note?: string }): Promise<Customer> =>
    unwrap(http.post<ApiResponse<Customer>>(`/v1/customers/${id}/blacklist`, payload)),

  unblacklist: (id: number): Promise<Customer> =>
    unwrap(http.post<ApiResponse<Customer>>(`/v1/customers/${id}/unblacklist`)),

  blacklistHistory: (id: number): Promise<BlacklistEntry[]> =>
    unwrap(http.get<ApiResponse<BlacklistEntry[]>>(`/v1/customers/${id}/blacklist-history`)),

  // --- Import ----------------------------------------------------------------

  importCsv: (file: File): Promise<CustomerImportResult> => {
    const form = new FormData()
    form.append('file', file)
    return unwrap(
      http.post<ApiResponse<CustomerImportResult>>('/v1/customers/import', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      }),
    )
  },
}
