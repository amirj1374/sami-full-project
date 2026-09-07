import { http, unwrap } from './http'
import type { ApiResponse } from '@/types/api'
import type { SalesInvoice, SalesInvoicePayload, InvoiceableLine, SalesInvoiceAudit } from '@/types/salesInvoices'
export const salesInvoicesApi={
 list:(companyId:number,branchId:number)=>unwrap(http.get<ApiResponse<SalesInvoice[]>>('/v1/sales-invoices',{params:{companyId,branchId}})),
 invoiceable:(orderId:number,companyId:number,branchId:number)=>unwrap(http.get<ApiResponse<InvoiceableLine[]>>(`/v1/sales-invoices/invoiceable/${orderId}`,{params:{companyId,branchId}})),
 get:(id:number)=>unwrap(http.get<ApiResponse<SalesInvoice>>(`/v1/sales-invoices/${id}`)),
 create:(p:SalesInvoicePayload)=>unwrap(http.post<ApiResponse<SalesInvoice>>('/v1/sales-invoices',p)),
 update:(id:number,p:SalesInvoicePayload)=>unwrap(http.put<ApiResponse<SalesInvoice>>(`/v1/sales-invoices/${id}`,p)),
 issue:(id:number)=>unwrap(http.post<ApiResponse<SalesInvoice>>(`/v1/sales-invoices/${id}/issue`)),
 cancel:(id:number)=>unwrap(http.post<ApiResponse<SalesInvoice>>(`/v1/sales-invoices/${id}/cancel`)),
 audit:(id:number)=>unwrap(http.get<ApiResponse<SalesInvoiceAudit[]>>(`/v1/sales-invoices/${id}/audit`))
}
