import type { ApiResponse, PageResponse } from '@/types/api'
import type {
  AdjustmentPayload, AdjustmentResult, InventoryAudit, InventoryBalance,
  InventoryCount, InventoryDashboard, InventoryImportResult, InventoryLocation,
  InventoryMovement, InventoryReport, InventoryReservation, InventorySerial,
  InventoryTransfer, LocationPayload, TransferPayload, Warehouse, WarehousePayload,
} from '@/types/inventory'
import { http, unwrap, unwrapVoid } from './http'

export interface InventoryPageParams { page?: number; size?: number }
export interface BalanceParams extends InventoryPageParams { search?: string; warehouseId?: number; lowStock?: boolean }
export interface MovementParams extends InventoryPageParams { search?: string; warehouseId?: number; movementType?: string; sourceType?: string; from?: string; to?: string }
export interface SerialParams extends InventoryPageParams { search?: string; warehouseId?: number; status?: string }

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

export const inventoryApi = {
  dashboard: (): Promise<InventoryDashboard> => unwrap(http.get<ApiResponse<InventoryDashboard>>('/v1/inventory/dashboard')),
  warehouses: (): Promise<Warehouse[]> => unwrap(http.get<ApiResponse<Warehouse[]>>('/v1/inventory/warehouses')),
  createWarehouse: (payload: WarehousePayload): Promise<Warehouse> => unwrap(http.post<ApiResponse<Warehouse>>('/v1/inventory/warehouses', payload)),
  updateWarehouse: (id: number, payload: WarehousePayload): Promise<Warehouse> => unwrap(http.put<ApiResponse<Warehouse>>(`/v1/inventory/warehouses/${id}`, payload)),
  deleteWarehouse: (id: number): Promise<void> => http.delete(`/v1/inventory/warehouses/${id}`).then(() => undefined),
  locations: (warehouseId: number): Promise<InventoryLocation[]> => unwrap(http.get<ApiResponse<InventoryLocation[]>>(`/v1/inventory/warehouses/${warehouseId}/locations`)),
  createLocation: (warehouseId: number, payload: LocationPayload): Promise<InventoryLocation> => unwrap(http.post<ApiResponse<InventoryLocation>>(`/v1/inventory/warehouses/${warehouseId}/locations`, payload)),
  updateLocation: (warehouseId: number, id: number, payload: LocationPayload): Promise<InventoryLocation> => unwrap(http.put<ApiResponse<InventoryLocation>>(`/v1/inventory/warehouses/${warehouseId}/locations/${id}`, payload)),
  deleteLocation: (warehouseId: number, id: number): Promise<void> => http.delete(`/v1/inventory/warehouses/${warehouseId}/locations/${id}`).then(() => undefined),
  balances: (params: BalanceParams = {}): Promise<PageResponse<InventoryBalance>> => unwrap(http.get<ApiResponse<PageResponse<InventoryBalance>>>('/v1/inventory/balances', { params })),
  movements: (params: MovementParams = {}): Promise<PageResponse<InventoryMovement>> => unwrap(http.get<ApiResponse<PageResponse<InventoryMovement>>>('/v1/inventory/movements', { params })),
  serials: (params: SerialParams = {}): Promise<PageResponse<InventorySerial>> => unwrap(http.get<ApiResponse<PageResponse<InventorySerial>>>('/v1/inventory/serials', { params })),
  updateSerialStatus: (id: number, status: string, reason?: string): Promise<void> => unwrapVoid(http.put<ApiResponse<unknown>>(`/v1/inventory/serials/${id}/status`, { status, reason })),
  reservations: (params: InventoryPageParams & { sourceType?: string; status?: string } = {}): Promise<PageResponse<InventoryReservation>> => unwrap(http.get<ApiResponse<PageResponse<InventoryReservation>>>('/v1/inventory/reservations', { params })),
  releaseReservation: (id: number, reason?: string): Promise<void> => unwrapVoid(http.post<ApiResponse<unknown>>(`/v1/inventory/reservations/${id}/release`, { reason })),
  adjust: (payload: AdjustmentPayload): Promise<AdjustmentResult> => unwrap(http.post<ApiResponse<AdjustmentResult>>('/v1/inventory/adjustments', payload)),
  transfers: (params: InventoryPageParams & { status?: string } = {}): Promise<PageResponse<InventoryTransfer>> => unwrap(http.get<ApiResponse<PageResponse<InventoryTransfer>>>('/v1/inventory/transfers', { params })),
  transfer: (id: number): Promise<InventoryTransfer> => unwrap(http.get<ApiResponse<InventoryTransfer>>(`/v1/inventory/transfers/${id}`)),
  createTransfer: (payload: TransferPayload): Promise<InventoryTransfer> => unwrap(http.post<ApiResponse<InventoryTransfer>>('/v1/inventory/transfers', payload)),
  shipTransfer: (id: number): Promise<InventoryTransfer> => unwrap(http.post<ApiResponse<InventoryTransfer>>(`/v1/inventory/transfers/${id}/ship`)),
  receiveTransfer: (id: number): Promise<InventoryTransfer> => unwrap(http.post<ApiResponse<InventoryTransfer>>(`/v1/inventory/transfers/${id}/receive`)),
  cancelTransfer: (id: number): Promise<InventoryTransfer> => unwrap(http.post<ApiResponse<InventoryTransfer>>(`/v1/inventory/transfers/${id}/cancel`)),
  counts: (params: InventoryPageParams & { status?: string } = {}): Promise<PageResponse<InventoryCount>> => unwrap(http.get<ApiResponse<PageResponse<InventoryCount>>>('/v1/inventory/counts', { params })),
  count: (id: number): Promise<InventoryCount> => unwrap(http.get<ApiResponse<InventoryCount>>(`/v1/inventory/counts/${id}`)),
  createCount: (payload: { warehouseId: number; locationId?: number; notes?: string }): Promise<InventoryCount> => unwrap(http.post<ApiResponse<InventoryCount>>('/v1/inventory/counts', payload)),
  submitCount: (id: number, lines: Array<{ productId: number; countedQuantity: number }>): Promise<InventoryCount> => unwrap(http.put<ApiResponse<InventoryCount>>(`/v1/inventory/counts/${id}/results`, { lines })),
  postCount: (id: number): Promise<InventoryCount> => unwrap(http.post<ApiResponse<InventoryCount>>(`/v1/inventory/counts/${id}/post`)),
  cancelCount: (id: number): Promise<InventoryCount> => unwrap(http.post<ApiResponse<InventoryCount>>(`/v1/inventory/counts/${id}/cancel`)),
  report: (params: { from?: string; to?: string } = {}): Promise<InventoryReport> => unwrap(http.get<ApiResponse<InventoryReport>>('/v1/inventory/reports', { params })),
  audit: (params: { entityType?: string; entityId?: number; limit?: number } = {}): Promise<InventoryAudit[]> => unwrap(http.get<ApiResponse<InventoryAudit[]>>('/v1/inventory/audit', { params })),
  importCsv: async (file: File): Promise<InventoryImportResult> => {
    const form = new FormData()
    form.append('file', file)
    return unwrap(http.post<ApiResponse<InventoryImportResult>>('/v1/inventory/import', form))
  },
  exportCsv: async (params: BalanceParams = {}): Promise<void> => {
    const response = await http.get<Blob>('/v1/inventory/export.csv', { params, responseType: 'blob' })
    downloadBlob(response.data, 'inventory-balances.csv')
  },
}
