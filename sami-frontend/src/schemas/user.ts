import { z } from 'zod'
import type { TranslateFn } from '@/i18n'

/**
 * Zod schema for the admin user form (create + edit share one shape so the
 * typed form stays uniform). Email/password only exist at creation; their
 * presence in create mode is enforced by the dialog on submit, their format
 * here. A factory taking `t` keeps validation messages localized.
 */
export const userFormSchema = (t: TranslateFn) =>
  z.object({
    email: z.string().email(t('validation.emailInvalid')).optional().or(z.literal('')),
    password: z
      .string()
      .min(8, t('validation.passwordMin'))
      .max(100, t('validation.passwordMax'))
      .optional()
      .or(z.literal('')),
    fullName: z.string().min(1, t('validation.fullNameRequired')).max(120),
    roleId: z
      .number({
        required_error: t('validation.roleRequired'),
        invalid_type_error: t('validation.roleRequired'),
      })
      .int()
      .positive(t('validation.roleRequired')),
    statusId: z
      .number({
        required_error: t('validation.statusRequired'),
        invalid_type_error: t('validation.statusRequired'),
      })
      .int()
      .positive(t('validation.statusRequired')),
  })

export type UserForm = z.infer<ReturnType<typeof userFormSchema>>
