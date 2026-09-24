import { http, unwrap } from './http'
import type { ApiResponse } from '@/types/api'

export interface CrmIntelligence {
  customerId: number
  score: number | null
  state: string
  reasons: string[]
  suggestions: string[]
  purchaseCount: number
  expectedCadenceDays: number | null
}

export const crmIntelligenceApi = {
  customer: (id: number): Promise<CrmIntelligence> =>
    unwrap(http.get<ApiResponse<CrmIntelligence>>(`/v1/crm/intelligence/customers/${id}`)),
}
