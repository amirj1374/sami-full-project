/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Base URL for API calls. Defaults to "/api" (proxied in dev, same-origin in prod). */
  readonly VITE_API_BASE_URL: string
  /** Development Mock Mode: "true" starts MSW and runs the app with no backend. */
  readonly VITE_ENABLE_MOCK_MODE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
