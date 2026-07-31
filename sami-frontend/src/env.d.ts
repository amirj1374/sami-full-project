/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_BUILD_BRANCH?: string
  readonly VITE_BUILD_COMMIT?: string
  readonly VITE_BUILD_TIMESTAMP?: string
  readonly VITE_APP_VERSION?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
