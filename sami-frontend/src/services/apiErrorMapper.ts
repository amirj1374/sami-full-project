import type { ApiError } from '@/types/api'

export type ApiErrorScope = 'default' | 'sales'

type ErrorTranslator = (key: string) => string
type MappableApiError = Pick<ApiError, 'code' | 'message'>

const salesBusinessErrorKeys: Readonly<Record<string, string>> = {
  'OPERATION_NOT_ALLOWED:Captured payments must equal the final amount': 'salesError.capturedPaymentsMustEqualFinal',
  'BAD_REQUEST:Payments cannot exceed the final amount': 'salesError.paymentsCannotExceedFinal',
  'OPERATION_NOT_ALLOWED:Sale cannot accept payments': 'salesError.saleCannotAcceptPayments',
  'OPERATION_NOT_ALLOWED:Only confirmed orders can be delivered': 'salesError.onlyConfirmedOrdersCanBeDelivered',
  'RESOURCE_CONFLICT:Delivery quantity exceeds the currently reserved quantity': 'salesError.deliveryQuantityExceedsReserved',
  'OPERATION_NOT_ALLOWED:Only confirmed orders can be invoiced': 'salesError.onlyConfirmedOrdersCanBeInvoiced',
  'RESOURCE_CONFLICT:Invoice quantity exceeds invoiceable delivered quantity': 'salesError.invoiceQuantityExceedsInvoiceable',
  'OPERATION_NOT_ALLOWED:Only draft invoices can be issued': 'salesError.onlyDraftInvoicesCanBeIssued',
  'OPERATION_NOT_ALLOWED:Accounting receivable posting is not available': 'salesError.accountingReceivableUnavailable',
  'OPERATION_NOT_ALLOWED:Only draft receipts can be allocated': 'salesError.onlyDraftReceiptsCanBeAllocated',
  'VALIDATION_FAILED:Allocation exceeds outstanding receipt or invoice': 'salesError.allocationExceedsOutstanding',
  'VALIDATION_FAILED:Receipt allocations are invalid': 'salesError.receiptAllocationsInvalid',
  'OPERATION_NOT_ALLOWED:Accounting settlement provider is unavailable': 'salesError.accountingSettlementUnavailable',
}

function errorMessageKey(error: MappableApiError, scope: ApiErrorScope): string {
  const code = error.code.trim().toUpperCase()
  if (scope === 'sales') {
    const businessKey = salesBusinessErrorKeys[`${code}:${error.message.trim()}`]
    if (businessKey) return businessKey
  }

  if (code.includes('NETWORK')) return 'apiError.network'
  if (code.includes('TIMEOUT')) return 'apiError.timeout'
  if (
    code.includes('UNAUTHORIZED') ||
    code.includes('UNAUTHENTICATED') ||
    code === 'INVALID_CREDENTIALS' ||
    code === 'INVALID_TOKEN' ||
    code === '401'
  ) return 'apiError.unauthorized'
  if (code.includes('FORBIDDEN') || code === 'ACCESS_DENIED' || code === '403') return 'apiError.forbidden'
  if (code.includes('NOT_FOUND') || code === '404') return 'apiError.notFound'
  if (code.includes('CONFLICT') || code.includes('VERSION') || code === '409') return 'apiError.conflict'
  if (
    code.includes('VALIDATION') ||
    code === 'BAD_REQUEST' ||
    code === '400' ||
    code === '422'
  ) return 'apiError.validation'
  if (code.includes('INTERNAL') || code.includes('SERVER') || /^5\d\d$/.test(code)) return 'apiError.server'
  if (scope === 'sales' && code === 'OPERATION_NOT_ALLOWED') return 'salesError.operationNotAllowed'
  return 'apiError.generic'
}

/**
 * Maps API failures to localized UI copy without exposing backend messages,
 * status codes, or technical error codes to the user.
 */
export function mapApiErrorMessage(
  error: MappableApiError,
  translate: ErrorTranslator,
  scope: ApiErrorScope = 'default',
): string {
  return translate(errorMessageKey(error, scope))
}
