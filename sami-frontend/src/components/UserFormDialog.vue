<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { usersApi, type RoleOption } from '@/api/users'
import { useApiError } from '@/composables/useApiError'
import { userFormSchema } from '@/schemas/user'
import type {
  ProfileFieldDefinition,
  UserDetail,
  UserProfilePayload,
  UserStatus,
} from '@/types/models'

/**
 * Create/edit user dialog with Account and Profile tabs. Custom profile fields
 * are rendered dynamically from the active field definitions — a field added in
 * configuration appears here without any code change. In edit mode the profile
 * image can be uploaded/replaced (stored behind the backend's storage port).
 */
const props = defineProps<{
  modelValue: boolean
  /** null = create mode */
  user: UserDetail | null
  roleOptions: RoleOption[]
  statusOptions: UserStatus[]
  fieldDefs: ProfileFieldDefinition[]
}>()

const emit = defineEmits<{ 'update:modelValue': [boolean]; saved: [] }>()

const { t } = useI18n()

const open = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const isEdit = computed(() => props.user !== null)
const tab = ref<'account' | 'profile'>('account')
const saving = ref(false)
const { message: formError, set: setFormError, clear: clearFormError } = useApiError()

/** Statuses selectable in the form: lifecycle states are set via dedicated actions. */
const selectableStatuses = computed(() =>
  props.statusOptions.filter((s) => !s.isArchivedState && !s.isDeletedState),
)

// --- Account fields (VeeValidate + Zod) ------------------------------------
const { handleSubmit, defineField, errors, resetForm, setFieldError } = useForm({
  validationSchema: toTypedSchema(userFormSchema(t)),
})

const [email, emailProps] = defineField('email')
const [fullName, fullNameProps] = defineField('fullName')
const [password, passwordProps] = defineField('password')
const [roleId, roleIdProps] = defineField('roleId')
const [statusId, statusIdProps] = defineField('statusId')

// --- Profile fields (plain refs; length limits enforced server-side too) ----
const profile = ref<UserProfilePayload>({})
const customValues = ref<Record<string, unknown>>({})

watch(open, (isOpen) => {
  if (!isOpen) return
  clearFormError()
  tab.value = 'account'
  const detail = props.user
  resetForm({
    values: detail
      ? {
          fullName: detail.user.fullName,
          roleId: detail.user.role.id,
          statusId: detail.user.status.id,
        }
      : { email: '', fullName: '', password: '' },
  })
  profile.value = detail?.profile
    ? {
        firstName: detail.profile.firstName ?? undefined,
        lastName: detail.profile.lastName ?? undefined,
        displayName: detail.profile.displayName ?? undefined,
        nationalCode: detail.profile.nationalCode ?? undefined,
        employeeCode: detail.profile.employeeCode ?? undefined,
        phoneNumber: detail.profile.phoneNumber ?? undefined,
        gender: detail.profile.gender ?? undefined,
        birthDate: detail.profile.birthDate ?? undefined,
        address: detail.profile.address ?? undefined,
        notes: detail.profile.notes ?? undefined,
      }
    : {}
  customValues.value = { ...(detail?.profile?.customFields ?? {}) }
  avatarFile.value = null
  void loadAvatar()
})

const submit = handleSubmit(async (values) => {
  // Create-mode presence checks (format is covered by the schema).
  if (!isEdit.value) {
    if (!values.email) {
      setFieldError('email', t('users.emailRequired'))
      return
    }
    if (!values.password) {
      setFieldError('password', t('users.passwordRequired'))
      return
    }
  }
  saving.value = true
  clearFormError()
  const profilePayload: UserProfilePayload = {
    ...profile.value,
    customFields: customValues.value,
  }
  try {
    if (props.user) {
      await usersApi.update(props.user.user.id, {
        fullName: values.fullName,
        roleId: values.roleId,
        statusId: values.statusId,
        profile: profilePayload,
        expectedVersion: props.user.user.version,
      })
    } else {
      await usersApi.create({
        email: values.email as string,
        fullName: values.fullName,
        password: values.password as string,
        roleId: values.roleId,
        statusId: values.statusId,
        profile: profilePayload,
      })
    }
    open.value = false
    emit('saved')
  } catch (err) {
    setFormError(err)
  } finally {
    saving.value = false
  }
})

// --- Avatar (edit mode only; uploads immediately) ---------------------------
// Fetched as an authenticated Blob (plain <img> URLs cannot carry the Bearer
// token) and shown via an object URL.
const avatarFile = ref<File | null>(null)
const avatarBusy = ref(false)
const avatarSrc = ref<string | null>(null)

function setAvatarSrc(blob: Blob | null) {
  if (avatarSrc.value) URL.revokeObjectURL(avatarSrc.value)
  avatarSrc.value = blob ? URL.createObjectURL(blob) : null
}

async function loadAvatar() {
  const detail = props.user
  if (!detail?.profile?.hasAvatar) {
    setAvatarSrc(null)
    return
  }
  setAvatarSrc(await usersApi.avatarBlob(detail.user.id))
}

async function uploadAvatar() {
  if (!props.user || !avatarFile.value) return
  avatarBusy.value = true
  clearFormError()
  try {
    await usersApi.uploadAvatar(props.user.user.id, avatarFile.value)
    avatarFile.value = null
    setAvatarSrc(await usersApi.avatarBlob(props.user.user.id))
    emit('saved')
  } catch (err) {
    setFormError(err)
  } finally {
    avatarBusy.value = false
  }
}

async function removeAvatar() {
  if (!props.user) return
  avatarBusy.value = true
  clearFormError()
  try {
    await usersApi.removeAvatar(props.user.user.id)
    setAvatarSrc(null)
    emit('saved')
  } catch (err) {
    setFormError(err)
  } finally {
    avatarBusy.value = false
  }
}

const genderItems = computed(() => [
  { title: t('users.genderMale'), value: 'male' },
  { title: t('users.genderFemale'), value: 'female' },
  { title: t('users.genderOther'), value: 'other' },
  { title: t('users.genderUnspecified'), value: 'unspecified' },
])
</script>

<template>
  <v-dialog v-model="open" max-width="720">
    <v-card rounded="lg">
      <v-card-title class="text-h6 pt-4 px-6">
        {{ isEdit ? t('users.editUser') : t('users.newUser') }}
      </v-card-title>

      <v-tabs v-model="tab" class="px-4">
        <v-tab value="account">{{ t('users.tabAccount') }}</v-tab>
        <v-tab value="profile">{{ t('users.tabProfile') }}</v-tab>
      </v-tabs>
      <v-divider />

      <v-card-text class="px-6" style="max-height: 60vh; overflow-y: auto">
        <v-alert v-if="formError" type="error" variant="tonal" density="compact" class="mb-4">
          {{ formError }}
        </v-alert>

        <v-form @submit.prevent="submit">
          <v-window v-model="tab">
            <v-window-item value="account">
              <v-text-field
                v-if="!isEdit"
                v-model="email"
                v-bind="emailProps"
                :error-messages="errors.email"
                :label="t('users.headerEmail')"
                type="email"
              />
              <v-text-field
                v-else
                :model-value="user?.user.email"
                :label="t('users.headerEmail')"
                disabled
                :hint="t('users.emailHint')"
                persistent-hint
                class="mb-2"
              />
              <v-text-field
                v-model="fullName"
                v-bind="fullNameProps"
                :error-messages="errors.fullName"
                :label="t('users.fullNameLabel')"
              />
              <v-text-field
                v-if="!isEdit"
                v-model="password"
                v-bind="passwordProps"
                :error-messages="errors.password"
                :label="t('users.passwordLabel')"
                type="password"
              />
              <v-select
                v-model="roleId"
                v-bind="roleIdProps"
                :error-messages="errors.roleId"
                :label="t('users.headerRole')"
                :items="roleOptions"
                item-title="name"
                item-value="id"
              />
              <v-select
                v-model="statusId"
                v-bind="statusIdProps"
                :error-messages="errors.statusId"
                :label="t('users.headerStatus')"
                :items="selectableStatuses"
                item-title="name"
                item-value="id"
                :hint="t('users.statusHint')"
                persistent-hint
              />
            </v-window-item>

            <v-window-item value="profile">
              <div v-if="isEdit" class="d-flex align-center mb-4">
                <v-avatar size="64" color="surface-variant" class="mr-4">
                  <v-img v-if="avatarSrc" :src="avatarSrc" cover />
                  <v-icon v-else icon="mdi-account" />
                </v-avatar>
                <v-file-input
                  v-model="avatarFile"
                  :label="t('users.profileImage')"
                  accept="image/jpeg,image/png,image/webp"
                  density="comfortable"
                  hide-details
                  class="mr-2"
                />
                <v-btn
                  size="small"
                  color="primary"
                  variant="tonal"
                  :disabled="!avatarFile"
                  :loading="avatarBusy"
                  class="mr-1"
                  @click="uploadAvatar"
                >
                  {{ t('users.upload') }}
                </v-btn>
                <v-btn
                  size="small"
                  variant="text"
                  :loading="avatarBusy"
                  @click="removeAvatar"
                >
                  {{ t('common.remove') }}
                </v-btn>
              </div>
              <p v-else class="text-body-2 text-medium-emphasis mb-4">
                {{ t('users.avatarCreateHint') }}
              </p>

              <v-row dense>
                <v-col cols="12" sm="6">
                  <v-text-field v-model="profile.firstName" :label="t('users.firstName')" maxlength="80" />
                </v-col>
                <v-col cols="12" sm="6">
                  <v-text-field v-model="profile.lastName" :label="t('users.lastName')" maxlength="80" />
                </v-col>
                <v-col cols="12" sm="6">
                  <v-text-field v-model="profile.displayName" :label="t('users.displayName')" maxlength="160" />
                </v-col>
                <v-col cols="12" sm="6">
                  <v-select v-model="profile.gender" :label="t('users.genderLabel')" :items="genderItems" clearable />
                </v-col>
                <v-col cols="12" sm="6">
                  <v-text-field v-model="profile.phoneNumber" :label="t('users.phoneNumber')" maxlength="32" />
                </v-col>
                <v-col cols="12" sm="6">
                  <v-text-field
                    v-model="profile.birthDate"
                    :label="t('users.birthDate')"
                    type="date"
                    clearable
                  />
                </v-col>
                <v-col cols="12" sm="6">
                  <v-text-field v-model="profile.employeeCode" :label="t('users.employeeCode')" maxlength="32" />
                </v-col>
                <v-col cols="12" sm="6">
                  <v-text-field v-model="profile.nationalCode" :label="t('users.nationalCode')" maxlength="32" />
                </v-col>
                <v-col cols="12">
                  <v-text-field v-model="profile.address" :label="t('users.address')" maxlength="500" />
                </v-col>
                <v-col cols="12">
                  <v-textarea v-model="profile.notes" :label="t('users.notes')" rows="2" maxlength="2000" />
                </v-col>
              </v-row>

              <template v-if="fieldDefs.length">
                <v-divider class="mb-4" />
                <p class="text-subtitle-2 mb-3">{{ t('users.additionalFields') }}</p>
                <v-row dense>
                  <v-col v-for="def in fieldDefs" :key="def.id" cols="12" sm="6">
                    <v-select
                      v-if="def.fieldType === 'SELECT'"
                      v-model="customValues[def.fieldKey] as string | undefined"
                      :label="def.label + (def.required ? ' *' : '')"
                      :items="def.options ?? []"
                      clearable
                    />
                    <v-switch
                      v-else-if="def.fieldType === 'BOOLEAN'"
                      v-model="customValues[def.fieldKey] as boolean | undefined"
                      :label="def.label"
                      color="primary"
                      inset
                    />
                    <v-text-field
                      v-else
                      v-model="customValues[def.fieldKey] as string | undefined"
                      :label="def.label + (def.required ? ' *' : '')"
                      :type="def.fieldType === 'NUMBER' ? 'number' : def.fieldType === 'DATE' ? 'date' : 'text'"
                    />
                  </v-col>
                </v-row>
              </template>
            </v-window-item>
          </v-window>
        </v-form>
      </v-card-text>

      <v-card-actions class="px-6 pb-4">
        <v-spacer />
        <v-btn variant="text" @click="open = false">{{ t('common.cancel') }}</v-btn>
        <v-btn color="primary" :loading="saving" @click="submit">{{ t('common.save') }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
