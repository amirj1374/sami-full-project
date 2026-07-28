<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { changePasswordSchema } from '@/schemas/auth'
import { authApi } from '@/api/auth'
import { useApiError } from '@/composables/useApiError'

const { t } = useI18n()

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const { message: errorMessage, set: setError, clear: clearError } = useApiError()
const loading = ref(false)
const snackbar = ref(false)

const { handleSubmit, defineField, errors, resetForm } = useForm({
  validationSchema: toTypedSchema(changePasswordSchema(t)),
  initialValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
})

const [currentPassword, currentPasswordProps] = defineField('currentPassword')
const [newPassword, newPasswordProps] = defineField('newPassword')
const [confirmPassword, confirmPasswordProps] = defineField('confirmPassword')

// Start from a clean form whenever the dialog opens.
watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    clearError()
    resetForm({ values: { currentPassword: '', newPassword: '', confirmPassword: '' } })
  },
)

function close() {
  emit('update:modelValue', false)
}

const onSubmit = handleSubmit(async (values) => {
  loading.value = true
  clearError()
  try {
    await authApi.changePassword({
      currentPassword: values.currentPassword,
      newPassword: values.newPassword,
    })
    snackbar.value = true
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
    max-width="480"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <v-card rounded="lg">
      <v-card-title class="text-h6 pt-4 px-6">{{ t('changePassword.title') }}</v-card-title>

      <v-card-text class="px-6">
        <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
          {{ errorMessage }}
        </v-alert>

        <v-form @submit.prevent="onSubmit">
          <v-text-field
            v-model="currentPassword"
            v-bind="currentPasswordProps"
            :error-messages="errors.currentPassword"
            :label="t('changePassword.current')"
            type="password"
            autocomplete="current-password"
            prepend-inner-icon="mdi-lock-outline"
          />
          <v-text-field
            v-model="newPassword"
            v-bind="newPasswordProps"
            :error-messages="errors.newPassword"
            :label="t('changePassword.new')"
            type="password"
            autocomplete="new-password"
            prepend-inner-icon="mdi-lock-plus-outline"
          />
          <v-text-field
            v-model="confirmPassword"
            v-bind="confirmPasswordProps"
            :error-messages="errors.confirmPassword"
            :label="t('changePassword.confirm')"
            type="password"
            autocomplete="new-password"
            prepend-inner-icon="mdi-lock-check-outline"
          />
        </v-form>
      </v-card-text>

      <v-card-actions class="px-6 pb-4">
        <v-spacer />
        <v-btn variant="text" @click="close">{{ t('common.cancel') }}</v-btn>
        <v-btn color="primary" :loading="loading" @click="onSubmit">{{ t('changePassword.submit') }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>

  <v-snackbar v-model="snackbar" color="success" timeout="4000">
    {{ t('changePassword.success') }}
  </v-snackbar>
</template>
