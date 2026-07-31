import { z } from 'zod'
import type { TranslateFn } from '@/i18n'

export const automationRuleSchema = (t: TranslateFn) =>
  z.object({
    code: z.string().trim().regex(/^[a-z][a-z0-9-]{1,63}$/, t('automation.validation.code')),
    name: z.string().trim().min(1, t('validation.required')),
    statusCode: z.string().min(1, t('validation.required')),
    triggerType: z.string().min(1, t('validation.required')),
  })
