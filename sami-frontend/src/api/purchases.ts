import type { ApiResponse, PageQuery, PageResponse } from '@/types/api'
import type {
  PurApprovalRule,
  PurApprovalRulePayload,
  PurCancelReason,
  PurCancelReasonPayload,
  PurIdentifierType,
  PurIdentifierTypePayload,
  PurStatus,
  PurType,
  PurTypePayload,
  PurWarehouse,
  PurchaseAttachment,
  PurchaseDashboard,
  PurchaseDetail,
  PurchaseFilterQuery,
  PurchaseImportResult,
  PurchaseLogEntry,
  PurchasePayload,
  PurchaseReceipt,
  PurchaseReport,
  PurchaseReturn,
  PurchaseRow,
  PurchaseSettlementPayload,
  ReceivePayload,
  ReturnPayload,
} from '@/types/models'
import { http, unwrap } from './http'

export type PurchaseListParams = PageQuery & PurchaseFilterQuery

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}

/** Purchasing API: documents, workflow, receiving, returns, attachments. */
export const purchasesApi = {
  list: (params: PurchaseListParams = {}): Promise<PageResponse<PurchaseRow>> =>
    unwrap(http.get<ApiResponse<PageResponse<PurchaseRow>>>('/v1/purchases', { params })),

  dashboard: (): Promise<PurchaseDashboard> =>
    unwrap(http.get<ApiResponse<PurchaseDashboard>>('/v1/purchases/dashboard')),

  report: (params: PurchaseFilterQuery = {}): Promise<PurchaseReport> =>
    unwrap(http.get<ApiResponse<PurchaseReport>>('/v1/purchases/reports', { params })),

  exportCsv: async (params: PurchaseFilterQuery = {}): Promise<void> => {
    const response = await http.get<Blob>('/v1/purchases/export.csv', {
      params,
      responseType: 'blob',
    })
    downloadBlob(response.data, 'purchases.csv')
  },

  importCsv: async (file: File): Promise<PurchaseImportResult> => {
    const form = new FormData()
    form.append('file', file)
    return unwrap(http.post<ApiResponse<PurchaseImportResult>>('/v1/purchases/import', form))
  },

  get: (id: number): Promise<PurchaseDetail> =>
    unwrap(http.get<ApiResponse<PurchaseDetail>>(`/v1/purchases/${id}`)),

  create: (payload: PurchasePayload): Promise<PurchaseDetail> =>
    unwrap(http.post<ApiResponse<PurchaseDetail>>('/v1/purchases', payload)),

  update: (id: number, payload: PurchasePayload): Promise<PurchaseDetail> =>
    unwrap(http.put<ApiResponse<PurchaseDetail>>(`/v1/purchases/${id}`, payload)),

  deleteDraft: (id: number): Promise<void> =>
    http.delete(`/v1/purchases/${id}`).then(() => undefined),

  submit: (id: number): Promise<PurchaseDetail> =>
    unwrap(http.post<ApiResponse<PurchaseDetail>>(`/v1/purchases/${id}/submit`)),

  approve: (id: number): Promise<PurchaseDetail> =>
    unwrap(http.post<ApiResponse<PurchaseDetail>>(`/v1/purchases/${id}/approve`)),

  updateSettlement: (id: number, payload: PurchaseSettlementPayload): Promise<PurchaseDetail> =>
    unwrap(http.put<ApiResponse<PurchaseDetail>>(`/v1/purchases/${id}/settlement`, payload)),

  reject: (id: number, note?: string): Promise<PurchaseDetail> =>
    unwrap(
      http.post<ApiResponse<PurchaseDetail>>(`/v1/purchases/${id}/reject`, undefined, {
        params: { note },
      }),
    ),

  cancel: (id: number, payload: { reasonId: number; note?: string }): Promise<PurchaseDetail> =>
    unwrap(http.post<ApiResponse<PurchaseDetail>>(`/v1/purchases/${id}/cancel`, payload)),

  receive: (id: number, payload: ReceivePayload): Promise<PurchaseReceipt> =>
    unwrap(http.post<ApiResponse<PurchaseReceipt>>(`/v1/purchases/${id}/receive`, payload)),

  returnToSupplier: (id: number, payload: ReturnPayload): Promise<PurchaseReturn> =>
    unwrap(http.post<ApiResponse<PurchaseReturn>>(`/v1/purchases/${id}/return`, payload)),

  receipts: (id: number): Promise<PurchaseReceipt[]> =>
    unwrap(http.get<ApiResponse<PurchaseReceipt[]>>(`/v1/purchases/${id}/receipts`)),

  returns: (id: number): Promise<PurchaseReturn[]> =>
    unwrap(http.get<ApiResponse<PurchaseReturn[]>>(`/v1/purchases/${id}/returns`)),

  logs: (id: number, params: PageQuery = {}): Promise<PageResponse<PurchaseLogEntry>> =>
    unwrap(http.get<ApiResponse<PageResponse<PurchaseLogEntry>>>(`/v1/purchases/${id}/logs`, { params })),

  attachments: (id: number): Promise<PurchaseAttachment[]> =>
    unwrap(http.get<ApiResponse<PurchaseAttachment[]>>(`/v1/purchases/${id}/attachments`)),

  uploadAttachment: (id: number, file: File): Promise<PurchaseAttachment> => {
    const form = new FormData()
    form.append('file', file)
    return unwrap(
      http.post<ApiResponse<PurchaseAttachment>>(`/v1/purchases/${id}/attachments`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      }),
    )
  },

  downloadAttachment: (id: number, attachmentId: number): Promise<Blob | null> =>
    http
      .get<Blob>(`/v1/purchases/${id}/attachments/${attachmentId}`, { responseType: 'blob' })
      .then((r) => r.data)
      .catch(() => null),

  deleteAttachment: (id: number, attachmentId: number): Promise<void> =>
    http.delete(`/v1/purchases/${id}/attachments/${attachmentId}`).then(() => undefined),
}

/** Purchasing configuration lookups. */
export const purchasingConfigApi = {
  statuses: (): Promise<PurStatus[]> =>
    unwrap(http.get<ApiResponse<PurStatus[]>>('/v1/purchasing/statuses')),

  types: (): Promise<PurType[]> =>
    unwrap(http.get<ApiResponse<PurType[]>>('/v1/purchasing/types')),

  createType: (payload: PurTypePayload): Promise<PurType> =>
    unwrap(http.post<ApiResponse<PurType>>('/v1/purchasing/types', payload)),

  updateType: (id: number, payload: PurTypePayload): Promise<PurType> =>
    unwrap(http.put<ApiResponse<PurType>>(`/v1/purchasing/types/${id}`, payload)),

  deleteType: (id: number): Promise<void> =>
    http.delete(`/v1/purchasing/types/${id}`).then(() => undefined),

  cancelReasons: (): Promise<PurCancelReason[]> =>
    unwrap(http.get<ApiResponse<PurCancelReason[]>>('/v1/purchasing/cancel-reasons')),

  createCancelReason: (payload: PurCancelReasonPayload): Promise<PurCancelReason> =>
    unwrap(http.post<ApiResponse<PurCancelReason>>('/v1/purchasing/cancel-reasons', payload)),

  updateCancelReason: (id: number, payload: PurCancelReasonPayload): Promise<PurCancelReason> =>
    unwrap(http.put<ApiResponse<PurCancelReason>>(`/v1/purchasing/cancel-reasons/${id}`, payload)),

  deleteCancelReason: (id: number): Promise<void> =>
    http.delete(`/v1/purchasing/cancel-reasons/${id}`).then(() => undefined),

  identifierTypes: (): Promise<PurIdentifierType[]> =>
    unwrap(http.get<ApiResponse<PurIdentifierType[]>>('/v1/purchasing/identifier-types')),

  createIdentifierType: (payload: PurIdentifierTypePayload): Promise<PurIdentifierType> =>
    unwrap(http.post<ApiResponse<PurIdentifierType>>('/v1/purchasing/identifier-types', payload)),

  updateIdentifierType: (id: number, payload: PurIdentifierTypePayload): Promise<PurIdentifierType> =>
    unwrap(http.put<ApiResponse<PurIdentifierType>>(`/v1/purchasing/identifier-types/${id}`, payload)),

  deleteIdentifierType: (id: number): Promise<void> =>
    http.delete(`/v1/purchasing/identifier-types/${id}`).then(() => undefined),

  warehouses: (): Promise<PurWarehouse[]> =>
    unwrap(http.get<ApiResponse<PurWarehouse[]>>('/v1/purchasing/warehouses')),

  approvalRules: (): Promise<PurApprovalRule[]> =>
    unwrap(http.get<ApiResponse<PurApprovalRule[]>>('/v1/purchasing/approval-rules')),

  createApprovalRule: (payload: PurApprovalRulePayload): Promise<PurApprovalRule> =>
    unwrap(http.post<ApiResponse<PurApprovalRule>>('/v1/purchasing/approval-rules', payload)),

  updateApprovalRule: (id: number, payload: PurApprovalRulePayload): Promise<PurApprovalRule> =>
    unwrap(http.put<ApiResponse<PurApprovalRule>>(`/v1/purchasing/approval-rules/${id}`, payload)),

  deleteApprovalRule: (id: number): Promise<void> =>
    http.delete(`/v1/purchasing/approval-rules/${id}`).then(() => undefined),
}
