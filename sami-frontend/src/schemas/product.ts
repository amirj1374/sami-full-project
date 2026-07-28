import { z } from 'zod'
import type { TranslateFn } from '@/i18n'

/** Validation for the product create/edit form. */
export const productSchema = (t: TranslateFn) =>
  z.object({
    name: z.string().min(1, t('validation.nameRequired')).max(255),
    sku: z.string().min(1, t('validation.skuRequired')).max(64),
    description: z.string().max(2000).optional().or(z.literal('')),
    price: z.coerce
      .number({ invalid_type_error: t('validation.priceNumber') })
      .min(0, t('validation.priceMin')),
    stockQuantity: z.coerce
      .number({ invalid_type_error: t('validation.stockNumber') })
      .int(t('validation.stockInteger'))
      .min(0, t('validation.stockMin')),
    active: z.boolean(),
  })

export type ProductForm = z.infer<ReturnType<typeof productSchema>>
