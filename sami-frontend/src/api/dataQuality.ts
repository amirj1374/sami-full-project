import type { ApiResponse, PageQuery, PageResponse } from '@/types/api'
import type {
  QualityCatalog,
  QualityIssue,
  QualityIssueSummary,
  QualityRule,
  QualityRulePayload,
} from '@/types/dataQuality'
import { http, unwrap } from './http'

export const dataQualityApi = {
  rules: (): Promise<QualityRule[]> =>
    unwrap(http.get<ApiResponse<QualityRule[]>>('/v1/quality/rules')),
  rule: (id: number): Promise<QualityRule> =>
    unwrap(http.get<ApiResponse<QualityRule>>(`/v1/quality/rules/${id}`)),
  createRule: (payload: QualityRulePayload): Promise<QualityRule> =>
    unwrap(http.post<ApiResponse<QualityRule>>('/v1/quality/rules', payload)),
  changeRuleStatus: (id: number, statusCode: string): Promise<QualityRule> =>
    unwrap(http.patch<ApiResponse<QualityRule>>(`/v1/quality/rules/${id}/status`, { statusCode })),
  deleteRule: (id: number): Promise<void> =>
    http.delete(`/v1/quality/rules/${id}`).then(() => undefined),
  issues: (status?: string, params: PageQuery = {}): Promise<PageResponse<QualityIssue>> =>
    unwrap(http.get<ApiResponse<PageResponse<QualityIssue>>>('/v1/quality/issues', { params: { ...params, status } })),
  issueSummary: (): Promise<QualityIssueSummary> =>
    unwrap(http.get<ApiResponse<QualityIssueSummary>>('/v1/quality/issues/summary')),
  resolveIssue: (id: number, note?: string): Promise<QualityIssue> =>
    unwrap(http.post<ApiResponse<QualityIssue>>(`/v1/quality/issues/${id}/resolve`, { note })),
  ignoreIssue: (id: number, note?: string): Promise<QualityIssue> =>
    unwrap(http.post<ApiResponse<QualityIssue>>(`/v1/quality/issues/${id}/ignore`, { note })),
  catalog: (): Promise<QualityCatalog> =>
    unwrap(http.get<ApiResponse<QualityCatalog>>('/v1/quality/catalog')),
}
