import type { ApiResponse } from "@/types/api";
import type {
  LicenseCatalog,
  LicenseFeature,
  LicensePayload,
  LicensePlan,
  LicenseRecord,
  LicenseSummary,
  LicenseTransfer,
  LicenseValidation,
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
  plans: (): Promise<LicensePlan[]> =>
    unwrap(http.get<ApiResponse<LicensePlan[]>>("/v1/licensing/plans")),
  features: (): Promise<LicenseFeature[]> =>
    unwrap(http.get<ApiResponse<LicenseFeature[]>>("/v1/licensing/features")),
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
  transfers: (id: number): Promise<LicenseTransfer[]> =>
    unwrap(
      http.get<ApiResponse<LicenseTransfer[]>>(
        `/v1/licensing/licenses/${id}/transfers`,
      ),
    ),
  exportReport: async (report: string, tenantId?: number): Promise<void> => {
    const response = await http.get<Blob>(
      `/v1/licensing/reports/${report}/export.csv`,
      { params: { tenantId }, responseType: "blob" },
    );
    const url = URL.createObjectURL(response.data);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${report}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  },
};
