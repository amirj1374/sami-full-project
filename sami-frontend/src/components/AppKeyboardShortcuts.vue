<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{ enabled: boolean }>()
const { t } = useI18n()

const TARGET_CLASS = 'app-shortcut-target'
const POSITIONED_CLASS = 'app-shortcut-target--positioned'
const BADGE_CLASS = 'app-shortcut-badge'
const ACTION_SELECTOR = [
  'button',
  'a[href]',
  '[role="button"]',
  '[role="menuitem"]',
  '[role="tab"]',
  '.v-list-item[tabindex]',
].join(', ')
const GROUP_KEYS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
const VALUE_KEYS = '1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ'

const activePrefix = ref('')
const targets = new Map<string, HTMLElement>()
let observer: MutationObserver | null = null
let refreshFrame: number | null = null
let prefixTimer: number | null = null

function shortcutCode(index: number): string | null {
  const group = Math.floor(index / VALUE_KEYS.length)
  if (group >= GROUP_KEYS.length) return null
  return `${GROUP_KEYS[group]}${VALUE_KEYS[index % VALUE_KEYS.length]}`
}

function physicalKey(event: KeyboardEvent): string | null {
  if (/^Key[A-Z]$/.test(event.code)) return event.code.slice(3)
  if (/^Digit[0-9]$/.test(event.code)) return event.code.slice(5)
  if (/^Numpad[0-9]$/.test(event.code)) return event.code.slice(6)
  return null
}

function isVisible(element: HTMLElement): boolean {
  if (!element.isConnected || element.closest('[inert], [aria-hidden="true"]')) return false
  const style = window.getComputedStyle(element)
  return style.display !== 'none' && style.visibility !== 'hidden' && element.getClientRects().length > 0
}

function isActionable(element: HTMLElement): boolean {
  if (!isVisible(element)) return false
  if (element.hasAttribute('data-sami-shortcut-skip')) return false
  if (element.matches(':disabled') || element.getAttribute('aria-disabled') === 'true') return false
  return !element.classList.contains('v-btn--disabled') && !element.classList.contains('v-btn--loading')
}

function clearBadges(): void {
  document.querySelectorAll<HTMLElement>(`.${TARGET_CLASS}, [data-sami-shortcut]`).forEach((element) => {
    element.classList.remove(TARGET_CLASS)
    element.classList.remove(POSITIONED_CLASS)
    delete element.dataset.samiShortcut
  })
  document.querySelectorAll<HTMLElement>(`.${BADGE_CLASS}`).forEach((badge) => badge.remove())
  targets.clear()
}

function activeScope(): ParentNode {
  const dialogs = Array.from(document.querySelectorAll<HTMLElement>('[role="dialog"]')).filter(isVisible)
  return dialogs.at(-1) ?? document
}

function refreshTargets(): void {
  refreshFrame = null
  clearBadges()
  if (!props.enabled) return

  const candidates = Array.from(new Set(activeScope().querySelectorAll<HTMLElement>(ACTION_SELECTOR)))
    .filter(isActionable)

  candidates.forEach((element, index) => {
    const code = shortcutCode(index)
    if (!code) return
    const badge = document.createElement('kbd')
    badge.className = BADGE_CLASS
    badge.ariaHidden = 'true'
    badge.textContent = code
    element.classList.add(TARGET_CLASS)
    if (window.getComputedStyle(element).position === 'static') element.classList.add(POSITIONED_CLASS)
    element.dataset.samiShortcut = code
    element.append(badge)
    targets.set(code, element)
  })
}

function scheduleRefresh(): void {
  if (refreshFrame != null) return
  refreshFrame = window.requestAnimationFrame(refreshTargets)
}

function clearPrefix(): void {
  activePrefix.value = ''
  if (prefixTimer != null) window.clearTimeout(prefixTimer)
  prefixTimer = null
}

function startPrefix(prefix: string): void {
  activePrefix.value = prefix
  if (prefixTimer != null) window.clearTimeout(prefixTimer)
  prefixTimer = window.setTimeout(clearPrefix, 1400)
}

function onKeydown(event: KeyboardEvent): void {
  if (!props.enabled || event.isComposing || event.repeat) return
  if (event.key === 'Escape' && activePrefix.value) {
    event.preventDefault()
    clearPrefix()
    return
  }

  const key = physicalKey(event)
  if (!key) return

  if (activePrefix.value) {
    event.preventDefault()
    event.stopPropagation()
    const target = targets.get(`${activePrefix.value}${key}`)
    clearPrefix()
    if (target && isActionable(target)) {
      target.focus({ preventScroll: true })
      target.click()
      scheduleRefresh()
    }
    return
  }

  if (!event.altKey || event.ctrlKey || event.metaKey) return
  if (![...targets.keys()].some((code) => code.startsWith(key))) return
  event.preventDefault()
  event.stopPropagation()
  startPrefix(key)
}

function mutationNeedsRefresh(mutations: MutationRecord[]): boolean {
  return mutations.some((mutation) => {
    if (mutation.type === 'attributes') return true
    const nodes = [...mutation.addedNodes, ...mutation.removedNodes]
    return nodes.some((node) => !(node instanceof HTMLElement && node.classList.contains(BADGE_CLASS)))
  })
}

onMounted(async () => {
  await nextTick()
  observer = new MutationObserver((mutations) => {
    if (mutationNeedsRefresh(mutations)) scheduleRefresh()
  })
  observer.observe(document.body, {
    subtree: true,
    childList: true,
    attributes: true,
    attributeFilter: ['disabled', 'aria-disabled', 'aria-hidden', 'hidden'],
  })
  window.addEventListener('keydown', onKeydown, true)
  window.addEventListener('resize', scheduleRefresh)
  scheduleRefresh()
})

watch(() => props.enabled, async () => {
  clearPrefix()
  await nextTick()
  scheduleRefresh()
})

onBeforeUnmount(() => {
  observer?.disconnect()
  observer = null
  window.removeEventListener('keydown', onKeydown, true)
  window.removeEventListener('resize', scheduleRefresh)
  if (refreshFrame != null) window.cancelAnimationFrame(refreshFrame)
  clearPrefix()
  clearBadges()
})
</script>

<template>
  <div
    v-if="activePrefix"
    class="app-shortcut-sequence"
    role="status"
    aria-live="polite"
  >
    {{ t('settings.keyboard.sequence', { prefix: activePrefix }) }}
  </div>
</template>

<style>
.app-shortcut-target--positioned {
  position: relative;
}
.app-shortcut-badge {
  position: absolute;
  z-index: 3;
  inset-inline-end: 2px;
  inset-block-end: 2px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 17px;
  height: 14px;
  padding-inline: 2px;
  border: 1px solid rgba(var(--v-theme-on-warning), 0.22);
  border-radius: 4px;
  background: rgb(var(--v-theme-warning));
  color: rgb(var(--v-theme-on-warning));
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.28);
  direction: ltr;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 8px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.04em;
  pointer-events: none;
}
.app-shortcut-sequence {
  position: fixed;
  z-index: 3000;
  inset-inline-start: 50%;
  inset-block-end: max(24px, env(safe-area-inset-bottom));
  transform: translateX(-50%);
  padding: 8px 14px;
  border: 1px solid rgba(var(--v-border-color), 0.18);
  border-radius: 999px;
  background: rgb(var(--v-theme-inverse-surface));
  color: rgb(var(--v-theme-inverse-on-surface));
  box-shadow: var(--app-shadow-lg);
  direction: ltr;
  font-size: 0.78rem;
  font-weight: 700;
  pointer-events: none;
}
@media (prefers-reduced-motion: no-preference) {
  .app-shortcut-badge { animation: app-shortcut-appear var(--app-dur-fast) var(--app-ease); }
}
@keyframes app-shortcut-appear {
  from { opacity: 0; transform: scale(0.8); }
  to { opacity: 1; transform: scale(1); }
}
</style>
