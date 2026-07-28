import { createI18n } from 'vue-i18n'
import { en as vuetifyEn, fa as vuetifyFa } from 'vuetify/locale'
import en from './locales/en.json'
import fa from './locales/fa.json'

/**
 * Application internationalization. English is the complete base catalogue;
 * Persian (fa) is RTL and falls back to English for any missing key. The active
 * locale is persisted and applied to `<html lang/dir>` so the whole app
 * (including Vuetify) switches direction.
 */
export type AppLocale = 'en' | 'fa'

/** Signature of a translate function, as consumed by the Zod schema factories. */
export type TranslateFn = (key: string, ...args: unknown[]) => string

const STORAGE_KEY = 'sami.locale'
const RTL_LOCALES: AppLocale[] = ['fa']

export const SUPPORTED_LOCALES: { code: AppLocale; name: string }[] = [
  { code: 'en', name: 'English' },
  { code: 'fa', name: 'فارسی' },
]

export const i18n = createI18n({
  legacy: false,
  locale: 'en',
  fallbackLocale: 'en',
  missingWarn: false,
  fallbackWarn: false,
  // Merge Vuetify's own component strings (pagination, data-table footer, clear,
  // no-data, …) under `$vuetify` so its built-ins localize with the app — using
  // Vuetify's official, complete Persian bundle.
  messages: {
    en: { ...en, $vuetify: vuetifyEn },
    fa: { ...fa, $vuetify: vuetifyFa },
  },
})

export function isRtl(locale: AppLocale): boolean {
  return RTL_LOCALES.includes(locale)
}

export function getStoredLocale(): AppLocale {
  const stored = localStorage.getItem(STORAGE_KEY)
  return stored === 'fa' || stored === 'en' ? stored : 'en'
}

/** Applies a locale everywhere: i18n, storage, and the document direction. */
export function setLocale(locale: AppLocale): void {
  // The composition-mode global exposes `locale` as a writable ref.
  ;(i18n.global.locale as unknown as { value: AppLocale }).value = locale
  localStorage.setItem(STORAGE_KEY, locale)
  const html = document.documentElement
  html.setAttribute('lang', locale)
  html.setAttribute('dir', isRtl(locale) ? 'rtl' : 'ltr')
}

/** Standalone translate — for use outside components (e.g. Zod schemas). */
export const t: TranslateFn = (key, ...args) =>
  (i18n.global.t as unknown as TranslateFn)(key, ...args)
