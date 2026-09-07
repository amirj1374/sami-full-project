export type SalesDocumentType = 'QUOTATION' | 'PROFORMA'
export type SalesDocumentStatus = 'DRAFT' | 'ISSUED' | 'CANCELLED' | 'EXPIRED'
export interface SalesDocumentLine { id:number; productId:number; sku:string; name:string; quantity:number; unitPrice:number; discount:number; lineTotal:number }
export interface SalesDocument { id:number; number:string; documentType:SalesDocumentType; status:SalesDocumentStatus; companyId:number; branchId:number; customerId:number; contactId:number; currency:string; subtotal:number; discountTotal:number; finalAmount:number; notes?:string; lines:SalesDocumentLine[]; createdAt:string; issuedAt?:string; version:number }
export interface SalesDocumentPayload { companyId:number; branchId:number; customerId:number; documentType:SalesDocumentType; currency?:string; notes?:string; lines:Array<{productId:number;quantity:number;unitPrice:number;discount?:number}>; expectedVersion?:number }
export interface SalesDocumentAudit { id:number; action:string; actorId?:number; actorEmail?:string; occurredAt:string }
