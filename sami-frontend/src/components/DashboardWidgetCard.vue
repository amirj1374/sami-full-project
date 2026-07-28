<script setup lang="ts">
import { computed } from 'vue'
import { resolveWidgetComponent } from '@/components/widgets/registry'
import type { DashboardWidget, WidgetData } from '@/types/models'

/**
 * Widget chrome: title bar, threshold chip, error/loading states, and — in the
 * builder's edit mode — drag handle, resize and edit/remove controls. The body
 * is rendered by the type-specific component resolved from the widget registry,
 * so this card never changes when widget types are added.
 */
const props = defineProps<{
  widget: DashboardWidget
  data?: WidgetData
  error?: string | null
  loading?: boolean
  editMode?: boolean
}>()

const emit = defineEmits<{
  edit: []
  remove: []
  resize: [dw: number, dh: number]
}>()

const meta = computed(() => props.data?.meta ?? {})
const icon = computed(() => props.widget.config?.icon as string | undefined)
const color = computed(() => String(props.widget.config?.color ?? 'primary'))

const thresholdColor = computed(() => {
  const level = meta.value.thresholdLevel as string | undefined
  const thresholds = (meta.value.thresholds as { level: string; color: string }[] | undefined) ?? []
  return thresholds.find((t) => t.level === level)?.color || 'grey'
})

const renderer = computed(() => resolveWidgetComponent(props.widget.widgetTypeCode))
</script>

<template>
  <v-card rounded="lg" border flat class="h-100 d-flex flex-column" :class="{ 'widget-editing': editMode }">
    <v-card-item class="pb-1">
      <div class="d-flex align-center">
        <v-icon
          v-if="editMode"
          icon="mdi-drag"
          size="small"
          class="mr-1 cursor-move"
          data-drag-handle
        />
        <v-icon v-else-if="icon" :icon="icon" :color="color" size="small" class="mr-2" />
        <span class="text-subtitle-2 text-truncate">{{ widget.title }}</span>
        <v-spacer />
        <v-chip v-if="meta.thresholdLevel" size="x-small" :color="thresholdColor" variant="flat">
          {{ meta.thresholdLevel }}
        </v-chip>
        <template v-if="editMode">
          <v-btn icon="mdi-minus" size="x-small" variant="text" @click.stop="emit('resize', -1, 0)" />
          <v-btn icon="mdi-plus" size="x-small" variant="text" @click.stop="emit('resize', 1, 0)" />
          <v-btn icon="mdi-arrow-collapse-vertical" size="x-small" variant="text" @click.stop="emit('resize', 0, -1)" />
          <v-btn icon="mdi-arrow-expand-vertical" size="x-small" variant="text" @click.stop="emit('resize', 0, 1)" />
          <v-btn icon="mdi-pencil" size="x-small" variant="text" @click.stop="emit('edit')" />
          <v-btn icon="mdi-close" size="x-small" variant="text" color="error" @click.stop="emit('remove')" />
        </template>
      </div>
    </v-card-item>

    <v-card-text class="flex-grow-1 d-flex flex-column justify-center overflow-auto">
      <v-progress-linear v-if="loading" indeterminate />
      <v-alert v-else-if="error" type="warning" variant="tonal" density="compact">
        {{ error }}
      </v-alert>
      <component :is="renderer" v-else :widget="widget" :data="data" />
    </v-card-text>
  </v-card>
</template>

<style scoped>
.widget-editing {
  outline: 2px dashed rgba(var(--v-theme-primary), 0.5);
}
.cursor-move {
  cursor: move;
}
</style>
