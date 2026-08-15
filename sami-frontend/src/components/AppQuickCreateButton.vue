<script setup lang="ts">
defineProps<{ label: string }>()
defineEmits<{ click: [] }>()
</script>

<template>
  <v-btn
    icon="mdi-plus"
    size="x-small"
    variant="tonal"
    color="primary"
    :title="label"
    :aria-label="label"
    @mousedown.stop
    @click.stop="$emit('click')"
  />
</template>
