import type { ApiResponse } from '@/types/api'
import { http, unwrap } from './http'

export interface HamtaRecord {
  id: number; product_name: string; sku: string; imei: string; serial_number?: string
  activation_code: string; delivered: boolean; delivery_datetime?: string
  delivered_by_user_id?: number; delivered_sale_id?: number; created_at: string
}

export const hamtaApi = {
  settings: (): Promise<{ enforcementEnabled: boolean }> => unwrap(http.get<ApiResponse<{ enforcementEnabled: boolean }>>('/v1/hamta/settings')),
  updateSettings: (enforcementEnabled: boolean) => unwrap(http.put('/v1/hamta/settings', { enforcementEnabled })),
  report: (delivered?: boolean): Promise<HamtaRecord[]> => unwrap(http.get<ApiResponse<HamtaRecord[]>>('/v1/hamta/report', { params: { delivered } })),
  byImei: (imei: string): Promise<Record<string, unknown>> => unwrap(http.get<ApiResponse<Record<string, unknown>>>(`/v1/hamta/serials/${encodeURIComponent(imei)}/code`)),
  updateCode: (imei: string, activationCode: string) => unwrap(http.put(`/v1/hamta/serials/${encodeURIComponent(imei)}/code`, { activationCode })),
  invoice: (saleId: number): Promise<Array<Record<string, unknown>>> => unwrap(http.get<ApiResponse<Array<Record<string, unknown>>>>(`/v1/hamta/sales/${saleId}/invoice`)),
  deliver: (saleId: number) => unwrap(http.post(`/v1/hamta/sales/${saleId}/deliver`)),
}
