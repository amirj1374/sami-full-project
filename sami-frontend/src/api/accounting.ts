import { http, unwrap } from './http'
import type { ApiResponse } from '@/types/api'

export interface JournalReportRow { id:number; posting_reference:string; description:string|null; currency_code:string; created_at:string; debit_total:number|string; credit_total:number|string }
export const accountingApi = { journals: (limit=50):Promise<JournalReportRow[]> => unwrap(http.get<ApiResponse<JournalReportRow[]>>('/v1/accounting/reports/journals',{params:{limit}})) }
