import type { ApiResponse, PageQuery, PageResponse } from '@/types/api'
import type {
  DashboardAuditEntry,
  DashboardChartType,
  DashboardCreatePayload,
  DashboardDataSource,
  DashboardDetail,
  DashboardKpiStatus,
  DashboardRefreshContext,
  DashboardRefreshPolicy,
  DashboardRow,
  DashboardSavedFilter,
  DashboardShare,
  DashboardStatus,
  DashboardVisibility,
  DashboardWidget,
  DashboardWidgetPayload,
  DashboardWidgetType,
  Kpi,
  KpiCalculation,
  KpiPayload,
  KpiValueEntry,
  WidgetDataResponse,
} from '@/types/models'
import { http, unwrap } from './http'

export type DashboardListParams = PageQuery & {
  search?: string
  statusId?: number
  visibilityId?: number
  favoritesOnly?: boolean
}

/** Dashboard, widget and executive-cockpit API. */
export const dashboardsApi = {
  list: (params: DashboardListParams = {}): Promise<PageResponse<DashboardRow>> =>
    unwrap(http.get<ApiResponse<PageResponse<DashboardRow>>>('/v1/dashboards', { params })),

  get: (id: number): Promise<DashboardDetail> =>
    unwrap(http.get<ApiResponse<DashboardDetail>>(`/v1/dashboards/${id}`)),

  refresh: (id: number, context: DashboardRefreshContext = {}): Promise<WidgetDataResponse[]> =>
    unwrap(http.post<ApiResponse<WidgetDataResponse[]>>(`/v1/dashboards/${id}/refresh`, context)),

  create: (payload: DashboardCreatePayload): Promise<DashboardDetail> =>
    unwrap(http.post<ApiResponse<DashboardDetail>>('/v1/dashboards', payload)),

  update: (id: number, payload: DashboardCreatePayload): Promise<DashboardDetail> =>
    unwrap(http.put<ApiResponse<DashboardDetail>>(`/v1/dashboards/${id}`, payload)),

  remove: (id: number): Promise<void> =>
    http.delete(`/v1/dashboards/${id}`).then(() => undefined),

  audit: (id: number, params: PageQuery = {}): Promise<PageResponse<DashboardAuditEntry>> =>
    unwrap(http.get<ApiResponse<PageResponse<DashboardAuditEntry>>>(`/v1/dashboards/${id}/audit`, { params })),

  // sharing
  shares: (id: number): Promise<DashboardShare[]> =>
    unwrap(http.get<ApiResponse<DashboardShare[]>>(`/v1/dashboards/${id}/shares`)),

  share: (
    id: number,
    payload: { targetType: 'USER' | 'ROLE'; targetUserId?: number; targetRoleId?: number; canEdit: boolean },
  ): Promise<DashboardShare> =>
    unwrap(http.post<ApiResponse<DashboardShare>>(`/v1/dashboards/${id}/shares`, payload)),

  unshare: (id: number, shareId: number): Promise<void> =>
    http.delete(`/v1/dashboards/${id}/shares/${shareId}`).then(() => undefined),

  // favorites
  addFavorite: (id: number): Promise<void> =>
    http.post(`/v1/dashboards/${id}/favorite`).then(() => undefined),

  removeFavorite: (id: number): Promise<void> =>
    http.delete(`/v1/dashboards/${id}/favorite`).then(() => undefined),

  // import / export
  export: (id: number): Promise<Record<string, unknown>> =>
    unwrap(http.get<ApiResponse<Record<string, unknown>>>(`/v1/dashboards/${id}/export`)),

  import: (snapshot: Record<string, unknown>): Promise<DashboardDetail> =>
    unwrap(http.post<ApiResponse<DashboardDetail>>('/v1/dashboards/import', snapshot)),

  // saved filters
  savedFilters: (): Promise<DashboardSavedFilter[]> =>
    unwrap(http.get<ApiResponse<DashboardSavedFilter[]>>('/v1/dashboards/saved-filters')),

  saveFilter: (payload: { name: string; filter: Record<string, unknown> }): Promise<DashboardSavedFilter> =>
    unwrap(http.post<ApiResponse<DashboardSavedFilter>>('/v1/dashboards/saved-filters', payload)),

  deleteSavedFilter: (id: number): Promise<void> =>
    http.delete(`/v1/dashboards/saved-filters/${id}`).then(() => undefined),

  // widgets
  addWidget: (dashboardId: number, payload: DashboardWidgetPayload): Promise<DashboardWidget> =>
    unwrap(http.post<ApiResponse<DashboardWidget>>(`/v1/dashboards/${dashboardId}/widgets`, payload)),

  updateWidget: (widgetId: number, payload: DashboardWidgetPayload): Promise<DashboardWidget> =>
    unwrap(http.put<ApiResponse<DashboardWidget>>(`/v1/widgets/${widgetId}`, payload)),

  removeWidget: (widgetId: number): Promise<void> =>
    http.delete(`/v1/widgets/${widgetId}`).then(() => undefined),

  saveLayout: (
    dashboardId: number,
    items: { widgetId: number; positionX: number; positionY: number; width: number; height: number }[],
  ): Promise<void> =>
    http.put(`/v1/dashboards/${dashboardId}/layout`, { items }).then(() => undefined),

  widgetData: (widgetId: number, context: DashboardRefreshContext = {}): Promise<WidgetDataResponse> =>
    unwrap(http.post<ApiResponse<WidgetDataResponse>>(`/v1/widgets/${widgetId}/data`, context)),

  // config lookups
  statuses: (): Promise<DashboardStatus[]> =>
    unwrap(http.get<ApiResponse<DashboardStatus[]>>('/v1/dashboard-config/statuses')),

  visibilities: (): Promise<DashboardVisibility[]> =>
    unwrap(http.get<ApiResponse<DashboardVisibility[]>>('/v1/dashboard-config/visibilities')),

  kpiStatuses: (): Promise<DashboardKpiStatus[]> =>
    unwrap(http.get<ApiResponse<DashboardKpiStatus[]>>('/v1/dashboard-config/kpi-statuses')),

  widgetTypes: (): Promise<DashboardWidgetType[]> =>
    unwrap(http.get<ApiResponse<DashboardWidgetType[]>>('/v1/dashboard-config/widget-types')),

  chartTypes: (): Promise<DashboardChartType[]> =>
    unwrap(http.get<ApiResponse<DashboardChartType[]>>('/v1/dashboard-config/chart-types')),

  dataSources: (): Promise<DashboardDataSource[]> =>
    unwrap(http.get<ApiResponse<DashboardDataSource[]>>('/v1/dashboard-config/data-sources')),

  refreshPolicies: (): Promise<DashboardRefreshPolicy[]> =>
    unwrap(http.get<ApiResponse<DashboardRefreshPolicy[]>>('/v1/dashboard-config/refresh-policies')),
}

/** KPI management API. */
export const kpisApi = {
  list: (params: PageQuery & { search?: string; statusId?: number } = {}): Promise<PageResponse<Kpi>> =>
    unwrap(http.get<ApiResponse<PageResponse<Kpi>>>('/v1/kpis', { params })),

  get: (id: number): Promise<Kpi> =>
    unwrap(http.get<ApiResponse<Kpi>>(`/v1/kpis/${id}`)),

  create: (payload: KpiPayload): Promise<Kpi> =>
    unwrap(http.post<ApiResponse<Kpi>>('/v1/kpis', payload)),

  update: (id: number, payload: KpiPayload): Promise<Kpi> =>
    unwrap(http.put<ApiResponse<Kpi>>(`/v1/kpis/${id}`, payload)),

  remove: (id: number): Promise<void> => http.delete(`/v1/kpis/${id}`).then(() => undefined),

  validate: (id: number): Promise<{ valid: boolean; message: string | null }> =>
    unwrap(http.get<ApiResponse<{ valid: boolean; message: string | null }>>(`/v1/kpis/${id}/validate`)),

  calculate: (id: number, value?: number): Promise<KpiCalculation> =>
    unwrap(http.post<ApiResponse<KpiCalculation>>(`/v1/kpis/${id}/calculate`, {}, { params: { value } })),

  history: (id: number, limit = 30): Promise<KpiValueEntry[]> =>
    unwrap(http.get<ApiResponse<KpiValueEntry[]>>(`/v1/kpis/${id}/history`, { params: { limit } })),

  exportCsv: (): Promise<string> =>
    http.get<string>('/v1/kpis/export', { responseType: 'text' }).then((r) => r.data),
}
