<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { authApi } from '@/api/auth'
import { useApiError } from '@/composables/useApiError'
import { resetPasswordSchema } from '@/schemas/auth'
import AuthCard from '@/components/AuthCard.vue'

const { t } = useI18n()
const route = useRoute()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()
const loading = ref(false)
const done = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)

/** The reset token arrives via the emailed link's `?token=` query parameter. */
const token = computed(() =>
  typeof route.query.token === 'string' && route.query.token !== '' ? route.query.token : null,
)

const { handleSubmit, defineField, errors } = useForm({
  validationSchema: computed(() => toTypedSchema(resetPasswordSchema(t))),
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
  <AuthCard :title="t('auth.resetTitle')" :subtitle="t('auth.resetSubtitle')">
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
            :label="t('auth.confirmNewPassword')"
            :type="showConfirmPassword ? 'text' : 'password'"
            autocomplete="new-password"
            prepend-inner-icon="mdi-lock-check-outline"
            :append-inner-icon="showConfirmPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
            @click:append-inner="showConfirmPassword = !showConfirmPassword"
          />

          <v-btn type="submit" color="primary" block size="large" :loading="loading" class="mt-2">
            {{ t('auth.resetButton') }}
          </v-btn>
        </v-form>
      </template>
    <template #actions>
      <v-btn v-if="!token" variant="text" :to="{ name: 'forgot-password' }" color="primary">
        {{ t('auth.requestNewLink') }}
      </v-btn>
      <v-btn v-else variant="text" :to="{ name: 'login' }" color="primary">
        {{ done ? t('auth.signInButton') : t('auth.backToSignIn') }}
      </v-btn>
    </template>
  </AuthCard>
</template>
