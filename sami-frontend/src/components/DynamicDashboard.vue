<script setup lang="ts">
import { ref } from 'vue'
import DashboardWidgetCard from '@/components/DashboardWidgetCard.vue'
import type { DashboardWidget, WidgetDataResponse } from '@/types/models'

/**
 * The single, generic dashboard renderer (no per-domain dashboard pages):
 * a 12-column CSS grid where each widget spans its configured width/height.
 * In edit mode widgets are draggable (HTML5 DnD reorder), resizable and
 * editable; the parent persists the resulting layout.
 */
const props = defineProps<{
  widgets: DashboardWidget[]
  data: Record<number, WidgetDataResponse>
  refreshing?: boolean
  editMode?: boolean
}>()

const emit = defineEmits<{
  reorder: [fromId: number, toId: number]
  resize: [widgetId: number, dw: number, dh: number]
  edit: [widget: DashboardWidget]
  remove: [widget: DashboardWidget]
}>()

const draggingId = ref<number | null>(null)

function onDragStart(widget: DashboardWidget, event: DragEvent): void {
  if (!props.editMode) return
  draggingId.value = widget.id
  event.dataTransfer?.setData('text/plain', String(widget.id))
}

function onDrop(target: DashboardWidget): void {
  if (!props.editMode || draggingId.value === null || draggingId.value === target.id) return
  emit('reorder', draggingId.value, target.id)
  draggingId.value = null
}

function span(cols: number): string {
  return `span ${Math.max(1, Math.min(12, cols))}`
}
</script>

<template>
  <div class="dashboard-grid">
    <div
      v-for="widget in widgets"
      :key="widget.id"
      class="dashboard-cell"
      :style="{ gridColumn: span(widget.width), gridRow: `span ${Math.max(1, widget.height)}` }"
      :draggable="editMode"
      @dragstart="onDragStart(widget, $event)"
      @dragover.prevent
      @drop.prevent="onDrop(widget)"
    >
      <DashboardWidgetCard
        :widget="widget"
        :data="data[widget.id]?.data"
        :error="data[widget.id]?.error"
        :loading="refreshing === true && !data[widget.id]"
        :edit-mode="editMode"
        @edit="emit('edit', widget)"
        @remove="emit('remove', widget)"
        @resize="(dw, dh) => emit('resize', widget.id, dw, dh)"
      />
    </div>
  </div>
</template>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  grid-auto-rows: 96px;
  gap: 12px;
}
.dashboard-cell {
  min-width: 0;
}
@media (max-width: 960px) {
  .dashboard-grid {
    grid-template-columns: repeat(1, 1fr);
  }
  .dashboard-cell {
    grid-column: span 1 !important;
  }
}
</style>
