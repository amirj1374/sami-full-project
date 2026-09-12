import { http, unwrap } from './http'
export interface PurchaseOrderLine { productId:number; quantity:number; unitPrice:number; lineTotal?:number }
export interface PurchaseOrder { id:number; order_number:string; status:string; supplier_id:number; company_id:number; branch_id:number; subtotal:number; total:number; lines?:PurchaseOrderLine[] }
export const purchaseOrdersApi = {
  list: () => unwrap<PurchaseOrder[]>(http.get('/v1/purchase-orders')),
  get: (id:number) => unwrap<PurchaseOrder>(http.get(`/v1/purchase-orders/${id}`)),
  create: (companyId:number, branchId:number, supplierId:number, lines:PurchaseOrderLine[], notes?:string) => unwrap<PurchaseOrder>(http.post('/v1/purchase-orders', lines, { params:{companyId,branchId,supplierId,notes} })),
  submit: (id:number) => unwrap(http.post(`/v1/purchase-orders/${id}/submit`)),
  approve: (id:number) => unwrap(http.post(`/v1/purchase-orders/${id}/approve`)),
  cancel: (id:number) => unwrap(http.post(`/v1/purchase-orders/${id}/cancel`)),
}
