<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { changePasswordSchema } from '@/schemas/auth'
import { authApi } from '@/api/auth'
import { useApiError } from '@/composables/useApiError'
import { useDisplay } from 'vuetify'
import { useNotifications } from '@/composables/useNotifications'

const { t } = useI18n()
const { xs } = useDisplay()
const notifications = useNotifications()

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const { message: errorMessage, set: setError, clear: clearError } = useApiError()
const loading = ref(false)
const showCurrentPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)

const { handleSubmit, defineField, errors, resetForm } = useForm({
  validationSchema: computed(() => toTypedSchema(changePasswordSchema(t))),
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
    notifications.success(t('changePassword.success'))
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
    :fullscreen="xs"
    :transition="xs ? 'dialog-bottom-transition' : 'dialog-transition'"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <v-card :rounded="xs ? 0 : 'xl'" class="change-password-card">
      <v-card-title class="d-flex align-center text-h6 pt-5 px-5 px-sm-6">
        <span class="change-password-icon me-3"><v-icon icon="mdi-shield-key-outline" /></span>
        {{ t('changePassword.title') }}
        <v-spacer />
        <v-btn icon="mdi-close" variant="text" :aria-label="t('common.cancel')" @click="close" />
      </v-card-title>

      <v-card-text class="px-5 px-sm-6">
        <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
          {{ errorMessage }}
        </v-alert>

        <v-form @submit.prevent="onSubmit">
          <v-text-field
            v-model="currentPassword"
            v-bind="currentPasswordProps"
            :error-messages="errors.currentPassword"
            :label="t('changePassword.current')"
            :type="showCurrentPassword ? 'text' : 'password'"
            autocomplete="current-password"
            prepend-inner-icon="mdi-lock-outline"
            :append-inner-icon="showCurrentPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
            @click:append-inner="showCurrentPassword = !showCurrentPassword"
          />
          <v-text-field
            v-model="newPassword"
            v-bind="newPasswordProps"
            :error-messages="errors.newPassword"
            :label="t('changePassword.new')"
            :type="showNewPassword ? 'text' : 'password'"
            autocomplete="new-password"
            prepend-inner-icon="mdi-lock-plus-outline"
            :append-inner-icon="showNewPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
            @click:append-inner="showNewPassword = !showNewPassword"
          />
          <v-text-field
            v-model="confirmPassword"
            v-bind="confirmPasswordProps"
            :error-messages="errors.confirmPassword"
            :label="t('changePassword.confirm')"
            :type="showConfirmPassword ? 'text' : 'password'"
            autocomplete="new-password"
            prepend-inner-icon="mdi-lock-check-outline"
            :append-inner-icon="showConfirmPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
            @click:append-inner="showConfirmPassword = !showConfirmPassword"
          />
        </v-form>
      </v-card-text>

      <v-card-actions class="px-5 px-sm-6 pb-5 change-password-actions">
        <v-spacer />
        <v-btn variant="text" @click="close">{{ t('common.cancel') }}</v-btn>
        <v-btn color="primary" :loading="loading" @click="onSubmit">{{ t('changePassword.submit') }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.change-password-card {
  background: rgb(var(--v-theme-surface));
}
.change-password-icon {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--app-radius);
  color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.1);
}
@media (max-width: 599px) {
  .change-password-card {
    padding-top: max(0px, env(safe-area-inset-top));
  }
  .change-password-actions {
    padding-bottom: max(20px, env(safe-area-inset-bottom)) !important;
  }
  .change-password-actions .v-btn {
    flex: 1;
  }
}
</style>
