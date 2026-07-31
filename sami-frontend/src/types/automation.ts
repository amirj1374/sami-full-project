export interface AutomationAction {
  id?: number
  stepOrder: number
  actionType: string
  name?: string | null
  config: Record<string, unknown>
  stepCondition: Record<string, unknown>
  runMode?: string | null
  continueOnError: boolean
  delaySeconds: number
  retryCount: number
  timeoutSeconds?: number | null
}

export interface AutomationRule {
  id: number
  code: string
  name: string
  description: string | null
  category: string | null
  priority: number
  companyId: number | null
  branchId: number | null
  statusCode: string
  statusName: string
  triggerType: string
  triggerConfig: Record<string, unknown>
  conditionConfig: Record<string, unknown>
  executionPolicy: Record<string, unknown>
  allowRecursion: boolean
  maxExecutions: number | null
  executionCount: number
  actions: AutomationAction[]
  createdAt: string
  updatedAt: string
  version: number
}

export interface AutomationRulePayload {
  code: string
  name: string
  description?: string
  category?: string
  priority?: number
  statusCode: string
  triggerType: string
  triggerConfig: Record<string, unknown>
  conditionConfig: Record<string, unknown>
  executionPolicy: Record<string, unknown>
  allowRecursion: boolean
  maxExecutions?: number
  actions: AutomationAction[]
  expectedVersion?: number
}

export interface AutomationRunPayload {
  entityType?: string
  entityId?: number
  data: Record<string, unknown>
}

export interface AutomationExecution {
  id: number
  executionNumber: string
  ruleId: number
  triggerType: string
  triggerRef: string | null
  entityType: string | null
  entityId: number | null
  status: string
  result: Record<string, unknown> | null
  error: string | null
  startedAt: string
  endedAt: string | null
  durationMs: number | null
  executedByEmail: string | null
}

export interface AutomationExecutionLog {
  id: number
  stepOrder: number
  actionType: string
  status: string
  message: string | null
  detail: Record<string, unknown> | null
  occurredAt: string
}

export interface AutomationStatus {
  id: number
  code: string
  name: string
  isActiveState: boolean
  isArchivedState: boolean
  isDefault: boolean
  displayOrder: number
}

export interface AutomationTrigger {
  type: string
  label: string
  category: string
}

export interface AutomationActionDescriptor {
  type: string
  label: string
}
