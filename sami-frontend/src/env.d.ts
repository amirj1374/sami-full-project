/// <reference types="vite/client" />

import type AppPersianDatePicker from './components/AppPersianDatePicker.vue'

declare module 'vue' {
  export interface GlobalComponents {
    AppPersianDatePicker: typeof AppPersianDatePicker
  }
}

interface ImportMetaEnv {
  readonly VITE_BUILD_BRANCH?: string
  readonly VITE_BUILD_COMMIT?: string
  readonly VITE_BUILD_TIMESTAMP?: string
  readonly VITE_APP_VERSION?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
