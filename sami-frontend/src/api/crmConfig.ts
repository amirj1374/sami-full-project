import type { ApiResponse } from '@/types/api'
import type {
  BlacklistReason,
  CustomerSource,
  CustomerStatus,
  CustomerTag,
  CustomerType,
  CrmSegment,
  PreferenceDefinition,
  RelationType,
} from '@/types/models'
import { http, unwrap } from './http'

export interface CustomerTypePayload {
  code: string
  name: string
  description?: string
  active: boolean
  displayOrder: number
}

export interface CustomerStatusPayload {
  code: string
  name: string
  description?: string
  isBlocking: boolean
  hiddenByDefault: boolean
  displayOrder: number
}

export interface CustomerSourcePayload {
  code: string
  name: string
  active: boolean
  displayOrder: number
}

export interface CustomerTagPayload {
  name: string
  color?: string
  description?: string
  active: boolean
}

export interface CrmSegmentPayload {
  name: string
  description?: string
  filter: Record<string, unknown>
}

export interface PreferenceDefinitionPayload {
  prefKey: string
  label: string
  fieldType: PreferenceDefinition['fieldType']
  required: boolean
  minLength?: number
  maxLength?: number
  pattern?: string
  options?: string[]
  active: boolean
  displayOrder: number
}

/** The CRM's configurable lookup data (types, statuses, sources, tags, rules). */
export const crmConfigApi = {
  types: (): Promise<CustomerType[]> =>
    unwrap(http.get<ApiResponse<CustomerType[]>>('/v1/crm/types')),

  createType: (payload: CustomerTypePayload): Promise<CustomerType> =>
    unwrap(http.post<ApiResponse<CustomerType>>('/v1/crm/types', payload)),

  updateType: (id: number, payload: CustomerTypePayload): Promise<CustomerType> =>
    unwrap(http.put<ApiResponse<CustomerType>>(`/v1/crm/types/${id}`, payload)),

  deleteType: (id: number): Promise<void> =>
    http.delete(`/v1/crm/types/${id}`).then(() => undefined),

  statuses: (): Promise<CustomerStatus[]> =>
    unwrap(http.get<ApiResponse<CustomerStatus[]>>('/v1/crm/statuses')),

  createStatus: (payload: CustomerStatusPayload): Promise<CustomerStatus> =>
    unwrap(http.post<ApiResponse<CustomerStatus>>('/v1/crm/statuses', payload)),

  updateStatus: (id: number, payload: CustomerStatusPayload): Promise<CustomerStatus> =>
    unwrap(http.put<ApiResponse<CustomerStatus>>(`/v1/crm/statuses/${id}`, payload)),

  deleteStatus: (id: number): Promise<void> =>
    http.delete(`/v1/crm/statuses/${id}`).then(() => undefined),

  sources: (): Promise<CustomerSource[]> =>
    unwrap(http.get<ApiResponse<CustomerSource[]>>('/v1/crm/sources')),

  createSource: (payload: CustomerSourcePayload): Promise<CustomerSource> =>
    unwrap(http.post<ApiResponse<CustomerSource>>('/v1/crm/sources', payload)),

  updateSource: (id: number, payload: CustomerSourcePayload): Promise<CustomerSource> =>
    unwrap(http.put<ApiResponse<CustomerSource>>(`/v1/crm/sources/${id}`, payload)),

  deleteSource: (id: number): Promise<void> =>
    http.delete(`/v1/crm/sources/${id}`).then(() => undefined),

  tags: (): Promise<CustomerTag[]> =>
    unwrap(http.get<ApiResponse<CustomerTag[]>>('/v1/crm/tags')),

  createTag: (payload: CustomerTagPayload): Promise<CustomerTag> =>
    unwrap(http.post<ApiResponse<CustomerTag>>('/v1/crm/tags', payload)),

  updateTag: (id: number, payload: CustomerTagPayload): Promise<CustomerTag> =>
    unwrap(http.put<ApiResponse<CustomerTag>>(`/v1/crm/tags/${id}`, payload)),

  deleteTag: (id: number): Promise<void> =>
    http.delete(`/v1/crm/tags/${id}`).then(() => undefined),

  relationTypes: (): Promise<RelationType[]> =>
    unwrap(http.get<ApiResponse<RelationType[]>>('/v1/crm/relation-types')),

  blacklistReasons: (): Promise<BlacklistReason[]> =>
    unwrap(http.get<ApiResponse<BlacklistReason[]>>('/v1/crm/blacklist-reasons')),

  preferenceDefinitions: (activeOnly = true): Promise<PreferenceDefinition[]> =>
    unwrap(
      http.get<ApiResponse<PreferenceDefinition[]>>('/v1/crm/preference-definitions', {
        params: { activeOnly },
      }),
    ),

  createPreferenceDefinition: (
    payload: PreferenceDefinitionPayload,
  ): Promise<PreferenceDefinition> =>
    unwrap(
      http.post<ApiResponse<PreferenceDefinition>>('/v1/crm/preference-definitions', payload),
    ),

  updatePreferenceDefinition: (
    id: number,
    payload: PreferenceDefinitionPayload,
  ): Promise<PreferenceDefinition> =>
    unwrap(
      http.put<ApiResponse<PreferenceDefinition>>(`/v1/crm/preference-definitions/${id}`, payload),
    ),

  deletePreferenceDefinition: (id: number): Promise<void> =>
    http.delete(`/v1/crm/preference-definitions/${id}`).then(() => undefined),

  duplicateRules: (): Promise<Record<string, boolean>> =>
    unwrap(http.get<ApiResponse<Record<string, boolean>>>('/v1/crm/duplicate-rules')),

  updateDuplicateRules: (
    rules: { identifier: string; enabled: boolean }[],
  ): Promise<Record<string, boolean>> =>
    unwrap(http.put<ApiResponse<Record<string, boolean>>>('/v1/crm/duplicate-rules', { rules })),

  segments: (): Promise<CrmSegment[]> =>
    unwrap(http.get<ApiResponse<CrmSegment[]>>('/v1/crm/segments')),

  createSegment: (payload: CrmSegmentPayload): Promise<CrmSegment> =>
    unwrap(http.post<ApiResponse<CrmSegment>>('/v1/crm/segments', payload)),

  updateSegment: (id: number, payload: CrmSegmentPayload): Promise<CrmSegment> =>
    unwrap(http.put<ApiResponse<CrmSegment>>(`/v1/crm/segments/${id}`, payload)),

  deleteSegment: (id: number): Promise<void> =>
    http.delete(`/v1/crm/segments/${id}`).then(() => undefined),
}
