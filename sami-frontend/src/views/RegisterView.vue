<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { useAuthStore } from '@/stores/auth'
import { useApiError } from '@/composables/useApiError'
import { registerSchema } from '@/schemas/auth'
import AuthCard from '@/components/AuthCard.vue'

const { t } = useI18n()
const auth = useAuthStore()
const router = useRouter()
const { message: errorMessage, set: setError, clear: clearError } = useApiError()
const loading = ref(false)
const showPassword = ref(false)

const { handleSubmit, defineField, errors } = useForm({
  validationSchema: computed(() => toTypedSchema(registerSchema(t))),
  initialValues: { fullName: '', email: '', password: '' },
})
const [fullName, fullNameProps] = defineField('fullName')
const [email, emailProps] = defineField('email')
const [password, passwordProps] = defineField('password')

const onSubmit = handleSubmit(async (values) => {
  loading.value = true
  clearError()
  try {
    await auth.register(values)
    await router.push({ name: 'dashboard' })
  } catch (err) {
    setError(err)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <AuthCard :title="t('auth.registerTitle')" :subtitle="t('auth.registerSubtitle')">
      <v-alert v-if="errorMessage" type="error" variant="tonal" density="compact" class="mb-4">
        {{ errorMessage }}
      </v-alert>

      <v-form @submit.prevent="onSubmit">
        <v-text-field
          v-model="fullName"
          v-bind="fullNameProps"
          :error-messages="errors.fullName"
          :label="t('auth.fullName')"
          autocomplete="name"
          prepend-inner-icon="mdi-account-outline"
        />
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
          :type="showPassword ? 'text' : 'password'"
          autocomplete="new-password"
          prepend-inner-icon="mdi-lock-outline"
          :append-inner-icon="showPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
          @click:append-inner="showPassword = !showPassword"
        />

        <v-btn type="submit" color="primary" block size="large" :loading="loading" class="mt-2">
          {{ t('auth.registerButton') }}
        </v-btn>
      </v-form>
    <template #actions>
      <span class="text-body-2">{{ t('auth.alreadyHaveAccount') }}</span>
      <v-btn variant="text" :to="{ name: 'login' }" color="primary">{{ t('auth.signInButton') }}</v-btn>
    </template>
  </AuthCard>
</template>
