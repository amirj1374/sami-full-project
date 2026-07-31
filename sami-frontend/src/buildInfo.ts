export interface BuildInfo {
  version: string
  branch: string
  commit: string
  timestamp: string
}

export const buildInfo: Readonly<BuildInfo> = Object.freeze({
  version: import.meta.env.VITE_APP_VERSION || '0.1.0',
  branch: import.meta.env.VITE_BUILD_BRANCH || 'local',
  commit: import.meta.env.VITE_BUILD_COMMIT || 'unknown',
  timestamp: import.meta.env.VITE_BUILD_TIMESTAMP || 'unknown',
})

declare global {
  interface Window {
    __SAMI_BUILD__: Readonly<BuildInfo>
  }
}

export function exposeBuildInfo(): void {
  window.__SAMI_BUILD__ = buildInfo
  console.info('[SAMI build]', buildInfo)
}
