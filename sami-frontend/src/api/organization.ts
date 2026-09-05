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

export const organizationApi = {
  list: () => unwrap(http.get<ApiResponse<Company[]>>('/v1/organization/companies')),
  create: (payload: CompanyPayload) => unwrap(http.post<ApiResponse<Company>>('/v1/organization/companies', payload)),
  update: (id: number, payload: CompanyPayload) => unwrap(http.put<ApiResponse<Company>>(`/v1/organization/companies/${id}`, payload)),
}
