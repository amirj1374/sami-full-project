export type DeliveryStatus = 'DRAFT' | 'CONFIRMED' | 'CANCELLED'
export interface DeliveryLine { id:number; orderLineId:number; productId:number; name:string; orderedQuantity:number; reservedQuantity:number; previouslyDeliveredQuantity:number; deliveryQuantity:number; backorderedQuantity:number; remainingQuantity:number }
export interface Delivery { id:number; number:string; status:DeliveryStatus; orderId:number; companyId:number; branchId:number; customerId:number; contactId:number; notes?:string; lines:DeliveryLine[]; createdAt:string; confirmedAt?:string; version:number }
export interface DeliveryPayload { companyId:number; branchId:number; notes?:string; lines:Array<{orderLineId:number;quantity:number}> }
export interface DeliveryAudit { id:number; action:string; actorId?:number; actorEmail?:string; occurredAt:string }
