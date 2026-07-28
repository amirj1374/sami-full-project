import type { Directive, DirectiveBinding } from 'vue'
import { useAuthStore } from '@/stores/auth'

/**
 * `v-can="'users:create'"` — removes the element when the current user lacks
 * the permission. `v-can.disable` disables the element instead (sets
 * `disabled`/`aria-disabled` and blocks pointer events) so layout is kept.
 *
 * Registered globally as `can` in `main.ts`. Checks funnel through the auth
 * store's `can()`, so super admins always pass.
 */

function setDisabled(el: HTMLElement, disabled: boolean): void {
  if (disabled) {
    el.setAttribute('disabled', '')
    el.setAttribute('aria-disabled', 'true')
    el.style.pointerEvents = 'none'
    el.style.opacity = '0.5'
  } else {
    el.removeAttribute('disabled')
    el.removeAttribute('aria-disabled')
    el.style.pointerEvents = ''
    el.style.opacity = ''
  }
}

function apply(el: HTMLElement, binding: DirectiveBinding<string>): void {
  const allowed = useAuthStore().can(binding.value)
  if (binding.modifiers.disable) {
    setDisabled(el, !allowed)
  } else if (!allowed) {
    el.remove()
  }
}

export const permissionDirective: Directive<HTMLElement, string> = {
  mounted: apply,
  updated: apply,
}
