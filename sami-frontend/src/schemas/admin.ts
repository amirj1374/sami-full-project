import { z } from 'zod'
import type { TranslateFn } from '@/i18n'

/** Zod schemas for the role / module / permission admin forms. */

/** Lowercase slug used for module codes and permission actions. */
const slugPattern = /^[a-z][a-z0-9-]{1,63}$/

// --- Roles -----------------------------------------------------------------

/** Validation for the role create/edit form. */
export const roleSchema = (t: TranslateFn) =>
  z.object({
    name: z.string().min(1, t('validation.nameRequired')).max(100),
    description: z.string().max(500).optional().or(z.literal('')),
  })

/** Validation for the duplicate-role name prompt. */
export const duplicateRoleSchema = (t: TranslateFn) =>
  z.object({
    name: z.string().min(1, t('validation.nameRequired')).max(100),
  })

// --- Modules ---------------------------------------------------------------

/**
 * Validation for the module create/edit form. The edit dialog omits the code
 * field (it is immutable) and seeds it from the module being edited.
 */
export const moduleSchema = (t: TranslateFn) =>
  z.object({
    code: z.string().min(1, t('validation.codeRequired')).regex(slugPattern, t('validation.slug')),
    name: z.string().min(1, t('validation.nameRequired')).max(100),
    description: z.string().max(500).optional().or(z.literal('')),
    icon: z.string().min(1, t('validation.iconRequired')).max(100),
    path: z
      .string()
      .min(1, t('validation.pathRequired'))
      .max(255)
      .regex(/^\//, t('validation.pathSlash')),
    displayOrder: z.coerce
      .number({ invalid_type_error: t('validation.displayOrderNumber') })
      .int(t('validation.displayOrderInteger'))
      .min(0, t('validation.displayOrderMin')),
    enabled: z.boolean(),
    createDefaultPermissions: z.boolean(),
  })

// --- Permissions -----------------------------------------------------------

/**
 * Validation for the permission create/edit form. The edit dialog only submits
 * name/description (module and action are immutable) but keeps the immutable
 * fields seeded so the full schema still validates.
 */
export const permissionSchema = (t: TranslateFn) =>
  z.object({
    moduleId: z
      .number({ required_error: t('validation.moduleRequired') })
      .int()
      .positive(t('validation.moduleRequired')),
    action: z.string().min(1, t('validation.actionRequired')).regex(slugPattern, t('validation.slug')),
    name: z.string().min(1, t('validation.nameRequired')).max(100),
    description: z.string().max(500).optional().or(z.literal('')),
  })

export type RoleForm = z.infer<ReturnType<typeof roleSchema>>
export type DuplicateRoleForm = z.infer<ReturnType<typeof duplicateRoleSchema>>
export type ModuleForm = z.infer<ReturnType<typeof moduleSchema>>
export type PermissionForm = z.infer<ReturnType<typeof permissionSchema>>
