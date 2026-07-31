import type { ApiResponse, PageQuery, PageResponse } from '@/types/api'
import type {
  CreateScheduledJobPayload,
  ScheduledJob,
  SchedulerExecution,
  SchedulerHandler,
  SchedulerStatus,
  UpdateScheduledJobPayload,
} from '@/types/scheduler'
import { http, unwrap } from './http'

export const schedulerApi = {
  jobs: (): Promise<ScheduledJob[]> => unwrap(http.get<ApiResponse<ScheduledJob[]>>('/v1/scheduler/jobs')),
  get: (id: number): Promise<ScheduledJob> => unwrap(http.get<ApiResponse<ScheduledJob>>(`/v1/scheduler/jobs/${id}`)),
  create: (payload: CreateScheduledJobPayload): Promise<ScheduledJob> =>
    unwrap(http.post<ApiResponse<ScheduledJob>>('/v1/scheduler/jobs', payload)),
  update: (id: number, payload: UpdateScheduledJobPayload): Promise<ScheduledJob> =>
    unwrap(http.put<ApiResponse<ScheduledJob>>(`/v1/scheduler/jobs/${id}`, payload)),
  changeStatus: (id: number, status: string): Promise<ScheduledJob> =>
    unwrap(http.patch<ApiResponse<ScheduledJob>>(`/v1/scheduler/jobs/${id}/status`, { status })),
  remove: (id: number): Promise<void> => http.delete(`/v1/scheduler/jobs/${id}`).then(() => undefined),
  run: (id: number): Promise<SchedulerExecution> =>
    unwrap(http.post<ApiResponse<SchedulerExecution>>(`/v1/scheduler/jobs/${id}/run`)),
  executions: (id: number, params: PageQuery = {}): Promise<PageResponse<SchedulerExecution>> =>
    unwrap(http.get<ApiResponse<PageResponse<SchedulerExecution>>>(`/v1/scheduler/jobs/${id}/executions`, { params })),
  recentExecutions: (params: PageQuery = {}): Promise<PageResponse<SchedulerExecution>> =>
    unwrap(http.get<ApiResponse<PageResponse<SchedulerExecution>>>('/v1/scheduler/executions', { params })),
  statuses: (): Promise<SchedulerStatus[]> =>
    unwrap(http.get<ApiResponse<SchedulerStatus[]>>('/v1/scheduler/statuses')),
  handlers: (): Promise<SchedulerHandler[]> =>
    unwrap(http.get<ApiResponse<SchedulerHandler[]>>('/v1/scheduler/handlers')),
}
