import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import { createVuetify } from 'vuetify'
import { aliases, mdi } from 'vuetify/iconsets/mdi'
import { useI18n } from 'vue-i18n'
import type { I18n } from 'vue-i18n'
import { createVueI18nAdapter } from 'vuetify/locale/adapters/vue-i18n'
import { i18n } from '@/i18n'

/**
 * Vuetify instance with a light/dark theme pair. Components are auto-imported by
 * vite-plugin-vuetify, so we only configure theming, icons and locale here.
 *
 * Localization is delegated to vue-i18n through the official adapter, so
 * Vuetify's built-in component strings share the app's `fa`/`en` messages and
 * direction (RTL for Persian, LTR for English) follows the active locale
 * automatically.
 */
export const vuetify = createVuetify({
  locale: {
    // vue-i18n infers a strict message/locale schema; the adapter expects the
    // loose `I18n<any, …, string, false>` shape, so we widen it at the boundary.
    adapter: createVueI18nAdapter({
      i18n: i18n as unknown as I18n<Record<string, unknown>, NonNullable<unknown>, NonNullable<unknown>, string, false>,
      useI18n,
    }),
    rtl: {
      fa: true,
      en: false,
    },
  },
  theme: {
    defaultTheme: 'light',
    themes: {
      // Palette extracted from the showroom: navy authority, clean white
      // surfaces, cool-gray neutrals and a restrained brass accent.
      light: {
        dark: false,
        colors: {
          background: '#F6F7F9',
          surface: '#FFFFFF',
          'surface-light': '#EEF0F4',
          'surface-variant': '#E7EAF0',
          'on-surface-variant': '#5B6474',
          primary: '#26375F',
          'primary-darken-1': '#1C2A49',
          secondary: '#5A6B8C',
          accent: '#B0894F',
          info: '#37618E',
          success: '#2E7D5B',
          warning: '#B27A1B',
          error: '#C0453C',
          'on-surface': '#1B2333',
          'on-background': '#1B2333',
        },
        variables: {
          'border-color': '#2A3757',
          'border-opacity': 0.14,
          'high-emphasis-opacity': 0.87,
          'medium-emphasis-opacity': 0.6,
          'disabled-opacity': 0.38,
          'hover-opacity': 0.04,
          'focus-opacity': 0.1,
          'selected-opacity': 0.08,
          'activated-opacity': 0.1,
          // Soft, premium shadows (low alpha) applied to every elevation utility.
          'shadow-key-umbra-opacity': 0.06,
          'shadow-key-penumbra-opacity': 0.042,
          'shadow-key-ambient-opacity': 0.032,
        },
      },
      // Dark counterpart (same navy identity, lightened for contrast). Not the
      // default; available if a theme toggle is added later.
      dark: {
        dark: true,
        colors: {
          background: '#0E141F',
          surface: '#161D2B',
          'surface-light': '#1E2637',
          'surface-variant': '#232C3E',
          'on-surface-variant': '#A6B0C2',
          primary: '#7E9AD8',
          'primary-darken-1': '#5F7CBE',
          secondary: '#8493B4',
          accent: '#C9A76E',
          info: '#6E9AD0',
          success: '#4FB488',
          warning: '#D6A24A',
          error: '#E0685E',
          'on-surface': '#E5E9F0',
          'on-background': '#E5E9F0',
        },
        variables: {
          'border-color': '#C7D0E0',
          'border-opacity': 0.12,
          'high-emphasis-opacity': 0.9,
          'medium-emphasis-opacity': 0.64,
          'disabled-opacity': 0.38,
          'hover-opacity': 0.06,
          'focus-opacity': 0.12,
          'selected-opacity': 0.1,
          'activated-opacity': 0.12,
          'shadow-key-umbra-opacity': 0.28,
          'shadow-key-penumbra-opacity': 0.18,
          'shadow-key-ambient-opacity': 0.12,
        },
      },
    },
  },
  // Global component defaults — the single lever that modernizes buttons, cards,
  // forms, inputs, tables, chips, alerts, header and drawer app-wide without
  // touching individual screens. Explicit props on a component still override.
  defaults: {
    VBtn: { flat: true, class: 'text-none font-weight-medium' },
    VCard: { variant: 'flat', border: true, rounded: 'lg' },
    VSheet: { rounded: 'lg' },
    VTextField: { variant: 'outlined', density: 'comfortable', color: 'primary', hideDetails: 'auto' },
    VTextarea: { variant: 'outlined', density: 'comfortable', color: 'primary', hideDetails: 'auto' },
    VSelect: { variant: 'outlined', density: 'comfortable', color: 'primary', menuIcon: 'mdi-chevron-down', hideDetails: 'auto' },
    VAutocomplete: { variant: 'outlined', density: 'comfortable', color: 'primary', menuIcon: 'mdi-chevron-down', hideDetails: 'auto' },
    VCombobox: { variant: 'outlined', density: 'comfortable', color: 'primary', menuIcon: 'mdi-chevron-down', hideDetails: 'auto' },
    VFileInput: { variant: 'outlined', density: 'comfortable', color: 'primary', hideDetails: 'auto' },
    VChip: { label: true },
    VAlert: { variant: 'tonal', border: 'start', rounded: 'lg' },
    VList: { density: 'comfortable' },
    VDataTable: { hover: true },
    VDataTableServer: { hover: true },
    VAppBar: { flat: true, border: 'b' },
    VNavigationDrawer: { border: 'e' },
    VTab: { class: 'text-none' },
    VTooltip: { location: 'top' },
    VBreadcrumbs: { density: 'compact' },
    VSwitch: { color: 'primary', inset: true, hideDetails: 'auto' },
    VCheckbox: { color: 'primary', hideDetails: 'auto' },
    VRadioGroup: { color: 'primary', hideDetails: 'auto' },
    VProgressLinear: { color: 'primary', rounded: true },
    VPagination: { activeColor: 'primary', rounded: 'lg' },
  },
  icons: {
    defaultSet: 'mdi',
    aliases,
    sets: { mdi },
  },
})
