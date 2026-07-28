import type { ApiResponse, PageQuery, PageResponse } from '@/types/api'
import type {
  SupCategory,
  SupDocumentType,
  SupPaymentTerm,
  SupRatingCriterion,
  SupStatus,
  SupTag,
  SupType,
  SupplierDetail,
  SupplierDocument,
  SupplierFilterQuery,
  SupplierLogEntry,
  SupplierPayload,
  SupplierRating,
  SupplierRow,
} from '@/types/models'
import { http, unwrap } from './http'

export type SupplierListParams = PageQuery & SupplierFilterQuery

/** Supplier registry API: profiles, lifecycle, ratings, documents, history. */
export const suppliersApi = {
  list: (params: SupplierListParams = {}): Promise<PageResponse<SupplierRow>> =>
    unwrap(http.get<ApiResponse<PageResponse<SupplierRow>>>('/v1/suppliers', { params })),

  get: (id: number): Promise<SupplierDetail> =>
    unwrap(http.get<ApiResponse<SupplierDetail>>(`/v1/suppliers/${id}`)),

  create: (payload: SupplierPayload): Promise<SupplierDetail> =>
    unwrap(http.post<ApiResponse<SupplierDetail>>('/v1/suppliers', payload)),

  update: (id: number, payload: SupplierPayload): Promise<SupplierDetail> =>
    unwrap(http.put<ApiResponse<SupplierDetail>>(`/v1/suppliers/${id}`, payload)),

  archive: (id: number): Promise<SupplierRow> =>
    unwrap(http.post<ApiResponse<SupplierRow>>(`/v1/suppliers/${id}/archive`)),

  restore: (id: number): Promise<SupplierRow> =>
    unwrap(http.post<ApiResponse<SupplierRow>>(`/v1/suppliers/${id}/restore`)),

  softDelete: (id: number): Promise<SupplierRow> =>
    unwrap(http.delete<ApiResponse<SupplierRow>>(`/v1/suppliers/${id}`)),

  purge: (id: number): Promise<void> =>
    http.delete(`/v1/suppliers/${id}/permanent`).then(() => undefined),

  ratings: (id: number): Promise<SupplierRating[]> =>
    unwrap(http.get<ApiResponse<SupplierRating[]>>(`/v1/suppliers/${id}/ratings`)),

  rate: (
    id: number,
    scores: { criterionId: number; score: number; note?: string }[],
  ): Promise<SupplierRating[]> =>
    unwrap(http.put<ApiResponse<SupplierRating[]>>(`/v1/suppliers/${id}/ratings`, { scores })),

  documents: (id: number): Promise<SupplierDocument[]> =>
    unwrap(http.get<ApiResponse<SupplierDocument[]>>(`/v1/suppliers/${id}/documents`)),

  uploadDocument: (id: number, file: File, docTypeId?: number): Promise<SupplierDocument> => {
    const form = new FormData()
    form.append('file', file)
    return unwrap(
      http.post<ApiResponse<SupplierDocument>>(`/v1/suppliers/${id}/documents`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        params: { docTypeId },
      }),
    )
  },

  downloadDocument: (id: number, documentId: number): Promise<Blob | null> =>
    http
      .get<Blob>(`/v1/suppliers/${id}/documents/${documentId}`, { responseType: 'blob' })
      .then((r) => r.data)
      .catch(() => null),

  deleteDocument: (id: number, documentId: number): Promise<void> =>
    http.delete(`/v1/suppliers/${id}/documents/${documentId}`).then(() => undefined),

  logs: (id: number, params: PageQuery = {}): Promise<PageResponse<SupplierLogEntry>> =>
    unwrap(http.get<ApiResponse<PageResponse<SupplierLogEntry>>>(`/v1/suppliers/${id}/logs`, { params })),

  exportCsv: (filter: SupplierFilterQuery = {}): Promise<string> =>
    http
      .get<string>('/v1/suppliers/export', { params: filter, responseType: 'text' })
      .then((r) => r.data),
}

/** Supplier configuration lookups. */
export const supplierConfigApi = {
  types: (): Promise<SupType[]> =>
    unwrap(http.get<ApiResponse<SupType[]>>('/v1/supplier-config/types')),

  statuses: (): Promise<SupStatus[]> =>
    unwrap(http.get<ApiResponse<SupStatus[]>>('/v1/supplier-config/statuses')),

  categories: (): Promise<SupCategory[]> =>
    unwrap(http.get<ApiResponse<SupCategory[]>>('/v1/supplier-config/categories')),

  tags: (): Promise<SupTag[]> =>
    unwrap(http.get<ApiResponse<SupTag[]>>('/v1/supplier-config/tags')),

  paymentTerms: (): Promise<SupPaymentTerm[]> =>
    unwrap(http.get<ApiResponse<SupPaymentTerm[]>>('/v1/supplier-config/payment-terms')),

  ratingCriteria: (): Promise<SupRatingCriterion[]> =>
    unwrap(http.get<ApiResponse<SupRatingCriterion[]>>('/v1/supplier-config/rating-criteria')),

  documentTypes: (): Promise<SupDocumentType[]> =>
    unwrap(http.get<ApiResponse<SupDocumentType[]>>('/v1/supplier-config/document-types')),
}
