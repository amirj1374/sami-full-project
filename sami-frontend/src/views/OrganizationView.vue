<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import { organizationApi, type Company, type CompanyPayload } from '@/api/organization'
import { useApiError } from '@/composables/useApiError'
import { usePermission } from '@/composables/usePermission'
import AppEmptyState from '@/components/AppEmptyState.vue'
import AppLoadingState from '@/components/AppLoadingState.vue'
import AppMobileRecordCard from '@/components/AppMobileRecordCard.vue'
import AppPageHeader from '@/components/AppPageHeader.vue'

const { t } = useI18n()
const { smAndDown } = useDisplay()
const { can } = usePermission()
const apiError = useApiError()
const rows = ref<Company[]>([])
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const editing = ref<Company | null>(null)

const headers = computed(() => [
  { title: t('organization.fields.code'), key: 'code' },
  { title: t('organization.fields.name'), key: 'name' },
  { title: t('organization.fields.legalName'), key: 'legalName' },
  { title: t('organization.fields.city'), key: 'city' },
  { title: t('common.status'), key: 'active' },
  { title: t('common.actions'), key: 'actions', sortable: false },
])

const form = reactive<CompanyPayload>(emptyForm())
function emptyForm(): CompanyPayload {
  return { code: '', name: '', currencyCode: 'IRR', timezone: 'Asia/Tehran', locale: 'fa', fiscalYearStartMonth: 1, active: true, displayOrder: 0 }
}

async function load() {
  loading.value = true
  apiError.clear()
  try { rows.value = await organizationApi.list() } catch (error) { apiError.set(error) } finally { loading.value = false }
}

function open(company?: Company) {
  editing.value = company ?? null
  Object.assign(form, emptyForm(), company ? {
    code: company.code, name: company.name, legalName: company.legalName ?? '', taxNumber: company.taxNumber ?? '',
    registrationNumber: company.registrationNumber ?? '', currencyCode: company.currencyCode, timezone: company.timezone,
    locale: company.locale, fiscalYearStartMonth: company.fiscalYearStartMonth, email: company.email ?? '', phone: company.phone ?? '',
    website: company.website ?? '', addressLine1: company.addressLine1 ?? '', city: company.city ?? '', state: company.state ?? '',
    postalCode: company.postalCode ?? '', countryCode: company.countryCode ?? '', active: company.active,
    displayOrder: company.displayOrder, expectedVersion: company.version,
  } : {})
  apiError.clear()
  dialog.value = true
}

function payload(): CompanyPayload {
  const clean = (value?: string) => value?.trim() || undefined
  return {
    ...form, code: form.code.trim(), name: form.name.trim(), legalName: clean(form.legalName), taxNumber: clean(form.taxNumber),
    registrationNumber: clean(form.registrationNumber), email: clean(form.email), phone: clean(form.phone), website: clean(form.website),
    addressLine1: clean(form.addressLine1), city: clean(form.city), state: clean(form.state), postalCode: clean(form.postalCode), countryCode: clean(form.countryCode),
  }
}

async function save() {
  if (!form.code.trim() || !form.name.trim()) return
  saving.value = true
  apiError.clear()
  try {
    if (editing.value) await organizationApi.update(editing.value.id, payload())
    else await organizationApi.create(payload())
    dialog.value = false
    await load()
  } catch (error) { apiError.set(error) } finally { saving.value = false }
}

onMounted(load)
</script>

<template>
  <div>
    <AppPageHeader :title="t('organization.title')" :subtitle="t('organization.subtitle')" icon="mdi-office-building-outline">
      <template #actions><v-btn v-if="can('organization:create')" color="primary" prepend-icon="mdi-plus" @click="open()">{{ t('organization.addCompany') }}</v-btn></template>
    </AppPageHeader>
    <v-alert v-if="apiError.message" type="error" variant="tonal" class="mb-4" closable @click:close="apiError.clear()">{{ apiError.message }}</v-alert>
    <v-card class="app-data-surface" rounded="xl">
      <v-data-table v-if="!smAndDown" :headers="headers" :items="rows" :loading="loading" item-value="id">
        <template #[`item.name`]="{ item }"><strong>{{ item.name }}</strong><v-chip v-if="item.isDefault" size="x-small" class="ms-2" color="primary" variant="tonal">{{ t('organization.defaultCompany') }}</v-chip></template>
        <template #[`item.active`]="{ item }"><v-chip :color="item.active ? 'success' : undefined" size="small" variant="tonal">{{ item.active ? t('common.active') : t('common.inactive') }}</v-chip></template>
        <template #[`item.actions`]="{ item }"><v-btn v-if="can('organization:edit')" icon="mdi-pencil" variant="text" :aria-label="t('common.edit')" @click="open(item)" /></template>
      </v-data-table>
      <AppLoadingState v-else-if="loading" variant="table" :rows="4" :label="t('common.loading')" />
      <div v-else-if="rows.length" class="d-grid ga-3 pa-3">
        <AppMobileRecordCard v-for="company in rows" :key="company.id" :label="company.name" @open="can('organization:edit') && open(company)">
          <div class="d-flex align-start justify-space-between ga-3"><div class="min-width-0"><strong class="d-block text-truncate">{{ company.name }}</strong><span class="text-caption text-medium-emphasis">{{ company.code }}</span></div><v-chip :color="company.active ? 'success' : undefined" size="x-small" variant="tonal">{{ company.active ? t('common.active') : t('common.inactive') }}</v-chip></div>
          <template #details><div class="text-body-2">{{ company.legalName || '—' }}</div><div v-if="company.city" class="text-caption text-medium-emphasis mt-1">{{ company.city }}</div></template>
          <template #actions><v-btn v-if="can('organization:edit')" icon="mdi-pencil" variant="tonal" color="primary" :aria-label="t('common.edit')" @click="open(company)" /></template>
        </AppMobileRecordCard>
      </div>
      <AppEmptyState v-else-if="!loading" icon="mdi-office-building-plus-outline" :title="t('organization.empty.title')" :description="t('organization.empty.description')"><template #actions><v-btn v-if="can('organization:create')" color="primary" @click="open()">{{ t('organization.addCompany') }}</v-btn></template></AppEmptyState>
    </v-card>

    <v-dialog v-model="dialog" :fullscreen="smAndDown" max-width="820" persistent>
      <v-card rounded="xl"><v-toolbar color="transparent"><v-toolbar-title>{{ editing ? t('organization.editCompany') : t('organization.addCompany') }}</v-toolbar-title><v-btn icon="mdi-close" :aria-label="t('common.close')" @click="dialog = false" /></v-toolbar>
        <v-card-text class="px-4 px-sm-6"><v-form @submit.prevent="save"><v-row>
          <v-col cols="12" sm="4"><v-text-field v-model="form.code" :label="t('organization.fields.code')" maxlength="64" :disabled="saving" /></v-col><v-col cols="12" sm="8"><v-text-field v-model="form.name" :label="t('organization.fields.name')" maxlength="255" :disabled="saving" /></v-col>
          <v-col cols="12" sm="6"><v-text-field v-model="form.legalName" :label="t('organization.fields.legalName')" maxlength="255" /></v-col><v-col cols="12" sm="6"><v-text-field v-model="form.taxNumber" :label="t('organization.fields.taxNumber')" maxlength="64" /></v-col>
          <v-col cols="12" sm="4"><v-text-field v-model="form.registrationNumber" :label="t('organization.fields.registrationNumber')" maxlength="64" /></v-col><v-col cols="6" sm="4"><v-text-field v-model="form.currencyCode" :label="t('organization.fields.currencyCode')" maxlength="8" /></v-col><v-col cols="6" sm="4"><v-text-field v-model.number="form.fiscalYearStartMonth" type="number" min="1" max="12" :label="t('organization.fields.fiscalMonth')" /></v-col>
            <v-col cols="12" sm="6"><v-text-field v-model="form.timezone" :label="t('organization.fields.timezone')" maxlength="64" /></v-col><v-col cols="12" sm="6"><v-select v-model="form.locale" :items="[{ title: t('organization.languages.fa'), value: 'fa' }, { title: t('organization.languages.en'), value: 'en' }]" :label="t('organization.fields.locale')" /></v-col>
          <v-col cols="12" sm="6"><v-text-field v-model="form.email" :label="t('organization.fields.email')" type="email" maxlength="255" /></v-col><v-col cols="12" sm="6"><v-text-field v-model="form.phone" :label="t('organization.fields.phone')" type="tel" inputmode="numeric" maxlength="64" dir="ltr" /></v-col>
          <v-col cols="12"><v-text-field v-model="form.addressLine1" :label="t('organization.fields.address')" maxlength="255" /></v-col><v-col cols="12" sm="4"><v-text-field v-model="form.city" :label="t('organization.fields.city')" maxlength="128" /></v-col><v-col cols="12" sm="4"><v-text-field v-model="form.state" :label="t('organization.fields.state')" maxlength="128" /></v-col><v-col cols="12" sm="4"><v-text-field v-model="form.postalCode" :label="t('organization.fields.postalCode')" inputmode="numeric" pattern="[0-9]*" maxlength="32" dir="ltr" /></v-col>
          <v-col cols="12" sm="6"><v-switch v-model="form.active" color="success" :label="t('common.active')" :disabled="editing?.isDefault" /></v-col><v-col cols="12" sm="6"><v-text-field v-model.number="form.displayOrder" type="number" min="0" :label="t('organization.fields.displayOrder')" /></v-col>
        </v-row></v-form></v-card-text>
        <v-card-actions class="pa-4"><v-spacer/><v-btn variant="text" :disabled="saving" @click="dialog = false">{{ t('common.cancel') }}</v-btn><v-btn color="primary" :loading="saving" :disabled="!form.code?.trim() || !form.name?.trim()" @click="save">{{ t('common.save') }}</v-btn></v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
