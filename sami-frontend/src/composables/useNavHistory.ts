import { ref } from 'vue'

/**
 * Sidebar personalization: favourite (pinned) menu items and a short
 * recently-visited trail, both keyed by route path and persisted to
 * localStorage. Module-level state so the drawer and command palette share one
 * source of truth.
 */
const FAV_KEY = 'sami.nav.favorites'
const RECENT_KEY = 'sami.nav.recent'
const RECENT_MAX = 6

const favorites = ref<string[]>(read(FAV_KEY))
const recent = ref<string[]>(read(RECENT_KEY))

function read(key: string): string[] {
  try {
    const raw = localStorage.getItem(key)
    const parsed = raw ? (JSON.parse(raw) as unknown) : []
    return Array.isArray(parsed) ? parsed.filter((v): v is string => typeof v === 'string') : []
  } catch {
    return []
  }
}

function persist(key: string, value: string[]): void {
  localStorage.setItem(key, JSON.stringify(value))
}

export function useNavHistory() {
  function isFavorite(path: string): boolean {
    return favorites.value.includes(path)
  }

  function toggleFavorite(path: string): void {
    favorites.value = isFavorite(path)
      ? favorites.value.filter((p) => p !== path)
      : [...favorites.value, path]
    persist(FAV_KEY, favorites.value)
  }

  function recordVisit(path: string): void {
    if (!path || path === '/') return
    recent.value = [path, ...recent.value.filter((p) => p !== path)].slice(0, RECENT_MAX)
    persist(RECENT_KEY, recent.value)
  }

  return { favorites, recent, isFavorite, toggleFavorite, recordVisit }
}
