import type { ApiResponse, PageQuery, PageResponse } from '@/types/api'
import type {
  AutomationActionDescriptor,
  AutomationExecution,
  AutomationExecutionLog,
  AutomationRule,
  AutomationRulePayload,
  AutomationStatus,
  AutomationTrigger,
} from '@/types/automation'
import { http, unwrap } from './http'

export type AutomationListParams = PageQuery & {
  search?: string
  triggerType?: string
  statusCode?: string
  category?: string
}

export const automationsApi = {
  list: (params: AutomationListParams = {}): Promise<PageResponse<AutomationRule>> =>
    unwrap(http.get<ApiResponse<PageResponse<AutomationRule>>>('/v1/automations', { params })),
  get: (id: number): Promise<AutomationRule> =>
    unwrap(http.get<ApiResponse<AutomationRule>>(`/v1/automations/${id}`)),
  create: (payload: AutomationRulePayload): Promise<AutomationRule> =>
    unwrap(http.post<ApiResponse<AutomationRule>>('/v1/automations', payload)),
  update: (id: number, payload: AutomationRulePayload): Promise<AutomationRule> =>
    unwrap(http.put<ApiResponse<AutomationRule>>(`/v1/automations/${id}`, payload)),
  changeStatus: (id: number, statusCode: string, expectedVersion: number): Promise<AutomationRule> =>
    unwrap(http.patch<ApiResponse<AutomationRule>>(`/v1/automations/${id}/status`, { statusCode, expectedVersion })),
  remove: (id: number): Promise<void> => http.delete(`/v1/automations/${id}`).then(() => undefined),
  run: (id: number): Promise<void> => http.post(`/v1/automations/${id}/run`).then(() => undefined),
  executions: (id: number, params: PageQuery = {}): Promise<PageResponse<AutomationExecution>> =>
    unwrap(http.get<ApiResponse<PageResponse<AutomationExecution>>>(`/v1/automations/${id}/executions`, { params })),
  executionLogs: (id: number): Promise<AutomationExecutionLog[]> =>
    unwrap(http.get<ApiResponse<AutomationExecutionLog[]>>(`/v1/automations/executions/${id}/logs`)),
  statuses: (): Promise<AutomationStatus[]> =>
    unwrap(http.get<ApiResponse<AutomationStatus[]>>('/v1/automations/statuses')),
  triggers: (): Promise<AutomationTrigger[]> =>
    unwrap(http.get<ApiResponse<AutomationTrigger[]>>('/v1/automations/triggers')),
  actions: (): Promise<AutomationActionDescriptor[]> =>
    unwrap(http.get<ApiResponse<AutomationActionDescriptor[]>>('/v1/automations/actions')),
}
