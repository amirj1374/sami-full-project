import { http, unwrap } from './http'
import type { ApiResponse } from '@/types/api'
import type { SalesReceipt } from '@/types/salesReceipts'
export const salesReceiptsApi={confirm:(id:number,treasuryAccountId:number)=>unwrap(http.post<ApiResponse<SalesReceipt>>(`/v1/sales-receipts/${id}/confirm-now`,null,{params:{treasuryAccountId}}))}
