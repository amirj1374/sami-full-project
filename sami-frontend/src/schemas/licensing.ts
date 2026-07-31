import { z } from 'zod'
import type { TranslateFn } from '@/i18n'
export const licenseSchema=(t:TranslateFn)=>z.object({ code:z.string().trim().regex(/^[A-Za-z0-9._-]{2,64}$/,t('licensing.validation.code')), typeCode:z.string().min(1,t('validation.required')), tenantId:z.number({message:t('validation.required')}).positive(), planCode:z.string().min(1,t('validation.required')) })
