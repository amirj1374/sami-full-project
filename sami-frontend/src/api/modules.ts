import type { ApiResponse, PageQuery, PageResponse } from '@/types/api'
import type {
  AppModule,
  CreateModulePayload,
  ModuleStatus,
  UpdateModuleLifecyclePayload,
  UpdateModulePayload,
  UpdateModuleStatusPayload,
} from '@/types/models'
import { http, unwrap, unwrapVoid } from './http'

/** Module administration API calls ("module" in API wording). */
export const modulesApi = {
  list: (params: PageQuery = {}): Promise<PageResponse<AppModule>> =>
    unwrap(http.get<ApiResponse<PageResponse<AppModule>>>('/v1/modules', { params })),

  create: (payload: CreateModulePayload): Promise<AppModule> =>
    unwrap(http.post<ApiResponse<AppModule>>('/v1/modules', payload)),

  /** Code is immutable; enabled changes go through {@link updateStatus}. */
  update: (id: number, payload: UpdateModulePayload): Promise<AppModule> =>
    unwrap(http.put<ApiResponse<AppModule>>(`/v1/modules/${id}`, payload)),

  updateStatus: (id: number, payload: UpdateModuleStatusPayload): Promise<void> =>
    unwrapVoid(http.patch<ApiResponse<unknown>>(`/v1/modules/${id}/status`, payload)),

  /** Configurable lifecycle stages, for the admin dropdowns. */
  lifecycleStatuses: (): Promise<ModuleStatus[]> =>
    unwrap(http.get<ApiResponse<ModuleStatus[]>>('/v1/modules/lifecycle-statuses')),

  /**
   * Updates the development lifecycle record. Distinct from {@link updateStatus},
   * which toggles enablement — the two words mean different things here.
   */
  updateLifecycle: (id: number, payload: UpdateModuleLifecyclePayload): Promise<AppModule> =>
    unwrap(http.patch<ApiResponse<AppModule>>(`/v1/modules/${id}/lifecycle`, payload)),

  remove: (id: number): Promise<void> => http.delete(`/v1/modules/${id}`).then(() => undefined),
}
