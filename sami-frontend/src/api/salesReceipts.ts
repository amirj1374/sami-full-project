import { http, unwrap } from './http'
import type { ApiResponse } from '@/types/api'
import type { SalesReceipt } from '@/types/salesReceipts'
export const salesReceiptsApi={create:(p:{companyId:number;branchId:number;customerId:number;amount:number;paymentMethod:string;reference?:string},key:string)=>unwrap(http.post<ApiResponse<SalesReceipt>>('/v1/sales-receipts',null,{params:p,headers:{'Idempotency-Key':key}})),allocate:(id:number,invoiceId:number,amount:number)=>unwrap(http.post<ApiResponse<void>>(`/v1/sales-receipts/${id}/allocations`,null,{params:{invoiceId,amount}})),confirm:(id:number,treasuryAccountId:number)=>unwrap(http.post<ApiResponse<SalesReceipt>>(`/v1/sales-receipts/${id}/confirm-now`,null,{params:{treasuryAccountId}}))}
