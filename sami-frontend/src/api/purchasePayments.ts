import type { ApiResponse } from '@/types/api'
import { http, unwrap } from './http'

export type PaymentStatus='DRAFT'|'WAITING_MANAGER'|'APPROVED'|'WAITING_PAYMENT'|'PARTIALLY_PAID'|'PAID'|'REJECTED'|'OVERDUE'|'CANCELLED'
export interface PurchasePaymentRequest { id:number; request_number:string; requester_id:number; supplier_name?:string; purpose:string; requested_amount:number; paid_amount:number; currency_code:string; document_reference?:string; status:PaymentStatus; rejection_reason?:string; delay_reason?:string; created_at:string; receipts?:PurchasePaymentReceipt[] }
export interface PurchasePaymentReceipt { id:number; treasury_account_id:number; method:string; amount:number; paid_at:string; reference_number:string; receipt_file_id?:number }
export const purchasePaymentsApi={
 mine:()=>unwrap(http.get<ApiResponse<PurchasePaymentRequest[]>>('/v1/purchase-payment-requests/mine')),
 all:()=>unwrap(http.get<ApiResponse<PurchasePaymentRequest[]>>('/v1/purchase-payment-requests')),
 create:(p:{amount:number;purpose:string;supplierName?:string;documentReference?:string;attachmentFileId?:number})=>unwrap(http.post<ApiResponse<PurchasePaymentRequest>>('/v1/purchase-payment-requests',p)),
 decide:(id:number,p:{approved:boolean;rejectionReason?:string;accountantId?:number})=>unwrap(http.post<ApiResponse<PurchasePaymentRequest>>(`/v1/purchase-payment-requests/${id}/decision`,p)),
 pay:(id:number,p:{treasuryAccountId:number;method:string;amount:number;paidAt:string;referenceNumber:string;receiptFileId?:number;note?:string;completeWithPartialAmount:boolean})=>unwrap(http.post<ApiResponse<PurchasePaymentRequest>>(`/v1/purchase-payment-requests/${id}/payments`,p)),
 cancel:(id:number)=>unwrap(http.post<ApiResponse<PurchasePaymentRequest>>(`/v1/purchase-payment-requests/${id}/cancel`)),
 setLimit:(p:{treasuryAccountId:number;paymentDate:string;method:string;limitAmount:number})=>unwrap(http.put<ApiResponse<{availableAmount:number}>>('/v1/purchase-payment-requests/limits',p)),
}
