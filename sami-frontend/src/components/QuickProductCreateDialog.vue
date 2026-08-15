<script setup lang="ts">
import ProductFormDialog from '@/components/ProductFormDialog.vue'
import type { Product } from '@/types/models'

defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; created: [product: Product] }>()
function created(product: Product) {
  emit('created', product)
  emit('update:modelValue', false)
}
</script>

<template>
  <ProductFormDialog :model-value="modelValue" :product="null" @update:model-value="$emit('update:modelValue', $event)" @saved="created" />
</template>
