export type SaleStatus='DRAFT'|'CONFIRMED'|'COMPLETED'|'CANCELLED'|'PARTIALLY_RETURNED'|'RETURNED'
export interface SaleItem{ id:number;productId:number;sku:string;name:string;serialNumber?:string;imei?:string;quantity:number;returnedQuantity:number;unitPrice:number;costPrice:number;discount:number;tax:number;lineTotal:number;profit:number }
export interface SaleServiceLine{ id:number;serviceType:string;description?:string;price:number;cost:number;employeeId?:number }
export interface SalePayment{ id:number;method:string;status:string;amount:number;reversedAmount:number;referenceNo?:string;paidAt:string }
export interface Sale{ id:number;invoiceNumber:string;companyId:number;branchId:number;customerId:number;sellerId:number;saleType:string;status:SaleStatus;currency:string;subtotal:number;discountTotal:number;taxTotal:number;costTotal:number;finalAmount:number;profit:number;commissionAmount:number;notes?:string;items:SaleItem[];services:SaleServiceLine[];payments:SalePayment[];createdAt:string;confirmedAt?:string;completedAt?:string;cancelledAt?:string;version:number }
export interface SaleItemPayload{productId:number;quantity:number;unitPrice:number;costPrice?:number;discount?:number;tax?:number;serialNumber?:string;imei?:string}
export interface SaleServicePayload{serviceType:string;description?:string;price:number;cost?:number;employeeId?:number}
export interface SalePayload{companyId:number;branchId:number;customerId:number;saleType:string;currency?:string;notes?:string;items:SaleItemPayload[];services?:SaleServicePayload[];expectedVersion?:number;idempotencyKey?:string}
export interface SaleAudit{ id:number;action:string;actorId?:number;actorEmail?:string;oldValue?:Record<string,unknown>;newValue?:Record<string,unknown>;occurredAt:string }
export interface SaleDiscount{ id:number;type:string;amount:number;reason:string;status:string;requestedBy?:number;approvedBy?:number;requestedAt:string;decidedAt?:string }
export interface SalesDashboard{drafts:number;confirmed:number;completed:number;cancelled:number;revenue:number;profit:number}
export interface SalesFilter{from?:string;to?:string;branchId?:number;sellerId?:number;status?:string;saleType?:string}
export interface SalesMetricPoint{label:string;value:number;count:number}
export interface SalesReport{revenue:number;grossProfit:number;averageTicket:number;discountRate:number;returnRate:number;invoiceCount:number;lostSaleCount:number;dailySales:SalesMetricPoint[];paymentMethods:SalesMetricPoint[];topProducts:SalesMetricPoint[];topSellers:SalesMetricPoint[]}
export interface SaleAccountingEntry{id:number;entryType:string;accountCode:string;debit:number;credit:number;referenceNo:string;postedAt:string}
export interface LostSale{id:number;companyId:number;branchId:number;customerId?:number;productId?:number;sellerId:number;reasonCode:string;notes?:string;expectedAmount:number;occurredAt:string;createdAt:string}
export interface LostSalePayload{companyId:number;branchId:number;customerId?:number;productId?:number;reasonCode:string;notes?:string;expectedAmount?:number;occurredAt?:string}
