<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { useAuthStore } from '@/stores/auth'
import { useApiError } from '@/composables/useApiError'
import { loginSchema } from '@/schemas/auth'

const { t } = useI18n()
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()
const loading = ref(false)

const { handleSubmit, defineField, errors } = useForm({
  validationSchema: toTypedSchema(loginSchema(t)),
  initialValues: { email: '', password: '' },
})
const [email, emailProps] = defineField('email')
const [password, passwordProps] = defineField('password')

const onSubmit = handleSubmit(async (values) => {
  loading.value = true
  clearError()
  try {
    await auth.login(values)
    const redirect = (route.query.redirect as string) ?? '/'
    await router.push(redirect)
  } catch (err) {
    setError(err)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <v-card elevation="4" rounded="lg">
    <v-card-title class="text-h5 pt-6 px-6">{{ t('auth.signInTitle') }}</v-card-title>
    <v-card-subtitle class="px-6">{{ t('auth.signInSubtitle') }}</v-card-subtitle>

    <v-card-text class="px-6">
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
        <v-text-field
          v-model="password"
          v-bind="passwordProps"
          :error-messages="errors.password"
          :label="t('auth.password')"
          type="password"
          autocomplete="current-password"
          prepend-inner-icon="mdi-lock-outline"
        />

        <div class="d-flex justify-end mb-2">
          <v-btn variant="text" size="small" color="primary" :to="{ name: 'forgot-password' }">
            {{ t('auth.forgotPasswordLink') }}
          </v-btn>
        </div>

        <v-btn type="submit" color="primary" block size="large" :loading="loading" class="mt-2">
          {{ t('auth.signInButton') }}
        </v-btn>
      </v-form>
    </v-card-text>

    <v-card-actions class="justify-center pb-6">
      <span class="text-body-2">{{ t('auth.noAccount') }}</span>
      <v-btn variant="text" :to="{ name: 'register' }" color="primary">{{ t('auth.createOne') }}</v-btn>
    </v-card-actions>
  </v-card>
</template>
