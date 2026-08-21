export interface LegacyBatch {
  id: number
  source_system: 'ASAN'
  original_filename: string
  sha256: string
  parser_version?: string
  status: 'UPLOADED' | 'ANALYZING' | 'READY' | 'IMPORTING' | 'COMPLETED' | 'COMPLETED_WITH_WARNINGS' | 'FAILED'
  dataset_count: number
  record_count: number
  warning_count: number
  error_count: number
  created_at: string
  migration_group_id?: number | null
  evidence_type?: 'ASAN_BACKUP' | 'ASAN_EXCEL_REPORT'
}

export interface LegacyMigrationGroup {
  id: number
  name: string
  status: 'DRAFT' | 'ACTIVE' | 'RECONCILING' | 'READY_FOR_ACCEPTANCE' | 'ACCEPTED' | 'BLOCKED'
  acceptance_status: 'PENDING' | 'PASS' | 'WARNING' | 'FAIL' | 'BLOCKED'
  created_at: string
  updated_at: string
}

export interface LegacyAcceptanceCheck {
  id: number
  check_code: string
  status: 'PASS' | 'FAIL' | 'WARNING' | 'PENDING'
  evidence: Record<string, unknown>
}

export interface LegacyReconciliationException {
  id: number
  domain: string
  source_key: string
  classification: string
  explanation?: string
  approval_status: 'NEEDS_INVESTIGATION' | 'EXPLAINED' | 'ACCEPTED' | 'REJECTED'
  difference_value: Record<string, unknown>
}

export interface LegacyReconciliation {
  id: number
  status: string
  acceptance_status: string
  checks: LegacyAcceptanceCheck[]
  exceptions: LegacyReconciliationException[]
  batches: LegacyBatch[]
  journalRows?: number
  trialBalanceRows?: number
  chequeRows?: number
}

export interface LegacyDataset {
  id: number
  dataset_key: string
  source_table: string
  semantic_type: string
  support_status: 'SUPPORTED' | 'PARTIAL' | 'UNSUPPORTED'
  source_record_count: number
  imported_record_count: number
  field_dictionary: Array<Record<string, unknown>>
  metadata: Record<string, unknown>
}
