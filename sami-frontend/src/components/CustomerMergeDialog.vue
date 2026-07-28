<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { customersApi } from '@/api/customers'
import { useApiError } from '@/composables/useApiError'
import type { Customer } from '@/types/models'

/**
 * Merge a duplicate (source) into a surviving record (target). The target is
 * picked via server-side search. The summary spells out the rules: everything
 * moves, nothing is lost, the source stays resolvable.
 */
const props = defineProps<{
  modelValue: boolean
  /** The record that will be absorbed. */
  source: Customer | null
}>()

const emit = defineEmits<{ 'update:modelValue': [boolean]; merged: [] }>()

const open = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const { t } = useI18n()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()
const search = ref('')
const candidates = ref<Customer[]>([])
const target = ref<Customer | null>(null)
const searching = ref(false)
const merging = ref(false)

watch(open, (isOpen) => {
  if (!isOpen) return
  clearError()
  search.value = ''
  candidates.value = []
  target.value = null
})

let searchTimer: ReturnType<typeof setTimeout> | undefined
watch(search, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    if (!search.value || search.value.length < 2) {
      candidates.value = []
      return
    }
    searching.value = true
    try {
      const page = await customersApi.list({ search: search.value, size: 8 })
      candidates.value = page.content.filter((c) => c.id !== props.source?.id)
    } catch (err) {
      setError(err)
    } finally {
      searching.value = false
    }
  }, 300)
})

async function merge() {
  if (!props.source || !target.value) return
  merging.value = true
  clearError()
  try {
    await customersApi.merge(props.source.id, target.value.id)
    open.value = false
    emit('merged')
  } catch (err) {
    setError(err)
  } finally {
    merging.value = false
  }
}
</script>

<template>
  <v-dialog v-model="open" max-width="560">
    <v-card rounded="lg">
      <v-card-title class="text-h6 pt-4 px-6">{{ t('customers.merge.title') }}</v-card-title>

      <v-card-text class="px-6">
        <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
          {{ errorMessage }}
        </v-alert>

        <i18n-t keypath="customers.merge.body" tag="p" class="text-body-2 mb-4">
          <template #name><strong>{{ source?.displayName }}</strong></template>
          <template #code>{{ source?.customerCode }}</template>
        </i18n-t>

        <v-text-field
          v-model="search"
          :label="t('customers.merge.searchLabel')"
          prepend-inner-icon="mdi-magnify"
          :loading="searching"
          clearable
        />

        <v-list v-if="candidates.length" density="compact" class="border rounded mb-2">
          <v-list-item
            v-for="candidate in candidates"
            :key="candidate.id"
            :active="target?.id === candidate.id"
            @click="target = candidate"
          >
            <v-list-item-title>
              {{ candidate.displayName }}
              <span class="text-caption text-medium-emphasis">{{ candidate.customerCode }}</span>
            </v-list-item-title>
            <template #append>
              <v-icon v-if="target?.id === candidate.id" icon="mdi-check" color="primary" />
            </template>
          </v-list-item>
        </v-list>

        <v-alert v-if="target" type="info" variant="tonal" density="compact">
          <i18n-t keypath="customers.merge.survivingRecord" tag="span">
            <template #name><strong>{{ target.displayName }}</strong></template>
            <template #code>{{ target.customerCode }}</template>
          </i18n-t>
        </v-alert>
      </v-card-text>

      <v-card-actions class="px-6 pb-4">
        <v-spacer />
        <v-btn variant="text" @click="open = false">{{ t('common.cancel') }}</v-btn>
        <v-btn color="primary" :disabled="!target" :loading="merging" @click="merge">{{ t('customers.merge.confirm') }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
