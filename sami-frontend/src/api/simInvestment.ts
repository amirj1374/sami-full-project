import type { ApiResponse } from '@/types/api'
import type { SimAnalysis, SimAnalysisPage, SimImportBatch, SimImportMessage, SimInvestmentOverview } from '@/types/simInvestment'
import { http, unwrap } from './http'

export const simInvestmentApi = {
  overview: (): Promise<SimInvestmentOverview> => unwrap(http.get<ApiResponse<SimInvestmentOverview>>('/v1/sim-investment/overview')),
  numbers: (params: Record<string, unknown>): Promise<SimAnalysisPage> => unwrap(http.get<ApiResponse<SimAnalysisPage>>('/v1/sim-investment/numbers', { params })),
  detail: (phone: string): Promise<Record<string, unknown>> => unwrap(http.get<ApiResponse<Record<string, unknown>>>(`/v1/sim-investment/numbers/${phone}`)),
  opportunities: (limit = 20): Promise<SimAnalysis[]> => unwrap(http.get<ApiResponse<SimAnalysis[]>>('/v1/sim-investment/opportunities', { params: { limit } })),
  imports: (): Promise<SimImportBatch[]> => unwrap(http.get<ApiResponse<SimImportBatch[]>>('/v1/sim-investment/imports')),
  importMessages: (id: number): Promise<SimImportMessage[]> => unwrap(http.get<ApiResponse<SimImportMessage[]>>(`/v1/sim-investment/imports/${id}/messages`)),
  importFile: (file: File, sourceCode: string, observedOn: string | null, fullSnapshot: boolean): Promise<SimImportBatch> => {
    const form = new FormData(); form.append('file', file); form.append('sourceCode', sourceCode); form.append('fullSnapshot', String(fullSnapshot))
    if (observedOn) form.append('observedOn', observedOn)
    return unwrap(http.post<ApiResponse<SimImportBatch>>('/v1/sim-investment/imports', form, { headers: { 'Content-Type': 'multipart/form-data' } }))
  },
  recalculate: (): Promise<{ analyzed: number; cohorts: number }> => unwrap(http.post<ApiResponse<{ analyzed: number; cohorts: number }>>('/v1/sim-investment/recalculate')),
}
