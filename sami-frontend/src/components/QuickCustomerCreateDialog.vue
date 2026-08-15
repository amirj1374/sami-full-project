<script setup lang="ts">
import { ref, watch } from 'vue'
import CustomerFormDialog from '@/components/CustomerFormDialog.vue'
import { crmConfigApi } from '@/api/crmConfig'
import { useApiError } from '@/composables/useApiError'
import type { CustomerDetail, CustomerSource, CustomerStatus, CustomerTag, CustomerType, PreferenceDefinition } from '@/types/models'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; created: [customer: CustomerDetail] }>()
const types = ref<CustomerType[]>([])
const statuses = ref<CustomerStatus[]>([])
const sources = ref<CustomerSource[]>([])
const tags = ref<CustomerTag[]>([])
const preferenceDefs = ref<PreferenceDefinition[]>([])
const error = useApiError()

watch(() => props.modelValue, async (open) => {
  if (!open || types.value.length) return
  try {
    ;[types.value, statuses.value, sources.value, tags.value, preferenceDefs.value] = await Promise.all([
      crmConfigApi.types(), crmConfigApi.statuses(), crmConfigApi.sources(), crmConfigApi.tags(), crmConfigApi.preferenceDefinitions(),
    ])
  } catch (reason) {
    error.set(reason)
  }
})

function created(customer: CustomerDetail) {
  emit('created', customer)
  emit('update:modelValue', false)
}
</script>

<template>
  <v-alert v-if="modelValue && error.message.value" type="error" closable class="mb-3" @click:close="error.clear()">{{ error.message.value }}</v-alert>
  <CustomerFormDialog :model-value="modelValue" :customer="null" :types="types" :statuses="statuses" :sources="sources" :tags="tags" :preference-defs="preferenceDefs" @update:model-value="$emit('update:modelValue', $event)" @saved="created" />
</template>
