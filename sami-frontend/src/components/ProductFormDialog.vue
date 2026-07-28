<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { productSchema } from '@/schemas/product'
import { productsApi } from '@/api/products'
import { useApiError } from '@/composables/useApiError'
import AppFormSection from '@/components/AppFormSection.vue'
import type { Product } from '@/types/models'

const { t } = useI18n()

const props = defineProps<{
  modelValue: boolean
  /** When set, the dialog edits this product; otherwise it creates a new one. */
  product: Product | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const { message: errorMessage, set: setError, clear: clearError } = useApiError()
const loading = ref(false)
const confirmDiscard = ref(false)
const nameField = ref<{ focus?: () => void } | null>(null)

const { handleSubmit, defineField, errors, resetForm, meta } = useForm({
  validationSchema: toTypedSchema(productSchema(t)),
  initialValues: { name: '', sku: '', description: '', price: 0, stockQuantity: 0, active: true },
})

const [name, nameProps] = defineField('name')
const [sku, skuProps] = defineField('sku')
const [description, descriptionProps] = defineField('description')
const [price, priceProps] = defineField('price')
const [stockQuantity, stockProps] = defineField('stockQuantity')
const [active, activeProps] = defineField('active')

const isEdit = computed(() => !!props.product)

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    clearError()
    confirmDiscard.value = false
    const p = props.product
    resetForm({
      values: p
        ? {
            name: p.name,
            sku: p.sku,
            description: p.description ?? '',
            price: p.price,
            stockQuantity: p.stockQuantity,
            active: p.active,
          }
        : { name: '', sku: '', description: '', price: 0, stockQuantity: 0, active: true },
    })
    await nextTick()
    nameField.value?.focus?.()
  },
)

function close() {
  emit('update:modelValue', false)
}

/** Guard against accidental data loss: confirm before closing a dirty form. */
function requestClose() {
  if (meta.value.dirty && !loading.value) {
    confirmDiscard.value = true
    return
  }
  close()
}

const onSubmit = handleSubmit(async (values) => {
  loading.value = true
  clearError()
  try {
    if (props.product) {
      await productsApi.update(props.product.id, {
        name: values.name,
        description: values.description || undefined,
        price: values.price,
        stockQuantity: values.stockQuantity,
        active: values.active,
      })
    } else {
      await productsApi.create({
        name: values.name,
        sku: values.sku,
        description: values.description || undefined,
        price: values.price,
        stockQuantity: values.stockQuantity,
        active: values.active,
      })
    }
    emit('saved')
    close()
  } catch (err) {
    setError(err)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <v-dialog
    :model-value="modelValue"
    max-width="640"
    scrollable
    @update:model-value="(v) => (v ? emit('update:modelValue', true) : requestClose())"
  >
    <v-card rounded="lg">
      <!-- Header -->
      <div class="d-flex align-center ga-3 px-6 pt-5 pb-3">
        <v-avatar rounded="lg" size="42" color="primary" variant="tonal">
          <v-icon :icon="isEdit ? 'mdi-pencil' : 'mdi-package-variant-closed-plus'" />
        </v-avatar>
        <div class="flex-grow-1">
          <div class="text-h6 font-weight-bold">
            {{ isEdit ? t('products.editTitle') : t('products.newProduct') }}
          </div>
          <div class="text-caption text-medium-emphasis">{{ t('products.title') }}</div>
        </div>
        <v-btn icon="mdi-close" variant="text" size="small" :aria-label="t('common.close')" @click="requestClose" />
      </div>
      <v-divider />

      <v-card-text class="px-6 py-5 app-form-body">
        <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
          {{ errorMessage }}
        </v-alert>

        <v-form @submit.prevent="onSubmit">
          <AppFormSection
            icon="mdi-information-outline"
            :title="t('products.sections.general')"
            :description="t('products.sections.generalDesc')"
          >
            <v-row>
              <v-col cols="12" md="7">
                <v-text-field
                  ref="nameField"
                  v-model="name"
                  v-bind="nameProps"
                  :error-messages="errors.name"
                >
                  <template #label>{{ t('products.fieldName') }} <span class="app-req">*</span></template>
                </v-text-field>
              </v-col>
              <v-col cols="12" md="5">
                <v-text-field
                  v-model="sku"
                  v-bind="skuProps"
                  :error-messages="errors.sku"
                  :disabled="isEdit"
                  :hint="t('products.skuHint')"
                  persistent-hint
                >
                  <template #label>{{ t('products.fieldSku') }} <span class="app-req">*</span></template>
                </v-text-field>
              </v-col>
              <v-col cols="12">
                <v-textarea
                  v-model="description"
                  v-bind="descriptionProps"
                  :error-messages="errors.description"
                  :label="t('products.fieldDescription')"
                  rows="3"
                  auto-grow
                  counter="2000"
                />
              </v-col>
            </v-row>
          </AppFormSection>

          <AppFormSection
            icon="mdi-cash-multiple"
            :title="t('products.sections.pricing')"
            :description="t('products.sections.pricingDesc')"
          >
            <v-row>
              <v-col cols="12" sm="6">
                <v-text-field
                  v-model="price"
                  v-bind="priceProps"
                  :error-messages="errors.price"
                  type="number"
                  step="0.01"
                  min="0"
                  :suffix="t('common.currency')"
                >
                  <template #label>{{ t('products.fieldPrice') }} <span class="app-req">*</span></template>
                </v-text-field>
              </v-col>
              <v-col cols="12" sm="6">
                <v-text-field
                  v-model="stockQuantity"
                  v-bind="stockProps"
                  :error-messages="errors.stockQuantity"
                  type="number"
                  min="0"
                  prepend-inner-icon="mdi-warehouse"
                >
                  <template #label>{{ t('products.fieldStock') }} <span class="app-req">*</span></template>
                </v-text-field>
              </v-col>
              <v-col cols="12">
                <v-switch
                  v-model="active"
                  v-bind="activeProps"
                  :label="active ? t('products.active') : t('products.inactive')"
                  color="primary"
                  inset
                  hide-details
                />
                <div class="text-caption text-medium-emphasis">{{ t('products.fieldActive') }}</div>
              </v-col>
            </v-row>
          </AppFormSection>
        </v-form>
      </v-card-text>

      <v-divider />
      <v-card-actions class="px-6 py-3">
        <v-spacer />
        <v-btn variant="text" @click="requestClose">{{ t('common.cancel') }}</v-btn>
        <v-btn
          color="primary"
          variant="flat"
          :loading="loading"
          :prepend-icon="isEdit ? 'mdi-content-save' : 'mdi-plus'"
          @click="onSubmit"
        >
          {{ isEdit ? t('common.save') : t('common.create') }}
        </v-btn>
      </v-card-actions>
    </v-card>

    <!-- Unsaved-changes guard -->
    <v-dialog v-model="confirmDiscard" max-width="420">
      <v-card rounded="lg">
        <v-card-title class="text-subtitle-1 font-weight-bold pt-5 px-5">{{ t('form.discardTitle') }}</v-card-title>
        <v-card-text class="px-5 text-body-2 text-medium-emphasis">{{ t('form.discardBody') }}</v-card-text>
        <v-card-actions class="px-5 pb-4">
          <v-spacer />
          <v-btn variant="text" @click="confirmDiscard = false">{{ t('form.keepEditing') }}</v-btn>
          <v-btn color="error" variant="tonal" @click="confirmDiscard = false; close()">{{ t('form.discard') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-dialog>
</template>

<style scoped>
.app-form-body {
  max-height: 70vh;
  background: rgba(var(--v-theme-on-surface), 0.02);
}
.app-req {
  color: rgb(var(--v-theme-error));
  font-weight: 700;
}
</style>
