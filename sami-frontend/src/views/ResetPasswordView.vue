<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { authApi } from '@/api/auth'
import { useApiError } from '@/composables/useApiError'
import { resetPasswordSchema } from '@/schemas/auth'

const { t } = useI18n()
const route = useRoute()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()
const loading = ref(false)
const done = ref(false)

/** The reset token arrives via the emailed link's `?token=` query parameter. */
const token = computed(() =>
  typeof route.query.token === 'string' && route.query.token !== '' ? route.query.token : null,
)

const { handleSubmit, defineField, errors } = useForm({
  validationSchema: toTypedSchema(resetPasswordSchema(t)),
  initialValues: { newPassword: '', confirmPassword: '' },
})
const [newPassword, newPasswordProps] = defineField('newPassword')
const [confirmPassword, confirmPasswordProps] = defineField('confirmPassword')

const onSubmit = handleSubmit(async (values) => {
  if (!token.value) return
  loading.value = true
  clearError()
  try {
    await authApi.resetPassword({ token: token.value, newPassword: values.newPassword })
    done.value = true
  } catch (err) {
    setError(err)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <v-card elevation="4" rounded="lg">
    <v-card-title class="text-h5 pt-6 px-6">{{ t('auth.resetTitle') }}</v-card-title>
    <v-card-subtitle class="px-6">{{ t('auth.resetSubtitle') }}</v-card-subtitle>

    <v-card-text class="px-6">
      <v-alert v-if="!token" type="warning" variant="tonal" density="compact" class="mb-4">
        {{ t('auth.resetInvalidLink') }}
      </v-alert>

      <v-alert v-else-if="done" type="success" variant="tonal" density="compact" class="mb-4">
        {{ t('auth.resetDone') }}
      </v-alert>

      <template v-else>
        <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
          {{ errorMessage }}
        </v-alert>

        <v-form @submit.prevent="onSubmit">
          <v-text-field
            v-model="newPassword"
            v-bind="newPasswordProps"
            :error-messages="errors.newPassword"
            :label="t('auth.newPassword')"
            type="password"
            autocomplete="new-password"
            prepend-inner-icon="mdi-lock-plus-outline"
          />
          <v-text-field
            v-model="confirmPassword"
            v-bind="confirmPasswordProps"
            :error-messages="errors.confirmPassword"
            :label="t('auth.confirmNewPassword')"
            type="password"
            autocomplete="new-password"
            prepend-inner-icon="mdi-lock-check-outline"
          />

          <v-btn type="submit" color="primary" block size="large" :loading="loading" class="mt-2">
            {{ t('auth.resetButton') }}
          </v-btn>
        </v-form>
      </template>
    </v-card-text>

    <v-card-actions class="justify-center pb-6">
      <v-btn v-if="!token" variant="text" :to="{ name: 'forgot-password' }" color="primary">
        {{ t('auth.requestNewLink') }}
      </v-btn>
      <v-btn v-else variant="text" :to="{ name: 'login' }" color="primary">
        {{ done ? t('auth.signInButton') : t('auth.backToSignIn') }}
      </v-btn>
    </v-card-actions>
  </v-card>
</template>
