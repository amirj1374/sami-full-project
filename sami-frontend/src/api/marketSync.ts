import type { ApiResponse } from '@/types/api'
import { http, unwrap } from './http'
export type MarketRow = Record<string, any>
export const marketSyncApi = {
  overview: (): Promise<MarketRow> => unwrap(http.get<ApiResponse<MarketRow>>('/v1/market-sync/overview')),
  sources: (): Promise<MarketRow[]> => unwrap(http.get<ApiResponse<MarketRow[]>>('/v1/market-sync/sources')),
  saveSource: (id: number | null, data: MarketRow) => unwrap(id ? http.put(`/v1/market-sync/sources/${id}`, data) : http.post('/v1/market-sync/sources', data)),
  profiles: (): Promise<MarketRow[]> => unwrap(http.get<ApiResponse<MarketRow[]>>('/v1/market-sync/pricing-profiles')),
  saveProfile: (id: number | null, data: MarketRow) => unwrap(id ? http.put(`/v1/market-sync/pricing-profiles/${id}`, data) : http.post('/v1/market-sync/pricing-profiles', data)),
  preview: (data: MarketRow): Promise<MarketRow> => unwrap(http.post<ApiResponse<MarketRow>>('/v1/market-sync/pricing-preview', data)),
  rules: (): Promise<MarketRow[]> => unwrap(http.get<ApiResponse<MarketRow[]>>('/v1/market-sync/publication-rules')),
  saveRule: (data: MarketRow) => unwrap(http.post('/v1/market-sync/publication-rules', data)),
  deleteRule: (id: number) => http.delete(`/v1/market-sync/publication-rules/${id}`),
  sync: (id: number) => unwrap(http.post(`/v1/market-sync/sources/${id}/sync`)),
  runs: (): Promise<MarketRow[]> => unwrap(http.get<ApiResponse<MarketRow[]>>('/v1/market-sync/runs')),
  history: (): Promise<MarketRow[]> => unwrap(http.get<ApiResponse<MarketRow[]>>('/v1/market-sync/price-history')),
  errors: (): Promise<MarketRow[]> => unwrap(http.get<ApiResponse<MarketRow[]>>('/v1/market-sync/errors')),
  productStatus: (productId: number): Promise<MarketRow> => unwrap(http.get<ApiResponse<MarketRow>>(`/v1/market-sync/products/${productId}`)),
}
