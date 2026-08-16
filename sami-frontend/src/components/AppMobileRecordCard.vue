<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'

const props = withDefaults(defineProps<{
  label: string
  actionsWidth?: number
}>(), { actionsWidth: 136 })

const emit = defineEmits<{ open: [] }>()
const { locale, t } = useI18n()
const expanded = ref(false)
const actionsOpen = ref(false)
const dragging = ref(false)
const startX = ref(0)
const offset = ref(0)
const moved = ref(false)
const rtl = computed(() => locale.value === 'fa')
const openOffset = computed(() => rtl.value ? props.actionsWidth : -props.actionsWidth)

function pointerDown(event: PointerEvent) {
  if (event.pointerType === 'mouse' && event.button !== 0) return
  startX.value = event.clientX
  offset.value = actionsOpen.value ? openOffset.value : 0
  moved.value = false
  dragging.value = true
  ;(event.currentTarget as HTMLElement).setPointerCapture?.(event.pointerId)
}

function pointerMove(event: PointerEvent) {
  if (!dragging.value) return
  const delta = event.clientX - startX.value
  if (Math.abs(delta) > 6) moved.value = true
  const target = (actionsOpen.value ? openOffset.value : 0) + delta
  offset.value = rtl.value
    ? Math.min(props.actionsWidth, Math.max(0, target))
    : Math.max(-props.actionsWidth, Math.min(0, target))
}

function pointerUp() {
  if (!dragging.value) return
  dragging.value = false
  actionsOpen.value = Math.abs(offset.value) >= props.actionsWidth * 0.38
  offset.value = actionsOpen.value ? openOffset.value : 0
}

function activate() {
  if (moved.value) return
  if (actionsOpen.value) {
    actionsOpen.value = false
    offset.value = 0
    return
  }
  expanded.value = !expanded.value
  emit('open')
}

function toggleActions() {
  actionsOpen.value = !actionsOpen.value
  offset.value = actionsOpen.value ? openOffset.value : 0
}
</script>

<template>
  <article class="mobile-record" :class="{ 'mobile-record--open': actionsOpen }">
    <div class="mobile-record__actions" :class="rtl ? 'mobile-record__actions--start' : 'mobile-record__actions--end'">
      <slot name="actions" :close="toggleActions" />
    </div>
    <v-card
      class="mobile-record__surface"
      rounded="xl"
      :style="{ transform: `translateX(${offset}px)` }"
      @pointerdown="pointerDown"
      @pointermove="pointerMove"
      @pointerup="pointerUp"
      @pointercancel="pointerUp"
      @click="activate"
    >
      <v-card-text>
        <div
          class="mobile-record__summary"
          tabindex="0"
          role="button"
          :aria-label="label"
          :aria-expanded="expanded"
          @click.stop="activate"
          @keydown.enter.prevent="activate"
          @keydown.space.prevent="activate"
        ><slot /></div>
        <v-expand-transition>
          <div v-if="expanded" class="mobile-record__details" @click.stop><slot name="details" /></div>
        </v-expand-transition>
        <div class="mobile-record__hint">
          <v-btn variant="text" size="small" :prepend-icon="expanded ? 'mdi-chevron-up' : 'mdi-chevron-down'" @click.stop="activate">
            {{ expanded ? t('common.hideDetails') : t('common.showDetails') }}
          </v-btn>
          <v-btn variant="text" size="small" icon="mdi-dots-horizontal" :aria-label="t('common.actions')" @click.stop="toggleActions" />
        </div>
      </v-card-text>
    </v-card>
  </article>
</template>

<style scoped>
.mobile-record { position: relative; overflow: hidden; border-radius: var(--app-radius-xl); background: rgb(var(--v-theme-surface-container)); }
.mobile-record__actions { position: absolute; inset-block: 0; width: 136px; display: flex; align-items: center; justify-content: center; gap: var(--app-space-1); padding: var(--app-space-2); }
.mobile-record__actions--start { inset-inline-start: 0; }
.mobile-record__actions--end { inset-inline-end: 0; }
.mobile-record__surface { position: relative; z-index: 1; touch-action: pan-y; user-select: none; transition: transform var(--app-dur) var(--app-ease-out), box-shadow var(--app-dur) var(--app-ease); will-change: transform; }
.mobile-record__summary:focus-visible { outline: 2px solid rgb(var(--v-theme-primary)); outline-offset: 2px; border-radius: var(--app-radius-sm); }
.mobile-record__summary { min-width: 0; }
.mobile-record__details { margin-top: var(--app-space-4); padding-top: var(--app-space-3); border-top: 1px solid var(--app-border); }
.mobile-record__hint { display: flex; align-items: center; justify-content: space-between; margin: var(--app-space-2) calc(var(--app-space-2) * -1) calc(var(--app-space-2) * -1); }
@media (prefers-reduced-motion: reduce) { .mobile-record__surface { transition: none; } }
</style>
