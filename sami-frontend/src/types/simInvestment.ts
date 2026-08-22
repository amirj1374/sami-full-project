import type { PageResponse } from './api'

export type SimNumberClass = 'ORDINARY' | 'SEMI_ROUND' | 'ROUND' | 'SPECIAL' | 'VIP'
export type AnalysisLevel = 'LOW' | 'MEDIUM' | 'HIGH'

export interface SimInvestmentOverview {
  analyzed_numbers: number
  activeListings: number
  portfolio_cost: number | string
  estimated_portfolio_value: number | string
  potential_portfolio_profit: number | string
  strong_opportunities: number
  high_liquidity: number
  low_confidence: number
  calculated_at?: string | null
  latestImport?: SimImportBatch | null
  classDistribution: Array<{ label: SimNumberClass; value: number }>
  conditionDistribution: Array<{ label: string; value: number }>
}

export interface SimAnalysis {
  id: number
  normalized_phone: string
  prefix: string
  number_class: SimNumberClass
  rondi_score: number
  rondi_patterns: string[]
  block_group?: string | null
  market_mean?: number | string | null
  market_median?: number | string | null
  market_floor?: number | string | null
  market_ceiling?: number | string | null
  best_buy_price?: number | string | null
  normal_buy_price?: number | string | null
  normal_sell_price?: number | string | null
  best_sell_price?: number | string | null
  sample_count: number
  confidence: AnalysisLevel
  liquidity: AnalysisLevel
  outlier: boolean
  trend_percent?: number | string | null
  actual_buy_price?: number | string | null
  actual_sell_price?: number | string | null
  potential_profit?: number | string | null
  investment_score: number
  last_observed_on: string
  current_asking_price?: number | string | null
  condition_code?: string
  seller_external_id?: string
  source_code?: string
}

export interface SimImportBatch {
  id: number
  source_code: string
  original_filename: string
  observed_on: string
  full_snapshot: boolean
  status: string
  source_row_count: number
  imported_count: number
  duplicate_count: number
  warning_count: number
  error_count: number
  started_at: string
  completed_at?: string | null
}

export interface SimImportMessage { severity: 'WARNING' | 'ERROR'; code: string; source_row_number?: number; message: string }
export type SimAnalysisPage = PageResponse<SimAnalysis>
