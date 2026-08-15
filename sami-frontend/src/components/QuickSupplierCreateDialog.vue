<script setup lang="ts">
import { ref, watch } from 'vue'
import SupplierFormDialog from '@/components/SupplierFormDialog.vue'
import { supplierConfigApi } from '@/api/suppliers'
import { useApiError } from '@/composables/useApiError'
import type { SupCategory, SupPaymentTerm, SupStatus, SupTag, SupType, SupplierDetail } from '@/types/models'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; created: [supplier: SupplierDetail] }>()
const types = ref<SupType[]>([])
const statuses = ref<SupStatus[]>([])
const categories = ref<SupCategory[]>([])
const tags = ref<SupTag[]>([])
const paymentTerms = ref<SupPaymentTerm[]>([])
const error = useApiError()

watch(() => props.modelValue, async (open) => {
  if (!open || types.value.length) return
  try {
    ;[types.value, statuses.value, categories.value, tags.value, paymentTerms.value] = await Promise.all([
      supplierConfigApi.types(), supplierConfigApi.statuses(), supplierConfigApi.categories(), supplierConfigApi.tags(), supplierConfigApi.paymentTerms(),
    ])
  } catch (reason) {
    error.set(reason)
  }
})

function created(supplier: SupplierDetail) {
  emit('created', supplier)
  emit('update:modelValue', false)
}
</script>

<template>
  <v-alert v-if="modelValue && error.message.value" type="error" closable class="mb-3" @click:close="error.clear()">{{ error.message.value }}</v-alert>
  <SupplierFormDialog :model-value="modelValue" :supplier="null" :types="types" :statuses="statuses" :categories="categories" :tags="tags" :payment-terms="paymentTerms" @update:model-value="$emit('update:modelValue', $event)" @saved="created" />
</template>
