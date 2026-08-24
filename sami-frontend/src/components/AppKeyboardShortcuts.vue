<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, watch } from 'vue'

const props = defineProps<{ enabled: boolean }>()

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
const SHORTCUT_KEYS = [
  ...'1234567890'.split('').map((label) => ({ code: `Digit${label}`, label })),
  ...'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('').map((label) => ({ code: `Key${label}`, label })),
  ...Array.from({ length: 12 }, (_, index) => ({ code: `F${index + 1}`, label: `F${index + 1}` })),
  { code: 'Minus', label: '-' },
  { code: 'Equal', label: '=' },
  { code: 'BracketLeft', label: '[' },
  { code: 'BracketRight', label: ']' },
  { code: 'Backslash', label: '\\' },
  { code: 'Semicolon', label: ';' },
  { code: 'Quote', label: "'" },
  { code: 'Comma', label: ',' },
  { code: 'Period', label: '.' },
  { code: 'Slash', label: '/' },
  { code: 'Backquote', label: '`' },
] as const

const targets = new Map<string, HTMLElement>()
let observer: MutationObserver | null = null
let refreshFrame: number | null = null

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
    element.removeAttribute('aria-keyshortcuts')
  })
  document.querySelectorAll<HTMLElement>(`.${BADGE_CLASS}`).forEach((badge) => badge.remove())
  targets.clear()
}

function activeScope(): ParentNode {
  const dialogs = Array.from(document.querySelectorAll<HTMLElement>('[role="dialog"]')).filter(isVisible)
  return dialogs.at(-1) ?? document
}

function orderedCandidates(): HTMLElement[] {
  const scope = activeScope()
  const regions: ParentNode[] = []
  if (scope === document) {
    for (const selector of ['main', 'header', 'nav']) {
      const region = document.querySelector(selector)
      if (region) regions.push(region)
    }
    regions.push(document)
  } else {
    regions.push(scope)
  }

  return Array.from(new Set(
    regions.flatMap((region) => Array.from(region.querySelectorAll<HTMLElement>(ACTION_SELECTOR))),
  )).filter(isActionable)
}

function refreshTargets(): void {
  refreshFrame = null
  clearBadges()
  if (!props.enabled) return

  const candidates = orderedCandidates()

  candidates.forEach((element, index) => {
    const shortcut = SHORTCUT_KEYS[index]
    if (!shortcut) return
    const badge = document.createElement('kbd')
    badge.className = BADGE_CLASS
    badge.ariaHidden = 'true'
    badge.textContent = `⇧${shortcut.label}`
    element.classList.add(TARGET_CLASS)
    if (window.getComputedStyle(element).position === 'static') element.classList.add(POSITIONED_CLASS)
    element.dataset.samiShortcut = `Shift+${shortcut.label}`
    element.setAttribute('aria-keyshortcuts', `Shift+${shortcut.label}`)
    element.append(badge)
    targets.set(shortcut.code, element)
  })
}

function scheduleRefresh(): void {
  if (refreshFrame != null) return
  refreshFrame = window.requestAnimationFrame(refreshTargets)
}

function isTypingTarget(target: EventTarget | null): boolean {
  return target instanceof HTMLElement
    && (target.matches('input, textarea, select') || target.isContentEditable)
}

function onKeydown(event: KeyboardEvent): void {
  if (!props.enabled || event.isComposing || event.repeat) return
  if (!event.shiftKey || event.altKey || event.ctrlKey || event.metaKey || isTypingTarget(event.target)) return
  const target = targets.get(event.code)
  if (!target || !isActionable(target)) return
  event.preventDefault()
  event.stopPropagation()
  target.focus({ preventScroll: true })
  target.click()
  scheduleRefresh()
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
  await nextTick()
  scheduleRefresh()
})

onBeforeUnmount(() => {
  observer?.disconnect()
  observer = null
  window.removeEventListener('keydown', onKeydown, true)
  window.removeEventListener('resize', scheduleRefresh)
  if (refreshFrame != null) window.cancelAnimationFrame(refreshFrame)
  clearBadges()
})
</script>

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
@media (prefers-reduced-motion: no-preference) {
  .app-shortcut-badge { animation: app-shortcut-appear var(--app-dur-fast) var(--app-ease); }
}
@keyframes app-shortcut-appear {
  from { opacity: 0; transform: scale(0.8); }
  to { opacity: 1; transform: scale(1); }
}
</style>
