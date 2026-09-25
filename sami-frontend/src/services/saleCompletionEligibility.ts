import type { Sale, SalePayment } from '@/types/sales'

export interface SaleCompletionEligibility {
  canComplete: boolean
  paymentRequired: boolean
}

const MONEY_SCALE = 100

function minorUnits(value: number): number | null {
  if (!Number.isFinite(value)) return null
  const result = Math.round(value * MONEY_SCALE)
  return Number.isSafeInteger(result) ? result : null
}

/**
 * Mirrors the backend's completion precondition for presentation only.
 * The backend remains authoritative and revalidates the same rule on mutation.
 */
export function getSaleCompletionEligibility(
  sale: Pick<Sale, 'status' | 'finalAmount'> & { payments: Array<Pick<SalePayment, 'status' | 'amount'>> },
): SaleCompletionEligibility {
  const finalAmount = minorUnits(sale.finalAmount)
  let capturedPayments = 0

  for (const payment of sale.payments) {
    if (payment.status !== 'CAPTURED') continue
    const amount = minorUnits(payment.amount)
    if (amount === null) return { canComplete: false, paymentRequired: false }
    capturedPayments += amount
  }

  if (finalAmount === null || !Number.isSafeInteger(capturedPayments)) {
    return { canComplete: false, paymentRequired: false }
  }

  const isConfirmed = sale.status === 'CONFIRMED'
  return {
    canComplete: isConfirmed && capturedPayments === finalAmount,
    paymentRequired: isConfirmed && capturedPayments < finalAmount,
  }
}
