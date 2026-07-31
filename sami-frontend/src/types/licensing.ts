export interface LicenseRecord {
  id: number
  code: string
  typeCode: string
  tenantId: number
  tenantCode: string
  companyId: number | null
  statusCode: string
  statusName: string
  planCode: string
  expiryBehavior: string | null
  activationDate: string | null
  expirationDate: string | null
  graceDays: number
  activatedAt: string | null
  activationMode: string | null
  paymentStatus: string | null
  autoRenew: boolean
  emergencyUntil: string | null
  transferCount: number
  limitOverrides: Record<string, unknown>
  featureOverrides: Record<string, boolean>
  createdAt: string
  version: number
}

export interface TenantRecord {
  id: number
  code: string
  name: string
  description: string | null
  statusCode: string
  statusName: string
  contactEmail: string | null
  config: Record<string, unknown>
  activatedAt: string | null
  suspendedAt: string | null
  createdAt: string
  version: number
}

export interface LicenseFeature {
  id: number
  code: string
  name: string
  description: string | null
  moduleCode: string | null
  licenseRequired: boolean
  core: boolean
  active: boolean
  stateCode: string
  trialDays: number
  system: boolean
  dependencyCodes: string[]
  version: number
}

export interface LicensePlan {
  id: number
  code: string
  name: string
  description: string | null
  statusCode: string
  durationDays: number
  renewalPolicy: string
  isDefault: boolean
  billingCycleCode: string | null
  priceConfig: Record<string, unknown>
  featureCodes: string[]
  limits: Record<string, number>
  system: boolean
  version: number
}

export interface LicenseLookup {
  id: number
  code: string
  name: string
  extra: string | null
}

export interface LicenseCatalog {
  licenseStatuses: LicenseLookup[]
  tenantStatuses: LicenseLookup[]
  licenseTypes: LicenseLookup[]
  expiryBehaviors: LicenseLookup[]
  limitTypes: LicenseLookup[]
  paymentStatuses: LicenseLookup[]
  billingCycles: LicenseLookup[]
  featureStates: LicenseLookup[]
  activationModes: string[]
  meteredLimitTypes: string[]
  expiryHandlers: string[]
}

export interface LicensePayload {
  code: string
  licenseKey?: string
  typeCode: string
  tenantId: number
  companyId?: number
  planCode: string
  expiryBehaviorCode?: string
  graceDays?: number
  expirationDate?: string
  limitOverrides: Record<string, unknown>
}

export interface LicenseUpdatePayload {
  planCode: string
  expiryBehaviorCode?: string
  graceDays?: number
  expirationDate?: string
  autoRenew: boolean
  limitOverrides: Record<string, unknown>
  expectedVersion: number
}

export interface TenantPayload {
  code: string
  name: string
  description?: string
  contactEmail?: string
  config: Record<string, unknown>
}

export interface TenantUpdatePayload extends Omit<TenantPayload, 'code'> {
  expectedVersion: number
}

export interface PlanPayload {
  code: string
  name: string
  description?: string
  statusCode: string
  durationDays: number
  renewalPolicy: string
  billingCycleCode?: string
  defaultPlan: boolean
  priceConfig: Record<string, unknown>
  featureCodes: string[]
  limits: Record<string, number>
  expectedVersion?: number
}

export interface FeaturePayload {
  code: string
  name: string
  description?: string
  moduleCode?: string
  licenseRequired: boolean
  core: boolean
  active: boolean
  stateCode: string
  trialDays: number
  dependencyCodes: string[]
  expectedVersion?: number
}

export interface LicenseValidation {
  valid: boolean
  reason: string
  licenseCode: string | null
  status: string | null
  planCode: string | null
  tenantId: number | null
  expirationDate: string | null
  withinGrace: boolean
  features: string[]
}

export interface UsageCheck {
  limitType: string
  limit: number
  current: number
  unlimited: boolean
  allowed: boolean
}

export interface LicenseSummary {
  total: number
  byStatus: Record<string, number>
  byPlan: Record<string, number>
  byActivationMode: Record<string, number>
  autoRenew: number
}

export interface LicenseTransfer {
  fromTenantId: string
  toTenantId: number
  reason: string
  transferredAt: string
  by: string
}

export interface LicenseAuditRecord {
  id: number
  tenantId: number
  entityType: string
  entityId: number
  action: string
  oldValues: Record<string, unknown> | null
  newValues: Record<string, unknown> | null
  actorEmail: string | null
  createdAt: string
}

export type LicensingReportRow = Record<string, unknown>
