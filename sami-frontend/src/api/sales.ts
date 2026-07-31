import type{ApiResponse,PageQuery,PageResponse}from '@/types/api';import type{Sale,SaleAudit,SaleDiscount,SalePayload,SalesDashboard}from '@/types/sales';import{http,unwrap}from './http'
export const salesApi={
 list:(params:PageQuery={}):Promise<PageResponse<Sale>>=>unwrap(http.get<ApiResponse<PageResponse<Sale>>>('/v1/sales',{params})),
 get:(id:number):Promise<Sale>=>unwrap(http.get<ApiResponse<Sale>>(`/v1/sales/${id}`)),
 create:(p:SalePayload):Promise<Sale>=>unwrap(http.post<ApiResponse<Sale>>('/v1/sales',p)),
 update:(id:number,p:SalePayload):Promise<Sale>=>unwrap(http.put<ApiResponse<Sale>>(`/v1/sales/${id}`,p)),
 confirm:(id:number):Promise<Sale>=>unwrap(http.post<ApiResponse<Sale>>(`/v1/sales/${id}/confirm`)),
 complete:(id:number):Promise<Sale>=>unwrap(http.post<ApiResponse<Sale>>(`/v1/sales/${id}/complete`)),
 cancel:(id:number,reason:string):Promise<Sale>=>unwrap(http.post<ApiResponse<Sale>>(`/v1/sales/${id}/cancel`,{reason})),
 addPayment:(id:number,p:{method:string;amount:number;referenceNo?:string}):Promise<Sale>=>unwrap(http.post<ApiResponse<Sale>>(`/v1/sales/${id}/payments`,p)),
 requestDiscount:(id:number,p:{type:string;amount:number;reason:string}):Promise<Sale>=>unwrap(http.post<ApiResponse<Sale>>(`/v1/sales/${id}/discounts`,p)),
 discounts:(id:number):Promise<SaleDiscount[]>=>unwrap(http.get<ApiResponse<SaleDiscount[]>>(`/v1/sales/${id}/discounts`)),
 decideDiscount:(id:number,discountId:number,approved:boolean):Promise<Sale>=>unwrap(http.post<ApiResponse<Sale>>(`/v1/sales/${id}/discounts/${discountId}/decision`,{approved})),
 returnSale:(id:number,p:{reason:string;refundMethod:string;items:{saleItemId:number;quantity:number}[]}):Promise<Sale>=>unwrap(http.post<ApiResponse<Sale>>(`/v1/sales/${id}/return`,p)),
 audit:(id:number):Promise<SaleAudit[]>=>unwrap(http.get<ApiResponse<SaleAudit[]>>(`/v1/sales/${id}/audit`)),
 dashboard:():Promise<SalesDashboard>=>unwrap(http.get<ApiResponse<SalesDashboard>>('/v1/sales/dashboard')),
 exportCsv:async():Promise<void>=>{const response=await http.get<Blob>('/v1/sales/reports/export.csv',{responseType:'blob'});const url=URL.createObjectURL(response.data);const anchor=document.createElement('a');anchor.href=url;anchor.download='sales-report.csv';anchor.click();URL.revokeObjectURL(url)},
}
