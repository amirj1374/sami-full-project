<script setup lang="ts">
import { computed, useAttrs } from 'vue'
import { useI18n } from 'vue-i18n'

defineOptions({ inheritAttrs: false })
const props = defineProps<{ modelValue?: number | null }>()
const emit = defineEmits<{ 'update:modelValue': [value: number | null] }>()
const attrs = useAttrs()
const { locale, t } = useI18n()

function normalizeDigits(value: string): string {
  return value
    .replace(/[۰-۹]/g, (digit) => String('۰۱۲۳۴۵۶۷۸۹'.indexOf(digit)))
    .replace(/[٠-٩]/g, (digit) => String('٠١٢٣٤٥٦٧٨٩'.indexOf(digit)))
    .replace(/[٬,\s]/g, '')
    .replace(/٫/g, '.')
}

const displayValue = computed({
  get: () => props.modelValue == null ? '' : new Intl.NumberFormat(
    locale.value === 'fa' ? 'fa-IR' : 'en-US', { maximumFractionDigits: 2 },
  ).format(props.modelValue),
  set: (value: string | number | null) => {
    const normalized = normalizeDigits(String(value ?? ''))
    if (!normalized) return emit('update:modelValue', null)
    const parsed = Number(normalized)
    if (Number.isFinite(parsed)) emit('update:modelValue', parsed)
  },
})
</script>

<template>
  <v-text-field v-bind="attrs" v-model="displayValue" class="app-money-field"
    inputmode="decimal" type="text" :suffix="t('common.currency')">
    <template v-for="(_, slotName) in $slots" #[slotName]="slotProps">
      <slot :name="slotName" v-bind="slotProps ?? {}" />
    </template>
  </v-text-field>
</template>

<style scoped>
.app-money-field :deep(input) {
  direction: ltr;
  text-align: end;
  font-variant-numeric: tabular-nums;
}
</style>
