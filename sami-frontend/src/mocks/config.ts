/**
 * Mock Mode flag. When {@code VITE_ENABLE_MOCK_MODE=true} the app runs entirely
 * against MSW request handlers and seeded data — no backend required — which is
 * handy for UI development and demos. Off by default (and in production), so the
 * whole mock layer is tree-shaken out of the bundle.
 */
export const MOCK_MODE = import.meta.env.VITE_ENABLE_MOCK_MODE === 'true'
