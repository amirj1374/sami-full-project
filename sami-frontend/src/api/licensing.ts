import type { ApiResponse } from "@/types/api";
import type {
  LicenseCatalog,
  LicenseAuditRecord,
  LicenseFeature,
  LicensePayload,
  LicensePlan,
  LicenseRecord,
  LicenseUpdatePayload,
  LicenseSummary,
  LicenseTransfer,
  LicenseValidation,
  LicensingReportRow,
  FeaturePayload,
  PlanPayload,
  TenantPayload,
  TenantUpdatePayload,
  TenantRecord,
  UsageCheck,
} from "@/types/licensing";
import { http, unwrap } from "./http";

export const licensingApi = {
  licenses: (): Promise<LicenseRecord[]> =>
    unwrap(http.get<ApiResponse<LicenseRecord[]>>("/v1/licensing/licenses")),
  license: (id: number): Promise<LicenseRecord> =>
    unwrap(
      http.get<ApiResponse<LicenseRecord>>(`/v1/licensing/licenses/${id}`),
    ),
  create: (payload: LicensePayload): Promise<LicenseRecord> =>
    unwrap(
      http.post<ApiResponse<LicenseRecord>>("/v1/licensing/licenses", payload),
    ),
  update: (id: number, payload: LicenseUpdatePayload): Promise<LicenseRecord> =>
    unwrap(http.put<ApiResponse<LicenseRecord>>(`/v1/licensing/licenses/${id}`, payload)),
  activate: (id: number, fingerprint?: string): Promise<LicenseRecord> =>
    unwrap(
      http.post<ApiResponse<LicenseRecord>>(
        `/v1/licensing/licenses/${id}/activate`,
        fingerprint ? { fingerprint } : undefined,
      ),
    ),
  renew: (
    id: number,
    days?: number,
    planCode?: string,
  ): Promise<LicenseRecord> =>
    unwrap(
      http.post<ApiResponse<LicenseRecord>>(
        `/v1/licensing/licenses/${id}/renew`,
        { days, planCode },
      ),
    ),
  changeStatus: (id: number, statusCode: string): Promise<LicenseRecord> =>
    unwrap(
      http.patch<ApiResponse<LicenseRecord>>(
        `/v1/licensing/licenses/${id}/status`,
        { statusCode },
      ),
    ),
  activateMode: (id: number, mode: string, grantDays?: number): Promise<LicenseRecord> =>
    unwrap(http.post<ApiResponse<LicenseRecord>>(`/v1/licensing/licenses/${id}/activate/${mode}`, { grantDays })),
  transfer: (id: number, toTenantId: number, reason?: string): Promise<LicenseRecord> =>
    unwrap(http.post<ApiResponse<LicenseRecord>>(`/v1/licensing/licenses/${id}/transfer`, { toTenantId, reason })),
  paymentStatus: (id: number, paymentStatus: string): Promise<LicenseRecord> =>
    unwrap(http.patch<ApiResponse<LicenseRecord>>(`/v1/licensing/licenses/${id}/payment-status`, { statusCode: paymentStatus })),
  toggleFeature: (
    id: number,
    featureCode: string,
    enabled: boolean,
  ): Promise<LicenseRecord> =>
    unwrap(
      http.post<ApiResponse<LicenseRecord>>(
        `/v1/licensing/licenses/${id}/features`,
        { featureCode, enabled },
      ),
    ),
  validate: (key: string): Promise<LicenseValidation> =>
    unwrap(
      http.get<ApiResponse<LicenseValidation>>(
        "/v1/licensing/licenses/validate",
        { params: { key } },
      ),
    ),
  tenants: (): Promise<TenantRecord[]> =>
    unwrap(http.get<ApiResponse<TenantRecord[]>>("/v1/licensing/tenants")),
  createTenant: (payload: TenantPayload): Promise<TenantRecord> =>
    unwrap(http.post<ApiResponse<TenantRecord>>("/v1/licensing/tenants", payload)),
  updateTenant: (id: number, payload: TenantUpdatePayload): Promise<TenantRecord> =>
    unwrap(http.put<ApiResponse<TenantRecord>>(`/v1/licensing/tenants/${id}`, payload)),
  activateTenant: (id: number): Promise<TenantRecord> =>
    unwrap(http.post<ApiResponse<TenantRecord>>(`/v1/licensing/tenants/${id}/activate`)),
  suspendTenant: (id: number): Promise<TenantRecord> =>
    unwrap(http.post<ApiResponse<TenantRecord>>(`/v1/licensing/tenants/${id}/suspend`)),
  plans: (): Promise<LicensePlan[]> =>
    unwrap(http.get<ApiResponse<LicensePlan[]>>("/v1/licensing/plans")),
  createPlan: (payload: PlanPayload): Promise<LicensePlan> =>
    unwrap(http.post<ApiResponse<LicensePlan>>("/v1/licensing/plans", payload)),
  updatePlan: (id: number, payload: PlanPayload): Promise<LicensePlan> =>
    unwrap(http.put<ApiResponse<LicensePlan>>(`/v1/licensing/plans/${id}`, payload)),
  features: (): Promise<LicenseFeature[]> =>
    unwrap(http.get<ApiResponse<LicenseFeature[]>>("/v1/licensing/features")),
  createFeature: (payload: FeaturePayload): Promise<LicenseFeature> =>
    unwrap(http.post<ApiResponse<LicenseFeature>>("/v1/licensing/features", payload)),
  updateFeature: (id: number, payload: FeaturePayload): Promise<LicenseFeature> =>
    unwrap(http.put<ApiResponse<LicenseFeature>>(`/v1/licensing/features/${id}`, payload)),
  usage: (tenantId?: number): Promise<UsageCheck[]> =>
    unwrap(
      http.get<ApiResponse<UsageCheck[]>>("/v1/licensing/usage", {
        params: { tenantId },
      }),
    ),
  catalog: (): Promise<LicenseCatalog> =>
    unwrap(http.get<ApiResponse<LicenseCatalog>>("/v1/licensing/catalog")),
  summary: (): Promise<LicenseSummary> =>
    unwrap(
      http.get<ApiResponse<LicenseSummary>>("/v1/licensing/reports/summary"),
    ),
  expiring: (withinDays = 30): Promise<Array<Record<string, unknown>>> =>
    unwrap(
      http.get<ApiResponse<Array<Record<string, unknown>>>>(
        "/v1/licensing/reports/expiring",
        { params: { withinDays } },
      ),
    ),
  tenantReport: (): Promise<LicensingReportRow[]> =>
    unwrap(http.get<ApiResponse<LicensingReportRow[]>>("/v1/licensing/reports/tenants")),
  featureUsageReport: (): Promise<LicensingReportRow[]> =>
    unwrap(http.get<ApiResponse<LicensingReportRow[]>>("/v1/licensing/reports/feature-usage")),
  planComparisonReport: (): Promise<LicensingReportRow[]> =>
    unwrap(http.get<ApiResponse<LicensingReportRow[]>>("/v1/licensing/reports/plan-comparison")),
  transfers: (id: number): Promise<LicenseTransfer[]> =>
    unwrap(
      http.get<ApiResponse<LicenseTransfer[]>>(
        `/v1/licensing/licenses/${id}/transfers`,
      ),
    ),
  audit: (id: number): Promise<LicenseAuditRecord[]> =>
    unwrap(http.get<ApiResponse<LicenseAuditRecord[]>>(`/v1/licensing/licenses/${id}/audit`)),
  exportReport: async (report: string, tenantId?: number, withinDays?: number): Promise<void> => {
    const response = await http.get<Blob>(
      `/v1/licensing/reports/${report}/export.csv`,
      { params: { tenantId, withinDays }, responseType: "blob" },
    );
    const url = URL.createObjectURL(response.data);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${report}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  },
};
