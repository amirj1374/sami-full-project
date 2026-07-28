<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { authApi } from '@/api/auth'
import { useApiError } from '@/composables/useApiError'
import { forgotPasswordSchema } from '@/schemas/auth'

const { t } = useI18n()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()
const loading = ref(false)
const sent = ref(false)

const { handleSubmit, defineField, errors } = useForm({
  validationSchema: toTypedSchema(forgotPasswordSchema(t)),
  initialValues: { email: '' },
})
const [email, emailProps] = defineField('email')

const onSubmit = handleSubmit(async (values) => {
  loading.value = true
  clearError()
  try {
    await authApi.forgotPassword(values)
    sent.value = true
  } catch (err) {
    setError(err)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <v-card elevation="4" rounded="lg">
    <v-card-title class="text-h5 pt-6 px-6">{{ t('auth.forgotTitle') }}</v-card-title>
    <v-card-subtitle class="px-6">{{ t('auth.forgotSubtitle') }}</v-card-subtitle>

    <v-card-text class="px-6">
      <v-alert v-if="sent" type="success" variant="tonal" density="compact" class="mb-4">
        {{ t('auth.forgotSent') }}
      </v-alert>

      <template v-else>
        <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
          {{ errorMessage }}
        </v-alert>

        <v-form @submit.prevent="onSubmit">
          <v-text-field
            v-model="email"
            v-bind="emailProps"
            :error-messages="errors.email"
            :label="t('auth.email')"
            type="email"
            autocomplete="email"
            prepend-inner-icon="mdi-email-outline"
          />

          <v-btn type="submit" color="primary" block size="large" :loading="loading" class="mt-2">
            {{ t('auth.sendResetLink') }}
          </v-btn>
        </v-form>
      </template>
    </v-card-text>

    <v-card-actions class="justify-center pb-6">
      <v-btn variant="text" :to="{ name: 'login' }" color="primary">{{ t('auth.backToSignIn') }}</v-btn>
    </v-card-actions>
  </v-card>
</template>
