import type { ApiResponse } from '@/types/api'
import { http, unwrap } from './http'

export interface Company {
  id: number
  code: string
  name: string
  legalName: string | null
  taxNumber: string | null
  registrationNumber: string | null
  currencyCode: string
  timezone: string
  locale: string
  fiscalYearStartMonth: number
  email: string | null
  phone: string | null
  website: string | null
  addressLine1: string | null
  city: string | null
  state: string | null
  postalCode: string | null
  countryCode: string | null
  isDefault: boolean
  active: boolean
  displayOrder: number
  version: number
}

export interface CompanyPayload {
  code: string
  name: string
  legalName?: string
  taxNumber?: string
  registrationNumber?: string
  currencyCode?: string
  timezone?: string
  locale?: string
  fiscalYearStartMonth?: number
  email?: string
  phone?: string
  website?: string
  addressLine1?: string
  city?: string
  state?: string
  postalCode?: string
  countryCode?: string
  active?: boolean
  displayOrder?: number
  expectedVersion?: number
}
export interface Branch { id: number; companyId: number; code: string; name: string; active: boolean }
export interface OrganizationAssignment { id: number; userId: number; companyId: number; roleId: number; active: boolean; branchIds: number[] }

export const organizationApi = {
  list: () => unwrap(http.get<ApiResponse<Company[]>>('/v1/organization/companies')),
  create: (payload: CompanyPayload) => unwrap(http.post<ApiResponse<Company>>('/v1/organization/companies', payload)),
  update: (id: number, payload: CompanyPayload) => unwrap(http.put<ApiResponse<Company>>(`/v1/organization/companies/${id}`, payload)),
  branches: (companyId: number) => unwrap(http.get<ApiResponse<Branch[]>>(`/v1/organization/companies/${companyId}/branches`)),
  assignments: (userId: number) => unwrap(http.get<ApiResponse<OrganizationAssignment[]>>(`/v1/organization/grants/users/${userId}`)),
  assign: (payload: { userId: number; companyId: number; roleId: number }) => unwrap(http.post<ApiResponse<OrganizationAssignment>>('/v1/organization/grants', payload)),
  grantBranch: (assignmentId: number, branchId: number) => unwrap(http.post<ApiResponse<OrganizationAssignment>>(`/v1/organization/grants/${assignmentId}/branches`, { branchId })),
  revokeBranch: (assignmentId: number, branchId: number) => http.delete(`/v1/organization/grants/${assignmentId}/branches/${branchId}`).then(() => undefined),
  revokeAssignment: (assignmentId: number) => http.delete(`/v1/organization/grants/${assignmentId}`).then(() => undefined),
}
