import { useI18n } from 'vue-i18n'

/**
 * Locale-aware formatting helpers (dates, date-times, numbers). Uses the active
 * i18n locale so output follows the user's language; falls back gracefully on
 * null/invalid input.
 */
export function useFormat() {
  const { locale } = useI18n()

  const intlLocale = (): string => (locale.value === 'fa' ? 'fa-IR' : 'en-US')

  function formatDate(value: string | number | Date | null | undefined): string {
    if (value === null || value === undefined || value === '') return ''
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return String(value)
    return new Intl.DateTimeFormat(intlLocale(), { dateStyle: 'medium' }).format(date)
  }

  function formatDateTime(value: string | number | Date | null | undefined): string {
    if (value === null || value === undefined || value === '') return ''
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return String(value)
    return new Intl.DateTimeFormat(intlLocale(), { dateStyle: 'medium', timeStyle: 'short' }).format(
      date,
    )
  }

  function formatNumber(
    value: number | string | null | undefined,
    options: Intl.NumberFormatOptions = {},
  ): string {
    if (value === null || value === undefined || value === '') return ''
    const n = typeof value === 'string' ? Number(value) : value
    if (typeof n !== 'number' || Number.isNaN(n)) return String(value)
    return new Intl.NumberFormat(intlLocale(), options).format(n)
  }

  return { formatDate, formatDateTime, formatNumber }
}
