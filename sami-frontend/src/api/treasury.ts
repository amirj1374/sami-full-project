import type { ApiResponse, PageResponse } from '@/types/api'
import { http, unwrap } from './http'

export interface TreasuryAccount { id:number; accountTypeId:number; accountTypeCode:string; accountTypeName:string; code:string; name:string; currencyCode:string; openingBalance:number; currentBalance:number; allowNegativeBalance:boolean; responsibleUserId?:number; bankName?:string; bankBranch?:string; iban?:string; accountNumber?:string; cardNumber?:string; accountHolder?:string; description?:string; active:boolean; version:number }
export interface TreasuryAccountType { id:number; code:string; name:string; requiresBankDetails:boolean; allowsNegativeBalance:boolean; active:boolean; displayOrder:number }
export interface TreasuryTransactionType { id:number; code:string; name:string; direction:'INFLOW'|'OUTFLOW'|'TRANSFER'; active:boolean; displayOrder:number }
export interface TreasuryTransaction { id:number; number:string; typeId:number; typeCode:string; typeName:string; direction:string; statusId:number; statusCode:string; statusName:string; categoryId?:number; categoryName?:string; sourceAccountId?:number; sourceAccountName?:string; destinationAccountId?:number; destinationAccountName?:string; amount:number; currencyCode:string; occurredAt:string; referenceModule?:string; referenceNumber?:string; description?:string; completedAt?:string; version:number }
export interface TreasuryCheque { id:number; direction:'RECEIVED'|'ISSUED'; chequeNumber:string; bankName:string; bankBranch?:string; amount:number; currencyCode:string; ownerName?:string; recipientName?:string; issueDate?:string; dueDate?:string; statusId:number; statusCode:string; statusName:string; treasuryAccountId?:number; transactionId?:number; description?:string; version:number }
export interface TreasuryDashboard { accounts:number; transactions:number; cheques:number; totalBalance:number; currencyCode:string }
export interface TreasuryAccountPayload { accountTypeId:number; code:string; name:string; currencyCode?:string; openingBalance:number; allowNegativeBalance:boolean; responsibleUserId?:number; bankName?:string; bankBranch?:string; iban?:string; accountNumber?:string; cardNumber?:string; accountHolder?:string; description?:string; active:boolean }
export interface TreasuryTransactionPayload { transactionTypeId:number; categoryId?:number; sourceAccountId?:number; destinationAccountId?:number; amount:number; currencyCode?:string; occurredAt:string; referenceModule?:string; referenceNumber?:string; description?:string }
export interface TreasuryChequePayload { direction:'RECEIVED'|'ISSUED'; chequeNumber:string; bankName:string; bankBranch?:string; amount:number; currencyCode?:string; ownerName?:string; recipientName?:string; issueDate?:string; dueDate?:string; statusId:number; treasuryAccountId?:number; transactionId?:number; imageFileId?:number; description?:string }

export const treasuryApi = {
 dashboard: () => unwrap(http.get<ApiResponse<TreasuryDashboard>>('/v1/treasury/dashboard')),
 accounts: () => unwrap(http.get<ApiResponse<TreasuryAccount[]>>('/v1/treasury/accounts')),
 accountTypes: () => unwrap(http.get<ApiResponse<TreasuryAccountType[]>>('/v1/treasury/account-types')),
 createAccount: (payload:TreasuryAccountPayload) => unwrap(http.post<ApiResponse<TreasuryAccount>>('/v1/treasury/accounts',payload)),
 updateAccount: (id:number,payload:TreasuryAccountPayload) => unwrap(http.put<ApiResponse<TreasuryAccount>>(`/v1/treasury/accounts/${id}`,payload)),
 transactionTypes: () => unwrap(http.get<ApiResponse<TreasuryTransactionType[]>>('/v1/treasury/transaction-types')),
 transactions: (page=0,size=20) => unwrap(http.get<ApiResponse<PageResponse<TreasuryTransaction>>>('/v1/treasury/transactions',{params:{page,size}})),
 createTransaction: (payload:TreasuryTransactionPayload) => unwrap(http.post<ApiResponse<TreasuryTransaction>>('/v1/treasury/transactions',payload)),
 submit: (id:number) => unwrap(http.post<ApiResponse<TreasuryTransaction>>(`/v1/treasury/transactions/${id}/submit`)),
 approve: (id:number) => unwrap(http.post<ApiResponse<TreasuryTransaction>>(`/v1/treasury/transactions/${id}/approve`)),
 complete: (id:number) => unwrap(http.post<ApiResponse<TreasuryTransaction>>(`/v1/treasury/transactions/${id}/complete`)),
 cancel: (id:number) => unwrap(http.post<ApiResponse<TreasuryTransaction>>(`/v1/treasury/transactions/${id}/cancel`)),
 chequeStatuses: () => unwrap(http.get<ApiResponse<Array<{id:number;code:string;name:string;terminal:boolean;active:boolean;displayOrder:number}>>>('/v1/treasury/cheque-statuses')),
 cheques: (page=0,size=20) => unwrap(http.get<ApiResponse<PageResponse<TreasuryCheque>>>('/v1/treasury/cheques',{params:{page,size}})),
 createCheque: (payload:TreasuryChequePayload) => unwrap(http.post<ApiResponse<TreasuryCheque>>('/v1/treasury/cheques',payload)),
 updateChequeStatus: (id:number,statusId:number) => unwrap(http.post<ApiResponse<TreasuryCheque>>(`/v1/treasury/cheques/${id}/status`,{statusId})),
}
