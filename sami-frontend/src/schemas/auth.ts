import { z } from 'zod'
import type { TranslateFn } from '@/i18n'

/**
 * Zod schemas shared by the auth forms; the source of truth for client
 * validation. Each is a factory taking the component's `t` so validation
 * messages are localized (and re-created if the schema is rebuilt on a locale
 * change).
 */

export const loginSchema = (t: TranslateFn) =>
  z.object({
    email: z.string().min(1, t('validation.emailRequired')).email(t('validation.emailInvalid')),
    password: z.string().min(1, t('validation.passwordRequired')),
  })

export const registerSchema = (t: TranslateFn) =>
  z.object({
    fullName: z.string().min(1, t('validation.fullNameRequired')).max(120),
    email: z.string().min(1, t('validation.emailRequired')).email(t('validation.emailInvalid')),
    password: z
      .string()
      .min(8, t('validation.passwordMin'))
      .max(100, t('validation.passwordMax')),
  })

export const changePasswordSchema = (t: TranslateFn) =>
  z
    .object({
      currentPassword: z.string().min(1, t('validation.currentPasswordRequired')),
      newPassword: z
        .string()
        .min(8, t('validation.passwordMin'))
        .max(100, t('validation.passwordMax')),
      confirmPassword: z.string().min(1, t('validation.confirmPasswordRequired')),
    })
    .refine((values) => values.newPassword === values.confirmPassword, {
      message: t('validation.passwordsMismatch'),
      path: ['confirmPassword'],
    })

export const forgotPasswordSchema = (t: TranslateFn) =>
  z.object({
    email: z.string().min(1, t('validation.emailRequired')).email(t('validation.emailInvalid')),
  })

export const resetPasswordSchema = (t: TranslateFn) =>
  z
    .object({
      newPassword: z
        .string()
        .min(8, t('validation.passwordMin'))
        .max(100, t('validation.passwordMax')),
      confirmPassword: z.string().min(1, t('validation.confirmPasswordRequired')),
    })
    .refine((values) => values.newPassword === values.confirmPassword, {
      message: t('validation.passwordsMismatch'),
      path: ['confirmPassword'],
    })

export type LoginForm = z.infer<ReturnType<typeof loginSchema>>
export type RegisterForm = z.infer<ReturnType<typeof registerSchema>>
export type ChangePasswordForm = z.infer<ReturnType<typeof changePasswordSchema>>
export type ForgotPasswordForm = z.infer<ReturnType<typeof forgotPasswordSchema>>
export type ResetPasswordForm = z.infer<ReturnType<typeof resetPasswordSchema>>
