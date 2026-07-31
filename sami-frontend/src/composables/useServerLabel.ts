import { useI18n } from 'vue-i18n'

/**
 * Translates labels that arrive from the backend as English codes/names into the
 * active locale.
 *
 * - Modules are keyed by their `code` under `server.module.*` (menu / breadcrumb).
 * - Every other backend display value (workflow statuses, customer/supplier
 *   types, sources, tags, categories, payment terms, warehouses, roles) is keyed
 *   by its English name under `server.label.*`.
 *
 * Falls back to the original value when no translation exists, so admin-created
 * custom values still render (untranslated) rather than breaking. This is how
 * Persian mode avoids showing the seeded backend English.
 */
export function useServerLabel() {
  const { t, te } = useI18n()

  const moduleLabel = (code?: string | null, fallback?: string): string => {
    if (!code) return fallback ?? ''
    const key = `server.module.${code}`
    return te(key) ? t(key) : (fallback ?? code)
  }

  /** Generic translator for any backend display name (status, type, tag, …). */
  const text = (name?: string | null): string => {
    if (!name) return ''
    const key = `server.label.${name}`
    return te(key) ? t(key) : name
  }

  const permissionLabel = (code?: string | null, fallback?: string): string => {
    if (!code) return fallback ?? ''
    const [moduleCode, actionCode] = code.split(':')
    if (!moduleCode || !actionCode) return fallback ?? code
    const actionKey = `server.action.${actionCode}`
    const action = te(actionKey) ? t(actionKey) : (fallback ?? actionCode)
    return `${moduleLabel(moduleCode, moduleCode)} — ${action}`
  }

  const actionLabel = (actionCode?: string | null): string => {
    if (!actionCode) return ''
    const key = `server.action.${actionCode}`
    return te(key) ? t(key) : actionCode
  }

  const enumLabel = (value?: string | null): string => {
    if (!value) return ''
    const key = `server.enum.${value}`
    return te(key) ? t(key) : text(value)
  }

  return {
    moduleLabel,
    text,
    statusLabel: text,
    roleLabel: text,
    typeLabel: text,
    lookupLabel: text,
    permissionLabel,
    actionLabel,
    enumLabel,
  }
}
