export type SalesInvoiceStatus='DRAFT'|'ISSUED'|'CANCELLED'
export interface SalesInvoiceLine { id:number; deliveryLineId:number; orderLineId:number; productId:number; name:string; quantity:number; unitPrice:number; discount:number; lineTotal:number }
export interface InvoiceableLine { deliveryLineId:number; orderLineId:number; productId:number; name:string; orderedQuantity:number; deliveredQuantity:number; issuedQuantity:number; invoiceableQuantity:number; unitPrice:number; discount:number }
export interface SalesInvoice { id:number; number:string; orderId:number; deliveryId?:number; companyId:number; branchId:number; customerId:number; status:SalesInvoiceStatus; currency:string; subtotal:number; discountTotal:number; finalAmount:number; receivablePostingKey?:string; lines:SalesInvoiceLine[] }
export interface SalesInvoicePayload { orderId:number; companyId:number; branchId:number; currency?:string; notes?:string; lines:{deliveryLineId:number;quantity:number}[] }
export interface SalesInvoiceAudit { id:number; action:string; occurredAt:string }
