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
          background: '#F3F5F8',
          surface: '#FFFFFF',
          'surface-bright': '#FFFFFF',
          'surface-container': '#EEF1F5',
          'surface-container-high': '#E5E9F0',
          'surface-light': '#F7F8FA',
          'surface-variant': '#E8ECF2',
          'on-surface-variant': '#5D6879',
          primary: '#19345A',
          'primary-darken-1': '#102744',
          secondary: '#53647C',
          accent: '#B88746',
          info: '#326B9B',
          success: '#227A5A',
          warning: '#A96E13',
          error: '#BE3F3A',
          'on-surface': '#172235',
          'on-background': '#172235',
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
          background: '#0B111B',
          surface: '#131C2A',
          'surface-bright': '#1B2637',
          'surface-container': '#172131',
          'surface-container-high': '#1D293B',
          'surface-light': '#202C3E',
          'surface-variant': '#243044',
          'on-surface-variant': '#A8B3C4',
          primary: '#8CACDD',
          'primary-darken-1': '#6E91C7',
          secondary: '#91A0B9',
          accent: '#D1A768',
          info: '#77A8D4',
          success: '#53B58B',
          warning: '#D7A249',
          error: '#E36B64',
          'on-surface': '#EEF2F7',
          'on-background': '#EEF2F7',
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
    VBtn: { flat: true, class: 'text-none font-weight-semibold', rounded: 'lg' },
    VCard: { variant: 'flat', border: true, rounded: 'xl' },
    VSheet: { rounded: 'lg' },
    // Form controls share one compact, touch-safe rhythm. `active` keeps labels
    // visible before interaction so mobile users never lose field context.
    VTextField: { variant: 'outlined', density: 'compact', color: 'primary', hideDetails: 'auto', rounded: 'lg', active: true },
    VTextarea: { variant: 'outlined', density: 'compact', color: 'primary', hideDetails: 'auto', rounded: 'lg', active: true },
    VSelect: { variant: 'outlined', density: 'compact', color: 'primary', menuIcon: 'mdi-chevron-down', hideDetails: 'auto', rounded: 'lg', active: true },
    VAutocomplete: { variant: 'outlined', density: 'compact', color: 'primary', menuIcon: 'mdi-chevron-down', hideDetails: 'auto', rounded: 'lg', active: true },
    VCombobox: { variant: 'outlined', density: 'compact', color: 'primary', menuIcon: 'mdi-chevron-down', hideDetails: 'auto', rounded: 'lg', active: true },
    VFileInput: { variant: 'outlined', density: 'compact', color: 'primary', hideDetails: 'auto', rounded: 'lg', active: true },
    VChip: { label: true, rounded: 'lg' },
    VAlert: { variant: 'tonal', border: 'start', rounded: 'lg' },
    VList: { density: 'comfortable' },
    VListItem: { rounded: 'lg' },
    VDataTable: { hover: true },
    VDataTableServer: { hover: true },
    VDialog: { scrollable: true },
    VMenu: { offset: 6 },
    VSnackbar: { rounded: 'lg', location: 'bottom end' },
    VExpansionPanel: { rounded: 'lg' },
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
