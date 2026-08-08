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
}
