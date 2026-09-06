import { http, unwrap } from './http'
import type { ApiResponse } from '@/types/api'
export interface OrganizationCompany { id: number; code: string; name: string }
export interface OrganizationBranch { id: number; companyId: number; code: string; name: string }
export interface OrganizationContext { companyId: number | null; branchId: number | null; companies: OrganizationCompany[]; branches: OrganizationBranch[] }
export const organizationContextApi = {
  current: () => unwrap(http.get<ApiResponse<OrganizationContext>>('/v1/organization/context')),
  select: (payload: { companyId: number; branchId: number | null }) => unwrap(http.put<ApiResponse<OrganizationContext>>('/v1/organization/context', payload)),
}
