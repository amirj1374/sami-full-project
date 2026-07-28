import type { ApiResponse } from '@/types/api'
import type {
  BlacklistReason,
  CustomerSource,
  CustomerStatus,
  CustomerTag,
  CustomerType,
  PreferenceDefinition,
  RelationType,
} from '@/types/models'
import { http, unwrap } from './http'

/** The CRM's configurable lookup data (types, statuses, sources, tags, rules). */
export const crmConfigApi = {
  types: (): Promise<CustomerType[]> =>
    unwrap(http.get<ApiResponse<CustomerType[]>>('/v1/crm/types')),

  statuses: (): Promise<CustomerStatus[]> =>
    unwrap(http.get<ApiResponse<CustomerStatus[]>>('/v1/crm/statuses')),

  sources: (): Promise<CustomerSource[]> =>
    unwrap(http.get<ApiResponse<CustomerSource[]>>('/v1/crm/sources')),

  tags: (): Promise<CustomerTag[]> =>
    unwrap(http.get<ApiResponse<CustomerTag[]>>('/v1/crm/tags')),

  createTag: (payload: { name: string; color?: string; active: boolean }): Promise<CustomerTag> =>
    unwrap(http.post<ApiResponse<CustomerTag>>('/v1/crm/tags', payload)),

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

  duplicateRules: (): Promise<Record<string, boolean>> =>
    unwrap(http.get<ApiResponse<Record<string, boolean>>>('/v1/crm/duplicate-rules')),

  updateDuplicateRules: (
    rules: { identifier: string; enabled: boolean }[],
  ): Promise<Record<string, boolean>> =>
    unwrap(http.put<ApiResponse<Record<string, boolean>>>('/v1/crm/duplicate-rules', { rules })),
}
