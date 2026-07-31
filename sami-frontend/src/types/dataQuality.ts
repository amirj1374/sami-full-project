export interface QualityRule {
  id: number
  code: string
  name: string
  description: string | null
  moduleCode: string
  entityCode: string
  category: string | null
  priority: number
  statusCode: string
  severityCode: string
  dimensionCode: string | null
  validationType: string
  targetField: string | null
  config: Record<string, unknown>
  conditionConfig: Record<string, unknown>
  weight: number
  createdAt: string
  version: number
}

export interface QualityRulePayload {
  code: string
  name: string
  description?: string
  moduleCode: string
  entityCode: string
  category?: string
  priority?: number
  statusCode?: string
  severityCode?: string
  dimensionCode?: string
  validationType: string
  targetField?: string
  config?: Record<string, unknown>
  conditionConfig?: Record<string, unknown>
  weight?: number
}

export interface QualityIssue {
  id: number
  ruleId: number
  moduleCode: string
  entityCode: string
  entityId: number | null
  fieldName: string | null
  severityCode: string
  dimensionCode: string | null
  message: string
  detail: Record<string, unknown>
  status: string
  resolutionNote: string | null
  resolvedAt: string | null
  resolvedByEmail: string | null
  createdAt: string
}

export interface QualityCatalog {
  validationTypes: string[]
  duplicateStrategies: string[]
  dimensions: string[]
  severities: string[]
  statuses: string[]
  scoreBands: string[]
  duplicateProviders: string[]
}

export type QualityIssueSummary = Record<string, number>
