import { z } from 'zod'
import type { TranslateFn } from '@/i18n'

export const warehouseSchema = (t: TranslateFn) => z.object({
  code: z.string().min(2, t('inventory.validation.code')).max(64),
  name: z.string().min(1, t('inventory.validation.name')).max(100),
  warehouseType: z.enum(['STANDARD', 'RETAIL', 'TRANSIT', 'QUARANTINE', 'RETURNS']),
})

export const adjustmentSchema = (t: TranslateFn) => z.object({
  warehouseId: z.number().positive(t('inventory.validation.warehouse')),
  reason: z.string().min(1, t('inventory.validation.reason')).max(500),
  lines: z.array(z.object({
    productId: z.number().positive(t('inventory.validation.product')),
    quantity: z.number().refine((value) => value !== 0, t('inventory.validation.quantity')),
    unitCost: z.number().min(0).optional(),
  })).min(1, t('inventory.validation.lines')),
})

export const transferSchema = (t: TranslateFn) => z.object({
  fromWarehouseId: z.number().positive(t('inventory.validation.warehouse')),
  toWarehouseId: z.number().positive(t('inventory.validation.warehouse')),
  lines: z.array(z.object({
    productId: z.number().positive(t('inventory.validation.product')),
    quantity: z.number().positive(t('inventory.validation.quantity')),
  })).min(1, t('inventory.validation.lines')),
}).refine((value) => value.fromWarehouseId !== value.toWarehouseId, {
  message: t('inventory.validation.distinctWarehouses'),
  path: ['toWarehouseId'],
})
