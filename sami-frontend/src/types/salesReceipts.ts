export type ReceiptStatus='DRAFT'|'CONFIRMED'|'CANCELLED'
export interface SalesReceipt { id:number; receiptNumber:string; customerId:number; amount:number; paymentMethod:string; status:ReceiptStatus; treasuryTransactionId?:number; accountingPostingReference?:string }
