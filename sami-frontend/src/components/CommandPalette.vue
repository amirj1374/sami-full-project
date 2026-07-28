<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMenuStore } from '@/stores/menu'
import { useNavHistory } from '@/composables/useNavHistory'
import { useServerLabel } from '@/composables/useServerLabel'
import AppEmptyState from '@/components/AppEmptyState.vue'

/**
 * Command palette (⌘K / Ctrl-K): fuzzy-ish search across the permission-filtered
 * menu with full keyboard control (↑/↓ to move, Enter to open, Esc to close).
 * Recently-visited items surface first when the query is empty.
 */
const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()

const { t } = useI18n()
const router = useRouter()
const menu = useMenuStore()
const { recent } = useNavHistory()
const { moduleLabel } = useServerLabel()

const query = ref('')
const activeIndex = ref(0)
const inputRef = ref<{ focus?: () => void } | null>(null)

interface Command {
  code: string
  name: string
  icon: string
  path: string
  recent: boolean
}

const commands = computed<Command[]>(() => {
  const q = query.value.trim().toLowerCase()
  const base = menu.items.map((m) => ({
    code: m.code,
    name: moduleLabel(m.code, m.name),
    icon: m.icon || 'mdi-circle-small',
    path: m.path,
    recent: recent.value.includes(m.path),
  }))
  const filtered = q
    ? base.filter((c) => c.name.toLowerCase().includes(q) || c.code.toLowerCase().includes(q))
    : base.slice().sort((a, b) => Number(b.recent) - Number(a.recent))
  return filtered
})

watch(
  () => props.modelValue,
  async (open) => {
    if (open) {
      query.value = ''
      activeIndex.value = 0
      await nextTick()
      inputRef.value?.focus?.()
    }
  },
)

watch(commands, () => {
  activeIndex.value = 0
})

function close(): void {
  emit('update:modelValue', false)
}

function run(cmd: Command | undefined): void {
  if (!cmd) return
  close()
  if (router.currentRoute.value.path !== cmd.path) void router.push(cmd.path)
}

function onKeydown(e: KeyboardEvent): void {
  const list = commands.value
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    activeIndex.value = (activeIndex.value + 1) % Math.max(list.length, 1)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    activeIndex.value = (activeIndex.value - 1 + list.length) % Math.max(list.length, 1)
  } else if (e.key === 'Enter') {
    e.preventDefault()
    run(list[activeIndex.value])
  }
}
</script>

<template>
  <v-dialog
    :model-value="modelValue"
    max-width="560"
    transition="dialog-top-transition"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <v-card rounded="lg" class="cmd-card">
      <div class="d-flex align-center ga-2 px-4 py-2">
        <v-icon icon="mdi-magnify" class="text-medium-emphasis" />
        <input
          ref="inputRef"
          v-model="query"
          class="cmd-input flex-grow-1"
          :placeholder="t('shell.commandPlaceholder')"
          :aria-label="t('shell.commandPlaceholder')"
          @keydown="onKeydown"
        />
        <v-chip size="x-small" variant="tonal" label>Esc</v-chip>
      </div>
      <v-divider />

      <div class="cmd-list py-2">
        <template v-if="commands.length">
          <button
            v-for="(cmd, i) in commands"
            :key="cmd.code"
            type="button"
            class="cmd-item"
            :class="{ 'cmd-item--active': i === activeIndex }"
            @mouseenter="activeIndex = i"
            @click="run(cmd)"
          >
            <v-icon :icon="cmd.icon" size="20" class="cmd-item__icon" />
            <span class="cmd-item__name">{{ cmd.name }}</span>
            <v-icon
              v-if="cmd.recent && !query"
              icon="mdi-history"
              size="16"
              class="text-disabled ms-auto"
            />
            <v-icon
              v-else
              icon="mdi-arrow-right-thin"
              size="18"
              class="cmd-item__enter ms-auto"
            />
          </button>
        </template>
        <AppEmptyState
          v-else
          dense
          icon="mdi-magnify-close"
          :title="t('shell.commandEmpty')"
        />
      </div>

      <v-divider />
      <div class="d-flex align-center ga-4 px-4 py-2 text-caption text-medium-emphasis">
        <span><v-icon icon="mdi-arrow-up-down" size="14" /> {{ t('shell.navMove') }}</span>
        <span><v-icon icon="mdi-keyboard-return" size="14" /> {{ t('shell.navOpen') }}</span>
      </div>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.cmd-input {
  border: none;
  outline: none;
  background: transparent;
  font: inherit;
  color: rgb(var(--v-theme-on-surface));
  padding: 10px 0;
}
.cmd-list {
  max-height: 52vh;
  overflow-y: auto;
}
.cmd-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px 16px;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: start;
  color: rgb(var(--v-theme-on-surface));
  transition: background var(--app-dur-fast) var(--app-ease);
}
.cmd-item--active {
  background: rgba(var(--v-theme-primary), 0.09);
}
.cmd-item__icon {
  color: rgb(var(--v-theme-primary));
  flex: none;
}
.cmd-item__name {
  font-size: 0.925rem;
  font-weight: 500;
}
.cmd-item__enter {
  opacity: 0;
  transition: opacity var(--app-dur-fast) var(--app-ease);
}
.cmd-item--active .cmd-item__enter {
  opacity: 0.6;
}
</style>
