export interface ScheduledJob {
  id: number
  code: string
  name: string
  description: string | null
  handlerKey: string
  handlerRegistered: boolean
  scheduleKind: string
  cronExpression: string | null
  intervalSeconds: number | null
  schedule: string
  timezone: string
  config: Record<string, unknown>
  statusCode: string
  statusName: string
  active: boolean
  nextRunAt: string | null
  lastRunAt: string | null
  lastStatus: string | null
  lastDurationMs: number | null
  consecutiveFailures: number
  maxFailures: number
  timeoutSeconds: number
  catchUp: boolean
  system: boolean
}

export interface SchedulerExecution {
  id: number
  executionNumber: string
  triggerKind: string
  status: string
  startedAt: string
  finishedAt: string | null
  durationMs: number | null
  outcome: string | null
  errorMessage: string | null
  itemsProcessed: number | null
  actorEmail: string | null
}

export interface SchedulerStatus {
  id: number
  code: string
  name: string
  allowsRun: boolean
  paused: boolean
  failed: boolean
}

export interface SchedulerHandler {
  key: string
  description: string
  inUse: boolean
}

export interface CreateScheduledJobPayload {
  code: string
  name: string
  description?: string
  handlerKey: string
  scheduleKind: string
  cronExpression?: string
  intervalSeconds?: number
  timezone?: string
  config: Record<string, unknown>
  timeoutSeconds?: number
  catchUp: boolean
  runAt?: string
}

export interface UpdateScheduledJobPayload {
  name: string
  description?: string
  cronExpression?: string
  intervalSeconds?: number
  timezone?: string
  config: Record<string, unknown>
  timeoutSeconds?: number
  maxFailures?: number
  catchUp: boolean
}
